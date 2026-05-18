package com.tinybrowse.ui.browser

import android.graphics.Bitmap
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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
    isDesktopMode: Boolean,
    isIncognito: Boolean,
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

    var currentLoadedUrl by remember { mutableStateOf("") }

    val webView = remember {
        WebView(context).apply {
            WebViewConfig.apply(settings)

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    url?.let { onPageStarted(it) }
                    onCanGoBackChanged(view.canGoBack())
                    onCanGoForwardChanged(view.canGoForward())
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    url?.let { onPageFinished(it, view.title ?: "") }
                    onCanGoBackChanged(view.canGoBack())
                    onCanGoForwardChanged(view.canGoForward())
                }

                override fun onReceivedError(
                    view: WebView, request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        onReceivedError(error?.description?.toString() ?: "Page failed to load")
                    }
                }

                override fun onReceivedSslError(
                    view: WebView, handler: SslErrorHandler,
                    error: android.net.http.SslError?
                ) {
                    onSslStateChanged(false)
                    handler.cancel()
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    onProgressChanged(newProgress)
                }
            }

            setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
            }

            webViewRef(this)
        }
    }

    // Apply user-agent based on desktop mode
    fun applyDesktopModeSettings(wv: WebView, desktopMode: Boolean) {
        wv.settings.userAgentString = if (desktopMode) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        } else {
            WebSettings.getDefaultUserAgent(context)
        }
        wv.settings.useWideViewPort = true  // always true for proper rendering
        wv.settings.loadWithOverviewMode = desktopMode
    }

    // Apply incognito settings
    LaunchedEffect(isIncognito) {
        if (isIncognito) {
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webView.settings.databaseEnabled = false
        } else {
            webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        }
    }

    // Load URL reactively — always apply user-agent BEFORE loading
    LaunchedEffect(url) {
        if (url.isNotEmpty()) {
            applyDesktopModeSettings(webView, isDesktopMode)
            webView.loadUrl(url)
            currentLoadedUrl = url
        }
    }

    // When desktop mode is toggled while a page is already loaded, reload it
    LaunchedEffect(isDesktopMode) {
        if (currentLoadedUrl.isNotEmpty()) {
            applyDesktopModeSettings(webView, isDesktopMode)
            webView.reload()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
