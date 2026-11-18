package com.wellbeing.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.wellbeing.R
import com.wellbeing.core.ScheduleEngine
import com.wellbeing.core.TimeWindow
import com.wellbeing.data.ScheduleRepository
import com.wellbeing.ui.BlockNoticeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Foreground service monitoring active foreground applications via UsageStatsManager.
 *
 * Implements specifications in USAGE_MONITOR.md.
 * Implements Object Calisthenics: zero 'else', early returns, single-level indentation.
 */
class UsageMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitoringJob: Job? = null
    private var screenReceiver: BroadcastReceiver? = null
    @Volatile private var isScreenOn = true

    override fun onCreate() {
        super.onCreate()
        instance = this
        promoteToForeground()
        initializeRepositoryCache()
        registerScreenStateReceiver()
        startMonitoringLoop()
    }

    private fun initializeRepositoryCache() {
        val repository = ScheduleRepository.getInstance(applicationContext)
        serviceScope.launch(Dispatchers.IO) {
            repository.refreshCache()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitoringLoop()
        return START_STICKY
    }

    override fun onDestroy() {
        instance = null
        unregisterScreenStateReceiver()
        stopMonitoringLoop()
        ForegroundTracker.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun promoteToForeground() {
        createNotificationChannel()
        val notification = buildForegroundNotification()
        startForegroundWithProperType(notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus Monitoring",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Thocus Active")
            .setContentText("Monitoring focus window limits")
            .setSmallIcon(R.drawable.ic_app_logo)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startForegroundWithProperType(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            return
        }
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun registerScreenStateReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                handleScreenIntent(intent?.action)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(receiver, filter)
        screenReceiver = receiver
    }

    private fun handleScreenIntent(action: String?) {
        if (action == Intent.ACTION_SCREEN_OFF) {
            isScreenOn = false
            return
        }
        if (action == Intent.ACTION_SCREEN_ON) {
            isScreenOn = true
            checkAndEnforce()
        }
    }

    private fun unregisterScreenStateReceiver() {
        val receiver = screenReceiver ?: return
        unregisterReceiver(receiver)
        screenReceiver = null
    }

    private fun startMonitoringLoop() {
        if (monitoringJob?.isActive == true) {
            return
        }
        monitoringJob = serviceScope.launch {
            runPeriodicInspection()
        }
    }

    private suspend fun runPeriodicInspection() {
        while (serviceScope.isActive) {
            inspectIfScreenActive()
            delay(POLL_INTERVAL_MILLIS)
        }
    }

    private fun inspectIfScreenActive() {
        if (!isScreenOn) {
            return
        }
        checkAndEnforce()
    }

    fun checkAndEnforce() {
        val foregroundPackage = resolveForegroundPackage() ?: return
        if (NeverBlockWhitelist.isWhitelisted(this, foregroundPackage)) {
            return
        }
        ForegroundTracker.updateForeground(foregroundPackage)
        enforceBlockIfRestricted(foregroundPackage)
    }

    private fun enforceBlockIfRestricted(packageName: String) {
        val activeWindows = resolveActiveWindows()
        val isBlocked = ScheduleEngine.isPackageBlocked(
            packageName = packageName,
            timeWindows = activeWindows,
            targetDateTime = LocalDateTime.now()
        )
        if (!isBlocked) {
            return
        }
        Log.i("UsageMonitorService", "INTERCEPTING RESTRICTED APP: $packageName")
        executeInterception(packageName)
    }

    private fun resolveActiveWindows(): List<TimeWindow> {
        val cached = ScheduleRepository.getActiveWindowsCached()
        if (cached.isNotEmpty()) {
            return cached
        }
        ScheduleRepository.getInstance(applicationContext)
        return ScheduleRepository.getActiveWindowsCached()
    }

    private fun executeInterception(packageName: String) {
        sendHomeIntent()
        launchBlockNotice(packageName)
    }

    private fun sendHomeIntent() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(homeIntent)
        } catch (exception: Exception) {
            Log.e("UsageMonitorService", "Failed to send Home intent", exception)
        }
    }

    private fun launchBlockNotice(packageName: String) {
        val noticeIntent = Intent(this, BlockNoticeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(BlockNoticeActivity.EXTRA_BLOCKED_PACKAGE, packageName)
        }
        try {
            startActivity(noticeIntent)
        } catch (exception: Exception) {
            Log.e("UsageMonitorService", "Failed to launch BlockNoticeActivity", exception)
        }
    }

    private fun resolveForegroundPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - USAGE_LOOKBACK_MILLIS
        val events = usageStatsManager.queryEvents(startTime, currentTime)
        val eventPackage = extractLastResumedPackage(events)
        if (eventPackage != null) {
            return eventPackage
        }
        return queryFallbackUsageStats(usageStatsManager, currentTime)
    }

    private fun queryFallbackUsageStats(usageStatsManager: UsageStatsManager, currentTime: Long): String? {
        val statsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            currentTime - USAGE_LOOKBACK_MILLIS,
            currentTime
        ) ?: return null
        if (statsList.isEmpty()) {
            return null
        }
        return statsList.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun extractLastResumedPackage(events: UsageEvents): String? {
        val event = UsageEvents.Event()
        var lastResumed: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastResumed = event.packageName
            }
        }
        return lastResumed
    }

    private fun stopMonitoringLoop() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "thocus_focus_monitor"
        private const val POLL_INTERVAL_MILLIS = 350L
        private const val USAGE_LOOKBACK_MILLIS = 60_000L

        @Volatile
        var instance: UsageMonitorService? = null
            private set

        fun start(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
                return
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java)
            context.stopService(intent)
        }

        fun checkAndEnforceCurrentForeground() {
            instance?.checkAndEnforce()
        }

        fun notifyWindowsChanged() {
            instance?.checkAndEnforce()
        }
    }
}
