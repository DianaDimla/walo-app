package com.dianadimla.walo.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Data class representing a single financial transaction.

data class Transaction(
    // The unique ID of the transaction document in Firestore.
    val id: String = "",
    // The amount of the transaction.
    val amount: Double = 0.0,
    // The category of the transaction (e.g., "Food", "Income").
    val category: String = "",
    // An optional note for the transaction.
    val note: String? = null,
    // A boolean flag indicating if this is an expense (true) or income (false).
    val expense: Boolean = true, 
    // The timestamp of when the transaction was created. Annotated to be set by the server.
    @ServerTimestamp val timestamp: Date? = null,
    // The ID of the pod this transaction belongs to.
    val podId: String? = null,
    // The name of the pod this transaction belongs to.
    val podName: String? = null
)
