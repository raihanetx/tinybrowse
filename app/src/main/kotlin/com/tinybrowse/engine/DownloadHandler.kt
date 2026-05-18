package com.tinybrowse.engine

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.webkit.URLUtil

/**
 * Handles downloads by delegating to Android's system DownloadManager.
 * No custom download logic. Let the OS handle it.
 */
object DownloadHandler {

    fun onDownloadStart(
        context: Context,
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    ) {
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                setDescription("Downloading...")
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        } catch (e: Exception) {
            // Silently fail — don't crash the browser for a download error
        }
    }
}
