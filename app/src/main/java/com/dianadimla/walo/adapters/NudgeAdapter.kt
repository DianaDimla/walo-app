package com.dianadimla.walo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.R
import com.dianadimla.walo.data.Nudge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Adapter to display AI nudge history in a list.
class NudgeAdapter : ListAdapter<Nudge, NudgeAdapter.NudgeViewHolder>(NudgeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NudgeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nudge, parent, false)
        return NudgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: NudgeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NudgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.nudge_message)
        private val timestampText: TextView = itemView.findViewById(R.id.nudge_timestamp)
        private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

        fun bind(nudge: Nudge) {
            messageText.text = nudge.message
            timestampText.text = dateFormat.format(Date(nudge.timestamp))
        }
    }

    class NudgeDiffCallback : DiffUtil.ItemCallback<Nudge>() {
        override fun areItemsTheSame(oldItem: Nudge, newItem: Nudge): Boolean = oldItem.timestamp == newItem.timestamp
        override fun areContentsTheSame(oldItem: Nudge, newItem: Nudge): Boolean = oldItem == newItem
    }
}
