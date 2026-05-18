# 🦁 TinyBrowse — V1 Architecture

> A lightweight Android browser for 4GB RAM mid-range devices.
> Kotlin + Jetpack Compose. V1 = basic features only. Optimization comes in V2.

---

## V1 Features (Final)

| # | Feature | Description |
|---|---|---|
| 1 | **Search bar** | Type URL → navigate. Type text → search DuckDuckGo. |
| 2 | **Back / Forward / Refresh** | Standard navigation buttons. |
| 3 | **Save sites** | Flat list of saved URLs. Tap to open, long-press to delete. No folders. |
| 4 | **Desktop mode toggle** | ON/OFF switch in menu. Sends desktop user-agent. |
| 5 | **Tabs** | Multiple tabs. Tab bar visible. Switch, create, close tabs. |
| 6 | **Incognito mode** | Separate session. No history, no cookies saved. |
| 7 | **SSL indicator** | Lock icon 🔒 for HTTPS, warning ⚠️ for HTTP. |
| 8 | **Progress bar** | Thin bar showing page load progress. |
| 9 | **Start page** | Search bar in center + list of saved sites below it. |
| 10 | **Error page** | Simple error message + retry button when page fails to load. |
| 11 | **Download handling** | Intercept downloads → hand to Android system DownloadManager. |

**NOT in V1:** History, Settings screen, Ad blocker, Image compression, Smart optimizations.

---

## Tech Stack

| Component | Choice | Why |
|---|---|---|
| Language | Kotlin | Required |
| UI | Jetpack Compose + Material3 | Required |
| WebView | Android WebView (Chromium) | Only option on Android |
| Database | SQLite (raw, no Room) | 2 tables, no need for Room overhead |
| DI | Manual (AppContainer) | No Hilt/Dagger — too heavy for this scope |
| Images | No library | Favicons stored as byte arrays in memory |
| Navigation | Manual backstack | No Navigation component — overkill |

---

## Project Structure

```
app/src/main/kotlin/com/tinybrowse/
├── TinyBrowseApp.kt                  # Application class
├── AppContainer.kt                   # Manual DI — all dependencies here
│
├── data/
│   ├── db/
│   │   ├── BrowseDatabase.kt         # SQLiteOpenHelper
│   │   └── SavedSiteDao.kt           # Save/delete/get saved sites
│   ├── model/
│   │   ├── Tab.kt                    # Tab state: url, title, isIncognito
│   │   └── SavedSite.kt             # Data class: id, url, title, favicon
│   └── prefs/
│       └── PrefsManager.kt          # SharedPreferences wrapper (desktop mode default, etc.)
│
├── engine/
│   ├── BrowserEngine.kt             # WebView wrapper — load, back, forward, reload
│   ├── WebViewConfig.kt             # Static WebView settings (cache, JS, etc.)
│   └── DownloadHandler.kt           # WebViewClient download listener → system DownloadManager
│
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                 # Material3 theme
│   │   ├── Color.kt                 # Static colors
│   │   └── Type.kt                  # System font only
│   ├── main/
│   │   ├── MainActivity.kt          # Single activity, hosts Compose
│   │   └── MainViewModel.kt         # Single ViewModel for entire app
│   ├── browser/
│   │   ├── BrowserScreen.kt         # WebView + toolbar + tab bar
│   │   ├── Toolbar.kt               # URL bar, back, forward, refresh, menu
│   │   ├── TabBar.kt                # Horizontal tab strip
│   │   └── WebViewWrapper.kt        # AndroidView composable wrapping WebView
│   ├── start/
│   │   └── StartPage.kt             # Search bar + saved sites grid
│   ├── saved/
│   │   └── SavedSitesScreen.kt      # Full list of saved sites
│   ├── incognito/
│   │   └── IncognitoScreen.kt       # Same as BrowserScreen but no persistence
│   └── error/
│       └── ErrorPage.kt             # Simple error composable with retry
│
└── util/
    ├── UrlUtils.kt                  # URL validation, search URL builder
    └── MemoryMonitor.kt             # Basic memory tracking (log only in V1)
```

**Total: ~20 files.** That's it. Small, focused, easy to understand.

---

## Database Schema

```sql
-- Only ONE table in V1. No history.
CREATE TABLE saved_sites (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    url TEXT NOT NULL,
    title TEXT NOT NULL DEFAULT '',
    favicon BLOB,                     -- Small icon bytes (max 5KB)
    created_at INTEGER NOT NULL       -- Unix timestamp
);

CREATE INDEX idx_saved_sites_url ON saved_sites(url);
```

**No history table in V1.** Added in V2.

---

## How Each Feature Works

### 1. Search Bar

```
User types: "cats"
  → Contains spaces, no dots → SEARCH
  → URL: https://duckduckgo.com/?q=cats

User types: "youtube.com"
  → Contains dot, no spaces → NAVIGATE
  → URL: https://youtube.com

User types: "https://youtube.com"
  → Already a URL → NAVIGATE directly
```

### 2. Back / Forward / Refresh

```kotlin
// Direct WebView calls — nothing fancy
webView.goBack()      // if canGoBack()
webView.goForward()   // if canGoForward()
webView.reload()
```

### 3. Save Sites

```
Save:  Toolbar menu → "Save this page"
       → INSERT into saved_sites (url, title)

View:  Start page shows saved sites below search bar
       → SELECT * FROM saved_sites ORDER BY created_at DESC

Open:  Tap → loadUrl(savedSite.url)

Delete: Long press → "Remove"
        → DELETE FROM saved_sites WHERE id = ?
```

