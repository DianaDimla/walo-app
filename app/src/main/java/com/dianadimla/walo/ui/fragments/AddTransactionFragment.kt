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
import com.dianadimla.walo.R
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

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var podAdapter: PodAdapter
    private val podList = mutableListOf<Pod>()

    private val db = Firebase.firestore
    private val currentUser = Firebase.auth.currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            showDeletePodDialog()
        }
    }

    private fun setupRecyclerViews() {
        transactionAdapter = TransactionAdapter()
        binding.expenseHistoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }

        podAdapter = PodAdapter()
        binding.podsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = podAdapter
        }
    }

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
                    val pods = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Pod::class.java)?.copy(id = doc.id)
                    }
                    podList.clear()
                    podList.addAll(pods)
                    podAdapter.submitList(pods.toList()) // Submit a copy

                    val totalBudget = pods.sumOf { it.balance }
                    binding.amountDisplay.text = String.format(Locale.getDefault(), "€%.2f", totalBudget)
                }
            }
    }

    private fun showCreatePodDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_pod, null)
        val podNameInput = dialogView.findViewById<EditText>(R.id.pod_name_input)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val name = podNameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    saveNewPod(name)
                } else {
                    Toast.makeText(requireContext(), "Pod name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveNewPod(podName: String) {
        val uid = currentUser?.uid ?: return
        val newPodRef = db.collection("users").document(uid).collection("pods").document()
        val pod = Pod(id = newPodRef.id, name = podName, balance = 0.0, startingBalance = 0.0)

        newPodRef.set(pod)
            .addOnSuccessListener { Toast.makeText(requireContext(), "Pod '$podName' created!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), "Failed to create pod: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun showDeletePodDialog() {
        if (podList.isEmpty()) {
            Toast.makeText(requireContext(), "There are no pods to delete.", Toast.LENGTH_SHORT).show()
            return
        }

        val podNames = podList.map { it.name }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select a Pod to Delete")
            .setItems(podNames) { _, which ->
                val selectedPod = podList[which]
                confirmAndDeletePod(selectedPod)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

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

    private fun deletePod(pod: Pod) {
        val uid = currentUser?.uid ?: return
        db.collection("users").document(uid).collection("pods").document(pod.id)
            .delete()
            .addOnSuccessListener { Toast.makeText(requireContext(), "Pod '${pod.name}' deleted successfully.", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), "Failed to delete pod: ${e.message}", Toast.LENGTH_LONG).show() }
    }


    private fun showIncomeDialog() {
        if (podList.isEmpty()) {
            Toast.makeText(requireContext(), "Please create a Pod first", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_budget, null)
        val amountInput = dialogView.findViewById<EditText>(R.id.editBudgetAmount)
        val podSpinner = dialogView.findViewById<Spinner>(R.id.pod_spinner_income)

        val podNames = podList.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, podNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        podSpinner.adapter = adapter

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

    private fun showExpenseDialog() {
        if (podList.isEmpty()) {
            Toast.makeText(requireContext(), "Please create a Pod first", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null)
        val amountInput = dialogView.findViewById<EditText>(R.id.expense_amount_input)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.expense_description_input)
        val podSpinner = dialogView.findViewById<Spinner>(R.id.pod_spinner_expense)

        val podNames = podList.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, podNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        podSpinner.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                val note = descriptionInput.text.toString().trim()

                if (amount != null && amount > 0) {
                    val selectedPod = podList[podSpinner.selectedItemPosition]
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

    private fun saveTransactionAndUpdatePod(amount: Double, pod: Pod, expense: Boolean, note: String?) {
        val uid = currentUser?.uid ?: return
        val podDocRef = db.collection("users").document(uid).collection("pods").document(pod.id)
        val newTransactionRef = db.collection("users").document(uid).collection("transactions").document()

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

        db.runTransaction { transaction ->
            val podSnapshot = transaction.get(podDocRef)
            val currentBalance = podSnapshot.getDouble("balance") ?: 0.0
            val currentStartingBalance = podSnapshot.getDouble("startingBalance") ?: 0.0

            val change = if (expense) -amount else amount
            val newBalance = currentBalance + change

            if (newBalance < 0) {
                throw Exception("Insufficient funds in ${pod.name} Pod for this transaction.")
            }

            val newStartingBalance = if (!expense) {
                currentStartingBalance + amount // Add to starting balance when income is added
            } else {
                currentStartingBalance // Keep it the same for expenses
            }

            transaction.update(podDocRef, "balance", newBalance, "startingBalance", newStartingBalance)
            transaction.set(newTransactionRef, newTransaction)
            null // Transaction success
        }.addOnSuccessListener {
            val action = if (expense) "Expense" else "Income"
            Toast.makeText(requireContext(), "$action of €$amount recorded in ${pod.name}", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Transaction failed: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun listenForExpenseHistory() {
        val uid = currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AddTransactionFragment", "Error listening for expense history", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Transaction::class.java)?.copy(id = doc.id)
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
