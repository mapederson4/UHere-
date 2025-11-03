package com.cs407.uhere.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.uhere.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GoalWithProgress(
    val category: LocationCategory,
    val targetHours: Float,
    val currentMinutes: Int,
    val progressPercentage: Float
)

class GoalViewModel(application: Application) : AndroidViewModel(application) {
    private val goalDao = UHereDatabase.getDatabase(application).goalDao()
    private val locationDao = UHereDatabase.getDatabase(application).locationDao()

    private val _goalsWithProgress = MutableStateFlow<List<GoalWithProgress>>(emptyList())
    val goalsWithProgress: StateFlow<List<GoalWithProgress>> = _goalsWithProgress.asStateFlow()

    fun loadGoalsWithProgress(userId: Int) {
        viewModelScope.launch {
            goalDao.getActiveGoals(userId).collect { goals ->
                val weekStart = getWeekStartDate()
                val goalsWithProgressList = goals.map { goal ->
                    val currentMinutes = locationDao.getTotalMinutesForCategory(
                        userId,
                        goal.locationCategory,
                        weekStart
                    ).first() ?: 0

                    val targetMinutes = (goal.targetHours * 60).toInt()
                    val progress = if (targetMinutes > 0) {
                        (currentMinutes.toFloat() / targetMinutes).coerceIn(0f, 1f)
                    } else 0f

                    GoalWithProgress(
                        category = goal.locationCategory,
                        targetHours = goal.targetHours,
                        currentMinutes = currentMinutes,
                        progressPercentage = progress
                    )
                }
                _goalsWithProgress.value = goalsWithProgressList
            }
        }
    }

    fun saveGoals(userId: Int, goals: Map<LocationCategory, Float>) {
        viewModelScope.launch {
            val weekStart = getWeekStartDate()
            goalDao.deactivateAllGoals(userId)

            goals.forEach { (category, hours) ->
                val goal = Goal(
                    userId = userId,
                    locationCategory = category,
                    targetHours = hours,
                    weekStartDate = weekStart
                )
                goalDao.insertGoal(goal)
            }

            loadGoalsWithProgress(userId)
        }
    }
}