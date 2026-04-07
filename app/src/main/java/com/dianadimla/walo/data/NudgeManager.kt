package com.dianadimla.walo.data

import android.util.Log

// Manages rule-based AI nudges to guide user spending behavior.
class NudgeManager {

    // Track the last shown nudge message per pod to prevent spam
    private val lastNudges = mutableMapOf<String, String>()
    
    // Cooldown in milliseconds (30 seconds)
    private val cooldownMs = 30000L
    private var lastNudgeTime = 0L


     // Checks if a pod needs a nudge based on its current spending and limit.
     // pod The pod to check.
     // onNudge Callback function triggered when a nudge is detected.

    fun checkPodNudges(pod: Pod, onNudge: (String) -> Unit) {
        if (pod.limit <= 0) return // Skip if no limit is set

        // Progress represents the percentage of budget remaining (Available / Limit)
        val progress = pod.currentSpending / pod.limit
        
        val nudgeMessage = when {
            // Rule 1: Limit reached or exceeded
            progress <= 0.0 -> "🚫 You've reached your ${pod.name} limit"
            
            // Rule 2: Low budget warning (less than or equal to 20% remaining)
            progress <= 0.2 -> "⚠️ You're running low on ${pod.name}"
            
            // Rule 3: Positive reinforcement (80% or more remaining)
            progress >= 0.8 -> "✅ You're managing ${pod.name} well"
            
            else -> null
        }

        // Anti-spam logic:
        if (nudgeMessage != null) {
            val currentTime = System.currentTimeMillis()
            val isDuplicate = lastNudges[pod.id] == nudgeMessage
            val isOnCooldown = currentTime - lastNudgeTime < cooldownMs

            // Only trigger if message is different AND not on cooldown
            if (!isDuplicate && !isOnCooldown) {
                lastNudges[pod.id] = nudgeMessage
                lastNudgeTime = currentTime
                
                // Save nudge locally before showing
                NudgeStorage.addNudge(nudgeMessage)
                
                onNudge(nudgeMessage)
            }
        }
    }
}
