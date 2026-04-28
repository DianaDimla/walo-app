/**
 * ViewModel for managing the lifecycle and data observation of financial goals.
 * Acts as a bridge between the GoalsRepository and the UI fragments.
 */
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

    // Observable stream for newly unlocked achievement notifications
    private val _achievementUnlocked = MutableLiveData<String>()
    val achievementUnlocked: LiveData<String> get() = _achievementUnlocked

    init {
        // Initialises real-time goal tracking from the repository
        repository.listenToGoals { receivedGoals ->
            _goals.postValue(receivedGoals)
        }
        
        // Relays achievement events from the repository to the UI layer
        repository.setAchievementListener { title ->
            _achievementUnlocked.postValue(title)
        }
    }

    // Persists a new goal through the repository
    fun addGoal(goal: Goal) {
        repository.addGoal(goal)
    }

    // Removes a goal record using its unique identifier
    fun deleteGoal(goal: Goal) {
        repository.deleteGoal(goal.id)
    }

    // Synchronises modifications to an existing goal
    fun updateGoal(goal: Goal) {
        repository.updateGoal(goal)
    }
}
