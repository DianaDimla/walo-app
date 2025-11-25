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
        private val progressBar: ProgressBar = itemView.findViewById(R.id.pod_progress_bar)

        fun bind(pod: Pod) {
            nameTextView.text = pod.name
            amountTextView.text = String.format("€%.2f", pod.balance)

            if (pod.startingBalance > 0) {
                progressBar.visibility = View.VISIBLE
                val progress = (pod.balance / pod.startingBalance * 100).toInt()
                progressBar.progress = progress

                val context = itemView.context
                val colorRes = when {
                    progress > 50 -> R.color.progress_green
                    progress > 25 -> R.color.progress_yellow
                    else -> R.color.progress_red
                }
                val color = ContextCompat.getColor(context, colorRes)
                progressBar.progressTintList = ColorStateList.valueOf(color)

            } else {
                progressBar.visibility = View.GONE
            }
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
