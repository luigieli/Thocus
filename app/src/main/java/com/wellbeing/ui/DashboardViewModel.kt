package com.wellbeing.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellbeing.core.ScheduleEngine
import com.wellbeing.core.TimeWindow
import com.wellbeing.data.ScheduleRepository
import com.wellbeing.service.BlockerAccessibilityService
import com.wellbeing.service.BoundaryAlarmScheduler
import com.wellbeing.service.NeverBlockWhitelist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

class DashboardViewModel(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val state: StateFlow<DashboardState> = mutableState.asStateFlow()

    fun reconcile(context: Context) {
        val isAccessibilityGranted = BlockerAccessibilityService.instance != null
        val isExactAlarmGranted = checkExactAlarmPermission(context)

        viewModelScope.launch {
            val allWindows = scheduleRepository.getAllWindows()
            val now = LocalDateTime.now()
            val remainingMinutes = ScheduleEngine.calculateActiveFocusRemainingMinutes(allWindows, now) ?: 0L
            val isFocusActive = remainingMinutes > 0
            val allBlockedPackages = extractAllBlockedPackages(allWindows)
            val installedApps = withContext(Dispatchers.IO) { queryInstalledApplications(context) }

            val event = DashboardEvent.StateReconciled(
                isAccessibilityGranted = isAccessibilityGranted,
                isExactAlarmGranted = isExactAlarmGranted,
                isFocusActive = isFocusActive,
                remainingFocusMinutes = remainingMinutes,
                activeWindows = allWindows,
                blockedPackages = allBlockedPackages,
                installedApps = installedApps
            )
            transition(event)
        }
    }

    private fun queryInstalledApplications(context: Context): List<InstalledAppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveList = packageManager.queryIntentActivities(intent, 0)
        val resultList = mutableListOf<InstalledAppInfo>()
        val ownPackage = context.packageName

        for (info in resolveList) {
            val pkg = info.activityInfo.packageName
            if (NeverBlockWhitelist.isWhitelisted(pkg, ownPackage)) {
                continue
            }
            val label = info.loadLabel(packageManager).toString()
            AppIconCache.load(context, pkg)
            resultList.add(InstalledAppInfo(packageName = pkg, applicationLabel = label))
        }
        return resultList.distinctBy { it.packageName }.sortedBy { it.applicationLabel.lowercase() }
    }

    fun openCreateModal() {
        transition(DashboardEvent.OpenConfigModal())
    }

    fun openEditModal(window: TimeWindow) {
        transition(DashboardEvent.OpenConfigModal(window))
    }

    fun closeModal() {
        transition(DashboardEvent.CloseConfigModal)
    }

    fun saveFocusWindow(
        id: Long = 0L,
        name: String,
        days: Set<DayOfWeek>,
        startTime: LocalTime,
        endTime: LocalTime,
        packages: Set<String>,
        breaks: List<com.wellbeing.core.BreakInterval>,
        isAllDay: Boolean = false,
        context: Context
    ) {
        viewModelScope.launch {
            val focusWindow = TimeWindow(
                id = id,
                name = name,
                enabled = true,
                daysOfWeek = days,
                startTime = startTime,
                endTime = endTime,
                blockedPackages = packages,
                breaks = breaks,
                isAllDay = isAllDay
            )
            scheduleRepository.saveWindow(focusWindow)
            updateCacheAndAlarms(context)
            transition(DashboardEvent.FeedbackUpdated("Focus window saved."))
            closeModal()
            reconcile(context)
        }
    }

    fun saveFocusWindow(
        id: Long = 0L,
        name: String,
        days: Set<DayOfWeek>,
        startTime: LocalTime,
        endTime: LocalTime,
        packages: Set<String>,
        breakStart: LocalTime?,
        breakEnd: LocalTime?,
        context: Context
    ) {
        val breaks = TimeWindow.createBreaksFromEndpoints(breakStart, breakEnd)
        saveFocusWindow(id, name, days, startTime, endTime, packages, breaks, false, context)
    }

    fun deleteFocusWindow(windowId: Long, context: Context) {
        viewModelScope.launch {
            scheduleRepository.deleteWindow(windowId)
            updateCacheAndAlarms(context)
            transition(DashboardEvent.FeedbackUpdated("Focus window removed."))
            reconcile(context)
        }
    }

    fun saveLock(
        id: Long = 0L,
        name: String,
        days: Set<DayOfWeek>,
        startTime: LocalTime,
        endTime: LocalTime,
        packages: Set<String>,
        breakStart: LocalTime?,
        breakEnd: LocalTime?,
        context: Context
    ) {
        saveFocusWindow(id, name, days, startTime, endTime, packages, breakStart, breakEnd, context)
    }

    fun deleteLock(windowId: Long, context: Context) {
        deleteFocusWindow(windowId, context)
    }

    private fun updateCacheAndAlarms(context: Context) {
        BoundaryAlarmScheduler.scheduleNextBoundary(context)
    }

    private fun extractAllBlockedPackages(windows: List<TimeWindow>): Set<String> {
        val set = mutableSetOf<String>()
        for (window in windows) {
            set.addAll(window.blockedPackages)
        }
        return set
    }

    private fun transition(event: DashboardEvent) {
        mutableState.value = DashboardReducer.reduce(mutableState.value, event)
    }

    private fun checkExactAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return alarmManager.canScheduleExactAlarms()
    }
}
