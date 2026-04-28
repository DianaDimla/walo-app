/**
 * Manages user statistics, daily activity streaks, and achievement unlocking logic.
 * Interacts with Firestore to persist progress and triggers UI notifications for milestones.
 */
package com.dianadimla.walo.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Calendar

class GamificationManager(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val TAG = "GamificationManager"

    // Callback to notify the UI when a new achievement or streak milestone is reached
    var onAchievementUnlocked: ((title: String) -> Unit)? = null

    private fun getSafeUid(): String? = auth.currentUser?.uid

    // Updates the daily activity streak and awards consistency-based achievements
    fun onAppOpened() {
        val uid = getSafeUid() ?: return
        val userRef = firestore.collection("users").document(uid)

        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) return@addOnSuccessListener

            val stats = snapshot.get("stats") as? Map<*, *>
            val lastActiveTimestamp = snapshot.getTimestamp("stats.lastActiveDate")
            
            // Normalise current time to midnight for date comparison
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Initialise streak for first-time active users
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

            // Exit if user has already logged in today
            if (!lastActiveDate.before(today)) return@addOnSuccessListener

            val yesterday = Calendar.getInstance().apply {
                time = today.time
                add(Calendar.DAY_OF_YEAR, -1)
            }

            var currentStreak = (stats?.get("currentStreak") as? Number)?.toLong() ?: 0L
            var longestStreak = (stats?.get("longestStreak") as? Number)?.toLong() ?: 0L

            // Increment streak if last login was yesterday; otherwise, reset to 1
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

            // Triggers specific feedback and achievements for a 3-day streak
            if (currentStreak == 3L) {
                onAchievementUnlocked?.invoke("Streak Started!\nKeep up the streak by opening Walo every day.")
                
                // Delay achievement unlock to distinguish it from the streak notification
                Handler(Looper.getMainLooper()).postDelayed({
                    unlockAchievement(uid, "CONSISTENT_TRACKER", "Consistent Tracker (3 Day Streak)")
                }, 500)
            }
            
            if (currentStreak == 7L) {
                unlockAchievement(uid, "SEVEN_DAY_STREAK", "7-Day Streak")
            }

        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to update daily streak", e)
        }
    }

    // Evaluates transaction history to unlock related badges
    fun checkAchievements(confirmedCount: Long? = null) {
        val uid = getSafeUid() ?: return
        
        if (confirmedCount != null) {
            if (confirmedCount >= 1L) {
                unlockAchievement(uid, "FIRST_ENTRY", "First Entry")
            }
        } else {
            // Fallback: Fetch latest counts from Firestore if not provided
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

    // Atomically increments a user metric in Firestore and returns the new value
    fun incrementStat(stat: String, onComplete: ((Long) -> Unit)? = null) {
        val uid = getSafeUid() ?: return
        val userRef = firestore.collection("users").document(uid)

        userRef.update("stats.$stat", FieldValue.increment(1L))
            .addOnSuccessListener {
                userRef.get().addOnSuccessListener { snapshot ->
                    val value = (snapshot.get("stats.$stat") as? Number)?.toLong() ?: 0L
                    onComplete?.invoke(value)
                }
            }
            .addOnFailureListener {
                // Initialize the stats map if the document structure is missing
                val updates = mapOf("stats" to mapOf(stat to 1L))
                userRef.set(updates, SetOptions.merge())
            }
    }

    // Rewards the user for creating their first goal
    fun onGoalCreated() {
        incrementStat("goalsCreated") { count ->
            if (count >= 1) {
                val uid = getSafeUid() ?: return@incrementStat
                unlockAchievement(uid, "FIRST_STEP", "First Step")
            }
        }
    }

    // Evaluates total completed goals to award progression-based achievements
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

    // Persists a newly earned achievement to the user profile if not already unlocked
    private fun unlockAchievement(uid: String, id: String, title: String) {
        val achRef = firestore.collection("users").document(uid)
            .collection("achievements").document(id)

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
                        onAchievementUnlocked?.invoke(title)
                    }
            }
        }
    }

    // Legacy support for transaction based logic
    fun onTransactionLogged() {
        checkAchievements()
    }
}
