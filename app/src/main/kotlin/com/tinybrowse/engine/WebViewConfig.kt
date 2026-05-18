package com.tinybrowse.engine

import android.webkit.WebSettings

/**
 * Static WebView configuration. Applied once when WebView is created.
 */
object WebViewConfig {

    fun apply(settings: WebSettings) {
        settings.apply {
            // Performance
            cacheMode = WebSettings.LOAD_DEFAULT
            setRenderPriority(WebSettings.RenderPriority.HIGH)

            // JavaScript (required for modern web)
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = false

            // Images
            loadsImagesAutomatically = true
            blockNetworkImage = false

            // Viewport — useWideViewPort=true + loadWithOverviewMode=false
            // gives us a wide desktop-like viewport that doesn't shrink to fit.
            // This is the correct default for a browser.
            useWideViewPort = true
            loadWithOverviewMode = false

            // Media
            mediaPlaybackRequiresUserGesture = true

            // Misc
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false

            // Text
            textZoom = 100
        }
    }

    /**
     * Desktop user-agent string — full Chrome on Linux.
     */
    const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /**
     * JavaScript to inject into every page to override viewport meta tag.
     * Forces the page to use a wide desktop width instead of device-width.
     */
    const val DESKTOP_VIEWPORT_JS = """
        (function() {
            var meta = document.querySelector('meta[name="viewport"]');
            if (meta) {
                meta.setAttribute('content', 'width=1024, initial-scale=1');
            } else {
                meta = document.createElement('meta');
                meta.name = 'viewport';
                meta.content = 'width=1024, initial-scale=1';
                document.head.appendChild(meta);
            }
        })();
    """

    /**
     * JavaScript to restore mobile viewport.
     */
    const val MOBILE_VIEWPORT_JS = """
        (function() {
            var meta = document.querySelector('meta[name="viewport"]');
            if (meta) {
                meta.setAttribute('content', 'width=device-width, initial-scale=1');
            }
        })();
    """
}
