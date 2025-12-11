package com.dianadimla.walo.data

// Represents a single spending pod.
data class Pod(
    val id: String = "", // The unique ID of the pod.
    val name: String = "", // The name of the pod.
    var balance: Double = 0.0, // The current balance of the pod.
    var startingBalance: Double = 0.0, // The initial balance of the pod.
    val icon: String = "" // The emoji icon for the pod.
)
