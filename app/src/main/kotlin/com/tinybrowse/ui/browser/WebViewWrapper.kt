package com.tinybrowse.ui.browser

import android.graphics.Bitmap
import android.graphics.Color
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.tinybrowse.engine.WebViewConfig

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
    var isFullscreen by remember { mutableStateOf(false) }
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // CRITICAL FIX: Track whether a page has actually started loading.
    // We keep the WebView INVISIBLE until onPageStarted fires for the
    // first time. This prevents the white about:blank page from being
    // visible during the gap between navigating away from the start page
    // and the URL actually starting to load in the WebView.
    var hasPageStartedLoading by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            // Apply all settings including cookies and debugging
            WebViewConfig.apply(settings, this)

            // CRITICAL FIX: Set background to transparent to reduce white flash.
            // The default white background is very jarring and makes it look
            // like the page is "blank" before content renders.
            setBackgroundColor(Color.TRANSPARENT)

            // Keep WebView focusable for video interaction
            isFocusable = true
            isFocusableInTouchMode = true

            // Start INVISIBLE — we only make it visible after a page
            // starts loading, to prevent the white about:blank flash
            visibility = View.INVISIBLE

            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView, request: WebResourceRequest?
                ): Boolean {
                    val requestUrl = request?.url?.toString() ?: return false

                    // Block unsupported schemes that would cause blank screen
                    if (requestUrl.startsWith("intent://") ||
                        requestUrl.startsWith("market://") ||
                        requestUrl.startsWith("tel:") ||
                        requestUrl.startsWith("mailto:")
                    ) {
                        Log.w("WebView", "Blocked unsupported scheme: $requestUrl")
                        return true
                    }

                    return false
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    // CRITICAL: Mark that a page has started loading.
                    // This triggers the WebView to become visible.
                    hasPageStartedLoading = true

                    url?.let { onPageStarted(it) }
                    onCanGoBackChanged(view.canGoBack())
                    onCanGoForwardChanged(view.canGoForward())
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    url?.let {
                        // Inject viewport override JS on every page load
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
                    Log.e("WebView", "Render process gone for ${view.url}, crashing=${detail?.didCrash()}")
                    if (view.url != null && view.url != "about:blank") {
                        view.postDelayed({ view.reload() }, 500)
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
                            resultMsg?.sendToTarget()
                        } else {
                            resultMsg?.sendToTarget()
                        }
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

    // Apply desktop/mobile WebView settings
    fun applyDesktopMode(wv: WebView, desktopMode: Boolean) {
        currentDesktopMode = desktopMode
        wv.settings.userAgentString = if (desktopMode) {
            WebViewConfig.DESKTOP_USER_AGENT
        } else {
            WebSettings.getDefaultUserAgent(context)
        }
        wv.settings.useWideViewPort = true
        wv.settings.loadWithOverviewMode = false

        if (desktopMode) {
            wv.setInitialScale(100)
        } else {
            wv.setInitialScale(0)
        }
    }

    // Apply incognito settings
    LaunchedEffect(isIncognito) {
        if (isIncognito) {
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        } else {
            webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        }
    }

    // Load URL when navigationId changes
    LaunchedEffect(navigationId) {
        if (url.isNotEmpty()) {
            applyDesktopMode(webView, isDesktopMode)
            webView.loadUrl(url)
        }
    }

    // When desktop mode is toggled while a page is already loaded, reload
    LaunchedEffect(isDesktopMode) {
        val wvUrl = webView.url
        if (wvUrl != null && wvUrl != "about:blank" && wvUrl.isNotEmpty()) {
            applyDesktopMode(webView, isDesktopMode)
            webView.reload()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize(),
            update = { wv ->
                // CRITICAL FIX: WebView visibility logic.
                // We keep the WebView INVISIBLE in these cases:
                // 1. When the start page is showing (no page loaded yet)
                // 2. When an error page is showing
                // 3. When a URL has been requested but onPageStarted hasn't
                //    fired yet (prevents white about:blank flash)
                //
                // The WebView becomes VISIBLE only when:
                // - The start page is NOT showing
                // - There's no error overlay
                // - onPageStarted has fired (page is actually loading)
                val shouldShowWebView = !showStartPage && !hasError && hasPageStartedLoading
                val targetVisibility = if (shouldShowWebView) View.VISIBLE else View.INVISIBLE
                if (wv.visibility != targetVisibility) {
                    wv.visibility = targetVisibility
                }
            }
        )
    }
}
