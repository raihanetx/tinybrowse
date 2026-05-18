# 🦁 TinyBrowse

A lightweight Android browser optimized for 4GB RAM mid-range devices.

Built with **Kotlin** + **Jetpack Compose** + **WebView**.

## Features

- 🔍 DuckDuckGo search
- ⬅️ Back / Forward / Refresh
- ⭐ Save sites (flat list)
- 🖥️ Desktop mode toggle
- 📑 Multiple tabs
- 🕵️ Incognito mode
- 🔒 SSL indicator
- 📊 Loading progress bar
- 🏠 Start page with saved sites
- ⚠️ Error page with retry
- 📥 Download handling (system DownloadManager)

## Build

The project uses GitHub Actions for CI/CD. Push to `main` and the APK will be built automatically.

Download the APK from the **Actions** tab → latest build → Artifacts.

### Local Build

```bash
# Requires JDK 17 + Android SDK
./gradlew assembleRelease
```

## Architecture

- **24 files, ~1700 lines of Kotlin**
- Manual dependency injection (no Hilt)
- Raw SQLite (no Room)
- Single ViewModel + StateFlow
- 7 dependencies total (all Compose/AndroidX)

## License

MIT
