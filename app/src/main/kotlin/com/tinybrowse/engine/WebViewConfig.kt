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

            // Viewport
            useWideViewPort = true
            loadWithOverviewMode = true

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
}
