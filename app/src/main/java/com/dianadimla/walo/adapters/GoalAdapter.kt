/**
 * Adapter for displaying and managing financial goals in a list.
 * Handles progress calculation, formatting, and interaction callbacks.
 */
package com.dianadimla.walo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.data.Goal
import com.dianadimla.walo.databinding.ItemGoalPodBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoalAdapter(
    private val onGoalClicked: (Goal) -> Unit,
    private val onGoalLongClicked: (Goal) -> Unit
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    private val goals = mutableListOf<Goal>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalPodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]
        holder.bind(goal)
    }

    override fun getItemCount(): Int = goals.size

    // Updates the internal goal list and refreshes the UI
    fun submitList(newGoals: List<Goal>) {
        goals.clear()
        goals.addAll(newGoals)
        notifyDataSetChanged()
    }

    // ViewHolder class for goal items with data binding
    inner class GoalViewHolder(private val binding: ItemGoalPodBinding) : RecyclerView.ViewHolder(binding.root) {
        // Binds goal data and sets up click listeners
        fun bind(goal: Goal) {
            binding.goalName.text = goal.title

            binding.goalAmountProgress.text = "€${goal.currentAmount.toInt()} of €${goal.targetAmount.toInt()}"

            // Calculates completion percentage for the progress bar
            val progress = if (goal.targetAmount > 0) {
                ((goal.currentAmount / goal.targetAmount) * 100).toInt()
            } else {
                0
            }
            binding.goalProgressBar.progress = progress
            binding.tvGoalPercentage.text = "$progress%"

            // Formats and displays the deadline if available
            if (goal.deadline != null) {
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(goal.deadline))
                binding.goalTimeframe.text = "📅 Due: $formattedDate"
                binding.goalTimeframe.visibility = View.VISIBLE
            } else {
                binding.goalTimeframe.visibility = View.GONE
            }

            // Click interaction for viewing details
            binding.root.setOnClickListener {
                onGoalClicked(goal)
            }

            // Long click interaction for editing/deleting
            binding.root.setOnLongClickListener {
                onGoalLongClicked(goal)
                true
            }
        }
    }
}
