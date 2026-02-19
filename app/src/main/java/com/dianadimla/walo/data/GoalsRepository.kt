package com.dianadimla.walo.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GoalsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val goalsCollection = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("goals")
    }

    fun listenToGoals(onDataChanged: (List<Goal>) -> Unit) {
        goalsCollection?.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            
            val goals = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Goal::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            
            onDataChanged(goals)
        }
    }

    fun addGoal(goal: Goal) {
        goalsCollection?.add(goal)
    }

    fun updateGoal(goal: Goal) {
        goalsCollection?.document(goal.id)?.set(goal)
    }

    fun deleteGoal(goalId: String) {
        goalsCollection?.document(goalId)?.delete()
    }
}
