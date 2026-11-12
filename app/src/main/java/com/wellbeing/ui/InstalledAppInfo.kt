package com.wellbeing.ui

import androidx.compose.runtime.Immutable

/**
 * Representation of an installed launchable application discovered on device.
 */
@Immutable
data class InstalledAppInfo(
    val packageName: String,
    val applicationLabel: String
)
