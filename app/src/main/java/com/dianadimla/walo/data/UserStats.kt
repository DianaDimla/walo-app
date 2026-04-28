/**
 * Data model for user gamification statistics.
 * Stores long-term progress metrics such as goal counts, streaks, and activity dates.
 */
package com.dianadimla.walo.data

import com.google.firebase.Timestamp

data class UserStats(
    val goalsCreated: Long = 0L,      // Total number of goals ever created
    val goalsCompleted: Long = 0L,    // Total number of goals successfully reached
    val totalTransactions: Long = 0L, // Total transaction count for achievement tracking
    val currentStreak: Long = 0L,     // Consecutive days the user has been active
    val longestStreak: Long = 0L,     // Record for the highest streak achieved
    val lastActiveDate: Timestamp? = null // Reference for calculating streak continuity
)
