package com.wellbeing.ui

import com.wellbeing.core.TimeWindow

/**
 * Finite State Machine (FSM) Events for the Thocus Dashboard.
 *
 * Implements specifications in UI_SPEC.md.
 */
sealed interface DashboardEvent {

    data class StateReconciled(
        val isAccessibilityGranted: Boolean,
        val isExactAlarmGranted: Boolean,
        val isFocusActive: Boolean,
        val remainingFocusMinutes: Long,
        val activeWindows: List<TimeWindow>,
        val blockedPackages: Set<String>,
        val installedApps: List<InstalledAppInfo>
    ) : DashboardEvent

    data class FeedbackUpdated(
        val message: String
    ) : DashboardEvent

    data class OpenConfigModal(
        val windowToEdit: TimeWindow? = null
    ) : DashboardEvent

    data object CloseConfigModal : DashboardEvent
}
