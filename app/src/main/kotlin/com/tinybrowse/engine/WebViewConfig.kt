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

            // JavaScript (required for modern web — YouTube, Google, etc.)
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true   // Required for video playback on some sites

            // Images — MUST be enabled for page rendering
            loadsImagesAutomatically = true
            blockNetworkImage = false

            // Viewport
            useWideViewPort = true
            loadWithOverviewMode = false

            // Media — MUST allow autoplay for YouTube etc.
            mediaPlaybackRequiresUserGesture = false

            // CRITICAL: Allow mixed content. Many sites load some
            // resources (fonts, scripts, images) over HTTP even when
            // the main page is HTTPS. MIXED_CONTENT_NEVER_ALLOW would
            // block these, causing blank/white screen.
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            // Misc
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            setSupportMultipleWindows(true)  // YouTube opens video in new window
            javaScriptCanOpenWindowsAutomatically = true

            // Text
            textZoom = 100

            // CRITICAL: Allow file access for content rendering
            allowFileAccess = true
            allowContentAccess = true

            // Ensure WebView can save/form data
            saveFormData = true
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
