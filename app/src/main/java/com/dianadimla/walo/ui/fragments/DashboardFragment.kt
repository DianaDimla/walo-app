package com.dianadimla.walo.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.dianadimla.walo.R
import com.dianadimla.walo.data.Pod
import com.dianadimla.walo.data.Transaction
import com.dianadimla.walo.databinding.FragmentDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val podList = mutableListOf<Pod>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // --- UI Logic ---
        val swimAnimation = AnimationUtils.loadAnimation(context, R.anim.slow_swim)
        binding.waloMascot.startAnimation(swimAnimation)

        binding.profileIcon.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        binding.btnSaveIncomeDashboard.setOnClickListener {
            showIncomeDialog()
        }

        binding.btnSaveExpenseDashboard.setOnClickListener {
            showExpenseDialog()
        }

        // --- Data Fetching Logic ---
        fetchAndDisplayUserData()
        listenForTotalBudget()
        listenForPods()
    }

    private fun listenForPods() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("pods")
            .orderBy("name")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("DashboardFragment", "Listen for pods failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val pods = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Pod::class.java)?.copy(id = doc.id)
                    }
                    podList.clear()
                    podList.addAll(pods)
                }
            }
    }

    private fun listenForTotalBudget() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).collection("pods")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("DashboardFragment", "Listen for total budget failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val totalBudget = snapshot.documents.sumOf { doc ->
                        doc.getDouble("balance") ?: 0.0
                    }
                    binding.dashboardAmountDisplay.text = String.format(Locale.getDefault(), "€%.2f", totalBudget)
                }
            }
    }

    private fun fetchAndDisplayUserData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.dashboardFragment, true)
                .build()
            findNavController().navigate(R.id.loginFragment, null, navOptions)
            return
        }

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName")
                    if (firstName != null) {
                        binding.greetingText.text = "Hi $firstName, here’s your weekly summary!"
                    } else {
                        binding.greetingText.text = "Hi, here’s your weekly summary!"
                    }
                } else {
                    Toast.makeText(context, "Could not find user data.", Toast.LENGTH_SHORT).show()
                    binding.greetingText.text = "Hi, here’s your weekly summary!"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching user data: ${e.message}", Toast.LENGTH_LONG).show()
                binding.greetingText.text = "Hi, here’s your weekly summary!"
            }
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
        val uid = auth.currentUser?.uid ?: return
        val podDocRef = firestore.collection("users").document(uid).collection("pods").document(pod.id)
        val newTransactionRef = firestore.collection("users").document(uid).collection("transactions").document()

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

        firestore.runTransaction { transaction ->
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}
