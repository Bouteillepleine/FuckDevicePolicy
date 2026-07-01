package com.strawing.duckdevicepolicy

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Xposed entry point. For each row in [Restrictions.ALL] we install a hook that,
 * when its category is enabled by the user, forces the "no restriction" return
 * value. State is read cross-process from the UI app via [XSharedPreferences].
 */
class MainModule : IXposedHookLoadPackage {

    // Reads the UI app's prefs from inside whatever process we're hooking.
    private val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, Prefs.NAME).also {
        @Suppress("DEPRECATION")
        it.makeWorldReadable()
    }

    @Volatile
    private var hooked = false

    /** master AND per-category gate; both default on so a fresh install works. */
    private fun bypass(category: String): Boolean {
        prefs.reload() // cheap no-op unless the prefs file actually changed
        if (!prefs.getBoolean(Prefs.KEY_MASTER, true)) return false
        return prefs.getBoolean(Prefs.key(category), Prefs.CATEGORY_DEFAULT)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // The targets are framework classes (boot class loader) shared by the
        // whole process, so hooking once per process is enough.
        if (hooked) return
        hooked = true
        installHooks(lpparam.classLoader)
    }

    private fun installHooks(classLoader: ClassLoader) {
        var installed = 0
        for (spec in Restrictions.ALL) {
            try {
                val clazz = XposedHelpers.findClass(spec.className, classLoader)
                // findAndHookMethod(class, name, paramType..., callback)
                val args = ArrayList<Any>(spec.paramTypes.size + 1)
                args.addAll(spec.paramTypes)
                args.add(object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (bypass(spec.category)) {
                            param.result = spec.result()
                        }
                    }
                })
                XposedHelpers.findAndHookMethod(clazz, spec.method, *args.toTypedArray())
                installed++
            } catch (t: Throwable) {
                // Method/class absent on this Android version or process — skip it,
                // never let one missing signature abort the rest.
            }
        }
        if (BuildConfig.DEBUG) {
            XposedBridge.log("$TAG: installed $installed/${Restrictions.ALL.size} hooks")
        }
    }

    private companion object {
        const val TAG = "DuckPolicy"
    }
}
