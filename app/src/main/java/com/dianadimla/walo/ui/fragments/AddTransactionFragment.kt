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
import androidx.recyclerview.widget.LinearLayoutManager
import com.dianadimla.walo.R
import com.dianadimla.walo.adapters.TransactionAdapter
import com.dianadimla.walo.data.Transaction
import com.dianadimla.walo.databinding.FragmentAddTransactionBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date
import java.util.Locale

// A fragment for adding new income or expense transactions and displaying transaction history.

class AddTransactionFragment : Fragment() {

    // View binding for the fragment's layout to safely access views.
    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    // Adapter for the RecyclerView that displays transaction history.
    private lateinit var transactionAdapter: TransactionAdapter

    // Inflates the fragment's layout.
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Sets up UI components and listeners after the view has been created.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        listenForBudgetChanges()
        listenForExpenseHistory()

        // Set click listener for the income button to show the income dialog.
        binding.btnSaveIncome.setOnClickListener {
            showIncomeDialog()
        }

        // Set click listener for the expense button to show the expense dialog.
        binding.btnSaveExpense.setOnClickListener {
            showExpenseDialog()
        }
    }

    // Initializes the RecyclerView for displaying the expense history.
    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.expenseHistoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    // Displays a dialog for the user to enter an income amount.
    private fun showIncomeDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_budget, null)
        val amountInput = dialogView.findViewById<EditText>(R.id.editBudgetAmount)

        AlertDialog.Builder(requireContext())
            .setTitle("Enter Income Amount (€)")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0) {
                    saveTransactionAndApplyToBudget(amount, expense = false, category = "Income", note = null)
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Displays a dialog for the user to add a new expense with details.
    private fun showExpenseDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null)
        val amountInput = dialogView.findViewById<EditText>(R.id.dialog_expense_amount)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.dialog_expense_category)
        val noteInput = dialogView.findViewById<EditText>(R.id.dialog_expense_note)

        // Setup category spinner with predefined categories.
        val categories = listOf("Food", "Transport", "Social", "Rent", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setTitle("Add Expense")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                val category = categorySpinner.selectedItem.toString()
                val note = noteInput.text.toString().trim()
                
                if (amount != null && amount > 0) {
                    saveTransactionAndApplyToBudget(amount, expense = true, category = category, note = note.takeIf { it.isNotEmpty() })
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Listens for real-time updates to the user's remaining budget in Firestore and updates the UI.
    private fun listenForBudgetChanges() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) { return@addSnapshotListener }
                
                val budget = snapshot.getDouble("remainingBudget") ?: 0.0
                val formattedBudget = String.format(Locale.getDefault(), "€%.2f", budget)
                binding.amountDisplay.text = formattedBudget
            }
    }

    // Saves a new transaction to Firestore and updates the user's total budget in a single atomic operation.
    private fun saveTransactionAndApplyToBudget(amount: Double, expense: Boolean, category: String, note: String?) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val userDocRef = Firebase.firestore.collection("users").document(uid)
        val transactionsColRef = userDocRef.collection("transactions")

        val newTransaction = Transaction(
            amount = amount,
            category = category,
            note = note,
            expense = expense,
            timestamp = Date()
        )

        // Use a Firestore transaction to ensure data consistency (budget update and transaction creation succeed or fail together).
        Firebase.firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDocRef)
            val currentBudget = snapshot.getDouble("remainingBudget") ?: 0.0
            val change = if (expense) -amount else amount
            val newBudget = currentBudget + change
            transaction.update(userDocRef, "remainingBudget", newBudget)
            transaction.set(transactionsColRef.document(), newTransaction)
            null
        }.addOnSuccessListener {
            val action = if (expense) "Expense" else "Income"
            Toast.makeText(requireContext(), "$action of €$amount saved!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to save transaction: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Listens for real-time updates to the expense history in Firestore and updates the RecyclerView.
    private fun listenForExpenseHistory() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("users").document(uid)
            .collection("transactions")
            .whereEqualTo("expense", true) // Query for documents where 'expense' is true.
            .orderBy("timestamp", Query.Direction.DESCENDING) // Order by newest first.
            .limit(20) // Limit to the last 20 transactions.
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AddTransactionFragment", "Error listening for expense history", e)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    return@addSnapshotListener
                }

                // Map the Firestore documents to Transaction objects.
                val transactions = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                }
                // Submit the new list to the adapter to update the UI.
                transactionAdapter.submitList(transactions)
            }
    }

    // Cleans up the view binding when the fragment's view is destroyed to prevent memory leaks.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
