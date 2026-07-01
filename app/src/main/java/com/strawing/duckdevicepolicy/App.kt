package com.strawing.duckdevicepolicy

import android.app.Application
import com.google.android.material.color.DynamicColors

/**
 * Applies Material You dynamic colours (wallpaper-based) to all activities on
 * Android 12+. On older versions this is a no-op and the Material3 baseline
 * palette is used. Only instantiated in the UI app's own process.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
