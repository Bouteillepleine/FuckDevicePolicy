package com.strawing.duckdevicepolicy

import android.content.SharedPreferences
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import java.lang.reflect.Method
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Xposed entry point (modern libxposed API). For each row in [Restrictions.ALL] we install a
 * hook that, when its category is enabled by the user, forces the "no restriction" return
 * value.
 *
 * ── What the move off the legacy API changed ──────────────────────────────────────────────
 * - `handleLoadPackage` fired once per package hosted in a process and left this class to
 *   work out from a package name where it was standing. The modern API splits that:
 *   [onSystemServerStarting] IS the system_server entry and [onPackageReady] is the per-app
 *   one, so the framework rows and Outlook's own private class no longer share a code path.
 * - Settings no longer travel through a world-readable `XSharedPreferences` file. The
 *   framework brokers them ([getRemotePreferences]), which is both readable from system_server
 *   and pushed on change — hence no `reload()` call anywhere below.
 * - There is no `XposedHelpers`: classes are resolved with `Class.forName` and methods with a
 *   hierarchy walk that reproduces what `findAndHookMethod` did.
 */
class MainModule : XposedModule() {

    /**
     * Framework-brokered settings. Null only if the framework refuses them, in which case
     * every category falls back to its shipped default (on) — the same end state as a legacy
     * build whose prefs file could not be read.
     */
    private val prefs: SharedPreferences? by lazy {
        runCatching { getRemotePreferences(Prefs.NAME) }
            .onFailure { log(Log.ERROR, TAG, "remote preferences unavailable; using defaults", it) }
            .getOrNull()
    }

    /** The framework rows are boot-classloader classes shared by the whole process. */
    private val frameworkHooked = AtomicBoolean(false)

    /** App-private rows, by package: onPackageReady can fire more than once per process. */
    private val appHooked: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    /** master AND per-category gate; both default on so a fresh install works. */
    private fun bypass(category: String): Boolean {
        val p = prefs ?: return Prefs.CATEGORY_DEFAULT
        if (!p.getBoolean(Prefs.KEY_MASTER, true)) return false
        return p.getBoolean(Prefs.key(category), Prefs.CATEGORY_DEFAULT)
    }

    // ------------------------------------------------------------------ entry points

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        if (BuildConfig.DEBUG) {
            log(Log.INFO, TAG, "loaded proc=${param.processName} systemServer=${param.isSystemServer} " +
                "framework=$frameworkName($frameworkVersionCode) API $apiVersion")
        }
    }

    /**
     * system_server. Under the legacy API this was reached through a `pkg == "android"`
     * comparison; the framework states it outright now.
     */
    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        installFrameworkHooks(param.classLoader, "system_server")
    }

    /**
     * Every scoped app process. The framework rows go in once per process; rows that name an
     * app-private class (Outlook's enrollment gate) go in only for their own package, where
     * that class is actually loadable.
     */
    override fun onPackageReady(param: PackageReadyParam) {
        installFrameworkHooks(param.classLoader, param.packageName)
        val appSpecs = Restrictions.ALL.filter { it.pkg == param.packageName }
        if (appSpecs.isNotEmpty() && appHooked.add(param.packageName)) {
            install(appSpecs, param.classLoader, param.packageName)
        }
    }

    // ------------------------------------------------------------------ hook installation

    private fun installFrameworkHooks(classLoader: ClassLoader, where: String) {
        if (!frameworkHooked.compareAndSet(false, true)) return
        install(Restrictions.ALL.filter { it.pkg == null }, classLoader, where)
    }

    private fun install(specs: List<Restrictions.Spec>, classLoader: ClassLoader, where: String) {
        var installed = 0
        for (spec in specs) {
            try {
                val clazz = Class.forName(spec.className, false, classLoader)
                val params = Array<Class<*>>(spec.paramTypes.size) {
                    paramClass(spec.paramTypes[it], classLoader)
                }
                val method = findMethod(clazz, spec.method, params) ?: continue
                hook(method).intercept(hookerFor(spec))
                installed++
            } catch (t: Throwable) {
                // Method/class absent on this Android version or process — skip it,
                // never let one missing signature abort the rest.
            }
        }
        if (installed == 0) {
            // Worth a line even when quiet: a process where nothing installed is a process
            // where this module does nothing, and that used to be invisible.
            log(Log.ERROR, TAG, "no hooks installed in $where (0/${specs.size})")
        } else if (BuildConfig.DEBUG) {
            log(Log.INFO, TAG, "installed $installed/${specs.size} hooks in $where")
        }
    }

    private fun paramClass(name: String, classLoader: ClassLoader): Class<*> = when (name) {
        "int" -> Integer.TYPE
        "long" -> java.lang.Long.TYPE
        "boolean" -> java.lang.Boolean.TYPE
        else -> Class.forName(name, false, classLoader)
    }

    /**
     * `XposedHelpers.findAndHookMethod` searched the superclass chain; `getDeclaredMethod`
     * does not. Walk it so rows that name a subclass keep resolving as they did.
     */
    private fun findMethod(clazz: Class<*>, name: String, params: Array<Class<*>>): Method? {
        var c: Class<*>? = clazz
        while (c != null) {
            try {
                return c.getDeclaredMethod(name, *params)
            } catch (_: NoSuchMethodException) {
                c = c.superclass
            }
        }
        return null
    }

    /**
     * One interceptor replaces the legacy before/after pair: returning a value without calling
     * [XposedInterface.Chain.proceed] is what `param.result = …` in `beforeHookedMethod` meant.
     */
    private fun hookerFor(spec: Restrictions.Spec) = XposedInterface.Hooker { chain ->
        if (bypass(spec.category) && appliesTo(spec, chain)) spec.result() else chain.proceed()
    }

    private fun appliesTo(spec: Restrictions.Spec, chain: XposedInterface.Chain): Boolean {
        val keys = spec.keys ?: return true
        val arg = runCatching { chain.getArg(spec.keyArg) }.getOrNull()
        return arg is String && arg in keys
    }

    private companion object {
        const val TAG = "DuckPolicy"
    }
}
