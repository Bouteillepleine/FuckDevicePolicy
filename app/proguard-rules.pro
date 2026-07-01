# Keep the Xposed entry point and hook classes intact (referenced by name via
# assets/xposed_init and by the framework).
-keep class com.strawing.duckdevicepolicy.MainModule { *; }
-keepnames class com.strawing.duckdevicepolicy.** { *; }

# Xposed API is provided at runtime.
-dontwarn de.robv.android.xposed.**
-keep class de.robv.android.xposed.** { *; }
