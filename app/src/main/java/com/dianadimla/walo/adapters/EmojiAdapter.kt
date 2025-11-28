package com.dianadimla.walo.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.R

class EmojiAdapter(
    private val emojis: List<String>,
    private val onEmojiSelected: (String) -> Unit
) : RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_emoji, parent, false)
        return EmojiViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        val emoji = emojis[position]
        holder.bind(emoji)

        // Highlight the selected emoji
        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(Color.LTGRAY)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        holder.itemView.setOnClickListener {
            // Update selection
            if (selectedPosition != position) {
                val previousSelectedPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousSelectedPosition)
                notifyItemChanged(selectedPosition)
                onEmojiSelected(emoji)
            }
        }
    }

    override fun getItemCount(): Int = emojis.size

    class EmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val emojiTextView: TextView = itemView.findViewById(R.id.emoji_text)

        fun bind(emoji: String) {
            emojiTextView.text = emoji
        }
    }
}
