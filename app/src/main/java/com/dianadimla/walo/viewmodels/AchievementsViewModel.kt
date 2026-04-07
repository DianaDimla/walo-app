package com.dianadimla.walo.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dianadimla.walo.R
import com.dianadimla.walo.model.Achievement
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AchievementsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    private val _achievements = MutableLiveData<List<Achievement>>()
    val achievements: LiveData<List<Achievement>> = _achievements

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val staticAchievements = listOf(
        // Transaction achievements
        Achievement(
            id = "FIRST_ENTRY",
            title = "First Entry",
            description = "Logged your first transaction",
            iconRes = R.drawable.first
        ),
        // Goal achievements
        Achievement(
            id = "FIRST_STEP",
            title = "First Step",
            description = "Created your first goal",
            iconRes = R.drawable.foot
        ),
        Achievement(
            id = "GOAL_GETTER",
            title = "Goal Getter",
            description = "Completed a goal",
            iconRes = R.drawable.goal
        ),
        Achievement(
            id = "MASTER_SAVER",
            title = "Master Saver",
            description = "Completed 3 goals",
            iconRes = R.drawable.crown
        ),
        Achievement(
            id = "WEALTH_BUILDER",
            title = "Wealth Builder",
            description = "Completed 5+ goals",
            iconRes = R.drawable.builder
        ),
        // Streak achievements
        Achievement(
            id = "CONSISTENT_TRACKER",
            title = "Consistent Tracker",
            description = "Logged activity for 3 days in a row",
            iconRes = R.drawable.three
        ),
        Achievement(
            id = "SEVEN_DAY_STREAK",
            title = "7-Day Streak",
            description = "Opened the app for 7 days in a row",
            iconRes = R.drawable.seven
        )
    )
    // Icons from Icons8

    fun startListeningForAchievements() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _achievements.value = staticAchievements
            return
        }

        _isLoading.value = true
        
        listenerRegistration?.remove()

        // Sync local list with unlocked achievements in Firestore
        listenerRegistration = firestore.collection("users").document(uid).collection("achievements")
            .addSnapshotListener { querySnapshot, error ->
                _isLoading.value = false
                
                if (error != null) {
                    _achievements.value = staticAchievements
                    return@addSnapshotListener
                }

                val unlockedIds = querySnapshot?.documents?.mapNotNull { it.id } ?: emptyList()
                val updatedList = staticAchievements.map { achievement ->
                    achievement.copy(isUnlocked = unlockedIds.contains(achievement.id))
                }
                _achievements.value = updatedList
            }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
