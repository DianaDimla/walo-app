package com.dianadimla.walo.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class GoalsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Ensure GamificationManager is properly initialized
    private val gamificationManager = GamificationManager(auth, firestore)
    
    // Expose the achievement listener so the fragment can show popups
    fun setAchievementListener(listener: (String) -> Unit) {
        gamificationManager.onAchievementUnlocked = listener
    }
    
    // Use a getter to ensure goalsCollection is not stale or null if the user session updates
    private val goalsCollection: CollectionReference?
        get() = auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).collection("goals")
        }

    fun listenToGoals(onDataChanged: (List<Goal>) -> Unit) {
        goalsCollection?.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("GoalsRepository", "Error listening to goals", error)
                return@addSnapshotListener
            }
            
            val goals = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Goal::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            
            onDataChanged(goals)
        }
    }

    fun addGoal(goal: Goal) {
        val collection = goalsCollection
        if (collection == null) {
            Log.e("GoalsRepository", "Cannot add goal: User not logged in")
            return
        }

        // Set lastUpdated to current server time if not set
        val goalToSave = if (goal.lastUpdated == 0L) {
            goal.copy(lastUpdated = System.currentTimeMillis())
        } else {
            goal
        }

        collection.add(goalToSave)
            .addOnSuccessListener {
                Log.d("GoalsRepository", "Goal created successfully")
                // Trigger gamification ONLY after success
                gamificationManager.onGoalCreated()
            }
            .addOnFailureListener { e ->
                Log.e("GoalsRepository", "Failed to create goal", e)
            }
    }

    fun updateGoal(goal: Goal) {
        val docRef = goalsCollection?.document(goal.id) ?: return
        
        // Ensure timestamp is refreshed on every manual update
        val updatedGoal = goal.copy(lastUpdated = System.currentTimeMillis())
        
        docRef.set(updatedGoal).addOnSuccessListener {
            // Check if goal was just completed
            if (updatedGoal.currentAmount >= updatedGoal.targetAmount && updatedGoal.targetAmount > 0) {
                Log.d("GoalsRepository", "Goal completed! Triggering gamification.")
                gamificationManager.onGoalCompleted()
            }
        }.addOnFailureListener { e ->
            Log.e("GoalsRepository", "Failed to update goal", e)
        }
    }

    fun deleteGoal(goalId: String) {
        goalsCollection?.document(goalId)?.delete()
    }
}
