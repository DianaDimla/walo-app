package com.dianadimla.walo.data

import com.google.firebase.firestore.PropertyName


 // Represents a spending pod.
 // 'currentSpending' tracks how much has been spent (e.g 20.0).
 // 'limit' tracks the total budget (e.g 100.0).
data class Pod(
    val id: String = "",
    val name: String = "",

    @get:PropertyName("currentSpending")
    @set:PropertyName("currentSpending")
    var currentSpending: Double = 0.0,
    
    val icon: String = "",
    var limit: Double = 0.0
) {
    // Legacy support: Allows Firestore to populate currentSpending from a 'balance' field if it exists
    @get:PropertyName("balance")
    @set:PropertyName("balance")
    var balance: Double
        get() = currentSpending
        set(value) { currentSpending = value }
}
