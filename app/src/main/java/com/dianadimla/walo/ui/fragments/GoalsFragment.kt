/**
 * Fragment for managing and tracking financial goals.
 * Provides functionality to create, edit, delete, and contribute to savings targets.
 */
package com.dianadimla.walo.ui.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dianadimla.walo.adapters.GoalAdapter
import com.dianadimla.walo.data.Goal
import com.dianadimla.walo.data.NudgeManager
import com.dianadimla.walo.databinding.DialogAddAmountBinding
import com.dianadimla.walo.databinding.DialogCreateGoalBinding
import com.dianadimla.walo.databinding.DialogGoalOptionsBinding
import com.dianadimla.walo.databinding.FragmentGoalsBinding
import com.dianadimla.walo.utils.NotificationHelper
import com.dianadimla.walo.viewmodels.GoalsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private lateinit var goalAdapter: GoalAdapter
    
    // Shared ViewModel instance to maintain state across the activity
    private val viewModel: GoalsViewModel by activityViewModels()
    
    private lateinit var nudgeManager: NudgeManager
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nudgeManager = NudgeManager.getInstance()
        notificationHelper = NotificationHelper(requireContext())

        setupRecyclerView()

        // Syncs the UI with the latest goals data
        viewModel.goals.observe(viewLifecycleOwner) { goals ->
            goalAdapter.submitList(goals)
            updateUI(goals)
        }
        
        // Displays achievement milestones as they are reached
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

    // Configures the list and handles user interaction events
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
    
    // Presents a feedback dialog when a milestone or streak is unlocked
    private fun showAchievementDialog(title: String) {
        if (!isAdded) return
        
        context?.let { ctx ->
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

    // Displays management options for a selected goal
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

    // Opens an input dialog to modify the goal's title
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

    // Opens the creation wizard for a new financial goal
    private fun showCreateGoalDialog() {
        val dialogBinding = DialogCreateGoalBinding.inflate(layoutInflater)
        val calendar = Calendar.getInstance()
        var selectedDeadline: Long? = null

        // Initialises date picker for target deadline selection
        dialogBinding.etDeadline.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    selectedDeadline = calendar.timeInMillis
                    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    dialogBinding.etDeadline.setText(format.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCreateGoal.setOnClickListener {
            val name = dialogBinding.etGoalName.text.toString()
            val amountStr = dialogBinding.etTargetAmount.text.toString()

            if (name.isNotBlank() && amountStr.isNotBlank() && selectedDeadline != null) {
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                val newGoal = Goal(
                    title = name,
                    targetAmount = amount,
                    deadline = selectedDeadline!!
                )
                
                viewModel.addGoal(newGoal)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please fill in all fields and select a deadline", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnCancelGoal.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Opens an input dialog to add savings progress to a specific goal
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
                // Caps the progress at the target amount
                val finalAmount = if (newAmount > goal.targetAmount) goal.targetAmount else newAmount
                
                val updatedGoal = goal.copy(
                    currentAmount = finalAmount,
                    lastUpdated = System.currentTimeMillis()
                )
                
                viewModel.updateGoal(updatedGoal)
                
                // Evaluates the updated progress for immediate AI feedback
                nudgeManager.checkGoalUpdateNudges(updatedGoal) { message ->
                    notificationHelper.showNotification(message)
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                }

                dialog.dismiss()
            }
        }

        dialogBinding.btnCancelAddAmount.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Toggles between goal list and empty state guidance
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
