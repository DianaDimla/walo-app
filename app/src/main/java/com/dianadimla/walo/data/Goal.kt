/**
 * Data model representing a financial goal.
 * Includes tracking for target amounts, current progress, and deadlines.
 */
package com.dianadimla.walo.data

data class Goal(
    val id: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val deadline: Long? = null, // Optional Unix timestamp for the goal's target date
    val lastUpdated: Long = System.currentTimeMillis() // Timestamp used for inactivity tracking
)
