/**
 * Utility object for consistent categorical colour mapping.
 * Provides a fixed palette of pastel colours for UI elements.
 */
package com.dianadimla.walo.utils

import android.graphics.Color
import kotlin.math.abs

object ColorUtils {
    // Fixed collection of pastel hex codes for categorical visualisation
    private val pastelPalette = listOf(
        "#FFB3BA", "#FFDFBA", "#FFFFBA", "#BAFFC9", "#BAE1FF",
        "#D4A5A5", "#F3E5AB", "#B2E2F2", "#E3D1FB", "#F19CBB",
        "#C1E1C1", "#FFD1DC", "#ECEAE4", "#A2C4C9", "#C5B4E3", "#F9F1F0"
    ).map { Color.parseColor(it) }

    /**
     * Maps a category string to a deterministic colour from the palette.
     * Uses hash code logic to ensure the same category always retrieves the same colour.
     */
    fun getCategoryColor(category: String): Int {
        val index = abs(category.hashCode()) % pastelPalette.size
        return pastelPalette[index]
    }
}
