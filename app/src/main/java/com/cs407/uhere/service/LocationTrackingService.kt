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

    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_USER_ID = "user_id"

        fun start(context: Context, userId: Int) {
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
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userId = intent?.getIntExtra(EXTRA_USER_ID, -1) ?: -1

        if (userId == -1) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()

        return START_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30000L // 30 seconds
        ).apply {
            setMinUpdateIntervalMillis(15000L) // 15 seconds
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun handleLocationUpdate(location: Location) {
        serviceScope.launch {
            val database = UHereDatabase.getDatabase(applicationContext)
            val placeDao = database.placeDao()
            val locationDao = database.locationDao()

            // Get all user places - FIXED LINE
            val places = placeDao.getUserPlaces(userId).first()

            // Find if user is at any place
            val currentPlace = places.firstOrNull { place ->
                val distance = calculateDistance(
                    location.latitude, location.longitude,
                    place.latitude, place.longitude
                )
                distance <= place.radius
            }

            if (currentPlace != null) {
                // User is at a place
                if (currentCategory != currentPlace.category) {
                    // End previous session if exists
                    endCurrentSession(locationDao)

                    // Start new session
                    startNewSession(currentPlace.category, locationDao)
                }
            } else {
                // User left all places
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
    }

    private suspend fun endCurrentSession(locationDao: com.cs407.uhere.data.LocationDao) {
        currentSessionId?.let { sessionId ->
            val now = System.currentTimeMillis()
            val duration = ((now - sessionStartTime) / 60000).toInt() // minutes

            // Get the session by ID
            val existingSession = locationDao.getSessionById(sessionId.toInt())

            existingSession?.let {
                val updatedSession = it.copy(
                    endTime = now,
                    durationMinutes = duration
                )

                locationDao.updateSession(updatedSession)
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
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // End current session if exists
        serviceScope.launch {
            val locationDao = UHereDatabase.getDatabase(applicationContext).locationDao()
            endCurrentSession(locationDao)
        }.invokeOnCompletion {
            serviceScope.cancel()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}