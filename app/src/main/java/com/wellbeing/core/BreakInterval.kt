package com.wellbeing.core

import java.time.LocalTime

/**
 * An internal break interval within a focus window.
 *
 * During this interval, target applications remain accessible and focus is paused.
 */
data class BreakInterval(
    val startTime: LocalTime,
    val endTime: LocalTime
)
