package com.dianadimla.walo.data

/**
 * Data class representing an AI nudge notification.
 */
data class Nudge(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
