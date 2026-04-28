/**
 * Container for calculated weekly financial summaries.
 * Aggregates spending, income, and category data to generate user-friendly feedback.
 */
package com.dianadimla.walo.data

import kotlin.math.abs

data class WeeklyInsights(
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val topCategory: String = "None",
    val differenceFromLastWeek: Double = 0.0,
    val totalBudget: Double = 0.0
) {
    /**
     * Translates raw financial data into human-readable insight messages.
     * Provides context on current spending compared to the previous week.
     */
    fun toUserFriendlyMessages(): List<String> {
        val messages = mutableListOf<String>()

        // Display current budget capacity
        messages.add("You have €${"%.2f".format(totalBudget)} in your budget.")

        // Summarize total expenditure for the week
        messages.add("You've spent €${"%.2f".format(totalSpent)} this week.")

        // Highlight the category with the highest expenditure
        if (topCategory != "None") {
            messages.add("Most of your expenses went to $topCategory.")
        }

        // Compare current performance with last week's metrics
        val diff = differenceFromLastWeek
        when {
            diff < 0 -> messages.add("Awesome! You spent €${"%.2f".format(abs(diff))} less than last week.")
            diff > 0 -> messages.add("You spent €${"%.2f".format(diff)} more than last week. Let's keep an eye on it!")
            else -> messages.add("Your spending is exactly the same as last week. Consistency is key!")
        }

        return messages
    }
}
