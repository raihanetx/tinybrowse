package com.tinybrowse.ui.browser

import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
 * WebViewWrapper — COMPLETELY REWRITTEN for v1.0.7
 *
 * Previous versions (v1.0.0–v1.0.6) tried to "fix" the white screen by
 * hiding the WebView with INVISIBLE/GONE states, using hasPageStartedLoading
 * flags, and other visibility tricks. This was WRONG — it caused the WebView
 * to never show up on some devices because the flag/state got out of sync.
 *
 * The correct approach: The WebView is ALWAYS VISIBLE. Always. No exceptions.
 * StartPage and ErrorPage are opaque Compose overlays drawn ON TOP of it.
 * The WebView is never hidden, never gone, never set to INVISIBLE.
 * It just works — like every other Android browser.
 *
 * For sites that show white: the issue was likely the WebView User-Agent
 * identifying itself as a WebView, causing some sites to serve blank pages.
 * Now we use a mobile Chrome UA that doesn't reveal it's a WebView.
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

                    // Only block truly unsupported schemes
                    // DO NOT block http/https — let WebView handle all web navigation
                    val scheme = request?.url?.scheme ?: return false
                    when (scheme) {
                        "http", "https" -> return false  // Always allow web URLs
                        "intent" -> {
                            Log.w("WebView", "Blocked intent:// scheme: $requestUrl")
                            return true
                        }
                        "market" -> {
                            Log.w("WebView", "Blocked market:// scheme: $requestUrl")
                            return true
                        }
                    }
                    // Let other schemes through or block as needed
                    return false
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    url?.let { onPageStarted(it) }
                    onCanGoBackChanged(view.canGoBack())
                    onCanGoForwardChanged(view.canGoForward())
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    url?.let {
                        if (currentDesktopMode) {
                            view.evaluateJavascript(WebViewConfig.DESKTOP_VIEWPORT_JS, null)
                        }
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
                    if (request?.isForMainFrame == true) {
                        onReceivedError(error?.description?.toString() ?: "Page failed to load")
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView, request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request?.isForMainFrame == true) {
                        Log.w("WebView", "HTTP error ${errorResponse?.statusCode} for $request")
                    }
                }

                override fun onReceivedSslError(
                    view: WebView, handler: SslErrorHandler,
                    error: SslError?
                ) {
                    Log.w("WebView", "SSL error for ${view.url}: ${error?.toString()}")
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
                        newWebView.settings.javaScriptEnabled = true
                        newWebView.settings.domStorageEnabled = true
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
                webView.settings.javaScriptEnabled = false
                webView.loadUrl("about:blank")
                webView.freeMemory()
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
        wv.settings.loadWithOverviewMode = false
        wv.setInitialScale(if (desktopMode) 100 else 0)
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

    // Reload when desktop mode is toggled while a page is loaded
    LaunchedEffect(isDesktopMode) {
        val wvUrl = webView.url
        if (wvUrl != null && wvUrl != "about:blank" && wvUrl.isNotEmpty()) {
            applyDesktopMode(webView, isDesktopMode)
            webView.reload()
        }
    }

    // THE KEY CHANGE: WebView is ALWAYS VISIBLE. No visibility tricks.
    // StartPage and ErrorPage are opaque Compose overlays drawn on top.
    // This eliminates ALL the state-sync bugs that caused white screens.
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
            // NO update block needed — we never change WebView visibility
        )
    }
}
