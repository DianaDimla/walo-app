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
import androidx.navigation.fragment.findNavController
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

// Fragment for managing transactions and spending pods.
class AddTransactionFragment : Fragment() {

    // View binding
    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    // Adapters for RecyclerViews
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var podAdapter: PodAdapter
    // Local list to hold pod data.
    private val podList = mutableListOf<Pod>()

    // Firebase Firestore and Auth instances.
    private val db = Firebase.firestore
    private val currentUser = Firebase.auth.currentUser
    
    // Managers, Repository and Notification Helper
    private lateinit var gamificationManager: GamificationManager
    private lateinit var nudgeManager: NudgeManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var transactionsRepository: TransactionsRepository
    
    // Queue for achievements to show them one after another
    private val achievementQueue = mutableListOf<String>()
    private var isDialogShowing = false

    // Default emojis for creating new pods.
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

    // View created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Managers, Helper and TransactionsRepository
        gamificationManager = GamificationManager(Firebase.auth, Firebase.firestore)
        nudgeManager = NudgeManager()
        notificationHelper = NotificationHelper(requireContext())
        transactionsRepository = TransactionsRepository(Firebase.auth, Firebase.firestore, gamificationManager, nudgeManager)

        // Set achievement unlock listener to use the queue
        gamificationManager.onAchievementUnlocked = { title ->
            queueAchievementDialog(title)
        }

        // Initialize RecyclerViews and listeners.
        setupRecyclerViews()
        listenForPods()
        listenForExpenseHistory()

        // Set click listener for adding income.
        binding.btnSaveIncome.setOnClickListener {
            showIncomeDialog()
        }

        // Set click listener for adding an expense.
        binding.btnSaveExpense.setOnClickListener {
            showExpenseDialog()
        }

        // Set click listener for adding a new pod.
        binding.btnAddPod.setOnClickListener {
            showCreatePodDialog()
        }

