/**
 * Data model for an AI-Driven spending nudge.
 * Used to store messages that provide behavioural feedback or reminders.
 */
package com.dianadimla.walo.data

data class Nudge(
    val message: String, // The content of the nudge notification
    val timestamp: Long = System.currentTimeMillis() // When the nudge was generated
)
