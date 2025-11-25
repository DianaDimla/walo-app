package com.dianadimla.project_walo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.R

class PodAdapter : ListAdapter<Pod, PodAdapter.PodViewHolder>(PodDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pod, parent, false)
        return PodViewHolder(view)
    }

    override fun onBindViewHolder(holder: PodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.pod_name_text)
        private val amountTextView: TextView = itemView.findViewById(R.id.pod_amount_text)

        fun bind(pod: Pod) {
            nameTextView.text = pod.name
            amountTextView.text = String.format("€%.2f", pod.balance)
        }
    }

    class PodDiffCallback : DiffUtil.ItemCallback<Pod>() {
        override fun areItemsTheSame(oldItem: Pod, newItem: Pod): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Pod, newItem: Pod): Boolean {
            return oldItem == newItem
        }
    }
}
