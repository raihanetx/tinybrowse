# ProGuard / R8 rules for TinyBrowse

# Keep WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Compose
-dontwarn androidx.compose.**

# Keep data classes
-keep class com.tinybrowse.data.model.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
