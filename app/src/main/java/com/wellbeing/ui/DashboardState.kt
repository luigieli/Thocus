package com.wellbeing.ui

import com.wellbeing.core.TimeWindow

/**
 * Finite State Machine (FSM) States for the Thocus Dashboard.
 *
 * Implements specifications in UI_SPEC.md.
 */
sealed interface DashboardState {

    data object Loading : DashboardState

    data class MissingPermissions(
        val isAccessibilityMissing: Boolean,
        val isExactAlarmMissing: Boolean
    ) : DashboardState

    data class Operational(
        val isFocusActive: Boolean,
        val remainingFocusMinutes: Long,
        val activeWindows: List<TimeWindow>,
        val blockedPackages: Set<String>,
        val installedApps: List<InstalledAppInfo> = emptyList(),
        val feedbackMessage: String = "",
        val isConfigModalOpen: Boolean = false,
        val editingWindow: TimeWindow? = null
    ) : DashboardState
}
