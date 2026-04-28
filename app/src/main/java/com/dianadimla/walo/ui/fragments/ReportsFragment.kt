/**
 * Fragment responsible for visualising financial reports and trends.
 * Displays weekly spending graphs and monthly category distributions using MPAndroidChart.
 */
package com.dianadimla.walo.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.data.*
import com.dianadimla.walo.databinding.FragmentReportsBinding
import com.dianadimla.walo.databinding.ItemWeeklyGraphBinding
import com.dianadimla.walo.viewmodels.InsightsViewModel
import com.dianadimla.walo.utils.ColorUtils
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val nudgeManager = NudgeManager.getInstance()
    private val gamificationManager = GamificationManager(auth, db)
    private val repository = TransactionsRepository(auth, db, gamificationManager, nudgeManager)

    private val insightsViewModel: InsightsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupInsightsObserver()
        setupViewPager()
        setupMonthSpinner()
        
        // Initialises insight calculation for the current user
        if (auth.currentUser != null) {
            insightsViewModel.calculateWeeklyInsights()
        }
    }

    // Observes AI-generated insight messages for display
    private fun setupInsightsObserver() {
        insightsViewModel.insightMessages.observe(viewLifecycleOwner) { messages ->
            if (messages != null && messages.isNotEmpty()) {
                binding.tvInsightMessages.text = messages.joinToString("\n\n")
            } else {
                binding.tvInsightMessages.text = "No transactions found for this week yet."
            }
        }
    }

    // Configures the ViewPager to display weekly comparison graphs
    private fun setupViewPager() {
        val offsets = listOf(-1, 0) // Represents last week and current week
        val adapter = WeeklyGraphAdapter(offsets, repository)
        binding.vpWeeklyGraphs.adapter = adapter
        binding.vpWeeklyGraphs.setCurrentItem(1, false)
    }

    // Configures the month selector for historical spending analysis
    private fun setupMonthSpinner() {
        val monthNames = mutableListOf<String>()
        val monthOffsets = mutableListOf<Int>()
        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        // Generates the last 6 months for selection
        for (i in 0 downTo -5) {
            val tempCal = Calendar.getInstance()
            tempCal.add(Calendar.MONTH, i)
            monthNames.add(monthFormat.format(tempCal.time))
            monthOffsets.add(i)
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, monthNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.monthSpinner.adapter = adapter

        binding.monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMonth = monthNames[position]
                val selectedOffset = monthOffsets[position]
                
                binding.tvSelectedMonthLabel.text = "Data for $selectedMonth"
                loadMonthlyData(selectedOffset)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // Retrieves transaction data for the selected month from the repository
    private fun loadMonthlyData(offset: Int) {
        repository.getMonthlyCategorySpending(offset, { spendingMap ->
            if (_binding != null && isAdded) {
                updatePieChart(spendingMap)
            }
        }, {
            // Error handling for data retrieval
        })
    }

    // Visualises categorical spending using a pie chart
    private fun updatePieChart(spendingByCategory: Map<String, Double>) {
        if (spendingByCategory.isEmpty()) {
            binding.spendingPieChart.visibility = View.GONE
            binding.emptyStateText.visibility = View.VISIBLE
            return
        }

        binding.spendingPieChart.visibility = View.VISIBLE
        binding.emptyStateText.visibility = View.GONE

        // Formats data entries with value labels for the legend
        val entries = spendingByCategory.map { (category, amount) ->
            PieEntry(amount.toFloat(), "$category (€${amount.toInt()})") 
        }
        
        // Maps categories to consistent theme colours
        val sliceColors = spendingByCategory.keys.map { category ->
            ColorUtils.getCategoryColor(category)
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = sliceColors
            sliceSpace = 3f
            selectionShift = 5f
            setDrawValues(false)
        }

        binding.spendingPieChart.apply {
            data = PieData(dataSet)
            setUsePercentValues(false)
            setDrawEntryLabels(false)
            description.isEnabled = false
            
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.CENTER
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
                xEntrySpace = 10f
                yEntrySpace = 8f
                textSize = 12f
            }

            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f

            animateY(1200)
            invalidate()
        }
    }

    // Adapter for managing the weekly spending line charts
    inner class WeeklyGraphAdapter(
        private val offsets: List<Int>,
        private val repository: TransactionsRepository
    ) : RecyclerView.Adapter<WeeklyGraphAdapter.GraphViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GraphViewHolder {
            val binding = ItemWeeklyGraphBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return GraphViewHolder(binding)
        }

        override fun onBindViewHolder(holder: GraphViewHolder, position: Int) {
            holder.bind(offsets[position])
        }

        override fun getItemCount(): Int = offsets.size

        inner class GraphViewHolder(private val itemBinding: ItemWeeklyGraphBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            
            // Initialises the weekly chart with data for the specified offset
            fun bind(offset: Int) {
                itemBinding.tvWeekLabel.text = calculateWeekRangeLabel(offset)

                repository.getWeeklySpending(offset, { values, labels ->
                    if (_binding != null && isAdded) {
                        setupLineChart(itemBinding, values, labels)
                    }
                }, { })
            }

            // Calculates the human-readable date range for the week offset
            private fun calculateWeekRangeLabel(weekOffset: Int): String {
                val calendar = Calendar.getInstance()
                val displayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                calendar.add(Calendar.DAY_OF_YEAR, weekOffset * 7)
                val endDate = calendar.time
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                val startDate = calendar.time
                return "Week of ${displayFormat.format(startDate)} – ${displayFormat.format(endDate)}"
            }

            // Configures the daily spending line chart visualisations
            private fun setupLineChart(itemBinding: ItemWeeklyGraphBinding, values: List<Float>, labels: List<String>) {
                if (values.isEmpty() || labels.isEmpty()) return
                val entries = values.mapIndexed { index, value -> Entry(index.toFloat(), value) }
                if (entries.isEmpty()) return

                val dataSet = LineDataSet(entries, "Daily Spending").apply {
                    color = Color.parseColor("#1E88E5")
                    setCircleColor(Color.parseColor("#1E88E5"))
                    lineWidth = 2.5f
                    circleRadius = 4.5f
                    setDrawCircleHole(false)
                    valueTextSize = 10f
                    setDrawFilled(true)
                    fillColor = Color.parseColor("#1E88E5")
                    fillAlpha = 40 
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawValues(values.any { it > 0 }) 
                }

                itemBinding.lineChart.apply {
                    data = LineData(dataSet)
                    description.isEnabled = false
                    legend.isEnabled = false
                    setScaleEnabled(false)
                    xAxis.apply {
                        valueFormatter = IndexAxisValueFormatter(labels)
                        position = XAxis.XAxisPosition.BOTTOM
                        setDrawGridLines(false)
                        granularity = 1f
                        textColor = Color.GRAY
                    }
                    axisLeft.apply {
                        axisMinimum = 0f
                        setDrawGridLines(true)
                        gridColor = Color.LTGRAY
                        textColor = Color.GRAY
                    }
                    axisRight.isEnabled = false
                    animateX(1000)
                    invalidate()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
