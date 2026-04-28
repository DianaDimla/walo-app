/**
 * Adapter for displaying gamification achievements in a RecyclerView.
 * Manages both locked and unlocked states with distinct visual styles.
 */
package com.dianadimla.walo.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.R
import com.dianadimla.walo.databinding.ItemAchievementBinding
import com.dianadimla.walo.model.Achievement

class AchievementAdapter(private var achievements: List<Achievement>) :
    RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {

    // ViewHolder holding references to achievement item views
    class AchievementViewHolder(val binding: ItemAchievementBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val binding = ItemAchievementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AchievementViewHolder(binding)
    }

    // Binds achievement data and applies visual styling based on unlock status
    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]

        holder.binding.apply {
            val context = root.context
            tvAchievementTitle.text = achievement.title
            tvAchievementDescription.text = achievement.description
            ivAchievementIcon.setImageResource(achievement.iconRes)

            if (achievement.isUnlocked) {
                // Apply high contrast colors and gold tint for earned achievements
                achievementContainer.alpha = 1.0f
                achievementContainer.setBackgroundColor(ContextCompat.getColor(context, R.color.badge_unlocked_bg))
                
                ivLockOverlay.visibility = View.GONE
                
                ivAchievementIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.badge_gold))
                
                tvAchievementTitle.setTextColor(ContextCompat.getColor(context, R.color.white))
                tvAchievementDescription.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                // Apply faded grey style and lock overlay for pending achievements
                achievementContainer.alpha = 0.5f
                achievementContainer.setBackgroundColor(ContextCompat.getColor(context, R.color.badge_locked_bg))
                
                ivLockOverlay.visibility = View.VISIBLE
                
                ivAchievementIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.badge_locked_grey))
                
                tvAchievementTitle.setTextColor(ContextCompat.getColor(context, R.color.black))
                tvAchievementDescription.setTextColor(ContextCompat.getColor(context, R.color.placeholder_text_color))
            }
        }
    }

    override fun getItemCount(): Int = achievements.size

    // Refreshes the adapter with a new list of achievements
    fun updateData(newAchievements: List<Achievement>) {
        achievements = newAchievements
        notifyDataSetChanged()
    }
}
