# Rules published by libxposed for modules (see io.github.libxposed:api README).
# -adaptresourcefilecontents rewrites META-INF/xposed/java_init.list when R8 renames the
# entry class, which is what makes obfuscation safe here: the file and the dex stay in sync.
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# The API is compileOnly — provided by the framework at runtime, absent from the APK.
-dontwarn io.github.libxposed.api.**
