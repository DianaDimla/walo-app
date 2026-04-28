/**
 * Fragment for managing transactions and spending pods.
 * Facilitates the creation, modification, and deletion of pods alongside transaction logging.
 */
package com.dianadimla.walo.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.R
import com.dianadimla.walo.adapters.EmojiAdapter
import com.dianadimla.walo.adapters.PodAdapter
import com.dianadimla.walo.adapters.TransactionAdapter
import com.dianadimla.walo.data.GamificationManager
import com.dianadimla.walo.data.NudgeManager
import com.dianadimla.walo.data.Pod
import com.dianadimla.walo.data.Transaction
import com.dianadimla.walo.data.TransactionsRepository
import com.dianadimla.walo.databinding.FragmentAddTransactionBinding
import com.dianadimla.walo.utils.NotificationHelper
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var podAdapter: PodAdapter
    private val podList = mutableListOf<Pod>()

    private val db = Firebase.firestore
    private val currentUser = Firebase.auth.currentUser
    
    private lateinit var gamificationManager: GamificationManager
    private lateinit var nudgeManager: NudgeManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var transactionsRepository: TransactionsRepository
    
    private val achievementQueue = mutableListOf<String>()
    private var isDialogShowing = false

    private val defaultEmojis = listOf(
        "💰", "🛒", "✈️", "🏠", "🍔", "🚗", "🎁", "🎓", "🏥", "👕", "🎉", "💡"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialises core managers and support helpers
        gamificationManager = GamificationManager(Firebase.auth, Firebase.firestore)
        nudgeManager = NudgeManager.getInstance()
        notificationHelper = NotificationHelper(requireContext())
        transactionsRepository = TransactionsRepository(Firebase.auth, Firebase.firestore, gamificationManager, nudgeManager)

        // Handles sequential achievement display logic
        gamificationManager.onAchievementUnlocked = { title ->
            queueAchievementDialog(title)
        }

        setupRecyclerViews()
        listenForPods()
        listenForExpenseHistory()

        binding.btnSaveIncome.setOnClickListener {
            showIncomeDialog()
        }

        binding.btnSaveExpense.setOnClickListener {
            showExpenseDialog()
        }

        binding.btnAddPod.setOnClickListener {
            showCreatePodDialog()
        }

        binding.btnEditPod.setOnClickListener {
            showPodOptionsDialog()
        }
    }

    // Appends an achievement message to the display queue
    private fun queueAchievementDialog(title: String) {
        if (!isAdded) return
        achievementQueue.add(title)
        if (!isDialogShowing) {
            showNextAchievement()
        }
    }

    // Presents the next achievement in the queue to avoid UI overlap
    private fun showNextAchievement() {
        if (achievementQueue.isEmpty() || !isAdded) {
            isDialogShowing = false
            return
        }

        val title = achievementQueue.removeAt(0)
        isDialogShowing = true

        context?.let { ctx ->
            val dialogTitle = if (title.contains("Streak Started")) {
                "Streak Progress!"
            } else {
                " \uD83C\uDFC6 Achievement Unlocked!"
            }

            AlertDialog.Builder(ctx)
                .setTitle(dialogTitle)
                .setMessage(title)
                .setCancelable(false) 
                .setPositiveButton("Awesome!") { dialog, _ ->
                    dialog.dismiss()
                    showNextAchievement()
                }
                .show()
        } ?: run {
            isDialogShowing = false
        }
    }

    // Initialises the adapters and RecyclerView components
    private fun setupRecyclerViews() {
        transactionAdapter = TransactionAdapter()
        binding.expenseHistoryRecyclerView.adapter = transactionAdapter

        podAdapter = PodAdapter()
        binding.podsRecyclerView.adapter = podAdapter
    }

    // Observes real-time updates to budget pods from Firestore
    private fun listenForPods() {
        val uid = currentUser?.uid ?: return
        db.collection("users").document(uid).collection("pods")
            .orderBy("name")
            .addSnapshotListener { snapshot, e ->
                if (!isAdded) return@addSnapshotListener
                if (e != null) return@addSnapshotListener

                if (snapshot != null) {
                    val pods = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Pod::class.java)?.copy(id = doc.id)
                    }
                    
                    podList.clear()
                    podList.addAll(pods)
                    podAdapter.submitList(pods.toList())

                    // Synchronises the total current spending display
                    val totalSpending = pods.sumOf { it.currentSpending }
                    binding.amountDisplay.text = String.format(Locale.getDefault(), "€%.2f", totalSpending)
                }
            }
    }

    // Opens the configuration wizard for creating or editing a budget pod
    private fun showCreatePodDialog(existingPod: Pod? = null) {
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_pod, null)
        
        val podNameLayout = dialogView.findViewById<TextInputLayout>(R.id.pod_name_layout)
        val podLimitLayout = dialogView.findViewById<TextInputLayout>(R.id.pod_limit_layout)
        val podNameInput = dialogView.findViewById<EditText>(R.id.pod_name_input)
        val podLimitInput = dialogView.findViewById<EditText>(R.id.pod_limit_input)
        val emojiRecyclerView = dialogView.findViewById<RecyclerView>(R.id.emoji_recycler_view)

        if (existingPod != null) {
            podNameInput.setText(existingPod.name)
            podLimitInput.setText(existingPod.limit.toString())
            podNameInput.isEnabled = false 
        }

        var selectedEmoji = existingPod?.icon ?: defaultEmojis[0]

        val emojiAdapter = EmojiAdapter(defaultEmojis) { emoji ->
            selectedEmoji = emoji
        }

        emojiRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 6)
            adapter = emojiAdapter
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(if (existingPod == null) "Create New Pod" else "Edit Pod")
            .setView(dialogView)
            .setPositiveButton(if (existingPod == null) "Create" else "Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = podNameInput.text.toString().trim()
                val limitString = podLimitInput.text.toString().trim()
                
                podNameLayout.error = null
                podLimitLayout.error = null

                var isValid = true

                if (name.isEmpty()) {
                    podNameLayout.error = "Pod name is required"
                    isValid = false
                }

                val limitValue = limitString.toDoubleOrNull()
                if (limitString.isEmpty()) {
                    podLimitLayout.error = "Budget limit is required"
                    isValid = false
                } else if (limitValue == null) {
                    podLimitLayout.error = "Enter a valid number"
                    isValid = false
                }

                if (isValid && limitValue != null) {
                    if (existingPod == null) {
                        saveNewPod(name, selectedEmoji, limitValue)
                    } else {
                        updatePodLimit(existingPod, limitValue, selectedEmoji)
                    }
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    // Persists modifications to an existing pod's parameters
    private fun updatePodLimit(pod: Pod, newLimit: Double, newIcon: String) {
        val uid = currentUser?.uid ?: return
        db.collection("users").document(uid).collection("pods").document(pod.id)
            .update(mapOf(
                "limit" to newLimit,
                "icon" to newIcon
            ))
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Pod updated successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Provisions a new budget pod within the user's Firestore collection
    private fun saveNewPod(podName: String, icon: String, limit: Double) {
        val uid = currentUser?.uid ?: return
        val newPodRef = db.collection("users").document(uid).collection("pods").document()
        
        val pod = Pod(
            id = newPodRef.id, 
            name = podName, 
            icon = icon, 
            currentSpending = 0.0, 
            limit = limit
        )

        newPodRef.set(pod)
            .addOnSuccessListener { 
                if (isAdded) {
                    Toast.makeText(context, "Pod '$podName' created!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e -> 
                if (isAdded) {
                    Toast.makeText(context, "Failed to create pod: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Displays management options for the selected budget pod
    private fun showPodOptionsDialog() {
        if (podList.isEmpty()) {
            Toast.makeText(context, "There are no pods to manage.", Toast.LENGTH_SHORT).show()
            return
        }

        val podNames = podList.map { it.name }.toTypedArray()
        val context = context ?: return

        AlertDialog.Builder(context)
            .setTitle("Manage Pod")
            .setItems(podNames) { _, which ->
                val selectedPod = podList[which]
                val options = arrayOf("Edit Limit/Icon", "Delete Pod")
                AlertDialog.Builder(context)
                    .setTitle("Options for ${selectedPod.name}")
                    .setItems(options) { _, optionIndex ->
                        when (optionIndex) {
                            0 -> showCreatePodDialog(selectedPod)
                            1 -> confirmAndDeletePod(selectedPod)
                        }
                    }
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Requests user confirmation before permanent pod deletion
    private fun confirmAndDeletePod(pod: Pod) {
        val context = context ?: return
        AlertDialog.Builder(context)
            .setTitle("Delete Pod")
            .setMessage("Are you sure you want to delete the '${pod.name}' pod? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePod(pod)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Permanently removes a pod document from Firestore
    private fun deletePod(pod: Pod) {
        val uid = currentUser?.uid ?: return
        db.collection("users").document(uid).collection("pods").document(pod.id)
            .delete()
            .addOnSuccessListener { 
                if (isAdded) {
                    Toast.makeText(context, "Pod '${pod.name}' deleted successfully.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e -> 
                if (isAdded) {
                    Toast.makeText(context, "Failed to delete pod: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Opens an input dialog to log income towards a pod
    private fun showIncomeDialog() {
        if (podList.isEmpty()) {
            Toast.makeText(context, "Please create a Pod first", Toast.LENGTH_SHORT).show()
            return
        }

        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_budget, null)
        val amountInput = dialogView.findViewById<EditText>(R.id.editBudgetAmount)
        val podSpinner = dialogView.findViewById<Spinner>(R.id.pod_spinner_income)

        val podNames = podList.map { it.name }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, podNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        podSpinner.adapter = adapter

        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0) {
                    val selectedPod = podList[podSpinner.selectedItemPosition]
                    saveTransactionAndUpdatePod(amount, selectedPod, expense = false, note = "Income")
                } else {
                    if (isAdded) {
                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Opens an input dialog to log an expenditure for a pod
    private fun showExpenseDialog() {
        if (podList.isEmpty()) {
            Toast.makeText(context, "Please create a Pod first", Toast.LENGTH_SHORT).show()
            return
        }

        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_expense, null)
        val amountInput = dialogView.findViewById<EditText>(R.id.expense_amount_input)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.expense_description_input)
        val podSpinner = dialogView.findViewById<Spinner>(R.id.pod_spinner_expense)

        val podNames = podList.map { it.name }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, podNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        podSpinner.adapter = adapter

        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                val note = descriptionInput.text.toString().trim()

                if (amount != null && amount > 0) {
                    val selectedPod = podList[podSpinner.selectedItemPosition]
                    saveTransactionAndUpdatePod(amount, selectedPod, expense = true, note = note.takeIf { it.isNotEmpty() })
                } else {
                    if (isAdded) {
                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Persists a financial transaction and updates the pod's accumulated spending
    private fun saveTransactionAndUpdatePod(amount: Double, pod: Pod, expense: Boolean, note: String?) {
        val newTransaction = Transaction(
            amount = amount,
            category = pod.name, 
            note = note,
            expense = expense,
            timestamp = null,
            podId = pod.id,
            podName = pod.name
        )

        transactionsRepository.saveTransaction(
            podId = pod.id,
            amount = amount,
            expense = expense,
            transaction = newTransaction,
            onSuccess = { nudgeMessage ->
                if (!isAdded) return@saveTransaction
                
                nudgeMessage?.let { message ->
                    notificationHelper.showNotification(message)
                }

                context?.let { ctx ->
                    val action = if (expense) "Expense" else "Income"
                    val displayMessage = nudgeMessage ?: "$action saved successfully!"
                    Toast.makeText(ctx, displayMessage, Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { e ->
                if (isAdded) {
                    Toast.makeText(context, "Transaction failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Tracks historical transaction data for UI display
    private fun listenForExpenseHistory() {
        val uid = currentUser?.uid ?: return
        db.collection("users").document(uid).collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50) 
            .addSnapshotListener { snapshot, e ->
                if (!isAdded) return@addSnapshotListener
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                        } catch (_: Exception) {
                            null 
                        }
                    }
                    transactionAdapter.submitList(transactions)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
