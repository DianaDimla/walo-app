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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.R
import com.dianadimla.walo.adapters.EmojiAdapter
import com.dianadimla.walo.adapters.PodAdapter
import com.dianadimla.walo.adapters.TransactionAdapter
import com.dianadimla.walo.data.Pod
import com.dianadimla.walo.data.Transaction
import com.dianadimla.walo.databinding.FragmentAddTransactionBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date
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
            showDeletePodDialog()
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
                if (e != null) {
                    Log.w("AddTransactionFragment", "Listen for pods failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Map documents to Pod objects.
                    val pods = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Pod::class.java)?.copy(id = doc.id)
                    }
                    // Update the local pod list and the adapter.
                    podList.clear()
                    podList.addAll(pods)
                    podAdapter.submitList(pods.toList()) // Submit a copy

                    // Calculate and display the total budget.
                    val totalBudget = pods.sumOf { it.balance }
                    binding.amountDisplay.text = String.format(Locale.getDefault(), "€%.2f", totalBudget)
                }
            }
    }

    // Shows a dialog to create a new pod.
    private fun showCreatePodDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_pod, null)
        val podNameInput = dialogView.findViewById<EditText>(R.id.pod_name_input)
        val emojiRecyclerView = dialogView.findViewById<RecyclerView>(R.id.emoji_recycler_view)

        var selectedEmoji = defaultEmojis[0]

        // Adapter for the emoji selector
        val emojiAdapter = EmojiAdapter(defaultEmojis) { emoji ->
            selectedEmoji = emoji
        }

        // Configure the emoji RecyclerView
        emojiRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 6) // 6 columns for emojis
            adapter = emojiAdapter
        }

        // Build and show the create pod dialog
        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val name = podNameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    saveNewPod(name, selectedEmoji)
                } else {
                    Toast.makeText(requireContext(), "Pod name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Saves a new pod to Firestore.
    private fun saveNewPod(podName: String, icon: String) {
        val uid = currentUser?.uid ?: return
        val newPodRef = db.collection("users").document(uid).collection(
            "pods").document()
        // Create a new Pod object.
        val pod = Pod(id = newPodRef.id, name = podName, icon = icon, balance = 0.0, startingBalance = 0.0)

        // Set the new pod in Firestore and show feedback.
        newPodRef.set(pod)
            .addOnSuccessListener { Toast.makeText(requireContext(), "Pod '$podName' created!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), "Failed to create pod: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    // Shows a dialog to select a pod for deletion.
    private fun showDeletePodDialog() {
        if (podList.isEmpty()) {
            Toast.makeText(requireContext(), "There are no pods to delete.", Toast.LENGTH_SHORT).show()
            return
        }

        val podNames = podList.map { it.name }.toTypedArray()

        // Build and show the selection dialog.
        AlertDialog.Builder(requireContext())
            .setTitle("Select a Pod to Delete")
            .setItems(podNames) { _, which ->
                val selectedPod = podList[which]
                confirmAndDeletePod(selectedPod)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Asks for user confirmation before deleting a pod.
    private fun confirmAndDeletePod(pod: Pod) {
        AlertDialog.Builder(requireContext())
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
            .addOnSuccessListener { Toast.makeText(requireContext(), "Pod '${pod.name}' deleted successfully.", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), "Failed to delete pod: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    // Shows a dialog to add income to a pod.
    private fun showIncomeDialog() {
        if (podList.isEmpty()) {
            Toast.makeText(requireContext(), "Please create a Pod first", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_budget, null)
        val amountInput = dialogView.findViewById<EditText>(R.id.editBudgetAmount)
        val podSpinner = dialogView.findViewById<Spinner>(R.id.pod_spinner_income)

        // Populate the spinner with pod names.
        val podNames = podList.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, podNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        podSpinner.adapter = adapter

        // Build and show the income dialog.
        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0) {
                    val selectedPod = podList[podSpinner.selectedItemPosition]
                    saveTransactionAndUpdatePod(amount, selectedPod, expense = false, note = "Income")
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Shows a dialog to add an expense to a pod.
    private fun showExpenseDialog() {
        if (podList.isEmpty()) {
            Toast.makeText(requireContext(), "Please create a Pod first", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null)
        val amountInput = dialogView.findViewById<EditText>(R.id.expense_amount_input)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.expense_description_input)
        val podSpinner = dialogView.findViewById<Spinner>(R.id.pod_spinner_expense)

        // Populate the spinner with pod names.
        val podNames = podList.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, podNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        podSpinner.adapter = adapter

        // Build and show the expense dialog.
        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                val note = descriptionInput.text.toString().trim()

                if (amount != null && amount > 0) {
                    val selectedPod = podList[podSpinner.selectedItemPosition]
                    // Check for sufficient funds before saving.
                    if (selectedPod.balance >= amount) {
                        saveTransactionAndUpdatePod(amount, selectedPod, expense = true, note = note.takeIf { it.isNotEmpty() })
                    } else {
                        Toast.makeText(requireContext(), "Insufficient funds in ${selectedPod.name} Pod", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Saves a transaction and updates the corresponding pod's balance atomically.
    private fun saveTransactionAndUpdatePod(amount: Double, pod: Pod, expense: Boolean, note: String?) {
        val uid = currentUser?.uid ?: return
        val podDocRef = db.collection("users").document(uid).collection("pods").document(pod.id)
        val newTransactionRef = db.collection("users").document(uid).collection("transactions").document()

        // Create a new transaction object.
        val newTransaction = Transaction(
            id = newTransactionRef.id,
            amount = amount,
            category = pod.name, // Use pod name as category
            note = note,
            expense = expense,
            timestamp = Date(),
            podId = pod.id,
            podName = pod.name
        )

        // Run an atomic transaction to update the pod and create the transaction record.
        db.runTransaction { transaction ->
            val podSnapshot = transaction.get(podDocRef)
            val currentBalance = podSnapshot.getDouble("balance") ?: 0.0
            val currentStartingBalance = podSnapshot.getDouble("startingBalance") ?: 0.0

            // Calculate the change in balance.
            val change = if (expense) -amount else amount
            val newBalance = currentBalance + change

            // Check for sufficient funds.
            if (newBalance < 0) {
                throw Exception("Insufficient funds in ${pod.name} Pod for this transaction.")
            }

            // Update starting balance for income, otherwise keep it the same.
            val newStartingBalance = if (!expense) {
                currentStartingBalance + amount // Add to starting balance when income is added
            } else {
                currentStartingBalance // Keep it the same for expenses
            }

            // Apply the updates in the transaction.
            transaction.update(podDocRef, "balance", newBalance, "startingBalance", newStartingBalance)
            transaction.set(newTransactionRef, newTransaction)
            null // Transaction success
        }.addOnSuccessListener {
            val action = if (expense) "Expense" else "Income"
            Toast.makeText(requireContext(), "$action saved successfully!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Transaction failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Listens for real time updates to the transaction history.
    private fun listenForExpenseHistory() {
        val uid = currentUser?.uid ?: return
        db.collection("users").document(uid).collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50) // Limit to the last 50 transactions.
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("AddTransactionFragment", "Listen for history failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Map documents to Transaction objects.
                    val transactions = snapshot.documents.mapNotNull { it.toObject(Transaction::class.java)?.copy(id = it.id) }
                    // Update the adapter.
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
