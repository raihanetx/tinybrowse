package com.tinybrowse.util

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import com.tinybrowse.BuildConfig

/**
 * Basic memory monitoring. Logs memory usage. No-op in release builds.
 */
object MemoryMonitor {

    private const val TAG = "MemoryMonitor"

    fun logMemory(context: Context, label: String) {
        if (!BuildConfig.DEBUG) return

        val runtime = Runtime.getRuntime()
        val usedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMB = runtime.maxMemory() / (1024 * 1024)
        val nativeMB = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)

        Log.d(TAG, "[$label] Java: ${usedMB}MB / ${maxMB}MB | Native: ${nativeMB}MB")
    }

    fun isLowMemory(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.lowMemory
    }
}
