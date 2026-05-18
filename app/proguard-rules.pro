# ProGuard / R8 rules for TinyBrowse

# Keep WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# CRITICAL: Keep WebView client subclasses. If R8 strips these,
# the WebView will have no client callbacks, causing blank/white screen
# because onPageStarted/onPageFinished/onReceivedError etc. won't fire.
-keep class * extends android.webkit.WebViewClient { *; }
-keep class * extends android.webkit.WebChromeClient { *; }

# Keep our app's WebViewConfig since it's accessed from anonymous inner classes
-keep class com.tinybrowse.engine.WebViewConfig { *; }

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
