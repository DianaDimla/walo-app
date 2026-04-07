package com.dianadimla.walo.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Calendar

// Manages user stats and achievement unlocking logic
class GamificationManager(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val TAG = "GamificationManager"

    // Callback to notify UI when a new achievement or streak event occurs
    var onAchievementUnlocked: ((title: String) -> Unit)? = null

    private fun getSafeUid(): String? = auth.currentUser?.uid

    // Updates the user's daily activity streak when the app is opened
    fun onAppOpened() {
        val uid = getSafeUid() ?: return
        val userRef = firestore.collection("users").document(uid)

        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) return@addOnSuccessListener

            val stats = snapshot.get("stats") as? Map<*, *>
            val lastActiveTimestamp = snapshot.getTimestamp("stats.lastActiveDate")
            
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (lastActiveTimestamp == null) {
                val initialUpdates = mapOf(
                    "stats.currentStreak" to 1L,
                    "stats.longestStreak" to 1L,
                    "stats.lastActiveDate" to FieldValue.serverTimestamp()
                )
                userRef.update(initialUpdates)
                return@addOnSuccessListener
            }

            val lastActiveDate = Calendar.getInstance().apply {
                time = lastActiveTimestamp.toDate()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (!lastActiveDate.before(today)) return@addOnSuccessListener

            val yesterday = Calendar.getInstance().apply {
                time = today.time
                add(Calendar.DAY_OF_YEAR, -1)
            }

            var currentStreak = (stats?.get("currentStreak") as? Number)?.toLong() ?: 0L
            var longestStreak = (stats?.get("longestStreak") as? Number)?.toLong() ?: 0L

            if (lastActiveDate == yesterday) {
                currentStreak++
            } else {
                currentStreak = 1L
            }

            if (currentStreak > longestStreak) {
                longestStreak = currentStreak
            }

            val updates = mapOf(
                "stats.currentStreak" to currentStreak,
                "stats.longestStreak" to longestStreak,
                "stats.lastActiveDate" to FieldValue.serverTimestamp()
            )
            userRef.update(updates)

            // Day 3 Behavior: Trigger two separate notifications
            if (currentStreak == 3L) {
                // First notification: Streak motivator
                onAchievementUnlocked?.invoke("Streak Started!\nKeep up the streak by opening Walo every day.")
                
                // Second notification: Achievement unlock with a delay to ensure they appear as separate events
                Handler(Looper.getMainLooper()).postDelayed({
                    unlockAchievement(uid, "CONSISTENT_TRACKER", "Consistent Tracker (3 Day Streak)")
                }, 500)
            }
            
            // Day 7 Behavior: Unlock achievement normally
            if (currentStreak == 7L) {
                unlockAchievement(uid, "SEVEN_DAY_STREAK", "7-Day Streak")
            }

        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to update daily streak", e)
        }
    }

    // Checks and unlocks transaction-related achievements
    fun checkAchievements(confirmedCount: Long? = null) {
        val uid = getSafeUid() ?: return
        
        if (confirmedCount != null) {
            // Uses confirmed transaction count to check for achievements
            if (confirmedCount >= 1L) {
                unlockAchievement(uid, "FIRST_ENTRY", "First Entry")
            }
        } else {
            // Fetches stats from Firestore if count is not provided
            val userRef = firestore.collection("users").document(uid)
            userRef.get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) return@addOnSuccessListener
                val stats = snapshot.get("stats") as? Map<*, *>
                val count = (stats?.get("totalTransactions") as? Number)?.toLong() ?: 0L
                if (count >= 1) {
                    unlockAchievement(uid, "FIRST_ENTRY", "First Entry")
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to check achievements", e)
            }
        }
    }

    // Atomically increments a specific user statistic in Firestore
    fun incrementStat(stat: String, onComplete: ((Long) -> Unit)? = null) {
        val uid = getSafeUid() ?: return
        val userRef = firestore.collection("users").document(uid)

        // Increment the nested stat field
        userRef.update("stats.$stat", FieldValue.increment(1L))
            .addOnSuccessListener {
                userRef.get().addOnSuccessListener { snapshot ->
                    val value = (snapshot.get("stats.$stat") as? Number)?.toLong() ?: 0L
                    onComplete?.invoke(value)
                }
            }
            .addOnFailureListener {
                // Initialize stats map if it doesn't exist
                val updates = mapOf("stats" to mapOf(stat to 1L))
                userRef.set(updates, SetOptions.merge())
            }
    }

    // Triggered when a goal is created to track progress and achievements
    fun onGoalCreated() {
        incrementStat("goalsCreated") { count ->
            if (count >= 1) {
                val uid = getSafeUid() ?: return@incrementStat
                unlockAchievement(uid, "FIRST_STEP", "First Step")
            }
        }
    }

    // Triggered when a goal is completed to award achievements
    fun onGoalCompleted() {
        incrementStat("goalsCompleted") { count ->
            val uid = getSafeUid() ?: return@incrementStat
            when (count) {
                1L -> unlockAchievement(uid, "GOAL_GETTER", "Goal Getter")
                3L -> unlockAchievement(uid, "MASTER_SAVER", "Master Saver")
                5L -> unlockAchievement(uid, "WEALTH_BUILDER", "Wealth Builder")
            }
        }
    }

    // Persists unlocked achievement to the user's collection in Firestore
    private fun unlockAchievement(uid: String, id: String, title: String) {
        val achRef = firestore.collection("users").document(uid)
            .collection("achievements").document(id)

        // Check if achievement is already unlocked before writing
        achRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val data = mapOf(
                    "id" to id,
                    "title" to title,
                    "unlockedAt" to FieldValue.serverTimestamp()
                )
                achRef.set(data)
                    .addOnSuccessListener {
                        Log.d(TAG, "Achievement unlocked: $title")
                        // Trigger the callback to notify the UI
                        onAchievementUnlocked?.invoke(title)
                    }
            }
        }
    }

    // Legacy method for logging transactions
    fun onTransactionLogged() {
        checkAchievements()
    }
}
