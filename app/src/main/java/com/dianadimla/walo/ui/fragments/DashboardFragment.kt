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

    // View binding
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    // Local cache of the user's pods, populated by a real-time listener.
    private val podList = mutableListOf<Pod>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    // View created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Start animation
        val swimAnimation = AnimationUtils.loadAnimation(context, R.anim.slow_swim)
        binding.waloMascot.startAnimation(swimAnimation) // Start mascot animation.

        // Set navigation and action button listeners.
        binding.profileIcon.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
        binding.btnSaveIncomeDashboard.setOnClickListener {
            showIncomeDialog()
        }
        binding.btnSaveExpenseDashboard.setOnClickListener {
            showExpenseDialog()
        }

        // Fetch initial data and set up real-time listeners.
        fetchAndDisplayUserData()
        listenForTotalBudget()
        listenForPods()
    }

    // Sets up a real time listener to keep the local pod list in sync with Firestore.
    private fun listenForPods() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("pods")
            .orderBy("name") // Sort for consistent UI order.
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("DashboardFragment", "Listen for pods failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Map documents to Pod objects.
                    val pods = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Pod::class.java)?.copy(id = doc.id)
                    }
                    podList.clear() // Clear the old list.
                    podList.addAll(pods) // Update with new data.
                }
            }
    }

    // Sets up a real time listener to calculate and display the total budget from all pods.
    private fun listenForTotalBudget() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).collection("pods")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("DashboardFragment", "Listen for total budget failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Sum the balance of all pods.
                    val totalBudget = snapshot.documents.sumOf { doc ->
                        doc.getDouble("balance") ?: 0.0
                    }
                    // Format as currency and display.
                    binding.dashboardAmountDisplay.text = String.format(Locale.getDefault(), "€%.2f", totalBudget)
                }
            }
    }

    // Fetches the user's first name once for a personalized greeting.
    private fun fetchAndDisplayUserData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // If user is not logged in, redirect to login, clearing the back stack.
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
                    binding.greetingText.text = if (firstName != null) "Hi $firstName, here’s your weekly summary!" else "Hi, here’s your weekly summary!"
                } else {
                    binding.greetingText.text = "Hi, here’s your weekly summary!"
                }
            }
            .addOnFailureListener { e ->
                Log.e("DashboardFragment", "Error fetching user data", e)
                binding.greetingText.text = "Hi, here’s your weekly summary!"
            }
    }

    // Displays a dialog for adding income to a selected Pod.
    private fun showIncomeDialog() {
        if (podList.isEmpty()) {
            Toast.makeText(requireContext(), "Please create a Pod first", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_budget, null)
        val amountInput = dialogView.findViewById<EditText>(R.id.editBudgetAmount)
        val podSpinner = dialogView.findViewById<Spinner>(R.id.pod_spinner_income)

        // Use the local podList to populate the spinner.
        val podNames = podList.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, podNames)
        podSpinner.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0) {
                    val selectedPod = podList[podSpinner.selectedItemPosition]
                    // Defer database logic to the transaction function.
                    saveTransactionAndUpdatePod(amount, selectedPod, expense = false, note = "Income")
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Displays a dialog for adding an expense to a selected Pod.
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
        podSpinner.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                val note = descriptionInput.text.toString().trim()

                if (amount != null && amount > 0) {
                    val selectedPod = podList[podSpinner.selectedItemPosition]
                    // Check for sufficient funds before processing an expense.
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

    // Atomically saves a transaction and updates the corresponding Pod's balance using a Firestore transaction.
    private fun saveTransactionAndUpdatePod(amount: Double, pod: Pod, expense: Boolean, note: String?) {
        val uid = auth.currentUser?.uid ?: return
        val podDocRef = firestore.collection("users").document(uid).collection("pods").document(pod.id)
        val newTransactionRef = firestore.collection("users").document(uid).collection("transactions").document()

        // Create the new transaction object.
        val newTransaction = Transaction(newTransactionRef.id, amount, pod.name, note, expense, Date(), pod.id, pod.name)

        // Run as a transaction to ensure both writes succeed or fail together.
        firestore.runTransaction { transaction ->
            val podSnapshot = transaction.get(podDocRef) // Read the pod's current state.
            val currentBalance = podSnapshot.getDouble("balance") ?: 0.0
            val newBalance = if (expense) currentBalance - amount else currentBalance + amount // Calculate new balance.

            if (newBalance < 0) { // Server-side validation for funds.
                throw Exception("Insufficient funds in ${pod.name} Pod for this transaction.")
            }

            // For income, also update the `startingBalance` for progress tracking.
            if (!expense) {
                val currentStartingBalance = podSnapshot.getDouble("startingBalance") ?: 0.0
                transaction.update(podDocRef, "startingBalance", currentStartingBalance + amount)
            }

            // Queue the write operations.
            transaction.update(podDocRef, "balance", newBalance)
            transaction.set(newTransactionRef, newTransaction)
            null // A null return indicates success.
        }.addOnSuccessListener {
            val action = if (expense) "Expense" else "Income"
            Toast.makeText(requireContext(), "$action recorded in ${pod.name}", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Transaction failed: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Destroy view
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
