package com.dianadimla.walo.data


 // Singleton object to store AI nudge notifications locally during the app session.
object NudgeStorage {
    // In-memory list to hold nudges
    private val nudgeList = mutableListOf<Nudge>()


    // Adds a new nudge to the top of the list.
    // message The nudge message text.
    fun addNudge(message: String) {
        val newNudge = Nudge(message)
        // Add to index 0 so the newest is always at the top
        nudgeList.add(0, newNudge)
    }


     // Returns the current list of stored nudges.
    fun getAllNudges(): List<Nudge> {
        return nudgeList.toList()
    }

     // Clears all stored nudges.
    fun clearNudges() {
        nudgeList.clear()
    }
}