### 4. Desktop Mode

```kotlin
// Toggle in toolbar menu
fun setDesktopMode(enabled: Boolean) {
    webView.settings.userAgentString = if (enabled) {
        "Mozilla/5.0 (X11; Linux x86_64) Chrome/120.0.0.0"
    } else {
        WebSettings.getDefaultUserAgent(context)
    }
    webView.settings.useWideViewPort = enabled
    webView.reload()
}
```

### 5. Tabs

```
Tab bar at bottom (horizontal scroll):
┌──────┬──────┬──────┬────┐
│ Tab1 │ Tab2 │ Tab3 │ +  │
└──────┴──────┴──────┴────┘

State management:
  - List<Tab> in ViewModel
  - Each Tab has: id, url, title, isIncognito
  - Active tab index tracked in ViewModel

Tab operations:
  Create:  + button → new Tab with default start page URL
  Switch:  Tap tab → update active index → loadUrl(tab.url)
  Close:   Long press tab → "Close" → remove from list
           If last tab → create new empty tab

WebView handling:
  - ONE WebView instance, reused across tabs
  - Switching tabs: save current tab's state, load new tab's URL
  - No suspend/resume in V1 (just URL-based switching)
```

### 6. Incognito Mode

```
How it works:
  - Opens new BrowserScreen with isIncognito = true
  - WebView uses separate CookieManager (or clears cookies after)
  - No save-site allowed (hide save button)
  - When incognito closes → clear cookies, clear cache for that session

Implementation:
  Toolbar menu → "New incognito tab"
  → New tab with isIncognito = true
  → Tab bar shows incognito icon on that tab
  → Close incognito tab → CookieManager.getInstance().removeAllCookies()
```

### 7. SSL Indicator

```kotlin
// In WebViewClient
override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
    // Show warning icon in toolbar
    // Don't proceed automatically — let user decide (V1: just show warning)
    handler.cancel()
}
```

### 8. Progress Bar

```kotlin
// In WebChromeClient
override fun onProgressChanged(view: WebView, newProgress: Int) {
    // Update Compose state: progress = newProgress / 100f
    // Show LinearProgressIndicator(progress) at top of screen
    // Hide when progress == 100
}
```

### 9. Start Page

```
┌─────────────────────────────────┐
│                                 │
│     🔍 Search DuckDuckGo       │  ← Centered search bar
│                                 │
├─────────────────────────────────┤
│  Saved Sites                    │
│  ┌─────┐ ┌─────┐ ┌─────┐      │
│  │ 🔗  │ │ 🔗  │ │ 🔗  │      │  ← Saved sites as cards
│  │YouTu│ │Reddi│ │Wiki │      │
│  └─────┘ └─────┘ └─────┘      │
│  ┌─────┐ ┌─────┐               │
│  │ 🔗  │ │ 🔗  │               │
│  │GitHu│ │News │               │
│  └─────┘ └─────┘               │
└─────────────────────────────────┘
```

### 10. Error Page

```
┌─────────────────────────────────┐
│                                 │
│         ⚠️                      │
│                                 │
│    Can't reach this page        │
│    Check your internet          │
│    connection and try again     │
│                                 │
│       [ Try Again ]             │  ← webView.reload()
│                                 │
└─────────────────────────────────┘
```

### 11. Download Handling

```kotlin
// In WebViewClient
override fun onDownloadStart(
    url: String, userAgent: String, contentDisposition: String,
    mimeType: String, contentLength: Long
) {
    val request = DownloadManager.Request(Uri.parse(url))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setMimeType(mimeType)
        .setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
    
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    dm.enqueue(request)
}
```

---

## Single ViewModel

```kotlin
// MainViewModel.kt — One ViewModel for the entire app
class MainViewModel : ViewModel() {

    // --- UI State ---
    data class UiState(
        val tabs: List<Tab> = listOf(Tab(id = 0, url = "start", title = "New Tab")),
        val activeTabIndex: Int = 0,
        val isIncognito: Boolean = false,
        val isDesktopMode: Boolean = false,
        val pageProgress: Int = 0,           // 0-100
        val isLoading: Boolean = false,
        val isSecure: Boolean = true,        // SSL
        val currentUrl: String = "",
        val currentTitle: String = "",
        val canGoBack: Boolean = false,
        val canGoForward: Boolean = false,
        val error: String? = null,
        val savedSites: List<SavedSite> = emptyList(),
        val showStartPage: Boolean = true,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Actions
    fun search(query: String) { ... }
    fun loadUrl(url: String) { ... }
    fun goBack() { ... }
    fun goForward() { ... }
    fun reload() { ... }
    fun toggleDesktopMode() { ... }
    fun saveCurrentSite() { ... }
    fun deleteSavedSite(id: Long) { ... }
    fun createTab() { ... }
    fun switchTab(index: Int) { ... }
    fun closeTab(index: Int) { ... }
    fun toggleIncognito() { ... }
    fun clearError() { ... }
}
```

**One ViewModel. One StateFlow. Compose observes it. That's the entire architecture.**

---

## Build Config

```kotlin
android {
    compileSdk = 35
    defaultConfig {
        minSdk = 26        // Android 8.0
        targetSdk = 35
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    // That's it. Nothing else.
}
```

---

## File Count

```
Total Kotlin files:  ~20
Total lines of code: ~1500-2000 (estimate)
APK size target:     < 5MB
Dependencies:        7 (all Compose/AndroidX)
```

---

_V1 Architecture — Final. Ready to build._
