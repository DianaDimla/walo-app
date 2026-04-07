package com.dianadimla.walo.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dianadimla.walo.adapters.GoalAdapter
import com.dianadimla.walo.data.Goal
import com.dianadimla.walo.databinding.DialogAddAmountBinding
import com.dianadimla.walo.databinding.DialogCreateGoalBinding
import com.dianadimla.walo.databinding.DialogGoalOptionsBinding
import com.dianadimla.walo.databinding.FragmentGoalsBinding
import com.dianadimla.walo.viewmodels.GoalsViewModel

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private lateinit var goalAdapter: GoalAdapter
    private val viewModel: GoalsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        // Observe LiveData from ViewModel
        viewModel.goals.observe(viewLifecycleOwner) { goals ->
            goalAdapter.submitList(goals)
            updateUI(goals)
        }
        
        // Observe achievement unlocks from the ViewModel
        viewModel.achievementUnlocked.observe(viewLifecycleOwner) { title ->
            showAchievementDialog(title)
        }

        binding.createGoalButton.setOnClickListener {
            showCreateGoalDialog()
        }

        binding.fabAddGoal.setOnClickListener {
            showCreateGoalDialog()
        }
    }

    private fun setupRecyclerView() {
        goalAdapter = GoalAdapter(
            onGoalClicked = { goal ->
                showAddAmountDialog(goal)
            },
            onGoalLongClicked = { goal ->
                showGoalOptionsDialog(goal)
            }
        )
        binding.goalsRecyclerView.apply {
            adapter = goalAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    // Shows a sequential dialog with a consistent "Achievement Unlocked" style
    private fun showAchievementDialog(title: String) {
        if (!isAdded) return
        
        context?.let { ctx ->
            // Consistent style for both streak and achievement notifications
            val dialogTitle = if (title.contains("Streak Started")) {
                "Streak Unlocked!"
            } else {
                "\uD83C\uDFC6 Achievement Unlocked!"
            }

            AlertDialog.Builder(ctx)
                .setTitle(dialogTitle)
                .setMessage(title)
                .setPositiveButton("Awesome!") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun showGoalOptionsDialog(goal: Goal) {
        val dialogBinding = DialogGoalOptionsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnEditGoalName.setOnClickListener {
            dialog.dismiss()
            showEditGoalNameDialog(goal)
        }

        dialogBinding.btnDeleteGoal.setOnClickListener {
            viewModel.deleteGoal(goal)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditGoalNameDialog(goal: Goal) {
        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val margin = (16 * resources.displayMetrics.density).toInt()
        params.setMargins(margin, margin / 2, margin, 0)
        
        val input = EditText(requireContext())
        input.layoutParams = params
        input.setText(goal.title)
        input.setSelection(goal.title.length)
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Goal Name")
            .setView(container)
            .setPositiveButton("Update") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotBlank()) {
                    viewModel.updateGoal(goal.copy(title = newName))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateGoalDialog() {
        val dialogBinding = DialogCreateGoalBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCreateGoal.setOnClickListener {
            val name = dialogBinding.etGoalName.text.toString()
            val amountStr = dialogBinding.etTargetAmount.text.toString()

            if (name.isNotBlank() && amountStr.isNotBlank()) {
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                val newGoal = Goal(
                    title = name,
                    targetAmount = amount
                )
                
                viewModel.addGoal(newGoal)
                dialog.dismiss()
            }
        }

        dialogBinding.btnCancelGoal.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddAmountDialog(goal: Goal) {
        val dialogBinding = DialogAddAmountBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnAddAmount.setOnClickListener {
            val amountStr = dialogBinding.etAddAmount.text.toString()
            if (amountStr.isNotBlank()) {
                val addedAmount = amountStr.toDoubleOrNull() ?: 0.0
                val newAmount = goal.currentAmount + addedAmount
                // Prevent exceeding targetAmount
                val finalAmount = if (newAmount > goal.targetAmount) goal.targetAmount else newAmount
                
                val updatedGoal = goal.copy(currentAmount = finalAmount)
                viewModel.updateGoal(updatedGoal)
                dialog.dismiss()
            }
        }

        dialogBinding.btnCancelAddAmount.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateUI(goals: List<Goal>) {
        if (goals.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.goalsRecyclerView.visibility = View.GONE
            binding.fabAddGoal.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.goalsRecyclerView.visibility = View.VISIBLE
            binding.fabAddGoal.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