        // Set click listener for editing/deleting a pod.
        binding.btnEditPod.setOnClickListener {
            showPodOptionsDialog()
        }
    }

    // Adds a new achievement message to the queue
    private fun queueAchievementDialog(title: String) {
        if (!isAdded) return
        achievementQueue.add(title)
        if (!isDialogShowing) {
            showNextAchievement()
        }
    }

    // Processes the next achievement in the queue
    private fun showNextAchievement() {
        if (achievementQueue.isEmpty() || !isAdded) {
            isDialogShowing = false
            return
        }

        val title = achievementQueue.removeAt(0)
        isDialogShowing = true

        context?.let { ctx ->
            // Consistent headers for streaks and achievements
            val dialogTitle = if (title.contains("Streak Started")) {
                "Streak Progress!"
            } else {
                " \uD83C\uDFC6 Achievement Unlocked!"
            }

            AlertDialog.Builder(ctx)
                .setTitle(dialogTitle)
                .setMessage(title)
                .setCancelable(false) // Ensures user must interact with the button
                .setPositiveButton("Awesome!") { dialog, _ ->
                    dialog.dismiss()
                    // TRIGGER NEXT: Only show the next notification after the user dismisses this one
                    showNextAchievement()
                }
                .show()
        } ?: run {
            isDialogShowing = false
        }
    }

    // Configures the RecyclerViews
    private fun setupRecyclerViews() {
        // Set up the transaction history RecyclerView.
        transactionAdapter = TransactionAdapter()
        binding.expenseHistoryRecyclerView.adapter = transactionAdapter

        // Set up the pods RecyclerView.
        podAdapter = PodAdapter()
        binding.podsRecyclerView.adapter = podAdapter
    }

    // Listens for real time updates to the pods collection in Firestore.
    private fun listenForPods() {
        val uid = currentUser?.uid ?: return
        db.collection("users").document(uid).collection("pods")
            .orderBy("name")
            .addSnapshotListener { snapshot, e ->
                if (!isAdded) return@addSnapshotListener
                if (e != null) {
                    Log.w("TraceFlow", "STEP 5 FAILURE: Listen for pods failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Map documents to Pod objects.
                    val pods = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Pod::class.java)?.copy(id = doc.id)
                    }
                    
                    Log.d("TraceFlow", "STEP 5: Snapshot Listener Triggered. Received ${pods.size} pods.")
                    pods.forEach { Log.d("TraceFlow", "Pod data received: Name=${it.name}, Spending=${it.currentSpending}") }

                    // Update the local pod list and the adapter immediately.
                    podList.clear()
                    podList.addAll(pods)
                    
                    // ListAdapter's submitList handles efficient UI updates automatically
                    Log.d("TraceFlow", "STEP 6: Calling submitList(pods)")
                    podAdapter.submitList(pods.toList())

                    // UI Update: Calculate and display the total spending vs total limit.
                    val totalSpending = pods.sumOf { it.currentSpending }
                    val totalLimit = pods.sumOf { it.limit }

                    if (totalLimit > 0) {
                        binding.amountDisplay.text = String.format(Locale.getDefault(), "€%.2f / €%.2f", totalSpending, totalLimit)
                    } else {
                        binding.amountDisplay.text = String.format(Locale.getDefault(), "€%.2f", totalSpending)
                    }
                }
            }
    }

    // Shows a dialog to create a new pod or edit an existing one.
    private fun showCreatePodDialog(existingPod: Pod? = null) {
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_pod, null)
        
        val podNameLayout = dialogView.findViewById<TextInputLayout>(R.id.pod_name_layout)
        val podLimitLayout = dialogView.findViewById<TextInputLayout>(R.id.pod_limit_layout)
        val podNameInput = dialogView.findViewById<EditText>(R.id.pod_name_input)
        val podLimitInput = dialogView.findViewById<EditText>(R.id.pod_limit_input)
        val emojiRecyclerView = dialogView.findViewById<RecyclerView>(R.id.emoji_recycler_view)

        // Pre-fill fields if editing
        if (existingPod != null) {
            podNameInput.setText(existingPod.name)
            podLimitInput.setText(existingPod.limit.toString())
            // Disable name editing to prevent confusion with transaction categories
            podNameInput.isEnabled = false 
        }

        var selectedEmoji = existingPod?.icon ?: defaultEmojis[0]

        // Adapter for the emoji selector
        val emojiAdapter = EmojiAdapter(defaultEmojis) { emoji ->
            selectedEmoji = emoji
        }

        // Configure the emoji RecyclerView
        emojiRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 6) // 6 columns for emojis
            adapter = emojiAdapter
        }

        // Build the dialog
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

    // Updates an existing pod's limit and icon in Firestore.
    private fun updatePodLimit(pod: Pod, newLimit: Double, newIcon: String) {
        val uid = currentUser?.uid ?: return
        Log.d("FirestoreUpdate", "Updating pod ${pod.id} for user $uid") // Debug Log

        db.collection("users").document(uid).collection("pods").document(pod.id)
            .update(mapOf(
                "limit" to newLimit,
                "icon" to newIcon
            ))
            .addOnSuccessListener {
                Log.d("FirestoreUpdate", "Pod ${pod.id} successfully updated.") // Success Log
                if (isAdded) {
                    Toast.makeText(context, "Pod updated successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreUpdate", "Error updating pod ${pod.id}", e) // Failure Log
                if (isAdded) {
                    Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Saves a new pod to Firestore.
    private fun saveNewPod(podName: String, icon: String, limit: Double) {
        val uid = currentUser?.uid ?: return
        val newPodRef = db.collection("users").document(uid).collection("pods").document()
        
        Log.d("FirestoreSave", "Creating new pod at: users/$uid/pods/${newPodRef.id}") // Debug Log

        // Create a new Pod object with the required limit.
        val pod = Pod(
            id = newPodRef.id, 
            name = podName, 
            icon = icon, 
            currentSpending = 0.0, 
            limit = limit
        )

        // Set the new pod in Firestore and show feedback.
        newPodRef.set(pod)
            .addOnSuccessListener { 
                Log.d("FirestoreSave", "Pod '$podName' successfully saved.") // Success Log
                if (isAdded) {
                    Toast.makeText(context, "Pod '$podName' created!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e -> 
                Log.e("FirestoreSave", "Error saving pod '$podName'", e) // Failure Log
                if (isAdded) {
                    Toast.makeText(context, "Failed to create pod: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Shows a dialog with options for a pod (Edit or Delete).
    private fun showPodOptionsDialog() {
        if (podList.isEmpty()) {
            Toast.makeText(context, "There are no pods to manage.", Toast.LENGTH_SHORT).show()
            return
        }

        val podNames = podList.map { it.name }.toTypedArray()
        val context = context ?: return

        // Step 1: Select which pod to manage
        AlertDialog.Builder(context)
            .setTitle("Manage Pod")
            .setItems(podNames) { _, which ->
                val selectedPod = podList[which]
                
                // Step 2: Choose action for selected pod
                val options = arrayOf("Edit Limit/Icon", "Delete Pod")
                AlertDialog.Builder(context)
                    .setTitle("Options for ${selectedPod.name}")
                    .setItems(options) { _, optionIndex ->
                        when (optionIndex) {
                            0 -> showCreatePodDialog(selectedPod) // Reuse dialog for editing
                            1 -> confirmAndDeletePod(selectedPod)
                        }
                    }
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Asks for user confirmation before deleting a pod.
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

    // Deletes a pod from Firestore
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

    // Shows a dialog to add income to a pod.
    private fun showIncomeDialog() {
        if (podList.isEmpty()) {
            Toast.makeText(context, "Please create a Pod first", Toast.LENGTH_SHORT).show()
            return
        }

        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_budget, null)
        val amountInput = dialogView.findViewById<EditText>(R.id.editBudgetAmount)
        val podSpinner = dialogView.findViewById<Spinner>(R.id.pod_spinner_income)

        // Populate the spinner with pod names.
        val podNames = podList.map { it.name }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, podNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        podSpinner.adapter = adapter

        // Build and show the income dialog.
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

    // Shows a dialog to add an expense to a pod.
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

        // Populate the spinner with pod names.
        val podNames = podList.map { it.name }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, podNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        podSpinner.adapter = adapter

        // Build and show the expense dialog.
        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                val note = descriptionInput.text.toString().trim()

                Log.d("TraceFlow", "STEP 1: Save Expense Clicked. Amount: $amount")

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

        Log.d("TraceFlow", "STEP 1b: saveTransactionAndUpdatePod triggered for Pod ID: ${pod.id}")

        transactionsRepository.saveTransaction(
            podId = pod.id,
            amount = amount,
            expense = expense,
            transaction = newTransaction,
            onSuccess = { nudgeMessage ->
                if (!isAdded) return@saveTransaction
                
                Log.d("TraceFlow", "STEP 7: Repository Success Callback in Fragment. Nudge: $nudgeMessage")

                // UI Nudge: Show system notification if a nudge message exists
                nudgeMessage?.let { message ->
                    notificationHelper.showNotification(message)
                }

                context?.let { ctx ->
                    val action = if (expense) "Expense" else "Income"
                    
                    // Show message as toast for immediate confirmation
                    val displayMessage = nudgeMessage ?: "$action saved successfully!"
                    Toast.makeText(ctx, displayMessage, Toast.LENGTH_SHORT).show()
                }
                
                try {
                    findNavController().navigateUp()
                } catch (e: Exception) {
                    Log.e("AddTransactionFragment", "Navigation failed: ${e.message}")
                }
            },
            onFailure = { e ->
                if (isAdded) {
                    Log.e("TraceFlow", "STEP 7 FAILURE: Repository Failure Callback: ${e.message}")
                    Toast.makeText(context, "Transaction failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Listens for real time updates to the transaction history.
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

    // Destroy view
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
