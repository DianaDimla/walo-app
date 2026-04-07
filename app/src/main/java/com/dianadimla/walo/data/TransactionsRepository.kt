package com.dianadimla.walo.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Repository for handling transaction related database operations
class TransactionsRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val gamificationManager: GamificationManager,
    private val nudgeManager: NudgeManager // Inject NudgeManager
) {
    // Atomically saves a transaction and updates the pod balance
    fun saveTransaction(
        podId: String,
        amount: Double,
        expense: Boolean,
        transaction: Transaction,
        onSuccess: (String?) -> Unit, // Updated to pass a nudge message
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: run {
            onFailure(Exception("User not authenticated"))
            return
        }

        val userRef = db.collection("users").document(uid)
        val podDocRef = userRef.collection("pods").document(podId)
        val newTransactionRef = userRef.collection("transactions").document()

        val finalTransaction = transaction.copy(id = newTransactionRef.id)

        var nudgeMessage: String? = null // Store detected nudge

        // Execute Firestore transaction for atomic data updates
        db.runTransaction { firestoreTransaction ->
            val userSnapshot = firestoreTransaction.get(userRef)
            val stats = userSnapshot.get("stats") as? Map<*, *>
            val currentTotal = (stats?.get("totalTransactions") as? Number)?.toLong() ?: 0L
            val newTotal = currentTotal + 1

            val podSnapshot = firestoreTransaction.get(podDocRef)
            if (!podSnapshot.exists()) {
                throw Exception("Pod does not exist")
            }

            val storedSpending = (podSnapshot.get("currentSpending") as? Number)?.toDouble()
            val storedBalance = (podSnapshot.get("balance") as? Number)?.toDouble()
            
            val currentVal = storedSpending ?: storedBalance ?: 0.0
            val limit = (podSnapshot.get("limit") as? Number)?.toDouble() ?: 0.0

            val change = if (expense) -amount else amount
            val newVal = currentVal + change

            // Validation logic
            if (!expense) {
                if (limit > 0 && newVal > limit) throw Exception("Exceeds limit")
            } else {
                if (newVal < 0) throw Exception("Insufficient funds")
            }

            // TRIGGER NUDGE CHECK: Check based on predicted new value
            val updatedPod = Pod(id = podId, name = podSnapshot.getString("name") ?: "", currentSpending = newVal, limit = limit)
            nudgeManager.checkPodNudges(updatedPod) { message ->
                nudgeMessage = message // Capture the nudge message
            }

            // Update Firestore
            firestoreTransaction.update(podDocRef, "currentSpending", newVal)
            firestoreTransaction.update(podDocRef, "balance", newVal)
            firestoreTransaction.set(newTransactionRef, finalTransaction)
            firestoreTransaction.update(userRef, "stats.totalTransactions", newTotal)
            
            newTotal 
        }.addOnSuccessListener { confirmedTotal ->
            gamificationManager.checkAchievements(confirmedTotal)
            onSuccess(nudgeMessage) // Pass message back to UI
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }
}
