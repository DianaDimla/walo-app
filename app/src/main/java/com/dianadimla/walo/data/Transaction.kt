package com.dianadimla.walo.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Represents a single financial transaction.
data class Transaction(
    val id: String = "", // The unique ID of the transaction.
    val amount: Double = 0.0, // The amount of the transaction.
    val category: String = "", // The category of the transaction.
    val note: String? = null, // An optional note for the transaction.
    val expense: Boolean = true, // True if it's an expense, false if income.
    @ServerTimestamp val timestamp: Date? = null, // The server-generated timestamp.
    val podId: String? = null, // The ID of the pod this transaction belongs to.
    val podName: String? = null // The name of the pod this transaction belongs to.
)
