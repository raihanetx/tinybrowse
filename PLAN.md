# TinyBrowse V1 — Execution Plan

## Rules
- One step at a time. Don't move to next until current is done.
- Every step produces a compilable state.
- Test each step before proceeding.
- Production-ready from day one (ProGuard, proper error handling, clean code).

---

## Step 1: Project Scaffold
Create Android project structure, Gradle config, dependencies, app class.
**Output:** Empty app that compiles and shows a blank screen.

## Step 2: Data Layer
SQLite database, SavedSite model, SavedSiteDao, PrefsManager.
**Output:** Data layer that can save/retrieve/delete sites. Testable independently.

## Step 3: UI Theme
Material3 theme, colors, typography. System fonts only. No custom fonts.
**Output:** Consistent visual foundation for all screens.

## Step 4: AppContainer + MainViewModel
Manual DI container. Single ViewModel with all UI state. StateFlow.
**Output:** ViewModel ready to connect to UI.

## Step 5: Start Page
Search bar (centered) + saved sites grid. DuckDuckGo search.
**Output:** Start page renders, search works, saved sites show.

## Step 6: Browser Screen + WebView
WebView wrapper in Compose. Toolbar (URL, back, forward, refresh). Progress bar. SSL indicator.
**Output:** Can browse websites. Back/forward work. Progress shows.

## Step 7: Tab Management
Tab bar at bottom. Create, switch, close tabs. One WebView reused.
**Output:** Multiple tabs work. Tab switching works.

## Step 8: Save Sites
Save current page from toolbar menu. Shows on start page. Delete on long-press.
**Output:** Full save/unsave flow works.

## Step 9: Desktop Mode
Toggle in menu. Switches user-agent. Persists in SharedPreferences.
**Output:** Desktop mode toggle works, persists across restarts.

## Step 10: Incognito Mode
New incognito tab. No cookie persistence. Clear on close.
**Output:** Incognito browsing works, data cleared on close.

## Step 11: Error Handling + Downloads
Error page with retry. Download interception → system DownloadManager.
**Output:** Errors handled gracefully. Downloads work.

## Step 12: Polish + Release
ProGuard rules. Edge cases. Memory cleanup. Final testing. APK audit.
**Output:** Production-ready APK.

---

## Status
- [x] Step 1: Project Scaffold
- [x] Step 2: Data Layer
- [x] Step 3: UI Theme
- [x] Step 4: AppContainer + MainViewModel
- [x] Step 5: Start Page
- [x] Step 6: Browser Screen + WebView
- [x] Step 7: Tab Management
- [x] Step 8: Save Sites
- [x] Step 9: Desktop Mode
- [x] Step 10: Incognito Mode
- [x] Step 11: Error Handling + Downloads
- [ ] Step 12: Polish + Release
