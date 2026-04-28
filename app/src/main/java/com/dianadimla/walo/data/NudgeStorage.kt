/**
 * In-memory storage for AI-Driven nudges during the current app session.
 * Provides central access to the nudge history for UI components.
 */
package com.dianadimla.walo.data

object NudgeStorage {
    // Stores the session's nudge history in a mutable list
    private val nudgeList = mutableListOf<Nudge>()

    /**
     * Appends a new message to the session history.
     * Inserts at the beginning to ensure the latest nudge appears first.
     */
    fun addNudge(message: String) {
        val newNudge = Nudge(message)
        nudgeList.add(0, newNudge)
    }

    // Retrieves all nudges captured in the current session
    fun getAllNudges(): List<Nudge> {
        return nudgeList.toList()
    }

    // Resets the nudge history
    fun clearNudges() {
        nudgeList.clear()
    }
}
