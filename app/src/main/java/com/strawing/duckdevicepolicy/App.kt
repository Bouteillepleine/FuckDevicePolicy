package com.strawing.duckdevicepolicy

import android.app.Application
import com.google.android.material.color.DynamicColors
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Two jobs, both UI-process only:
 *
 *  1. Material You dynamic colours (wallpaper-based) on Android 12+.
 *  2. Holding the libxposed framework service. The binder does not arrive synchronously — it
 *     is delivered to a ContentProvider inside the service library and handed on from a binder
 *     thread, which may be before or after the first activity is created — and
 *     [XposedServiceHelper] keeps room for exactly ONE listener process-wide. So the
 *     Application registers itself and fans out to whoever is on screen.
 *
 * `service == null` is also the honest answer to "is the framework there at all": under the
 * legacy API the UI could not tell, and wrote settings into a file that might never be read.
 */
class App : Application(), XposedServiceHelper.OnServiceListener {

    companion object {
        /** Null until the framework binds, and again if it dies. */
        @Volatile
        var service: XposedService? = null
            private set

        private val listeners = CopyOnWriteArraySet<(XposedService?) -> Unit>()

        /** Registers [l] and calls it immediately with the current state. */
        fun addListener(l: (XposedService?) -> Unit) {
            listeners.add(l)
            l(service)
        }

        fun removeListener(l: (XposedService?) -> Unit) = listeners.remove(l)

        private fun dispatch(svc: XposedService?) = listeners.forEach { it(svc) }
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        Companion.service = service
        dispatch(service)
    }

    override fun onServiceDied(service: XposedService) {
        Companion.service = null
        dispatch(null)
    }
}
