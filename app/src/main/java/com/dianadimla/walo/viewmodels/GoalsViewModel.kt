package com.dianadimla.walo.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dianadimla.walo.data.Goal
import com.dianadimla.walo.data.GoalsRepository

class GoalsViewModel : ViewModel() {
    private val repository = GoalsRepository()
    private val _goals = MutableLiveData<List<Goal>>()
    val goals: LiveData<List<Goal>> get() = _goals

    // LiveData to notify fragment about unlocked achievements
    private val _achievementUnlocked = MutableLiveData<String>()
    val achievementUnlocked: LiveData<String> get() = _achievementUnlocked

    init {
        repository.listenToGoals { receivedGoals ->
            _goals.postValue(receivedGoals)
        }
        
        // Connect repository achievement triggers to ViewModel LiveData
        repository.setAchievementListener { title ->
            _achievementUnlocked.postValue(title)
        }
    }

    fun addGoal(goal: Goal) {
        repository.addGoal(goal)
    }

    fun deleteGoal(goal: Goal) {
        repository.deleteGoal(goal.id)
    }

    fun updateGoal(goal: Goal) {
        repository.updateGoal(goal)
    }
}
