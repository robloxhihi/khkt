package com.example.service

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.data.model.AppCategoryUsage
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Calendar

class TelemetryAndUsageHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Check if user granted Usage Access permission
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Intent to open Usage Access Settings screen in Android
     */
    fun getUsageAccessSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Fetch real usage stats for today and categorize apps
     */
    fun getTodayUsageCategories(): List<AppCategoryUsage> {
        if (!hasUsageStatsPermission()) {
            return emptyList()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        var socialMinutes = 0
        var studyMinutes = 0
        var gameMinutes = 0
        var otherMinutes = 0

        for (item in stats) {
            val totalTimeForegroundMs = item.totalTimeInForeground
            if (totalTimeForegroundMs < 60_000) continue // Skip under 1 min

            val mins = (totalTimeForegroundMs / (1000 * 60)).toInt()
            val pkg = item.packageName.lowercase()

            when {
                pkg.contains("zalo") || pkg.contains("facebook") || pkg.contains("tiktok") ||
                pkg.contains("youtube") || pkg.contains("instagram") || pkg.contains("twitter") ||
                pkg.contains("threads") || pkg.contains("messenger") -> {
                    socialMinutes += mins
                }
                pkg.contains("zoom") || pkg.contains("teams") || pkg.contains("classroom") ||
                pkg.contains("duolingo") || pkg.contains("quizlet") || pkg.contains("khan") ||
                pkg.contains("study") || pkg.contains("sach") || pkg.contains("toan") -> {
                    studyMinutes += mins
                }
                pkg.contains("game") || pkg.contains("roblox") || pkg.contains("freefire") ||
                pkg.contains("garena") || pkg.contains("minecraft") || pkg.contains("pubg") ||
                pkg.contains("genshin") || pkg.contains("honor") -> {
                    gameMinutes += mins
                }
                else -> {
                    otherMinutes += mins
                }
            }
        }

        val total = (socialMinutes + studyMinutes + gameMinutes + otherMinutes).coerceAtLeast(1)

        return listOf(
            AppCategoryUsage("Mạng xã hội", socialMinutes, "social", socialMinutes.toFloat() / total),
            AppCategoryUsage("Học tập", studyMinutes, "study", studyMinutes.toFloat() / total),
            AppCategoryUsage("Trò chơi", gameMinutes, "game", gameMinutes.toFloat() / total),
            AppCategoryUsage("Khác", otherMinutes, "other", otherMinutes.toFloat() / total)
        )
    }

    /**
     * Query detailed per-app usage list (app name, minutes used, category)
     */
    fun getDetailedAppUsageList(): List<com.example.data.model.DetailedAppUsage> {
        if (!hasUsageStatsPermission()) {
            return emptyList()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        val pm = context.packageManager
        val list = mutableListOf<com.example.data.model.DetailedAppUsage>()

        for (item in stats) {
            val totalMs = item.totalTimeInForeground
            if (totalMs < 60_000) continue // Skip under 1 minute

            val mins = (totalMs / (1000 * 60)).toInt()
            val pkg = item.packageName
            val pkgLower = pkg.lowercase()

            val appLabel = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                pkg.substringAfterLast('.')
            }

            val category = when {
                pkgLower.contains("zalo") || pkgLower.contains("facebook") || pkgLower.contains("tiktok") ||
                pkgLower.contains("youtube") || pkgLower.contains("instagram") || pkgLower.contains("twitter") ||
                pkgLower.contains("threads") || pkgLower.contains("messenger") -> "Mạng xã hội"

                pkgLower.contains("zoom") || pkgLower.contains("teams") || pkgLower.contains("classroom") ||
                pkgLower.contains("duolingo") || pkgLower.contains("quizlet") || pkgLower.contains("khan") ||
                pkgLower.contains("study") || pkgLower.contains("sach") || pkgLower.contains("toan") -> "Học tập"

                pkgLower.contains("game") || pkgLower.contains("roblox") || pkgLower.contains("freefire") ||
                pkgLower.contains("garena") || pkgLower.contains("minecraft") || pkgLower.contains("pubg") ||
                pkgLower.contains("genshin") || pkgLower.contains("honor") -> "Trò chơi"

                else -> "Tiện ích & Khác"
            }

            list.add(
                com.example.data.model.DetailedAppUsage(
                    packageName = pkg,
                    appName = appLabel,
                    minutesUsed = mins,
                    category = category,
                    lastTimeUsed = item.lastTimeUsed
                )
            )
        }

        return list.sortedByDescending { it.minutesUsed }
    }

    /**
     * Get current battery percentage & charging status
     */
    fun getBatteryInfo(): Pair<Int, Boolean> {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 85

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        return Pair(batteryPct, isCharging)
    }

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Fetch current device GPS location
     */
    fun fetchCurrentLocation(onLocation: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            onLocation(null)
            return
        }

        try {
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cts.token
            ).addOnSuccessListener { loc ->
                if (loc != null) {
                    onLocation(loc)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        onLocation(lastLoc)
                    }.addOnFailureListener {
                        onLocation(null)
                    }
                }
            }.addOnFailureListener {
                onLocation(null)
            }
        } catch (_: SecurityException) {
            onLocation(null)
        }
    }
}
