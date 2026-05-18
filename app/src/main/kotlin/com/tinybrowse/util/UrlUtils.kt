package com.tinybrowse.util

/**
 * URL validation and search URL building.
 */
object UrlUtils {

    private const val SEARCH_URL = "https://duckduckgo.com/?q="
    private val URL_PATTERN = Regex(
        "^(https?://)?[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z]{2,})+.*$"
    )

    /**
     * Determines if input is a URL or a search query.
     * Returns the URL to load.
     */
    fun parseInput(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        // Already has protocol
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }

        // Looks like a URL (has dot, no spaces)
        if (URL_PATTERN.matches(trimmed) && !trimmed.contains(" ")) {
            return "https://$trimmed"
        }

        // Otherwise, search
        return "$SEARCH_URL${trimmed.replace(" ", "+")}"
    }

    /**
     * Extracts domain from URL for display.
     */
    fun getDomain(url: String): String {
        return try {
            val host = java.net.URI(url).host ?: url
            host.removePrefix("www.")
        } catch (e: Exception) {
            url
        }
    }
}
