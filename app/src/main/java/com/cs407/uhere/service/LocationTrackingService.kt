package com.cs407.uhere.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.cs407.uhere.MainActivity
import com.cs407.uhere.R
import com.cs407.uhere.data.LocationCategory
import com.cs407.uhere.data.LocationSession
import com.cs407.uhere.data.Place
import com.cs407.uhere.data.UHereDatabase
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LocationTrackingService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var currentSessionId: Long? = null
    private var currentCategory: LocationCategory? = null
    private var sessionStartTime: Long = 0
    private var userId: Int = -1

    // Cache places to avoid DB query on every location update
    private var cachedPlaces: List<Place> = emptyList()
    private var lastCacheUpdate: Long = 0
    private val CACHE_DURATION = 60000L // 1 minute

    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_USER_ID = "user_id"

        fun start(context: Context, userId: Int) {
            android.util.Log.d("LocationService", "Starting service for user $userId")
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            android.util.Log.d("LocationService", "Stopping service")
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("LocationService", "Service onCreate")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userId = intent?.getIntExtra(EXTRA_USER_ID, -1) ?: -1
        android.util.Log.d("LocationService", "onStartCommand - userId: $userId")

        if (userId == -1) {
            android.util.Log.e("LocationService", "Invalid userId, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())

        // Pre-load places cache before starting location updates
        serviceScope.launch {
            updatePlacesCache()
            withContext(Dispatchers.Main) {
                startLocationUpdates()
            }
        }

        return START_STICKY
    }

    private fun startLocationUpdates() {
        android.util.Log.d("LocationService", "Starting location updates")
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30000L  // 30 seconds
        ).apply {
            setMinUpdateIntervalMillis(15000L)  // At most every 15 seconds
            setMinUpdateDistanceMeters(25f)  // Only update if moved 25 meters
            setMaxUpdateDelayMillis(60000L)  // Batch updates
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // CRITICAL FIX: Immediately offload to background thread
                    // Don't do ANY work on the main thread
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            handleLocationUpdate(location)
                        } catch (e: Exception) {
                            android.util.Log.e("LocationService", "Error in location update", e)
                        }
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            android.util.Log.d("LocationService", "Location updates requested successfully")
        } catch (e: SecurityException) {
            android.util.Log.e("LocationService", "Security exception: ${e.message}")
            e.printStackTrace()
            stopSelf()
        }
    }

    private suspend fun updatePlacesCache() {
        val database = UHereDatabase.getDatabase(applicationContext)
        cachedPlaces = database.placeDao().getUserPlaces(userId).first()
        lastCacheUpdate = System.currentTimeMillis()
        android.util.Log.d("LocationService", "Updated places cache: ${cachedPlaces.size} places")
    }

    // This now runs entirely on IO dispatcher
    private suspend fun handleLocationUpdate(location: Location) {
        android.util.Log.d("LocationService", "=== LOCATION UPDATE ===")
        android.util.Log.d("LocationService", "Current location: ${location.latitude}, ${location.longitude}")
        android.util.Log.d("LocationService", "Accuracy: ${location.accuracy}m")
        android.util.Log.d("LocationService", "UserId: $userId")

        // Update cache if needed
        val now = System.currentTimeMillis()
        if (now - lastCacheUpdate > CACHE_DURATION) {
            updatePlacesCache()
        }

        val database = UHereDatabase.getDatabase(applicationContext)
        val locationDao = database.locationDao()

        // Use cached places instead of querying DB every time
        val places = cachedPlaces
        android.util.Log.d("LocationService", "Checking ${places.size} places (from cache)")

        if (places.isEmpty()) {
            android.util.Log.w("LocationService", "⚠️ No places found for this user!")
        }

        // Find which place we're currently in (if any)
        val currentPlace = places.firstOrNull { place ->
            val distance = calculateDistance(
                location.latitude, location.longitude,
                place.latitude, place.longitude
            )
            android.util.Log.d("LocationService", "Distance to '${place.name}': ${distance.toInt()}m (needs < ${place.radius.toInt()}m)")
            distance <= place.radius
        }

        if (currentPlace != null) {
            android.util.Log.d("LocationService", "✓ INSIDE: ${currentPlace.name} (${currentPlace.category})")
            if (currentCategory != currentPlace.category) {
                android.util.Log.d("LocationService", "Category changed from $currentCategory to ${currentPlace.category}")
                endCurrentSession(locationDao)
                startNewSession(currentPlace.category, locationDao)
            } else {
                android.util.Log.d("LocationService", "Already tracking ${currentPlace.category}")
            }
        } else {
            android.util.Log.d("LocationService", "✗ NOT at any place")
            if (currentSessionId != null) {
                android.util.Log.d("LocationService", "Ending current session")
                endCurrentSession(locationDao)
            }
        }
    }

    private suspend fun startNewSession(category: LocationCategory, locationDao: com.cs407.uhere.data.LocationDao) {
        val now = System.currentTimeMillis()
        sessionStartTime = now
        currentCategory = category

        val session = LocationSession(
            userId = userId,
            locationCategory = category,
            startTime = now,
            endTime = null,
            durationMinutes = 0
        )

        currentSessionId = locationDao.insertSession(session)
        android.util.Log.d("LocationService", "✅ STARTED new session: ID=$currentSessionId, category=$category, time=$now")
    }

    private suspend fun endCurrentSession(locationDao: com.cs407.uhere.data.LocationDao) {
        currentSessionId?.let { sessionId ->
            val now = System.currentTimeMillis()
            val duration = ((now - sessionStartTime) / 60000).toInt() // minutes

            android.util.Log.d("LocationService", "⏹️ ENDING session: ID=$sessionId, duration=$duration minutes")

            val existingSession = locationDao.getSessionById(sessionId.toInt())

            existingSession?.let {
                val updatedSession = it.copy(
                    endTime = now,
                    durationMinutes = duration
                )

                locationDao.updateSession(updatedSession)
                android.util.Log.d("LocationService", "✅ Session saved: $updatedSession")
            } ?: run {
                android.util.Log.e("LocationService", "❌ Could not find session $sessionId to update")
            }
        } ?: run {
            android.util.Log.d("LocationService", "No active session to end")
        }

        currentSessionId = null
        currentCategory = null
        sessionStartTime = 0
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun createNotification(): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UHere Location Tracking")
            .setContentText("Tracking your location for goals")
            .setSmallIcon(R.drawable.baseline_alarm_24)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("LocationService", "Service onDestroy")
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // End any active session before stopping
        serviceScope.launch {
            val locationDao = UHereDatabase.getDatabase(applicationContext).locationDao()
            endCurrentSession(locationDao)
        }.invokeOnCompletion {
            serviceScope.cancel()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}