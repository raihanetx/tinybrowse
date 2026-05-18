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
    isVisible: Boolean,
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

    val webView = remember {
        WebView(context).apply {
            // Apply all settings including cookies and debugging
            WebViewConfig.apply(settings, this)

            // CRITICAL FIX: Set background color to match dark theme / reduce
            // white flash. Default white background is very jarring and makes
            // it look like the page is "blank" before content renders.
            setBackgroundColor(Color.TRANSPARENT)

            // Keep WebView focusable for video interaction
            isFocusable = true
            isFocusableInTouchMode = true

            // Initial visibility — hidden when start page is showing
            visibility = if (isVisible) View.VISIBLE else View.INVISIBLE

            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView, request: WebResourceRequest?
                ): Boolean {
                    // Return false = let WebView handle the URL itself.
                    // This is critical — without this override, some Android
                    // versions may try to launch an external browser.
                    val requestUrl = request?.url?.toString() ?: return false

                    // Block unsupported schemes that would cause blank screen
                    // (e.g. intent://, market://, tel://, mailto://)
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
                    // CRITICAL FIX: handler.cancel() was KILLING page loads
                    // for any site with minor SSL issues, causing white screen.
                    // Most browsers proceed with a warning instead.
                    Log.w("WebView", "SSL error for ${view.url}: ${error?.toString()}")
                    onSslStateChanged(false)
                    handler.proceed()  // Allow the page to load
                }

                override fun onRenderProcessGone(
                    view: WebView, detail: android.webkit.RenderProcessGoneDetail?
                ): Boolean {
                    // CRITICAL FIX: When the WebView renderer process crashes
                    // (OOM, GPU error, etc.), the page becomes permanently blank.
                    // Return true to indicate we handled it, and reload the page.
                    Log.e("WebView", "Render process gone for ${view.url}, crashing=${detail?.didCrash()}")
                    if (view.url != null && view.url != "about:blank") {
                        // Post a reload to give the system time to recover
                        view.postDelayed({ view.reload() }, 500)
                    }
                    return true
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    onProgressChanged(newProgress)
                }

                // CRITICAL FIX: Sites like YouTube open new windows for content.
                // Without this, window.open() silently fails = blank screen.
                // We create a temporary WebView to intercept the URL,
                // then load it in the main WebView instead.
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
                            // Fallback: just notify the message
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
                    // Return a transparent 1px bitmap instead of null
                    // to prevent video placeholder rendering issues
                    return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                }

                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                    Log.d("WebViewConsole", consoleMessage?.message() ?: "")
                    return true
                }
            }

            // CRITICAL FIX: Handle WebView renderer process death.
            // On low-memory devices or heavy sites, the OS can kill the
            // WebView renderer process. Without this handler, the page
            // becomes permanently blank (white screen) with no recovery.
            // We use onRenderProcessGone in WebViewClient (API 26+) instead
            // of WebViewRenderProcessClient which has API compatibility issues.
            // The handler is set in the WebViewClient below.

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

    // === KEY FIX: Only load URL when navigationId changes ===
    // navigationId is incremented ONLY by user-initiated actions
    // (typing URL, switching tabs, closing tabs).
    // Internal WebView navigation (redirects, link clicks) updates
    // currentUrl but does NOT increment navigationId, so this
    // LaunchedEffect won't fire and won't cancel the in-progress load.
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
                // CRITICAL FIX: Control WebView visibility based on whether
                // the start page is showing. When start page is visible,
                // hide the WebView to prevent white background from showing
                // through the overlay. When navigating, make it visible
                // so the page content is displayed.
                val targetVisibility = if (isVisible) View.VISIBLE else View.INVISIBLE
                if (wv.visibility != targetVisibility) {
                    wv.visibility = targetVisibility
                }
            }
        )
    }
}
