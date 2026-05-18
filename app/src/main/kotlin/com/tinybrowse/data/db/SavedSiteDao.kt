package com.tinybrowse.data.db

import android.content.ContentValues
import com.tinybrowse.data.model.SavedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Data access for saved sites. Raw SQL, no ORM.
 */
class SavedSiteDao(private val db: BrowseDatabase) {

    fun getAll(): Flow<List<SavedSite>> = flow {
        val sites = mutableListOf<SavedSite>()
        val cursor = db.readableDatabase.rawQuery(
            "SELECT id, url, title, favicon, created_at FROM saved_sites ORDER BY created_at DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                sites.add(
                    SavedSite(
                        id = it.getLong(0),
                        url = it.getString(1),
                        title = it.getString(2),
                        favicon = it.getBlob(3),
                        createdAt = it.getLong(4)
                    )
                )
            }
        }
        emit(sites)
    }.flowOn(Dispatchers.IO)

    suspend fun insert(url: String, title: String, favicon: ByteArray? = null): Long {
        return withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("url", url)
                put("title", title)
                put("favicon", favicon)
                put("created_at", System.currentTimeMillis())
            }
            db.writableDatabase.insert("saved_sites", null, values)
        }
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            db.writableDatabase.delete("saved_sites", "id = ?", arrayOf(id.toString()))
        }
    }

    suspend fun isSaved(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.rawQuery(
                "SELECT COUNT(*) FROM saved_sites WHERE url = ?",
                arrayOf(url)
            )
            cursor.use {
                it.moveToFirst() && it.getInt(0) > 0
            }
        }
    }
}
