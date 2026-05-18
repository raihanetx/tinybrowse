package com.tinybrowse.data.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences wrapper. Simple key-value storage.
 * No DataStore overhead.
 */
class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tinybrowse_prefs", Context.MODE_PRIVATE)

    var isDesktopMode: Boolean
        get() = prefs.getBoolean(KEY_DESKTOP_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DESKTOP_MODE, value).apply()

    var defaultSearchEngine: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, SEARCH_DDG) ?: SEARCH_DDG
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()

    companion object {
        private const val KEY_DESKTOP_MODE = "desktop_mode"
        private const val KEY_SEARCH_ENGINE = "search_engine"
        const val SEARCH_DDG = "duckduckgo"
    }
}
