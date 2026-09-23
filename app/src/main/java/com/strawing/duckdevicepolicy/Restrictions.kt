package com.strawing.duckdevicepolicy

import android.os.Bundle

/**
 * Declarative table of every method we neutralise, grouped into user-facing
 * categories. Adding coverage = adding a row; the hook engine and the UI both
 * derive from this list, so nothing else needs to change.
 *
 * Each [Spec.result] is the value the method should return when its category is
 * bypassed (the "no restriction" answer). Types matter: Int 0, Long 0L,
 * Boolean, empty Bundle, or null (null generally means "no allow-list = allow
 * everything" for the permitted-* getters).
 */
object Restrictions {

    // ---- class names ----
    private const val DPM = "android.app.admin.DevicePolicyManager"
    private const val UM = "android.os.UserManager"
    private const val RM = "android.content.RestrictionsManager"
    private const val UMS = "com.android.server.pm.UserManagerService"
    private const val UMS_LOCAL = "com.android.server.pm.UserManagerService\$LocalService"
    private const val OUTLOOK_DP = "com.microsoft.office.outlook.olmcore.managers.mdm.DevicePolicy"
    private const val OUTLOOK_PKG = "com.microsoft.office.outlook"

    // ---- param type names ----
    private const val CN = "android.content.ComponentName"
    private const val STR = "java.lang.String"
    private const val INT = "int"

    // ---- categories (keys used for prefs + UI) ----
    const val CAMERA = "camera"
    const val SCREEN_CAPTURE = "screen_capture"
    const val ADMIN = "admin"
    const val PASSWORD = "password"
    const val KEYGUARD = "keyguard"
    const val ENCRYPTION = "encryption"
    const val APP_RESTRICTIONS = "app_restrictions"
    const val USER_RESTRICTIONS = "user_restrictions"
    const val PACKAGE_INSTALL = "package_install"
    const val DEBUGGING = "debugging"
    const val MAX_LOCK = "max_lock"
    const val LOCK_TASK = "lock_task"
    const val PERMITTED = "permitted"
    const val MISC = "misc"
    const val OUTLOOK_ENROLLMENT = "outlook_enrollment"

    data class Category(val key: String, val title: String, val subtitle: String)

    /** Order here is the order shown in the UI. */
    val CATEGORIES: List<Category> = listOf(
        Category(CAMERA, "Camera block", "Report the camera as not disabled"),
        Category(SCREEN_CAPTURE, "Screenshot / screen-record block", "Allow screen capture"),
        Category(ADMIN, "Device admin & owner checks", "Look unmanaged (no admin / owner / managed profile)"),
        Category(PASSWORD, "Password & PIN policy", "Drop length, complexity, expiry and wipe rules"),
        Category(KEYGUARD, "Lock-screen feature limits", "Re-enable camera, notifications, etc. on keyguard"),
        Category(ENCRYPTION, "Storage-encryption enforcement", "Report encryption as not required"),
        Category(APP_RESTRICTIONS, "Managed app configuration", "Return empty app-restriction bundles"),
        Category(USER_RESTRICTIONS, "User restrictions (DISALLOW_*)", "Clear user restrictions incl. UserManager checks"),
        Category(PACKAGE_INSTALL, "App install / uninstall block", "Sideload APKs from any app (Telegram, browsers, file managers) and uninstall admin-locked apps"),
        Category(DEBUGGING, "Developer options & USB debugging block", "Re-enable developer settings and ADB"),
        Category(MAX_LOCK, "Auto-lock timeout", "Remove the forced maximum time-to-lock"),
        Category(LOCK_TASK, "Kiosk / lock-task mode", "Report lock-task as permitted / unrestricted"),
        Category(PERMITTED, "Allowed IMEs & accessibility", "Remove input-method / accessibility allow-lists"),
        Category(MISC, "Misc (auto-time, cross-profile, BT)", "Clear assorted smaller restrictions"),
        Category(OUTLOOK_ENROLLMENT, "Outlook enrollment gate", "Report the device as already MDM-enrolled/compliant inside Outlook"),
    )

    data class Spec(
        val category: String,
        val className: String,
        val method: String,
        val paramTypes: Array<String>,
        /**
         * Package whose own private class this row names, or null for a framework class every
         * process shares. The modern API hands the hook a separate entry point per process, so
         * app-private rows are installed only where their class is loadable instead of being
         * attempted (and silently failing) everywhere.
         */
        val pkg: String? = null,
        val keys: Set<String>? = null,
        val keyArg: Int = 0,
        val result: () -> Any?,
    )

    private fun bundle(): Any = Bundle()

    private val INSTALL_KEYS = setOf(
        "no_install_unknown_sources",
        "no_install_unknown_sources_globally",
        "no_install_apps",
        "no_uninstall_apps",
    )

    private val DEBUGGING_KEYS = setOf("no_debugging_features")

    private fun serviceRows(category: String, keys: Set<String>): List<Spec> = listOf(
        Spec(category, UMS, "hasUserRestriction", arrayOf(STR, INT), keys = keys) { false },
        Spec(category, UMS, "hasUserRestrictionOnAnyUser", arrayOf(STR), keys = keys) { false },
        Spec(category, UMS, "getUserRestrictionSource", arrayOf(STR, INT), keys = keys) { 0 },
        Spec(category, UMS, "getUserRestrictionSources", arrayOf(STR, INT), keys = keys) { emptyList<Any>() },
        Spec(category, UMS_LOCAL, "getUserRestriction", arrayOf(INT, STR), keys = keys, keyArg = 1) { false },
    )

