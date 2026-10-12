package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale
import com.example.MainActivity
import com.example.R
import com.example.data.model.LocationData
import com.example.data.model.LocationHistoryEntry
import com.example.data.repository.VesperaRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VesperaTelemetryService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var telemetryHelper: TelemetryAndUsageHelper
    private lateinit var repository: VesperaRepository
    private var locationCallback: LocationCallback? = null
    private var periodicUsageJob: Job? = null
    private var lastKnownLocation: Location? = null
    private var lastHistoryLoggedLocation: Location? = null
    private var lastHistoryLoggedTime: Long = 0L

    companion object {
        const val CHANNEL_ID = "vespera_telemetry_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_TELEMETRY"
        const val ACTION_STOP = "ACTION_STOP_TELEMETRY"

        var isServiceRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, VesperaTelemetryService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VesperaTelemetryService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        telemetryHelper = TelemetryAndUsageHelper(applicationContext)
        repository = VesperaRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopLocationUpdates()
                periodicUsageJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                isServiceRunning = false
                return START_NOT_STICKY
            }
            else -> {
                startForegroundWithNotification()
                startLocationUpdates()
                startPeriodicUsageAndTelemetrySync()
                isServiceRunning = true
                return START_STICKY
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gia Đình Việt - Giám sát vị trí & Gia đình",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Chạy ngầm định vị GPS và kiểm tra thời gian sử dụng ứng dụng liên tục"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sosIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_SOS"
            putExtra("TRIGGER_SOS", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sosPendingIntent = PendingIntent.getActivity(
            this,
            1,
            sosIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gia Đình Việt • Giám sát & SOS 24/7")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_vespera_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_vespera_logo, "🚨 BẤM SOS KHẨN CẤP", sosPendingIntent)
            .build()
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Đang đồng bộ vị trí GPS và thời gian sử dụng ứng dụng...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotificationText(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun startLocationUpdates() {
        if (!telemetryHelper.hasLocationPermission()) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30_000L)
            .setMinUpdateIntervalMillis(15_000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    lastKnownLocation = location
                    syncTelemetryWithLocation(location)
                }
            }
        }

        try {
            locationCallback?.let { cb ->
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    cb,
                    Looper.getMainLooper()
                )
            }
        } catch (e: SecurityException) {
            Log.e("VesperaService", "Location permission missing", e)
        }
    }

    /**
     * Periodic background loop running every 45s:
     * Reads usage stats (which app, how long) and updates notification and Firestore!
     */
    private fun startPeriodicUsageAndTelemetrySync() {
        periodicUsageJob?.cancel()
        periodicUsageJob = serviceScope.launch {
            while (isActive) {
                try {
                    val loc = lastKnownLocation
                    if (loc != null) {
                        syncTelemetryWithLocation(loc)
                    } else {
                        // Attempt fallback last location
                        telemetryHelper.fetchCurrentLocation { fetchedLoc ->
                            if (fetchedLoc != null) {
                                lastKnownLocation = fetchedLoc
                                syncTelemetryWithLocation(fetchedLoc)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("VesperaService", "Periodic background tick failed", e)
                }
                delay(45_000L) // Refresh every 45 seconds
            }
        }
    }

    private fun syncTelemetryWithLocation(location: Location) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentUserName = FirebaseAuth.getInstance().currentUser?.displayName.orEmpty()
        val (batteryLevel, isCharging) = telemetryHelper.getBatteryInfo()

        // Tiết kiệm pin thông minh & tự động kích hoạt khi di chuyển > 5m
        val distanceFromLast = lastHistoryLoggedLocation?.distanceTo(location) ?: 999f
        val isMoving = location.speed > 0.8f || distanceFromLast >= 5f
        val now = System.currentTimeMillis()
        val timeSinceLastLog = now - lastHistoryLoggedTime

        serviceScope.launch {
            try {
                // Fetch real app usage from UsageStatsManager
                val detailedApps = telemetryHelper.getDetailedAppUsageList()
                val topApp = detailedApps.firstOrNull()?.appName ?: "Đang chờ dữ liệu"
                val topAppMins = detailedApps.firstOrNull()?.minutesUsed ?: 0
                val totalUsedMins = detailedApps.sumOf { it.minutesUsed }.coerceAtLeast(topAppMins)

                // Detect Safe Zone based on coordinates / speed
                val safeZone = when {
                    location.speed > 8f -> "Đang di chuyển (${(location.speed * 3.6f).toInt()} km/h)"
                    Math.abs(location.latitude - 21.0378) < 0.005 && Math.abs(location.longitude - 105.8325) < 0.005 -> "Trường học"
                    else -> "Nhà riêng / Điểm an toàn"
                }

                // Update Foreground Notification so user sees GPS & app tracking live in notification bar
                val notifStatus = "📍 GPS: %.4f, %.4f | 📱 %s: %d ph | ⚡ Pin: %d%%%s".format(
                    location.latitude,
                    location.longitude,
                    topApp,
                    topAppMins,
                    batteryLevel,
                    if (isCharging) " (Sạc)" else ""
                )
                updateNotificationText(notifStatus)

                // Push to Firestore LocationData
                val locData = LocationData(
                    userId = currentUserId,
                    userName = currentUserName,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    speed = location.speed,
                    batteryLevel = batteryLevel,
                    isCharging = isCharging,
                    isPrivacyActive = false,
                    currentSafeZone = safeZone,
                    topActiveApp = topApp,
                    topActiveAppMinutes = topAppMins,
                    totalUsedMinutes = totalUsedMins,
                    updatedAt = now
                )
                repository.updateLocation(locData)

                // TIẾT KIỆM PIN & GHI LỊCH SỬ DI CHUYỂN:
                val prefs = getSharedPreferences("vespera_prefs", Context.MODE_PRIVATE)
                val historyEnabled = prefs.getBoolean("history_enabled", true)
                if (historyEnabled && (isMoving || timeSinceLastLog >= 15 * 60 * 1000L)) {
                    val profile = repository.getUserProfile(currentUserId)
                    val pairingId = profile?.activePairingId.orEmpty()

                    val calendar = java.util.Calendar.getInstance()
                    val year = calendar.get(java.util.Calendar.YEAR)
                    val month = calendar.get(java.util.Calendar.MONTH) + 1
                    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                    val minute = calendar.get(java.util.Calendar.MINUTE)
                    val second = calendar.get(java.util.Calendar.SECOND)

                    val dateStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month, year)
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hour, minute, second)

                    val historyEntry = LocationHistoryEntry(
                        id = "${currentUserId}_${now}",
                        pairingId = pairingId,
                        userId = currentUserId,
                        userName = currentUserName,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        placeName = safeZone,
                        speed = location.speed,
                        batteryLevel = batteryLevel,
                        isCharging = isCharging,
                        timestamp = now,
                        dateStr = dateStr,
                        timeStr = timeStr,
                        year = year,
                        month = month,
                        day = day,
                        hour = hour,
                        minute = minute,
                        second = second
                    )
                    repository.saveLocationHistory(historyEntry)
                    lastHistoryLoggedLocation = location
                    lastHistoryLoggedTime = now
                    Log.d("VesperaService", "Đã ghi lịch sử di chuyển: $safeZone lúc $timeStr ngày $dateStr")
                }
            } catch (e: Exception) {
                Log.w("VesperaService", "Background location & usage sync failed", e)
            }
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        periodicUsageJob?.cancel()
        serviceScope.cancel()
        isServiceRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
