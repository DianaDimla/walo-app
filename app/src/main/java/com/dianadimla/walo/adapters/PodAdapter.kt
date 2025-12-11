package com.dianadimla.walo.adapters

import android.content.res.ColorStateList
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

// Adapter for the pod list.
class PodAdapter : ListAdapter<Pod, PodAdapter.PodViewHolder>(PodDiffCallback()) {

    // Creates new views.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pod, parent, false)
        return PodViewHolder(view)
    }

    // Binds data to views.
    override fun onBindViewHolder(holder: PodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Holds the views for a pod item.
    class PodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconTextView: TextView = itemView.findViewById(R.id.pod_icon_text)
        private val nameTextView: TextView = itemView.findViewById(R.id.pod_name_text)
        private val amountTextView: TextView = itemView.findViewById(R.id.pod_amount_text)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.pod_progress_bar)

        // Binds pod data.
        fun bind(pod: Pod) {
            iconTextView.text = pod.icon
            nameTextView.text = pod.name
            amountTextView.text = String.format("€%.2f", pod.balance)

            // Show progress bar if starting balance exists.
            if (pod.startingBalance > 0) {
                progressBar.visibility = View.VISIBLE
                val progress = (pod.balance / pod.startingBalance * 100).toInt()
                progressBar.progress = progress

                // Set progress bar color based on balance.
                val context = itemView.context
                val colorRes = when {
                    progress > 50 -> R.color.progress_green
                    progress > 25 -> R.color.progress_yellow
                    else -> R.color.progress_red
                }
                val color = ContextCompat.getColor(context, colorRes)
                progressBar.progressTintList = ColorStateList.valueOf(color)

            } else {
                // Hide progress bar if no starting balance.
                progressBar.visibility = View.GONE
            }
        }
    }

    // DiffUtil for list updates.
    class PodDiffCallback : DiffUtil.ItemCallback<Pod>() {
        // Check if items are the same.
        override fun areItemsTheSame(oldItem: Pod, newItem: Pod): Boolean {
            return oldItem.id == newItem.id
        }

        // Check if contents are the same.
        override fun areContentsTheSame(oldItem: Pod, newItem: Pod): Boolean {
            return oldItem == newItem
        }
    }
}
