/**
 * Data model for a spending pod, representing a specific budget category.
 * Tracks current expenditures against a defined monetary limit.
 */
package com.dianadimla.walo.data

import com.google.firebase.firestore.PropertyName

data class Pod(
    val id: String = "",
    val name: String = "",

    @get:PropertyName("currentSpending")
    @set:PropertyName("currentSpending")
    var currentSpending: Double = 0.0, // Accumulated spending in this pod
    
    val icon: String = "",
    var limit: Double = 0.0 // Maximum budget allocated to this pod
) {
    // Legacy support: Synchronises 'balance' with 'currentSpending' for Firestore compatibility
    @get:PropertyName("balance")
    @set:PropertyName("balance")
    var balance: Double
        get() = currentSpending
        set(value) { currentSpending = value }
}
