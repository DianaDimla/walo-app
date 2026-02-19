package com.dianadimla.walo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.data.Goal
import com.dianadimla.walo.databinding.ItemGoalPodBinding

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

    fun submitList(newGoals: List<Goal>) {
        goals.clear()
        goals.addAll(newGoals)
        notifyDataSetChanged()
    }

    inner class GoalViewHolder(private val binding: ItemGoalPodBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(goal: Goal) {
            binding.goalName.text = goal.title

            // Update progress text: "$currentAmount / $targetAmount"
            binding.goalAmountProgress.text = "${goal.currentAmount.toInt()} / ${goal.targetAmount.toInt()}"

            // Update progress bar calculation: percentage = (currentAmount / targetAmount) * 100
            val progress = if (goal.targetAmount > 0) {
                ((goal.currentAmount / goal.targetAmount) * 100).toInt()
            } else {
                0
            }
            binding.goalProgressBar.progress = progress
            binding.tvGoalPercentage.text = "$progress%"

            binding.root.setOnClickListener {
                onGoalClicked(goal)
            }

            binding.root.setOnLongClickListener {
                onGoalLongClicked(goal)
                true
            }
        }
    }
}
