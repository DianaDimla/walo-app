package com.dianadimla.walo.ui.fragments

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.R
import com.dianadimla.walo.adapters.NudgeAdapter
import com.dianadimla.walo.adapters.PodAdapter
import com.dianadimla.walo.adapters.TransactionAdapter
import com.dianadimla.walo.data.GamificationManager
import com.dianadimla.walo.data.NudgeManager
import com.dianadimla.walo.data.NudgeStorage
import com.dianadimla.walo.data.Pod
import com.dianadimla.walo.data.Transaction
import com.dianadimla.walo.data.TransactionsRepository
import com.dianadimla.walo.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// Main dashboard displaying user spending, pods, and recent activity
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val podList = mutableListOf<Pod>()

    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var podAdapter: PodAdapter 
    
    private lateinit var transactionsRepository: TransactionsRepository
    private lateinit var gamificationManager: GamificationManager
    private lateinit var nudgeManager: NudgeManager
    
    // Store listeners to remove them when view is destroyed to prevent crashes
    private val registrations = mutableListOf<ListenerRegistration>()
    
    // Queue for achievements to show them one after another only after user interaction
    private val achievementQueue = mutableListOf<String>()
    private var isDialogShowing = false

    private val pastelPalette = listOf(
        "#FFB3BA", "#FFDFBA", "#FFFFBA", "#BAFFC9", "#BAE1FF",
        "#D4A5A5", "#F3E5AB", "#B2E2F2", "#E3D1FB", "#F19CBB",
        "#C1E1C1", "#FFD1DC", "#ECEAE4", "#A2C4C9", "#C5B4E3", "#F9F1F0"
    ).map { Color.parseColor(it) }

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
        
        // Initialize managers and transaction repository
        gamificationManager = GamificationManager(auth, firestore)
        nudgeManager = NudgeManager()
        transactionsRepository = TransactionsRepository(auth, firestore, gamificationManager, nudgeManager)

        // Set achievement unlock listener to use the queue
        gamificationManager.onAchievementUnlocked = { title ->
            queueAchievementDialog(title)
        }

        // Trigger daily streak check when the dashboard loads
        gamificationManager.onAppOpened()

        setupRecyclerView()

        val swimAnimation = AnimationUtils.loadAnimation(context, R.anim.slow_swim)
        binding.waloMascot.startAnimation(swimAnimation)

        binding.profileIcon.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
        
        binding.trophyIcon.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_achievements)
        }

        // Notification Bell Click: Show AI Nudge Dropdown Panel
        binding.notificationBell.setOnClickListener {
            showNudgeDropdown(it)
        }
        
        binding.btnSaveIncomeDashboard.setOnClickListener {
            showIncomeDialog()
        }
        binding.btnSaveExpenseDashboard.setOnClickListener {
            showExpenseDialog()
        }
        binding.btnViewAllTransactions.setOnClickListener {
            findNavController().navigate(R.id.addTransactionFragment)
        }

        fetchAndDisplayUserData()
        listenForTotalBudget()
        listenForPods()
        listenForRecentTransactions()
        listenForMonthlySpending()
        listenForStreak()
    }

    // Shows a dropdown PopupWindow containing AI nudge history with enhanced UI.
    // anchor The view (bell icon) that the dropdown will anchor to.
    private fun showNudgeDropdown(anchor: View) {
        val layoutInflater = LayoutInflater.from(requireContext())
        val popupView = layoutInflater.inflate(R.layout.popup_nudge_history, null)

        // Create the PopupWindow with slide-down animation
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            animationStyle = R.style.PopupAnimation // Added slide-down animation
            elevation = 15f
        }

        val recyclerView = popupView.findViewById<RecyclerView>(R.id.nudge_recycler_view)
        val emptyState = popupView.findViewById<TextView>(R.id.nudge_empty_state)
        val clearButton = popupView.findViewById<Button>(R.id.btn_clear_nudges)
        val nudgeAdapter = NudgeAdapter()

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = nudgeAdapter

        val nudges = NudgeStorage.getAllNudges()
        
        // Handle Empty State Visibility
        if (nudges.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            clearButton.isEnabled = false
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            clearButton.isEnabled = true
            nudgeAdapter.submitList(nudges)
        }

        clearButton.setOnClickListener {
            NudgeStorage.clearNudges()
            nudgeAdapter.submitList(emptyList())
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            clearButton.isEnabled = false
            Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
        }

        // Position anchored to bell icon with offset to stay on screen
        popupWindow.showAsDropDown(anchor, -260, 10)
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
                "\uD83C\uDFC6 Achievement Unlocked!"
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

    private fun setupRecyclerView() {
        // Setup Transaction Adapter
        transactionAdapter = TransactionAdapter()
        binding.recentTransactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
            isNestedScrollingEnabled = false
        }

        // NEW: Setup PodAdapter for Dashboard
        podAdapter = PodAdapter()
        binding.podsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = podAdapter
            isNestedScrollingEnabled = false
        }
    }

    // Listens for realtime updates to the user's activity streak
    private fun listenForStreak() {
        val uid = auth.currentUser?.uid ?: return
        val registration = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener
                
                if (e != null) {
                    Log.w("DashboardFragment", "Listen for streak failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val stats = snapshot.get("stats") as? Map<*, *>
                    val streak = (stats?.get("currentStreak") as? Number)?.toInt() ?: 0
                    
                    if (streak >= 3) {
                        binding.streakContainer.visibility = View.VISIBLE
                        binding.streakNumberText.text = streak.toString()
                    } else {
                        binding.streakContainer.visibility = View.GONE
                    }
                }
            }
        registrations.add(registration)
    }

    // Listens for real time updates to recent transactions
    private fun listenForRecentTransactions() {
        val uid = auth.currentUser?.uid ?: return
        val registration = firestore.collection("users").document(uid).collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(5)
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener
                
                if (e != null) {
                    Log.w("DashboardFragment", "Listen for recent transactions failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { it.toObject(Transaction::class.java)?.copy(id = it.id) }
                    transactionAdapter.submitList(transactions)
                }
            }
        registrations.add(registration)
    }

    // Listens for monthly transactions to update the spending chart
    private fun listenForMonthlySpending() {
        val uid = auth.currentUser?.uid ?: return
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.time

        val registration = firestore.collection("users").document(uid).collection("transactions")
            .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener
                
                if (e != null) {
                    Log.w("DashboardFragment", "Listen for monthly transactions failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { it.toObject(Transaction::class.java) }
                    updateSpendingChart(transactions)
                }
            }
        registrations.add(registration)
    }

    // Updates the pie chart with categorized spending data
    private fun updateSpendingChart(transactions: List<Transaction>) {
        val spendingByCategory = transactions
            .filter { it.expense }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        if (spendingByCategory.isEmpty()) {
            binding.spendingPieChart.visibility = View.GONE
            binding.emptyStateText.visibility = View.VISIBLE
            return
        }

        binding.spendingPieChart.visibility = View.VISIBLE
        binding.emptyStateText.visibility = View.GONE

        val entries = spendingByCategory.map { PieEntry(it.value.toFloat(), it.key) }
        
        val sliceColors = entries.map { entry ->
            val index = abs(entry.label.hashCode()) % pastelPalette.size
            pastelPalette[index]
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = sliceColors
            valueTextSize = 12f
            setDrawValues(true)
        }

        binding.spendingPieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setHoleColor(Color.TRANSPARENT)
            setEntryLabelColor(Color.BLACK)
            animateY(1000)
            invalidate()
        }
    }

    // Keeps local pod data synchronized with Firestore and updates UI
    private fun listenForPods() {
        val uid = auth.currentUser?.uid ?: return
        val registration = firestore.collection("users").document(uid).collection("pods")
            .orderBy("name")
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener
                
                if (e != null) {
                    Log.w("DashboardFragment", "Listen for pods failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val pods = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Pod::class.java)?.copy(id = doc.id)
                    }
                    
                    // Added Debug logs to track real-time updates
                    Log.d("DashboardUpdate", "Firestore listener triggered. Received ${pods.size} pods.")
                    pods.forEach { Log.d("DashboardUpdate", "Pod: ${it.name}, CurrentSpending: ${it.currentSpending}") }

                    podList.clear()
                    podList.addAll(pods)
                    
                    // Update the adapter with the new list
                    podAdapter.submitList(pods.toList()) 
                }
            }
        registrations.add(registration)
    }

    // Listens for budget changes to update the total display
    private fun listenForTotalBudget() {
        val userId = auth.currentUser?.uid ?: return
        val registration = firestore.collection("users").document(userId).collection("pods")
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener
                
                if (e != null) {
                    Log.w("DashboardFragment", "Listen for total budget failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Refactored to use 'currentSpending' instead of 'balance'
                    val totalSpending = snapshot.documents.sumOf { doc ->
                        doc.getDouble("currentSpending") ?: 0.0
                    }
                    binding.dashboardAmountDisplay.text = String.format(Locale.getDefault(), "€%.2f", totalSpending)
                }
            }
        registrations.add(registration)
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
                if (_binding == null) return@addOnSuccessListener
                
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName")
                    binding.greetingText.text = if (firstName != null) "Hi $firstName, here’s your weekly summary!" else "Hi, here’s your weekly summary!"
                } else {
                    binding.greetingText.text = "Hi, here’s your weekly summary!"
                }
            }
            .addOnFailureListener { e ->
                Log.e("DashboardFragment", "Error fetching user data", e)
                if (_binding != null) {
                    binding.greetingText.text = "Hi, here’s your weekly summary!"
                }
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
        podSpinner.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                val note = descriptionInput.text.toString().trim()

                if (amount != null && amount > 0) {
                    val selectedPod = podList[podSpinner.selectedItemPosition]
                    // Use 'currentSpending' instead of 'balance'
                    val newSpending = selectedPod.currentSpending + amount
                    if (selectedPod.limit == 0.0 || newSpending <= selectedPod.limit) {
                        saveTransactionAndUpdatePod(amount, selectedPod, expense = true, note = note.takeIf { it.isNotEmpty() })
                    } else {
                        Toast.makeText(requireContext(), "This expense exceeds your ${selectedPod.name} limit", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Prepares and saves a transaction through the repository
    private fun saveTransactionAndUpdatePod(amount: Double, pod: Pod, expense: Boolean, note: String?) {
        val newTransaction = Transaction(
            amount = amount,
            category = pod.name,
            note = note,
            expense = expense,
            timestamp = Date(),
            podId = pod.id,
            podName = pod.name
        )

        // Delegates transaction saving and gamification triggering to the repository
        transactionsRepository.saveTransaction(
            podId = pod.id,
            amount = amount,
            expense = expense,
            transaction = newTransaction,
            onSuccess = { nudgeMessage ->
                if (_binding == null) return@saveTransaction
                val action = if (expense) "Expense" else "Income"
                
                // Show nudge if present, else standard success
                val message = nudgeMessage ?: "$action recorded in ${pod.name}"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            },
            onFailure = { e ->
                if (_binding == null) return@saveTransaction
                Toast.makeText(requireContext(), "Transaction failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up listeners to prevent memory leaks and crashes from background updates
        registrations.forEach { it.remove() }
        registrations.clear()
        _binding = null
    }
}
