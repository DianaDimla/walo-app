package com.dianadimla.walo.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.dianadimla.walo.R
import com.dianadimla.walo.data.Pod
import com.dianadimla.walo.databinding.FragmentReportsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ReportsFragment : Fragment() {

    // View binding
    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private val db = Firebase.firestore
    private val currentUser = Firebase.auth.currentUser

    // Inflate the layout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // View created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenForPodUpdates()
    }

    // Listen for pod updates
    private fun listenForPodUpdates() {
        val uid = currentUser?.uid ?: return

        db.collection("users").document(uid).collection("pods")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ReportsFragment", "Listen for pods failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val pods = snapshot.toObjects(Pod::class.java)
                    setupBarChart(pods)
                } else {
                    Log.d("ReportsFragment", "No pods found.")
                    binding.barChart.clear() // Clear chart
                    binding.barChart.invalidate()
                }
            }
    }

    // Set up the bar chart
    private fun setupBarChart(pods: List<Pod>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val barColors = ArrayList<Int>()
        var index = 0f
        
        val sortedPods = pods.sortedBy { it.name }

        sortedPods.forEach { pod ->
            entries.add(BarEntry(index, pod.balance.toFloat())) // Use current balance
            labels.add(pod.name)

            // Calculate progress
            val progress = if (pod.startingBalance > 0) {
                (pod.balance / pod.startingBalance * 100).toInt()
            } else {
                0 // Progress is zero if starting balance is zero
            }
            barColors.add(getProgressBarColor(progress)) // Get color
            index++
        }

        val dataSet = BarDataSet(entries, "Pod Balances")
        dataSet.colors = barColors // Use dynamic colors
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)

        binding.barChart.apply {
            this.data = barData
            description.isEnabled = false
            legend.isEnabled = false

            // Disable zooming
            setScaleEnabled(false)
            isDoubleTapToZoomEnabled = false

            // X-Axis style
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true

            // Y-Axis style
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false

            animateY(1000) // Animate
            invalidate()
        }
    }

    // Get progress bar color
    private fun getProgressBarColor(progress: Int): Int {
        val colorRes = when {
            progress > 50 -> R.color.progress_green
            progress > 25 -> R.color.progress_yellow
            else -> R.color.progress_red
        }
        return ContextCompat.getColor(requireContext(), colorRes)
    }

    // Destroy view
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
