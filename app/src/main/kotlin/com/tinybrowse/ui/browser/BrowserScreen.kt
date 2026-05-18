package com.tinybrowse.ui.browser

import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tinybrowse.ui.error.ErrorPage
import com.tinybrowse.ui.main.MainViewModel
import com.tinybrowse.ui.start.StartPage

@Composable
fun BrowserScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        // Tab bar (only show if more than 1 tab)
        if (state.tabs.size > 1) {
            TabBar(
                tabs = state.tabs,
                activeTabIndex = state.activeTabIndex,
                onTabClick = { viewModel.switchTab(it) },
                onTabClose = { viewModel.closeTab(it) },
                onNewTab = { viewModel.createTab() }
            )
        }

        // Toolbar
        Toolbar(
            currentUrl = state.currentUrl,
            currentTitle = state.currentTitle,
            isLoading = state.isLoading,
            isSecure = state.isSecure,
            canGoBack = state.canGoBack,
            canGoForward = state.canGoForward,
            onNavigate = { viewModel.loadUrl(it) },
            onBack = { webView?.goBack() },
            onForward = { webView?.goForward() },
            onReload = { webView?.reload() },
            onStop = { webView?.stopLoading() },
            onMenuClick = { showMenu = true }
        )

        // Progress bar
        ProgressBar(
            progress = state.pageProgress,
            isLoading = state.isLoading
        )

        // Menu dropdown
        Box {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (state.isCurrentSiteSaved) "✓ Saved" else "Save this page") },
                    onClick = {
                        viewModel.saveCurrentSite()
                        showMenu = false
                    },
                    enabled = !state.showStartPage && !state.isCurrentSiteSaved
                )

                DropdownMenuItem(
                    text = {
                        Text(if (state.isDesktopMode) "✓ Desktop mode" else "Desktop mode")
                    },
                    onClick = {
                        viewModel.toggleDesktopMode()
                        showMenu = false
                    }
                )

                DropdownMenuItem(
                    text = { Text("New tab") },
                    onClick = {
                        viewModel.createTab()
                        showMenu = false
                    }
                )

                DropdownMenuItem(
                    text = { Text("New incognito tab") },
                    onClick = {
                        viewModel.createTab(incognito = true)
                        showMenu = false
                    }
                )

                // Home — just show the start page overlay.
                // Do NOT load about:blank — it causes white screen on next navigation.
                DropdownMenuItem(
                    text = { Text("Home") },
                    onClick = {
                        viewModel.showStartPage()
                        showMenu = false
                    }
                )
            }
        }

        // === CONTENT AREA ===
        // The WebView is ALWAYS in the composition tree and ALWAYS VISIBLE.
        // StartPage and ErrorPage are OPAQUE overlays drawn on top.
        // We NEVER change the WebView's visibility — this is the key fix.
        Box(modifier = Modifier.weight(1f)) {
            // WebView — always composed, always visible
            WebViewWrapper(
                url = state.currentUrl,
                navigationId = state.navigationId,
                isDesktopMode = state.isDesktopMode,
                isIncognito = state.tabs.getOrNull(state.activeTabIndex)?.isIncognito == true,
                showStartPage = state.showStartPage,
                hasError = state.error != null,
                onPageStarted = { viewModel.onPageStarted(it) },
                onPageFinished = { url, title -> viewModel.onPageFinished(url, title) },
                onProgressChanged = { viewModel.onProgressChanged(it) },
                onReceivedError = { viewModel.onReceivedError(it) },
                onSslStateChanged = { viewModel.onSslStateChanged(it) },
                onCanGoBackChanged = { viewModel.onCanGoBackChanged(it) },
                onCanGoForwardChanged = { viewModel.onCanGoForwardChanged(it) },
                onDownloadStart = { url, ua, disposition, mime, size ->
                    com.tinybrowse.engine.DownloadHandler.onDownloadStart(
                        context, url, ua, disposition, mime, size
                    )
                },
                webViewRef = { webView = it }
            )

            // Start page — opaque overlay that FULLY covers the WebView
            if (state.showStartPage) {
                StartPage(
                    savedSites = state.savedSites,
                    onSearch = { viewModel.search(it) },
                    onSiteClick = { viewModel.loadUrl(it.url) },
                    onSiteLongClick = { viewModel.deleteSavedSite(it.id) },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }

            // Error page — opaque overlay that FULLY covers the WebView
            if (!state.showStartPage && state.error != null) {
                ErrorPage(
                    errorMessage = state.error ?: "",
                    onRetry = {
                        viewModel.clearError()
                        webView?.reload()
                    }
                )
            }
        }
    }
}
