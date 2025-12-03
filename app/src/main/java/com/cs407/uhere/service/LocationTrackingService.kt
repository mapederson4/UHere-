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
        const val CHECKIN_CHANNEL_ID = "checkin_notification_channel"
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
        // Don't create channels here - create them when needed
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userId = intent?.getIntExtra(EXTRA_USER_ID, -1) ?: -1
        android.util.Log.d("LocationService", "onStartCommand - userId: $userId")

        if (userId == -1) {
            android.util.Log.e("LocationService", "Invalid userId, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        // Create channels before starting foreground
        createNotificationChannels()
        startForeground(NOTIFICATION_ID, createForegroundNotification())

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
            setMinUpdateIntervalMillis(15000L)
            setMinUpdateDistanceMeters(25f)
            setMaxUpdateDelayMillis(60000L)
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
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

    private suspend fun handleLocationUpdate(location: Location) {
        android.util.Log.d("LocationService", "=== LOCATION UPDATE ===")
        android.util.Log.d("LocationService", "Current location: ${location.latitude}, ${location.longitude}")
        android.util.Log.d("LocationService", "Accuracy: ${location.accuracy}m")

        // Update cache if needed
        val now = System.currentTimeMillis()
        if (now - lastCacheUpdate > CACHE_DURATION) {
            updatePlacesCache()
        }

        val database = UHereDatabase.getDatabase(applicationContext)
        val locationDao = database.locationDao()

        val places = cachedPlaces
        android.util.Log.d("LocationService", "Checking ${places.size} places")

        if (places.isEmpty()) {
            android.util.Log.w("LocationService", "No places found for this user!")
            return
        }

        val currentPlace = places.firstOrNull { place ->
            val distance = calculateDistance(
                location.latitude, location.longitude,
                place.latitude, place.longitude
            )
            android.util.Log.d("LocationService", "Distance to '${place.name}': ${distance.toInt()}m (radius: ${place.radius.toInt()}m)")
            distance <= place.radius
        }

        if (currentPlace != null) {
            android.util.Log.d("LocationService", "INSIDE: ${currentPlace.name} (${currentPlace.category})")

            if (currentCategory != currentPlace.category) {
                android.util.Log.d("LocationService", "Category changed from $currentCategory to ${currentPlace.category}")
                endCurrentSession(locationDao)
                startNewSession(currentPlace.category, locationDao)

                // Send notification - must be on main thread
                withContext(Dispatchers.Main) {
                    sendCheckInNotification(currentPlace.category)
                }
            }
        } else {
            android.util.Log.d("LocationService", "NOT at any place")
            if (currentSessionId != null) {
                endCurrentSession(locationDao)
            }
        }
    }

    private fun sendCheckInNotification(category: LocationCategory) {
        try {
            android.util.Log.d("LocationService", "Sending check-in notification for $category")

            // Create channel every time like the working app
            createNotificationChannels()

            val categoryName = when (category) {
                LocationCategory.LIBRARY -> "Library"
                LocationCategory.GYM -> "Gym"
                LocationCategory.BAR -> "Bar"
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Build notification exactly like the working app
            val builder = NotificationCompat.Builder(this, CHECKIN_CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_alarm_24)
                .setContentTitle("Checked In!")
                .setContentText("You've arrived at the $categoryName")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Use category ordinal as ID like you suggested
            val notificationId = 1000 + category.ordinal
            notificationManager.notify(notificationId, builder.build())

            android.util.Log.d("LocationService", "Notification posted with ID: $notificationId")

        } catch (e: Exception) {
            android.util.Log.e("LocationService", "Error sending notification", e)
            e.printStackTrace()
        }
    }

    private fun createNotificationChannels() {
        // Only create channel on API 26+ (Android O and above) - just like working app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Service channel
            val serviceChannelName = "Location Tracking Service"
            val serviceChannelDescription = "Shows that location tracking is active"
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                serviceChannelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = serviceChannelDescription
            }
            notificationManager.createNotificationChannel(serviceChannel)

            // Check-in channel - exactly like working app
            val checkinChannelName = "Check-in Alerts"
            val checkinChannelDescription = "Notifications when you arrive at a location"
            val checkinChannel = NotificationChannel(
                CHECKIN_CHANNEL_ID,
                checkinChannelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = checkinChannelDescription
            }
            notificationManager.createNotificationChannel(checkinChannel)

            android.util.Log.d("LocationService", "✓ Notification channels created")
        }
    }

    private fun createForegroundNotification(): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UHere Tracking")
            .setContentText("Tracking your location for goals")
            .setSmallIcon(R.drawable.baseline_alarm_24)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private suspend fun updatePlacesCache() {
        val database = UHereDatabase.getDatabase(applicationContext)
        cachedPlaces = database.placeDao().getUserPlaces(userId).first()
        lastCacheUpdate = System.currentTimeMillis()
        android.util.Log.d("LocationService", "Updated places cache: ${cachedPlaces.size} places")
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
        android.util.Log.d("LocationService", "STARTED session: ID=$currentSessionId, category=$category")
    }

    private suspend fun endCurrentSession(locationDao: com.cs407.uhere.data.LocationDao) {
        currentSessionId?.let { sessionId ->
            val now = System.currentTimeMillis()
            val duration = ((now - sessionStartTime) / 60000).toInt()

            android.util.Log.d("LocationService", "ENDING session: ID=$sessionId, duration=$duration min")

            val existingSession = locationDao.getSessionById(sessionId.toInt())
            existingSession?.let {
                val updatedSession = it.copy(
                    endTime = now,
                    durationMinutes = duration
                )
                locationDao.updateSession(updatedSession)
                android.util.Log.d("LocationService", "Session saved: $duration minutes")
            }
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

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("LocationService", "Service onDestroy")
        fusedLocationClient.removeLocationUpdates(locationCallback)

        serviceScope.launch {
            val locationDao = UHereDatabase.getDatabase(applicationContext).locationDao()
            endCurrentSession(locationDao)
        }.invokeOnCompletion {
            serviceScope.cancel()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}