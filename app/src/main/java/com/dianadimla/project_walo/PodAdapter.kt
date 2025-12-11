package com.dianadimla.project_walo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.R
// Adapter for the pod list.
class PodAdapter : ListAdapter<Pod, PodAdapter.PodViewHolder>(PodDiffCallback()) {

    // Creates new views for pod items.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pod, parent, false)
        return PodViewHolder(view)
    }

    // Binds pod data to a view.
    override fun onBindViewHolder(holder: PodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // View holder for a single pod item.
    class PodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.pod_name_text)
        private val amountTextView: TextView = itemView.findViewById(R.id.pod_amount_text)

        // Populates the view with pod data.
        fun bind(pod: Pod) {
            nameTextView.text = pod.name
            amountTextView.text = String.format("€%.2f", pod.balance)
        }
    }

    // DiffUtil for calculating list changes.
    class PodDiffCallback : DiffUtil.ItemCallback<Pod>() {
        /** Checks if IDs are the same. */
        override fun areItemsTheSame(oldItem: Pod, newItem: Pod): Boolean {
            return oldItem.id == newItem.id
        }

        // Checks if all data is the same.
        override fun areContentsTheSame(oldItem: Pod, newItem: Pod): Boolean {
            return oldItem == newItem
        }
    }
}
