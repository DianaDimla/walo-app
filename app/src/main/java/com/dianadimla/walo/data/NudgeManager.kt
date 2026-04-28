/**
 * Core engine for AI-Driven financial nudges.
 * Monitors spending patterns and goal progress to trigger behavioral interventions.
 * Implements a Singleton pattern to manage global cooldowns and prevent message spam.
 */
package com.dianadimla.walo.data

import android.util.Log

class NudgeManager private constructor() {

    companion object {
        @Volatile
        private var instance: NudgeManager? = null

        // Returns the global singleton instance
        fun getInstance(): NudgeManager {
            return instance ?: synchronized(this) {
                instance ?: NudgeManager().also { instance = it }
            }
        }
    }

    // Tracks the most recent message per entity (pod/goal) to avoid repetition
    private val lastNudges = mutableMapOf<String, String>()
    
    // Cooldown duration for background-triggered notifications
    private val backgroundCooldownMs = 60000L 
    private var lastBackgroundNudgeTime = 0L

    /**
     * Evaluates pod spending and triggers warnings or encouragement.
     * Overrides background cooldown as it responds to direct user interaction.
     */
    fun checkPodNudges(pod: Pod, onNudge: (String) -> Unit) {
        if (pod.limit <= 0) return 

        val progress = pod.currentSpending / pod.limit
        
        val nudgeMessage = when {
            progress <= 0.0 -> "🚫 You've reached your ${pod.name} limit"
            progress <= 0.2 -> "⚠️ You're running low on ${pod.name}"
            progress >= 0.8 -> "✅ You're managing ${pod.name} well"
            else -> null
        }

        if (nudgeMessage != null) {
            triggerNudge(pod.id, nudgeMessage, isBackground = false, onNudge = onNudge)
        }
    }

    /**
     * Analyses goal progress and deadlines to motivate the user.
     * Clears inactivity tracking for the goal to reset the engagement cycle.
     */
    fun checkGoalUpdateNudges(goal: Goal, onNudge: (String) -> Unit) {
        lastNudges.remove(goal.id)

        if (goal.targetAmount <= 0) return

        val progress = goal.currentAmount / goal.targetAmount
        val currentTime = System.currentTimeMillis()
        
        // Progression thresholds for milestone feedback
        var nudgeMessage = when {
            progress >= 1.0 -> "🎉 Goal completed: ${goal.title}!"
            progress >= 0.8 -> "🚀 Almost there! ${goal.title} is at 80%"
            progress >= 0.5 -> "⭐ Halfway there! Keep it up for ${goal.title}"
            progress >= 0.2 -> "🌱 Good start on your ${goal.title} goal!"
            else -> null
        }

        // Overrides progression message if the deadline is imminent
        goal.deadline?.let { deadline ->
            val threeDaysInMs = 3 * 24 * 60 * 60 * 1000L
            val timeLeft = deadline - currentTime
            
            if (timeLeft in 0..threeDaysInMs && progress < 1.0) {
                nudgeMessage = "⏳ Only a few days left for your goal: ${goal.title}!"
            }
        }

        if (nudgeMessage != null) {
            triggerNudge(goal.id, nudgeMessage!!, isBackground = false, onNudge = onNudge)
        }
    }

    /**
     * Detects periods of inactivity for specific goals and prompts for updates.
     * Respects the global background cooldown to avoid over-notifying.
     */
    fun checkGoalInactivityNudges(goal: Goal, onNudge: (String) -> Unit) {
        if (goal.targetAmount <= 0) return

        val progress = goal.currentAmount / goal.targetAmount
        val currentTime = System.currentTimeMillis()
        
        // Triggers nudge if the goal has not been updated within the threshold
        val inactivityThresholdMs = 60 * 1000L // Set to 1 minute for Demo Purposes
        val timeSinceUpdate = currentTime - goal.lastUpdated
        
        if (goal.lastUpdated > 0 && timeSinceUpdate >= inactivityThresholdMs && progress < 1.0) {
            Log.d("NudgeManager", "Inactivity detected for ${goal.title}")
            val nudgeMessage = "Want to add a small amount today for ${goal.title}? 💰"
            triggerNudge(goal.id, nudgeMessage, isBackground = true, onNudge = onNudge)
        }
    }

    /**
     * Logic for executing the nudge including anti-spam and cooldown checks.
     * Saves unique nudges to persistent session storage.
     */
    private fun triggerNudge(
        id: String, 
        message: String, 
        isBackground: Boolean, 
        onNudge: (String) -> Unit
    ) {
        val currentTime = System.currentTimeMillis()
        val isDuplicate = lastNudges[id] == message
        val isOnCooldown = isBackground && (currentTime - lastBackgroundNudgeTime < backgroundCooldownMs)

        if (!isDuplicate && !isOnCooldown) {
            lastNudges[id] = message
            
            if (isBackground) {
                lastBackgroundNudgeTime = currentTime
            }
            
            // Persist the message to session history
            NudgeStorage.addNudge(message)
            onNudge(message)
        }
    }
}
