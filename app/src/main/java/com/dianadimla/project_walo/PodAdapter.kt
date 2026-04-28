/**
 * Adapter class for managing and displaying the list of budget pods in a RecyclerView.
 */
package com.dianadimla.project_walo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.R

// Handles the display and updates of Pod items in a list
class PodAdapter : ListAdapter<Pod, PodAdapter.PodViewHolder>(PodDiffCallback()) {

    // Inflates the layout for individual pod items
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pod, parent, false)
        return PodViewHolder(view)
    }

    // Connects pod data to the view holder at a specific position
    override fun onBindViewHolder(holder: PodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ViewHolder class to hold and bind UI components for each pod item
    class PodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.pod_name_text)
        private val amountTextView: TextView = itemView.findViewById(R.id.pod_amount_text)

        // Maps pod data to the UI elements
        fun bind(pod: Pod) {
            nameTextView.text = pod.name
            amountTextView.text = String.format("€%.2f", pod.balance)
        }
    }

    // Efficiently calculates differences between old and new lists
    class PodDiffCallback : DiffUtil.ItemCallback<Pod>() {
        // Compares unique identifiers to check if items are identical
        override fun areItemsTheSame(oldItem: Pod, newItem: Pod): Boolean {
            return oldItem.id == newItem.id
        }

        // Compares data content to check if UI update is necessary
        override fun areContentsTheSame(oldItem: Pod, newItem: Pod): Boolean {
            return oldItem == newItem
        }
    }
}
