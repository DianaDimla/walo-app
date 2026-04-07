package com.dianadimla.walo.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconRes: Int,
    val isUnlocked: Boolean = false
)