    val ALL: List<Spec> = listOf(
        // camera
        Spec(CAMERA, DPM, "getCameraDisabled", arrayOf(CN)) { false },

        // screen capture
        Spec(SCREEN_CAPTURE, DPM, "getScreenCaptureDisabled", arrayOf(CN)) { false },

        // admin / ownership
        Spec(ADMIN, DPM, "isAdminActive", arrayOf(CN)) { false },
        Spec(ADMIN, DPM, "isDeviceOwnerApp", arrayOf(STR)) { false },
        Spec(ADMIN, DPM, "isProfileOwnerApp", arrayOf(STR)) { false },
        Spec(ADMIN, DPM, "isManagedProfile", arrayOf(CN)) { false },
        Spec(ADMIN, DPM, "isDeviceManaged", arrayOf()) { false },

        // password / credential policy
        Spec(PASSWORD, DPM, "getPasswordQuality", arrayOf(CN)) { 0 },
        Spec(PASSWORD, DPM, "getPasswordMinimumLength", arrayOf(CN)) { 0 },
        Spec(PASSWORD, DPM, "getPasswordMinimumLetters", arrayOf(CN)) { 0 },
        Spec(PASSWORD, DPM, "getPasswordMinimumNumeric", arrayOf(CN)) { 0 },
        Spec(PASSWORD, DPM, "getPasswordMinimumSymbols", arrayOf(CN)) { 0 },
        Spec(PASSWORD, DPM, "getPasswordMinimumUpperCase", arrayOf(CN)) { 0 },
        Spec(PASSWORD, DPM, "getPasswordMinimumLowerCase", arrayOf(CN)) { 0 },
        Spec(PASSWORD, DPM, "getPasswordMinimumNonLetter", arrayOf(CN)) { 0 },
        Spec(PASSWORD, DPM, "getPasswordHistoryLength", arrayOf(CN)) { 0 },
        Spec(PASSWORD, DPM, "getPasswordExpiration", arrayOf(CN)) { 0L },
        Spec(PASSWORD, DPM, "getPasswordExpirationTimeout", arrayOf(CN)) { 0L },
        Spec(PASSWORD, DPM, "getMaximumFailedPasswordsForWipe", arrayOf(CN)) { 0 },
        Spec(PASSWORD, DPM, "getRequiredPasswordComplexity", arrayOf()) { 0 },
        Spec(PASSWORD, DPM, "isActivePasswordSufficient", arrayOf()) { true },

        // keyguard features
        Spec(KEYGUARD, DPM, "getKeyguardDisabledFeatures", arrayOf(CN)) { 0 },

        // storage encryption
        Spec(ENCRYPTION, DPM, "getStorageEncryption", arrayOf(CN)) { false },

        // managed application restrictions
        Spec(APP_RESTRICTIONS, DPM, "getApplicationRestrictions", arrayOf(CN, STR)) { bundle() },
        Spec(APP_RESTRICTIONS, RM, "getApplicationRestrictions", arrayOf()) { bundle() },

        // user restrictions (DISALLOW_*) — DPM + UserManager (where most are read)
        Spec(USER_RESTRICTIONS, DPM, "getUserRestrictions", arrayOf(CN)) { bundle() },
        Spec(USER_RESTRICTIONS, UM, "getUserRestrictions", arrayOf()) { bundle() },
        Spec(USER_RESTRICTIONS, UM, "hasUserRestriction", arrayOf(STR)) { false },

        // maximum time to lock
        Spec(MAX_LOCK, DPM, "getMaximumTimeToLock", arrayOf(CN)) { 0L },

        // lock task / kiosk
        Spec(LOCK_TASK, DPM, "isLockTaskPermitted", arrayOf(STR)) { true },
        Spec(LOCK_TASK, DPM, "getLockTaskFeatures", arrayOf(CN)) { 0 },

        // permitted allow-lists (null = no allow-list = everything allowed)
        Spec(PERMITTED, DPM, "getPermittedInputMethods", arrayOf(CN)) { null },
        Spec(PERMITTED, DPM, "getPermittedAccessibilityServices", arrayOf(CN)) { null },

        // misc smaller restrictions
        Spec(MISC, DPM, "getAutoTimeRequired", arrayOf()) { false },
        Spec(MISC, DPM, "getBluetoothContactSharingDisabled", arrayOf(CN)) { false },
        Spec(MISC, DPM, "getCrossProfileCallerIdDisabled", arrayOf(CN)) { false },
        Spec(MISC, DPM, "getCrossProfileContactsSearchDisabled", arrayOf(CN)) { false },

        // Outlook's own enrollment/compliance gate (app-private class, not a
        // framework or MAM-SDK type). Tagged with its package so it is installed
        // from onPackageReady in Outlook's process only.
        Spec(OUTLOOK_ENROLLMENT, OUTLOOK_DP, "requiresDeviceManagement", arrayOf(), OUTLOOK_PKG) { false },
        Spec(OUTLOOK_ENROLLMENT, OUTLOOK_DP, "isPolicyApplied", arrayOf(), OUTLOOK_PKG) { true },
    ) + serviceRows(PACKAGE_INSTALL, INSTALL_KEYS) + serviceRows(DEBUGGING, DEBUGGING_KEYS)
}
