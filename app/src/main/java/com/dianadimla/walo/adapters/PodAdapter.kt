/**
 * Adapter for managing the list of budget pods.
 * Calculates and displays remaining budget progress using dynamic color indicators.
 */
package com.dianadimla.walo.adapters

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.R
import com.dianadimla.walo.data.Pod

class PodAdapter : ListAdapter<Pod, PodAdapter.PodViewHolder>(PodDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pod, parent, false)
        return PodViewHolder(view)
    }

    override fun onBindViewHolder(holder: PodViewHolder, position: Int) {
        val pod = getItem(position)
        holder.bind(pod)
    }

    // ViewHolder class that maps pod data to the UI components
    class PodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconTextView: TextView = itemView.findViewById(R.id.pod_icon_text)
        private val nameTextView: TextView = itemView.findViewById(R.id.pod_name_text)
        private val amountTextView: TextView = itemView.findViewById(R.id.pod_amount_text)
        private val limitTextView: TextView = itemView.findViewById(R.id.pod_limit_text)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.pod_progress_bar)

        // Updates views with pod information and calculates spending progress
        fun bind(pod: Pod) {
            iconTextView.text = pod.icon
            nameTextView.text = pod.name
            amountTextView.text = String.format("€%.2f", pod.currentSpending)

            // Calculate and display progress only if a budget limit is set
            if (pod.limit > 0) {
                limitTextView.visibility = View.VISIBLE
                limitTextView.text = String.format("/ €%.2f", pod.limit)

                // Progress represents the percentage of budget spent
                val remainingPercent = ((pod.currentSpending / pod.limit) * 100).toInt().coerceIn(0, 100)

                progressBar.visibility = View.VISIBLE
                progressBar.progress = remainingPercent

                val context = itemView.context
                // Dynamic tinting based on spending threshold
                val colorRes = when {
                    remainingPercent >= 80 -> R.color.progress_green
                    remainingPercent >= 50 -> R.color.progress_yellow
                    else -> R.color.progress_red
                }

                progressBar.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
            } else {
                limitTextView.visibility = View.GONE
                progressBar.visibility = View.GONE
            }
        }
    }

    // Handles efficient list item updates using DiffUtil
    class PodDiffCallback : DiffUtil.ItemCallback<Pod>() {
        override fun areItemsTheSame(oldItem: Pod, newItem: Pod): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Pod, newItem: Pod): Boolean = oldItem == newItem
    }
}
