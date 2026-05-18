package com.tinybrowse.engine

import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * Static WebView configuration. Applied once when WebView is created.
 */
object WebViewConfig {

    /**
     * Mobile Chrome User-Agent — does NOT identify as a WebView.
     *
     * CRITICAL: The default Android WebView UA contains "wv" or "Version/4.0"
     * which many websites detect as a WebView browser. Sites like Facebook,
     * Instagram, Twitter, banking sites, and many others serve BLANK or
     * degraded pages to WebView UAs. By using a standard Chrome Mobile UA,
     * these sites serve their full content like they would to Chrome.
     */
    const val MOBILE_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.113 Mobile Safari/537.36"

    /**
     * Desktop Chrome User-Agent — full Chrome on Linux for desktop mode.
     */
    const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.113 Safari/537.36"

    /**
     * Apply WebView settings and cookie configuration.
     * Called once when the WebView is created.
     */
    fun apply(settings: WebSettings, webView: WebView? = null) {
        settings.apply {
            // === PERFORMANCE ===
            cacheMode = WebSettings.LOAD_DEFAULT

            // === JAVASCRIPT — required for ALL modern websites ===
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true

            // === IMAGES — must be enabled for page rendering ===
            loadsImagesAutomatically = true
            blockNetworkImage = false

            // === VIEWPORT ===
            useWideViewPort = true
            loadWithOverviewMode = false

            // === MEDIA — allow autoplay for YouTube, video sites ===
            mediaPlaybackRequiresUserGesture = false

            // === MIXED CONTENT — allow HTTP resources on HTTPS pages ===
            // Many CDNs serve fonts/scripts/images over HTTP even when
            // the main page is HTTPS. Blocking these = blank page.
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            // === USER AGENT ===
            // Set mobile Chrome UA by default — this is THE KEY FIX for
            // sites that show blank/white screen in WebView browsers.
            // The default WebView UA contains "wv" which sites detect.
            userAgentString = MOBILE_USER_AGENT

            // === WINDOWS / POPUPS ===
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true

            // === ZOOM ===
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // === TEXT ===
            textZoom = 100

            // === FILE ACCESS ===
            allowFileAccess = true
            allowContentAccess = true
        }

        // === COOKIES — must accept all cookies for sites to function ===
        webView?.let {
            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(it, true)
            } catch (e: Exception) {
                // Non-fatal on some WebView implementations
            }
        }

        // Enable WebView debugging
        WebView.setWebContentsDebuggingEnabled(true)
    }

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
}
