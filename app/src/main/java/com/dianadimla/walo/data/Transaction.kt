/**
 * Data model for a financial transaction.
 * Supports both expenses and income, and maintains a reference to its parent pod.
 */
package com.dianadimla.walo.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Transaction(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val note: String? = null,
    val expense: Boolean = true, // Distinguishes between money out and money in
    @ServerTimestamp val timestamp: Date? = null, // Automated timestamp from Firestore
    val podId: String? = null, // Reference to the budget pod the transaction belongs to
    val podName: String? = null
)
