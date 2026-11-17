package com.wellbeing

import android.app.Application
import com.wellbeing.data.ScheduleRepository

/**
 * Custom Application class ensuring repository initialization on process start.
 *
 * Implements Object Calisthenics: single-purpose lifecycle hooks.
 */
class ThocusApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ScheduleRepository.initialize(this)
    }
}
