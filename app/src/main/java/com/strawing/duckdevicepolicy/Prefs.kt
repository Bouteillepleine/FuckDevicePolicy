package com.strawing.duckdevicepolicy

/**
 * Single source of truth for the preference contract shared between the UI
 * (writer) and the Xposed hook (reader, via XSharedPreferences).
 */
object Prefs {
    const val NAME = "bypass_prefs"

    /** Master on/off. Defaults to true so a fresh install is active. */
    const val KEY_MASTER = "bypass_enabled"

    /** Per-category key, e.g. "cat_camera". */
    fun key(category: String) = "cat_$category"

    /** Per-category default (bypass everything until the user says otherwise). */
    const val CATEGORY_DEFAULT = true
}
