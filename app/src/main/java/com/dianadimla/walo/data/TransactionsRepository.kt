package com.dianadimla.walo.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

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

    /**
     * Fetches transactions within a specific date range.
     */
    fun fetchTransactionsByDateRange(
        startDate: Date,
        endDate: Date,
        onSuccess: (List<Transaction>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: run {
            onFailure(Exception("User not authenticated"))
            return
        }

        db.collection("users").document(uid).collection("transactions")
            .whereGreaterThanOrEqualTo("timestamp", startDate)
            .whereLessThanOrEqualTo("timestamp", endDate)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val transactions = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                }
                onSuccess(transactions)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    /**
     * SHARED AGGREGATION LOGIC: Reused by Dashboard and Reports to ensure consistency.
     * Groups expense transactions by category and sums their amounts.
     */
    fun aggregateCategorySpending(transactions: List<Transaction>): Map<String, Double> {
        return transactions
            .filter { it.expense }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    /**
     * Gets daily spending for a specific week offset.
     * weekOffset 0 = this week, -1 = last week, etc.
     */
    fun getWeeklySpending(
        weekOffset: Int,
        onResult: (List<Float>, List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        
        calendar.add(Calendar.DAY_OF_YEAR, weekOffset * 7)
        val endDate = calendar.time
        
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = calendar.time

        fetchTransactionsByDateRange(startDate, endDate, { transactions ->
            val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val dailyTotals = mutableMapOf<String, Float>()
            val labels = mutableListOf<String>()

            val tempCal = Calendar.getInstance()
            tempCal.time = startDate
            for (i in 0..6) {
                val dayName = dateFormat.format(tempCal.time)
                dailyTotals[dayName] = 0f
                labels.add(dayName)
                tempCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            transactions.filter { it.expense }.forEach { transaction ->
                transaction.timestamp?.let {
                    val dayName = dateFormat.format(it)
                    if (dailyTotals.containsKey(dayName)) {
                        dailyTotals[dayName] = dailyTotals[dayName]!! + transaction.amount.toFloat()
                    }
                }
            }

            val resultValues = labels.map { dailyTotals[it] ?: 0f }
            onResult(resultValues, labels)
            
        }, onFailure)
    }

    /**
     * Calculates monthly spending breakdown by category using shared aggregation logic.
     * monthOffset 0 = this month, -1 = last month, etc.
     */
    fun getMonthlyCategorySpending(
        monthOffset: Int,
        onResult: (Map<String, Double>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, monthOffset)

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.time

        fetchTransactionsByDateRange(startDate, endDate, { transactions ->
            // Reusing the shared aggregation logic
            onResult(aggregateCategorySpending(transactions))
        }, onFailure)
    }
}
