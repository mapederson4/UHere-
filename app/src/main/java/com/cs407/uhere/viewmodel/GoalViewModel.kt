package com.cs407.uhere.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.uhere.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

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

    // **DEMO DATA FUNCTION**
    fun insertDemoProgress(userId: Int) {
        viewModelScope.launch {
            val weekStart = getWeekStartDate()
            val now = System.currentTimeMillis()

            // Library: 6 hours (60% of typical 10-hour goal)
            val librarySessions = listOf(
                LocationSession(
                    userId = userId,
                    locationCategory = LocationCategory.LIBRARY,
                    startTime = now - TimeUnit.DAYS.toMillis(2),
                    endTime = now - TimeUnit.DAYS.toMillis(2) + TimeUnit.HOURS.toMillis(2),
                    durationMinutes = 120, // 2 hours
                    weekStartDate = weekStart
                ),
                LocationSession(
                    userId = userId,
                    locationCategory = LocationCategory.LIBRARY,
                    startTime = now - TimeUnit.DAYS.toMillis(1),
                    endTime = now - TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(3),
                    durationMinutes = 180, // 3 hours
                    weekStartDate = weekStart
                ),
                LocationSession(
                    userId = userId,
                    locationCategory = LocationCategory.LIBRARY,
                    startTime = now - TimeUnit.HOURS.toMillis(4),
                    endTime = now - TimeUnit.HOURS.toMillis(3),
                    durationMinutes = 60, // 1 hour
                    weekStartDate = weekStart
                )
            )

            // Gym: 3 hours (60% of typical 5-hour goal)
            val gymSessions = listOf(
                LocationSession(
                    userId = userId,
                    locationCategory = LocationCategory.GYM,
                    startTime = now - TimeUnit.DAYS.toMillis(3),
                    endTime = now - TimeUnit.DAYS.toMillis(3) + TimeUnit.MINUTES.toMillis(90),
                    durationMinutes = 90, // 1.5 hours
                    weekStartDate = weekStart
                ),
                LocationSession(
                    userId = userId,
                    locationCategory = LocationCategory.GYM,
                    startTime = now - TimeUnit.DAYS.toMillis(1),
                    endTime = now - TimeUnit.DAYS.toMillis(1) + TimeUnit.MINUTES.toMillis(90),
                    durationMinutes = 90, // 1.5 hours
                    weekStartDate = weekStart
                )
            )

            // Bar: 2.5 hours (83% of typical 3-hour goal)
            val barSessions = listOf(
                LocationSession(
                    userId = userId,
                    locationCategory = LocationCategory.BAR,
                    startTime = now - TimeUnit.DAYS.toMillis(2),
                    endTime = now - TimeUnit.DAYS.toMillis(2) + TimeUnit.MINUTES.toMillis(90),
                    durationMinutes = 90, // 1.5 hours
                    weekStartDate = weekStart
                ),
                LocationSession(
                    userId = userId,
                    locationCategory = LocationCategory.BAR,
                    startTime = now - TimeUnit.HOURS.toMillis(6),
                    endTime = now - TimeUnit.HOURS.toMillis(5),
                    durationMinutes = 60, // 1 hour
                    weekStartDate = weekStart
                )
            )

            // Insert all sessions
            (librarySessions + gymSessions + barSessions).forEach { session ->
                locationDao.insertSession(session)
            }

            // Reload progress to update UI immediately
            loadGoalsWithProgress(userId)
        }
    }

    // **CLEAR DEMO DATA**
    fun clearDemoProgress(userId: Int) {
        viewModelScope.launch {
            val weekStart = getWeekStartDate()
            locationDao.clearSessionsForWeek(userId, weekStart)
            loadGoalsWithProgress(userId)
        }
    }
}