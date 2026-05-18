package com.tinybrowse.data.model

/**
 * Represents a single browser tab.
 */
data class Tab(
    val id: Int,
    val url: String = "start",
    val title: String = "New Tab",
    val isIncognito: Boolean = false
)
