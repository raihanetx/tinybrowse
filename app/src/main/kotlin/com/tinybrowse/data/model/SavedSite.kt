package com.tinybrowse.data.model

/**
 * A saved website. Flat structure, no folders.
 */
data class SavedSite(
    val id: Long = 0,
    val url: String,
    val title: String = "",
    val favicon: ByteArray? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavedSite) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
