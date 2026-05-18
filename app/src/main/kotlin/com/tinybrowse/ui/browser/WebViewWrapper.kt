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
import kotlinx.coroutines.delay

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

    // Track last loaded navigationId to avoid duplicate loads
    var lastLoadedNavId by remember { mutableStateOf(-1) }

    val webView = remember {
        WebView(context).apply {
            // Apply all settings including cookies and debugging
            WebViewConfig.apply(settings, this)

            // Set background to app-like color (not white, not transparent)
            // This prevents the jarring "white screen" flash when WebView
            // is visible but hasn't rendered content yet
            setBackgroundColor(Color.parseColor("#FAFAFA"))

            // Keep WebView focusable for video interaction
            isFocusable = true
            isFocusableInTouchMode = true

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

    // Clean up WebView when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            try {
                webView.stopLoading()
                webView.settings.javaScriptEnabled = false
                webView.destroy()
            } catch (e: Exception) {
                Log.w("WebView", "Error destroying WebView", e)
            }
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

    // Load URL when navigationId changes — with safety checks and retry
    LaunchedEffect(navigationId) {
        if (url.isNotEmpty() && navigationId != lastLoadedNavId) {
            lastLoadedNavId = navigationId
            try {
                applyDesktopMode(webView, isDesktopMode)

                // CRITICAL: Make sure WebView is attached before loading.
                // If WebView has no parent, it's not in the layout tree yet.
                // Post the loadUrl to the next frame to ensure attachment.
                if (webView.parent != null) {
                    webView.loadUrl(url)
                } else {
                    // WebView not yet attached — post to next frame
                    webView.post { webView.loadUrl(url) }
                }

                Log.d("WebView", "Loading URL: $url (navId=$navigationId)")
            } catch (e: Exception) {
                Log.e("WebView", "Failed to load URL: $url", e)
                // Retry after a short delay
                try {
                    delay(500)
                    webView.loadUrl(url)
                    Log.d("WebView", "Retry loading URL: $url")
                } catch (e2: Exception) {
                    Log.e("WebView", "Retry also failed for URL: $url", e2)
                    onReceivedError("Failed to load page: ${e2.message}")
                }
            }
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

    // Fallback timeout: if page hasn't started loading after 15 seconds, retry once
    LaunchedEffect(navigationId) {
        if (url.isNotEmpty() && navigationId > 0) {
            delay(15_000)
            // If WebView is still showing about:blank or the URL doesn't match
            val currentWvUrl = webView.url
            if (currentWvUrl == null || currentWvUrl == "about:blank" || currentWvUrl.isEmpty()) {
                Log.w("WebView", "Page load timeout, retrying: $url")
                try {
                    webView.loadUrl(url)
                } catch (e: Exception) {
                    Log.e("WebView", "Timeout retry failed", e)
                }
            }
        }
    }

    // SIMPLIFIED VISIBILITY LOGIC:
    // - WebView is ALWAYS VISIBLE when the start page is not showing and there's no error.
    // - The StartPage and ErrorPage overlays are drawn ON TOP of the WebView.
    // - No more `hasPageStartedLoading` flag that was causing persistent white screens
    //   when onPageStarted didn't fire for about:blank on some devices.
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize(),
            update = { wv ->
                // Simple visibility: show WebView when not on start page and no error
                val shouldShow = !showStartPage && !hasError
                val targetVisibility = if (shouldShow) View.VISIBLE else View.GONE
                if (wv.visibility != targetVisibility) {
                    wv.visibility = targetVisibility
                }
            }
        )
    }
}
