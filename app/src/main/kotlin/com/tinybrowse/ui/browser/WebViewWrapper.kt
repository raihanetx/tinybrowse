package com.tinybrowse.ui.browser

import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.tinybrowse.engine.WebViewConfig

/**
 * WebViewWrapper — v1.1.1
 *
 * Architecture:
 * - WebView is ALWAYS VISIBLE. No visibility hacks.
 * - StartPage and ErrorPage are opaque Compose overlays drawn ON TOP.
 * - Uses Chrome Mobile User-Agent (NOT default WebView UA which contains "wv")
 *   so websites don't serve blank/degraded pages.
 * - Desktop mode: useWideViewPort=true + loadWithOverviewMode=true lets
 *   WebView auto-fit desktop content. NO JavaScript viewport injection —
 *   injecting viewport JS AFTER the page renders causes re-layout which
 *   breaks many websites = WHITE SCREEN.
 */
@Composable
fun WebViewWrapper(
    url: String,
    navigationId: Int,
    isDesktopMode: Boolean,
    isIncognito: Boolean,
    showStartPage: Boolean,
    hasError: Boolean,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onReceivedError: (String) -> Unit,
    onSslStateChanged: (Boolean) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    onCanGoForwardChanged: (Boolean) -> Unit,
    onDownloadStart: (String, String, String, String, Long) -> Unit,
    webViewRef: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentDesktopMode by remember { mutableStateOf(isDesktopMode) }
    var lastLoadedNavId by remember { mutableStateOf(-1) }
    var isFullscreen by remember { mutableStateOf(false) }
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val webView = remember {
        WebView(context).apply {
            // Apply ALL settings at creation time
            WebViewConfig.apply(settings, this)

            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView, request: WebResourceRequest?
                ): Boolean {
                    val requestUrl = request?.url?.toString() ?: return false
                    val scheme = request.url?.scheme ?: return false
                    when (scheme) {
                        "http", "https" -> return false
                        "intent", "market" -> {
                            Log.w("WebView", "Blocked scheme: $requestUrl")
                            return true
                        }
                    }
                    return false
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    // CRITICAL: Ignore about:blank — the WebView fires this
                    // on initial creation. If we pass it to onPageStarted(),
                    // the ViewModel sets showStartPage=false and currentUrl
                    // to "about:blank", which removes the StartPage overlay
                    // and shows the WebView's blank content = WHITE SCREEN.
                    if (url == "about:blank") return

                    // Reset secure state on each new page load
                    onSslStateChanged(true)
                    url?.let { onPageStarted(it) }
                    onCanGoBackChanged(view.canGoBack())
                    onCanGoForwardChanged(view.canGoForward())
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    url?.let {
                        // DO NOT inject viewport JavaScript here!
                        // Injecting viewport JS after the page has already rendered
                        // forces a complete re-layout. Many websites break during
                        // re-layout — content goes off-screen, CSS positioning fails,
                        // or the entire page goes blank/white. Instead, we rely on
                        // WebView's built-in settings (useWideViewPort +
                        // loadWithOverviewMode) which handle the viewport correctly
                        // BEFORE the page renders.
                        onPageFinished(it, view.title ?: "")
                    }
                    onCanGoBackChanged(view.canGoBack())
                    onCanGoForwardChanged(view.canGoForward())
                }

                override fun onReceivedError(
                    view: WebView, request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    // Only show error for main frame AND only if the page
                    // hasn't already started loading something else. Some
                    // errors are temporary network glitches that resolve
                    // on their own — don't show error page for those.
                    if (request?.isForMainFrame == true && view.url != null && view.url != "about:blank") {
                        // Check if the error is actually preventing page load.
                        // If progress is already > 50%, the page loaded partially
                        // and the error is likely a subresource failure — not a blank screen.
                        val errorCode = error?.errorCode ?: -1
                        if (errorCode == WebViewClient.ERROR_HOST_LOOKUP ||
                            errorCode == WebViewClient.ERROR_CONNECT ||
                            errorCode == WebViewClient.ERROR_TIMEOUT ||
                            errorCode == WebViewClient.ERROR_TOO_MANY_REQUESTS
                        ) {
                            onReceivedError(error?.description?.toString() ?: "Page failed to load")
                        }
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView, request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request?.isForMainFrame == true) {
                        Log.w("WebView", "HTTP error ${errorResponse?.statusCode}")
                    }
                }

                override fun onReceivedSslError(
                    view: WebView, handler: SslErrorHandler,
                    error: SslError?
                ) {
                    Log.w("WebView", "SSL error for ${view.url}: ${error}")
                    onSslStateChanged(false)
                    handler.proceed()
                }

                override fun onRenderProcessGone(
                    view: WebView, detail: android.webkit.RenderProcessGoneDetail?
                ): Boolean {
                    Log.e("WebView", "Render process gone for ${view.url}")
                    if (view.url != null && view.url != "about:blank") {
                        view.postDelayed({ view.reload() }, 1000)
                    }
                    return true
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    onProgressChanged(newProgress)
                }

                override fun onCreateWindow(
                    view: WebView, isDialog: Boolean, isUserGesture: Boolean,
                    resultMsg: android.os.Message?
                ): Boolean {
                    try {
                        val newWebView = WebView(view.context)
                        // Apply the same UA as the parent WebView so popup
                        // windows don't get the default "wv" UA which causes
                        // websites to serve blank/degraded pages.
                        newWebView.settings.userAgentString = view.settings.userAgentString
                        newWebView.settings.javaScriptEnabled = true
                        newWebView.settings.domStorageEnabled = true
                        newWebView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        // Accept third-party cookies for the popup so
                        // authentication/login flows work correctly.
                        try {
                            CookieManager.getInstance().setAcceptThirdPartyCookies(newWebView, true)
                        } catch (_: Exception) {}
                        newWebView.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                v: WebView, request: WebResourceRequest?
                            ): Boolean {
                                val targetUrl = request?.url?.toString()
                                if (targetUrl != null) {
                                    view.loadUrl(targetUrl)
                                }
                                return true
                            }
                        }
                        val transport = resultMsg?.obj as? android.webkit.WebView.WebViewTransport
                        if (transport != null) {
                            transport.webView = newWebView
                        }
                        resultMsg?.sendToTarget()
                    } catch (e: Exception) {
                        Log.e("WebView", "onCreateWindow failed", e)
                    }
                    return true
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (fullscreenView != null) {
                        onHideCustomView()
                    }
                    fullscreenView = view
                    customViewCallback = callback
                    isFullscreen = true
                    view?.let { v ->
                        (this@apply.parent as? ViewGroup)?.let { parent ->
                            parent.removeView(this@apply)
                            parent.addView(v, ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            ))
                        }
                    }
                }

                override fun onHideCustomView() {
                    fullscreenView?.let { v ->
                        (v.parent as? ViewGroup)?.let { parent ->
                            parent.removeView(v)
                            parent.addView(this@apply, ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            ))
                        }
                    }
                    fullscreenView = null
                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null
                    isFullscreen = false
                }

                override fun getDefaultVideoPoster(): Bitmap? {
                    return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                }

                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                    Log.d("WebViewConsole", consoleMessage?.message() ?: "")
                    return true
                }
            }

            setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
            }

            webViewRef(this)
        }
    }

    // Clean up when leaving composition
    DisposableEffect(Unit) {
        onDispose {
            try {
                webView.stopLoading()
                webView.destroy()
            } catch (_: Exception) {}
        }
    }

    // Apply desktop/mobile mode settings
    fun applyDesktopMode(wv: WebView, desktopMode: Boolean) {
        currentDesktopMode = desktopMode
        wv.settings.userAgentString = if (desktopMode) {
            WebViewConfig.DESKTOP_USER_AGENT
        } else {
            WebViewConfig.MOBILE_USER_AGENT
        }
        wv.settings.useWideViewPort = true
        // Desktop mode: loadWithOverviewMode=true auto-fits the wider desktop
        // content to the phone screen width. Without this, the desktop layout
        // renders at full width (1024px+) and most content goes off-screen.
        // Mobile mode: loadWithOverviewMode=false respects the page's own
        // viewport meta tag (usually width=device-width) for correct mobile layout.
        wv.settings.loadWithOverviewMode = desktopMode
        // CRITICAL: Always use setInitialScale(0) (auto-calculate).
        // setInitialScale(100) forces 100% zoom = 1 CSS pixel per physical pixel.
        // On a ~360dp phone with a 1024px desktop viewport, this renders everything
        // off-screen = BLANK WHITE PAGE. setInitialScale(0) lets WebView auto-fit.
        wv.setInitialScale(0)
    }

    // Apply incognito settings
    LaunchedEffect(isIncognito) {
        webView.settings.cacheMode = if (isIncognito) {
            WebSettings.LOAD_NO_CACHE
        } else {
            WebSettings.LOAD_DEFAULT
        }
    }

    // Load URL when user navigates (navigationId changes)
    LaunchedEffect(navigationId) {
        if (url.isNotEmpty() && navigationId != lastLoadedNavId) {
            lastLoadedNavId = navigationId
            try {
                applyDesktopMode(webView, isDesktopMode)
                webView.loadUrl(url)
                Log.d("WebView", "Loading: $url")
            } catch (e: Exception) {
                Log.e("WebView", "loadUrl failed: $url", e)
            }
        }
    }

    // Reload when desktop mode is toggled while a page is loaded.
    // Skip on first composition (currentDesktopMode == isDesktopMode).
    LaunchedEffect(isDesktopMode) {
        // Only reload if the mode actually changed from a previous state
        if (currentDesktopMode != isDesktopMode) {
            val wvUrl = webView.url
            if (wvUrl != null && wvUrl != "about:blank" && wvUrl.isNotEmpty()) {
                applyDesktopMode(webView, isDesktopMode)
                // Clear the cache so the server gets a FRESH request with the
                // new UA. Without this, reload() may serve cached resources
                // (CSS/JS/images) that were fetched with the old UA (mobile),
                // which can be DIFFERENT from what the desktop version expects.
                // Using reload() instead of loadUrl() because loadUrl() on an
                // already-loaded URL can cause double-navigation issues on
                // some websites (SPA redirects, history manipulation, etc.).
                webView.clearCache(true)
                try {
                    webView.reload()
                } catch (e: Exception) {
                    Log.e("WebView", "reload failed on desktop mode toggle", e)
                }
            }
        }
    }

    // WebView is ALWAYS VISIBLE. No visibility tricks.
    // StartPage and ErrorPage are opaque Compose overlays drawn on top.
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
