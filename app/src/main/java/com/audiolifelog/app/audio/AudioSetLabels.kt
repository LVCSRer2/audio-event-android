package com.audiolifelog.app.audio

import android.content.Context

/**
 * Loads and caches AudioSet 527-class labels from the bundled CSV asset.
 *
 * The expected CSV format (assets/class_labels_indices.csv):
 *   index,mid,display_name
 *   0,/m/09x0r,"Speech"
 *   1,/m/05zppz,"Male speech, man speaking"
 *   ...
 */
object AudioSetLabels {

    @Volatile
    private var cachedLabels: List<String>? = null

    /**
     * Load labels from assets/class_labels_indices.csv.
     * The result is cached after the first successful load.
     *
     * @return Ordered list of display names (index 0 .. 526).
     */
    fun load(context: Context): List<String> {
        cachedLabels?.let { return it }

        synchronized(this) {
            cachedLabels?.let { return it }

            val labels = mutableListOf<Pair<Int, String>>()

            context.assets.open("class_labels_indices.csv").bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    // Handle quoted display_name that may contain commas
                    val parts = parseCsvLine(line)
                    if (parts.size >= 3) {
                        val index = parts[0].trim().toIntOrNull() ?: return@forEach
                        val displayName = parts[2].trim().removeSurrounding("\"")
                        labels.add(index to displayName)
                    }
                }
            }

            labels.sortBy { it.first }
            val result = labels.map { it.second }
            cachedLabels = result
            return result
        }
    }

    /**
     * Parses a single CSV line, respecting quoted fields that may contain commas.
     */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        fields.add(current.toString())
        return fields
    }
}
