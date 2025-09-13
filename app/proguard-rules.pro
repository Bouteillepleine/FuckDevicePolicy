# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Xposed framework
-keep class de.robv.android.xposed.** { *; }
-keep class * extends de.robv.android.xposed.IXposedHookLoadPackage
-keep class * extends de.robv.android.xposed.IXposedHookZygoteInit
-keep class * extends de.robv.android.xposed.IXposedHookInitPackageResources

# Keep module entry point
-keep class com.bouteillepleine.fuckdevicepolicy.** { *; }

# Basic Android rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Don't warn about missing classes
-dontwarn de.robv.android.xposed.**
