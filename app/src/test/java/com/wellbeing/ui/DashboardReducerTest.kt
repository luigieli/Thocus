package com.wellbeing.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardReducerTest {

    @Test
    fun `reducer - transitions to MissingPermissions when accessibility is missing`() {
        val initialState = DashboardState.Loading
        val event = DashboardEvent.StateReconciled(
            isAccessibilityGranted = false,
            isExactAlarmGranted = true,
            isFocusActive = false,
            remainingFocusMinutes = 0L,
            activeWindows = emptyList(),
            blockedPackages = emptySet(),
            installedApps = emptyList()
        )

        val nextState = DashboardReducer.reduce(initialState, event)
        assertTrue(nextState is DashboardState.MissingPermissions)
        val missing = nextState as DashboardState.MissingPermissions
        assertTrue(missing.isAccessibilityMissing)
    }

    @Test
    fun `reducer - transitions to Operational when all permissions granted`() {
        val initialState = DashboardState.Loading
        val event = DashboardEvent.StateReconciled(
            isAccessibilityGranted = true,
            isExactAlarmGranted = true,
            isFocusActive = true,
            remainingFocusMinutes = 45L,
            activeWindows = emptyList(),
            blockedPackages = setOf("com.android.chrome"),
            installedApps = listOf(InstalledAppInfo("com.android.chrome", "Chrome"))
        )

        val nextState = DashboardReducer.reduce(initialState, event)
        assertTrue(nextState is DashboardState.Operational)
        val operational = nextState as DashboardState.Operational
        assertTrue(operational.isFocusActive)
        assertEquals(45L, operational.remainingFocusMinutes)
        assertEquals(1, operational.installedApps.size)
    }

    @Test
    fun `reducer - opens and closes config modal`() {
        val operational = DashboardState.Operational(
            isFocusActive = false,
            remainingFocusMinutes = 0L,
            activeWindows = emptyList(),
            blockedPackages = emptySet()
        )

        val openState = DashboardReducer.reduce(operational, DashboardEvent.OpenConfigModal())
        assertTrue((openState as DashboardState.Operational).isConfigModalOpen)

        val closedState = DashboardReducer.reduce(openState, DashboardEvent.CloseConfigModal)
        assertFalse((closedState as DashboardState.Operational).isConfigModalOpen)
    }
}
