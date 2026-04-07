package com.dianadimla.walo.data

import com.google.firebase.Timestamp


// Data class representing user-specific gamification statistics stored in Firestore.
// Using Long for numeric fields to ensure compatibility with Firestore's internal storage.

data class UserStats(
    val goalsCreated: Long = 0L,
    val goalsCompleted: Long = 0L,
    val totalTransactions: Long = 0L,
    val currentStreak: Long = 0L,
    val longestStreak: Long = 0L,
    val lastActiveDate: Timestamp? = null
)
