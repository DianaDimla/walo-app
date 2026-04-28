/**
 * ViewModel responsible for calculating and providing financial insights.
 * Aggregates transaction data to generate summaries of weekly spending behaviour.
 */
package com.dianadimla.walo.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dianadimla.walo.data.NudgeManager
import com.dianadimla.walo.data.Transaction
import com.dianadimla.walo.data.TransactionsRepository
import com.dianadimla.walo.data.WeeklyInsights
import com.dianadimla.walo.data.GamificationManager
import com.dianadimla.walo.data.Pod
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date

class InsightsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val nudgeManager = NudgeManager.getInstance()
    private val gamificationManager = GamificationManager(auth, db)
    private val repository = TransactionsRepository(auth, db, gamificationManager, nudgeManager)

    private val _insights = MutableLiveData<WeeklyInsights>()
    val insights: LiveData<WeeklyInsights> get() = _insights

    private val _insightMessages = MutableLiveData<List<String>>()
    val insightMessages: LiveData<List<String>> get() = _insightMessages

    /**
     * Orchestrates the retrieval and calculation of weekly spending metrics.
     * Compares current session data with historical records to identify trends.
     */
    fun calculateWeeklyInsights() {
        val calendar = Calendar.getInstance()
        val now = calendar.time

        // Defines the boundaries for the current 7-day tracking period
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startOfThisWeek = calendar.time

        // Defines the boundaries for the previous comparison period
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startOfLastWeek = calendar.time

        val uid = auth.currentUser?.uid
        if (uid != null) {
            // Retrieves the user's budget pods to determine the overall financial capacity
            db.collection("users").document(uid).collection("pods").get()
                .addOnSuccessListener { podSnapshot ->
                    val pods = podSnapshot.toObjects(Pod::class.java)
                    val totalBudget = pods.sumOf { it.currentSpending }

                    repository.fetchTransactionsByDateRange(startOfThisWeek, now, { thisWeekTransactions ->
                        repository.fetchTransactionsByDateRange(startOfLastWeek, startOfThisWeek, { lastWeekTransactions ->
                            
                            val lastWeekSpent = lastWeekTransactions.filter { it.expense }.sumOf { it.amount }
                            val weeklyInsights = performCalculations(thisWeekTransactions, lastWeekSpent, totalBudget)
                            
                            _insights.postValue(weeklyInsights)
                            _insightMessages.postValue(weeklyInsights.toUserFriendlyMessages())
                            
                        }, { 
                            // Handles retrieval errors for historical comparison data
                        })
                    }, { 
                        // Handles retrieval errors for current period data
                    })
                }
        }
    }

    /**
     * Aggregates raw transaction data into structured weekly insights.
     * Identifies total expenditure, income, and the primary spending category.
     */
    private fun performCalculations(
        transactions: List<Transaction>, 
        lastWeekSpent: Double, 
        totalBudget: Double
    ): WeeklyInsights {
        val totalSpent = transactions.filter { it.expense }.sumOf { it.amount }
        val totalIncome = transactions.filter { !it.expense }.sumOf { it.amount }
        
        // Identifies the category with the highest monetary impact
        val topCategory = transactions
            .filter { it.expense }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .maxByOrNull { it.value }
            ?.key ?: "None"

        return WeeklyInsights(
            totalSpent = totalSpent,
            totalIncome = totalIncome,
            topCategory = topCategory,
            differenceFromLastWeek = totalSpent - lastWeekSpent,
            totalBudget = totalBudget
        )
    }
}
