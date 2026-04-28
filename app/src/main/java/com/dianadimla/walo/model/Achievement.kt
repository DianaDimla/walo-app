/**
 * Data model for user achievements within the gamification system.
 * Defines the structure for badges that users can earn through financial milestones.
 */
package com.dianadimla.walo.model

data class Achievement(
    val id: String,               // Unique identifier for the achievement badge
    val title: String,            // Name of the achievement displayed to the user
    val description: String,      // Summary of the achievement's purpose or unlock criteria
    val iconRes: Int,             // Drawable resource reference for the badge icon
    val isUnlocked: Boolean = false // Tracks the current earned status for the user
)
