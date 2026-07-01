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

    // ---- param type names ----
    private const val CN = "android.content.ComponentName"
    private const val STR = "java.lang.String"

    // ---- categories (keys used for prefs + UI) ----
    const val CAMERA = "camera"
    const val SCREEN_CAPTURE = "screen_capture"
    const val ADMIN = "admin"
    const val PASSWORD = "password"
    const val KEYGUARD = "keyguard"
    const val ENCRYPTION = "encryption"
    const val APP_RESTRICTIONS = "app_restrictions"
    const val USER_RESTRICTIONS = "user_restrictions"
    const val MAX_LOCK = "max_lock"
    const val LOCK_TASK = "lock_task"
    const val PERMITTED = "permitted"
    const val MISC = "misc"

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
        Category(MAX_LOCK, "Auto-lock timeout", "Remove the forced maximum time-to-lock"),
        Category(LOCK_TASK, "Kiosk / lock-task mode", "Report lock-task as permitted / unrestricted"),
        Category(PERMITTED, "Allowed IMEs & accessibility", "Remove input-method / accessibility allow-lists"),
        Category(MISC, "Misc (auto-time, cross-profile, BT)", "Clear assorted smaller restrictions"),
    )

    data class Spec(
        val category: String,
        val className: String,
        val method: String,
        val paramTypes: Array<String>,
        val result: () -> Any?,
    )

    private fun bundle(): Any = Bundle()

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
    )
}
