package com.tinybrowse

import android.content.Context
import com.tinybrowse.data.db.BrowseDatabase
import com.tinybrowse.data.db.SavedSiteDao
import com.tinybrowse.data.prefs.PrefsManager

/**
 * Manual dependency injection container.
 * All app-wide singletons live here. No Hilt, no Dagger, no reflection.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    // Database
    val database: BrowseDatabase = BrowseDatabase(appContext)

    // DAOs
    val savedSiteDao: SavedSiteDao = SavedSiteDao(database)

    // Preferences
    val prefs: PrefsManager = PrefsManager(appContext)
}
