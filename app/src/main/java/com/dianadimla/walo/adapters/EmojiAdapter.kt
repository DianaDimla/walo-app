package com.dianadimla.walo.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.R

// Adapter for the emoji selection grid.
class EmojiAdapter(
    private val emojis: List<String>, // List of emojis to display.
    private val onEmojiSelected: (String) -> Unit // Callback when an emoji is picked.
) : RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {

    // Tracks the currently selected emoji's position.
    private var selectedPosition = -1

    // Creates new views.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_emoji, parent, false)
        return EmojiViewHolder(view)
    }

    // Binds data and handles selection.
    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        val emoji = emojis[position]
        holder.bind(emoji)

        // Highlight the selected emoji.
        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(Color.LTGRAY)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        holder.itemView.setOnClickListener {
            // Update selection and notify adapter.
            if (selectedPosition != position) {
                val previousSelectedPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousSelectedPosition)
                notifyItemChanged(selectedPosition)
                onEmojiSelected(emoji)
            }
        }
    }

    // Returns the total item count.
    override fun getItemCount(): Int = emojis.size

    // Holds the view for an emoji item.
    class EmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val emojiTextView: TextView = itemView.findViewById(R.id.emoji_text)

        // Sets the emoji text.
        fun bind(emoji: String) {
            emojiTextView.text = emoji
        }
    }
}
