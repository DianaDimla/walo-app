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

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]

        holder.binding.apply {
            val context = root.context
            tvAchievementTitle.text = achievement.title
            tvAchievementDescription.text = achievement.description
            ivAchievementIcon.setImageResource(achievement.iconRes)

            if (achievement.isUnlocked) {
                // Unlocked State
                achievementContainer.alpha = 1.0f
                achievementContainer.setBackgroundColor(ContextCompat.getColor(context, R.color.badge_unlocked_bg))
                
                ivLockOverlay.visibility = View.GONE
                
                // Icon Gold Tint
                ivAchievementIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.badge_gold))
                
                // Text White
                tvAchievementTitle.setTextColor(ContextCompat.getColor(context, R.color.white))
                tvAchievementDescription.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                // Locked State
                achievementContainer.alpha = 0.5f
                achievementContainer.setBackgroundColor(ContextCompat.getColor(context, R.color.badge_locked_bg))
                
                ivLockOverlay.visibility = View.VISIBLE
                
                // Icon Grey Tint
                ivAchievementIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.badge_locked_grey))
                
                // Text Dark Grey
                tvAchievementTitle.setTextColor(ContextCompat.getColor(context, R.color.black))
                tvAchievementDescription.setTextColor(ContextCompat.getColor(context, R.color.placeholder_text_color))
            }
        }
    }

    override fun getItemCount(): Int = achievements.size

    fun updateData(newAchievements: List<Achievement>) {
        achievements = newAchievements
        notifyDataSetChanged()
    }
}
