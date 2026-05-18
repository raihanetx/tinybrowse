package com.tinybrowse.ui.browser

import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }

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
                // Save site
                DropdownMenuItem(
                    text = { Text(if (state.isCurrentSiteSaved) "✓ Saved" else "Save this page") },
                    onClick = {
                        viewModel.saveCurrentSite()
                        showMenu = false
                    },
                    enabled = !state.showStartPage && !state.isCurrentSiteSaved
                )

                // Desktop mode toggle
                DropdownMenuItem(
                    text = {
                        Text(if (state.isDesktopMode) "✓ Desktop mode" else "Desktop mode")
                    },
                    onClick = {
                        // Just toggle the ViewModel state — WebViewWrapper's
                        // LaunchedEffect(isDesktopMode) will apply settings + reload
                        viewModel.toggleDesktopMode()
                        showMenu = false
                    }
                )

                // New tab
                DropdownMenuItem(
                    text = { Text("New tab") },
                    onClick = {
                        viewModel.createTab()
                        showMenu = false
                    }
                )

                // New incognito tab
                DropdownMenuItem(
                    text = { Text("New incognito tab") },
                    onClick = {
                        viewModel.createTab(incognito = true)
                        showMenu = false
                    }
                )

                // Home
                DropdownMenuItem(
                    text = { Text("Home") },
                    onClick = {
                        viewModel.showStartPage()
                        webView?.loadUrl("about:blank")
                        showMenu = false
                    }
                )
            }
        }

        // Content area
        Box(modifier = Modifier.weight(1f)) {
            when {
                // Show start page
                state.showStartPage -> {
                    StartPage(
                        savedSites = state.savedSites,
                        onSearch = { viewModel.search(it) },
                        onSiteClick = { viewModel.loadUrl(it.url) },
                        onSiteLongClick = { viewModel.deleteSavedSite(it.id) }
                    )
                }

                // Show error page
                state.error != null -> {
                    ErrorPage(
                        errorMessage = state.error ?: "",
                        onRetry = {
                            viewModel.clearError()
                            webView?.reload()
                        }
                    )
                }

                // Show WebView
                else -> {
                    WebViewWrapper(
                        url = state.currentUrl,
                        isDesktopMode = state.isDesktopMode,
                        isIncognito = state.tabs.getOrNull(state.activeTabIndex)?.isIncognito == true,
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
                }
            }
        }
    }
}
