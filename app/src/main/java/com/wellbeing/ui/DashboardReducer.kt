package com.wellbeing.ui

/**
 * Pure state transition reducer for the Dashboard FSM.
 *
 * Implements Object Calisthenics: zero 'else', early returns, single-level indentation.
 */
object DashboardReducer {

    fun reduce(currentState: DashboardState, event: DashboardEvent): DashboardState {
        if (event is DashboardEvent.StateReconciled) {
            return handleStateReconciled(currentState, event)
        }
        if (event is DashboardEvent.FeedbackUpdated) {
            return handleFeedbackUpdated(currentState, event)
        }
        if (event is DashboardEvent.OpenConfigModal) {
            return handleOpenConfigModal(currentState, event)
        }
        if (event is DashboardEvent.CloseConfigModal) {
            return handleCloseConfigModal(currentState)
        }
        return currentState
    }

    private fun handleStateReconciled(
        currentState: DashboardState,
        event: DashboardEvent.StateReconciled
    ): DashboardState {
        val hasMissingPermissions = !event.isAccessibilityGranted || !event.isExactAlarmGranted
        if (hasMissingPermissions) {
            return DashboardState.MissingPermissions(
                isAccessibilityMissing = !event.isAccessibilityGranted,
                isExactAlarmMissing = !event.isExactAlarmGranted
            )
        }
        val isModalOpen = (currentState as? DashboardState.Operational)?.isConfigModalOpen ?: false
        val editingWindow = (currentState as? DashboardState.Operational)?.editingWindow
        return DashboardState.Operational(
            isFocusActive = event.isFocusActive,
            remainingFocusMinutes = event.remainingFocusMinutes,
            activeWindows = event.activeWindows,
            blockedPackages = event.blockedPackages,
            installedApps = event.installedApps,
            isConfigModalOpen = isModalOpen,
            editingWindow = editingWindow
        )
    }

    private fun handleFeedbackUpdated(
        currentState: DashboardState,
        event: DashboardEvent.FeedbackUpdated
    ): DashboardState {
        if (currentState !is DashboardState.Operational) {
            return currentState
        }
        return currentState.copy(feedbackMessage = event.message)
    }

    private fun handleOpenConfigModal(
        currentState: DashboardState,
        event: DashboardEvent.OpenConfigModal
    ): DashboardState {
        if (currentState !is DashboardState.Operational) {
            return currentState
        }
        return currentState.copy(
            isConfigModalOpen = true,
            editingWindow = event.windowToEdit
        )
    }

    private fun handleCloseConfigModal(currentState: DashboardState): DashboardState {
        if (currentState !is DashboardState.Operational) {
            return currentState
        }
        return currentState.copy(
            isConfigModalOpen = false,
            editingWindow = null
        )
    }
}
