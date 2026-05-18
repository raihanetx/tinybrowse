package com.tinybrowse.ui.main

import android.app.Application
import android.webkit.CookieManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinybrowse.TinyBrowseApp
import com.tinybrowse.data.model.SavedSite
import com.tinybrowse.data.model.Tab
import com.tinybrowse.util.UrlUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as TinyBrowseApp).container
    private val savedSiteDao = container.savedSiteDao
    private val prefs = container.prefs

    // --- UI State ---
    data class UiState(
        val tabs: List<Tab> = listOf(Tab(id = 0)),
        val activeTabIndex: Int = 0,
        val currentUrl: String = "",
        val currentTitle: String = "New Tab",
        val isLoading: Boolean = false,
        val pageProgress: Int = 0,
        val isSecure: Boolean = true,
        val canGoBack: Boolean = false,
        val canGoForward: Boolean = false,
        val isDesktopMode: Boolean = false,
        val showStartPage: Boolean = true,
        val savedSites: List<SavedSite> = emptyList(),
        val error: String? = null,
        val isCurrentSiteSaved: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var nextTabId = 1

    init {
        // Load saved sites
        viewModelScope.launch {
            savedSiteDao.getAll().collect { sites ->
                _state.update { it.copy(savedSites = sites) }
            }
        }

        // Load desktop mode preference
        _state.update { it.copy(isDesktopMode = prefs.isDesktopMode) }
    }

    // --- Navigation ---

    fun loadUrl(url: String) {
        val finalUrl = UrlUtils.parseInput(url)
        if (finalUrl.isEmpty()) return

        _state.update {
            it.copy(
                currentUrl = finalUrl,
                isLoading = true,
                pageProgress = 0,
                error = null,
                showStartPage = false
            )
        }

        // Update active tab
        updateActiveTab(url = finalUrl)
    }

    fun search(query: String) {
        loadUrl(query)
    }

    fun goBack() {
        // WebView handles this directly via canGoBack/goBack
        // State is updated via onPageStarted callback
    }

    fun goForward() {
        // WebView handles this directly via canGoForward/goForward
    }

    fun reload() {
        _state.update { it.copy(isLoading = true, pageProgress = 0, error = null) }
    }

    // --- Page callbacks (called from WebViewClient) ---

    fun onPageStarted(url: String) {
        _state.update {
            it.copy(
                currentUrl = url,
                isLoading = true,
                pageProgress = 0,
                error = null,
                showStartPage = false
            )
        }
        checkIfSaved(url)
    }

    fun onPageFinished(url: String, title: String) {
        _state.update {
            it.copy(
                isLoading = false,
                pageProgress = 100,
                currentTitle = title.ifEmpty { UrlUtils.getDomain(url) }
            )
        }
        updateActiveTab(url = url, title = title)
    }

    fun onProgressChanged(progress: Int) {
        _state.update { it.copy(pageProgress = progress) }
    }

    fun onReceivedError(description: String) {
        _state.update { it.copy(isLoading = false, error = description) }
    }

    fun onSslStateChanged(isSecure: Boolean) {
        _state.update { it.copy(isSecure = isSecure) }
    }

    fun onCanGoBackChanged(canGoBack: Boolean) {
        _state.update { it.copy(canGoBack = canGoBack) }
    }

    fun onCanGoForwardChanged(canGoForward: Boolean) {
        _state.update { it.copy(canGoForward = canGoForward) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // --- Tabs ---

    fun createTab(incognito: Boolean = false) {
        val newTab = Tab(
            id = nextTabId++,
            isIncognito = incognito
        )
        _state.update { state ->
            state.copy(
                tabs = state.tabs + newTab,
                activeTabIndex = state.tabs.size,
                currentUrl = "",
                currentTitle = "New Tab",
                showStartPage = true,
                isLoading = false,
                pageProgress = 0,
                error = null,
                canGoBack = false,
                canGoForward = false
            )
        }
    }

    fun switchTab(index: Int) {
        val state = _state.value
        if (index < 0 || index >= state.tabs.size) return

        val tab = state.tabs[index]
        _state.update {
            it.copy(
                activeTabIndex = index,
                currentUrl = if (tab.url == "start") "" else tab.url,
                currentTitle = tab.title,
                showStartPage = tab.url == "start",
                isLoading = false,
                pageProgress = 0,
                error = null
            )
        }
    }

    fun closeTab(index: Int) {
        val state = _state.value
        val tab = state.tabs.getOrNull(index) ?: return

        // Clear cookies when closing incognito tab
        if (tab.isIncognito) {
            CookieManager.getInstance().removeAllCookies(null)
        }

        if (state.tabs.size <= 1) {
            // Last tab — create a new empty one
            createTab()
            return
        }

        val newTabs = state.tabs.toMutableList().apply { removeAt(index) }
        val newActiveIndex = when {
            index < state.activeTabIndex -> state.activeTabIndex - 1
            index == state.activeTabIndex && index >= newTabs.size -> newTabs.size - 1
            else -> state.activeTabIndex
        }.coerceIn(0, newTabs.size - 1)

        val activeTab = newTabs[newActiveIndex]
        _state.update {
            it.copy(
                tabs = newTabs,
                activeTabIndex = newActiveIndex,
                currentUrl = if (activeTab.url == "start") "" else activeTab.url,
                currentTitle = activeTab.title,
                showStartPage = activeTab.url == "start",
                isLoading = false,
                error = null
            )
        }
    }

    private fun updateActiveTab(url: String? = null, title: String? = null) {
        _state.update { state ->
            val tabs = state.tabs.toMutableList()
            val active = tabs[state.activeTabIndex]
            tabs[state.activeTabIndex] = active.copy(
                url = url ?: active.url,
                title = title ?: active.title
            )
            state.copy(tabs = tabs)
        }
    }

    // --- Desktop Mode ---

    fun toggleDesktopMode() {
        val newMode = !_state.value.isDesktopMode
        prefs.isDesktopMode = newMode
        _state.update { it.copy(isDesktopMode = newMode) }
    }

    // --- Saved Sites ---

    fun saveCurrentSite() {
        val state = _state.value
        if (state.currentUrl.isEmpty() || state.showStartPage) return

        viewModelScope.launch {
            savedSiteDao.insert(
                url = state.currentUrl,
                title = state.currentTitle
            )
            _state.update { it.copy(isCurrentSiteSaved = true) }
        }
    }

    fun deleteSavedSite(id: Long) {
        viewModelScope.launch {
            savedSiteDao.delete(id)
        }
    }

    private fun checkIfSaved(url: String) {
        viewModelScope.launch {
            val isSaved = savedSiteDao.isSaved(url)
            _state.update { it.copy(isCurrentSiteSaved = isSaved) }
        }
    }

    // --- Incognito ---

    fun clearIncognitoData() {
        // Called when incognito tab is closed
        // WebView cookie/cache clearing handled in UI layer
    }

    // --- Start Page ---

    fun showStartPage() {
        _state.update {
            it.copy(
                showStartPage = true,
                currentUrl = "",
                currentTitle = "New Tab",
                error = null
            )
        }
        updateActiveTab(url = "start", title = "New Tab")
    }
}
