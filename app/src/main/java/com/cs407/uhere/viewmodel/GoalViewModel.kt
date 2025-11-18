package com.cs407.uhere.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.uhere.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Calendar
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

    private var refreshJob: Job? = null
    private var currentUserId: Int? = null

    fun loadGoalsWithProgress(userId: Int) {
        // If user changed, clear state first
        if (currentUserId != userId) {
            _goalsWithProgress.value = emptyList()
            currentUserId = userId
        }

        viewModelScope.launch {
            // Check for weekly reset
            checkAndResetWeeklyGoals(userId)

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

    private suspend fun checkAndResetWeeklyGoals(userId: Int) {
        val currentWeekStart = getWeekStartDate()
        val activeGoals = goalDao.getActiveGoals(userId).first()

        // If goals exist but are from previous week, deactivate them
        val needsReset = activeGoals.any { it.weekStartDate < currentWeekStart }

        if (needsReset) {
            // Deactivate old goals
            goalDao.deactivateAllGoals(userId)

            // Create new goals for current week with same targets
            activeGoals.forEach { oldGoal ->
                val newGoal = Goal(
                    userId = userId,
                    locationCategory = oldGoal.locationCategory,
                    targetHours = oldGoal.targetHours,
                    weekStartDate = currentWeekStart,
                    isActive = true
                )
                goalDao.insertGoal(newGoal)
            }
        }
    }

    fun startAutoRefresh(userId: Int) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                loadGoalsWithProgress(userId)
                delay(30000)
            }
        }
    }

    fun stopAutoRefresh() {
        refreshJob?.cancel()
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

    fun clearState() {
        stopAutoRefresh()
        _goalsWithProgress.value = emptyList()
        currentUserId = null
    }

    fun insertDemoProgress(userId: Int) {
        viewModelScope.launch {
            val weekStart = getWeekStartDate()
            val now = System.currentTimeMillis()

            val librarySessions = listOf(
                LocationSession(
                    userId = userId,
                    locationCategory = LocationCategory.LIBRARY,
                    startTime = now - TimeUnit.DAYS.toMillis(2),
                    endTime = now - TimeUnit.DAYS.toMillis(2) + TimeUnit.HOURS.toMillis(2),
                    durationMinutes = 120,
                    weekStartDate = weekStart
                ),
                LocationSession(
                    userId = userId,
                    locationCategory = LocationCategory.LIBRARY,
                    startTime = now - TimeUnit.DAYS.toMillis(1),
                    endTime = now - TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(3),
                    durationMinutes = 180,
                    weekStartDate = weekStart
                ),
                LocationSession(
                    userId = userId,
                    locationCategory = LocationCategory.LIBRARY,
                    startTime = now - TimeUnit.HOURS.toMillis(4),
                    endTime = now - TimeUnit.HOURS.toMillis(3),
                    durationMinutes = 60,
                    weekStartDate = weekStart
                )
            )

            val gymSessions = listOf(
                LocationSession(
                    userId = userId,
                    locationCategory = LocationCategory.GYM,
                    startTime = now - TimeUnit.DAYS.toMillis(3),
                    endTime = now - TimeUnit.DAYS.toMillis(3) + TimeUnit.MINUTES.toMillis(90),
                    durationMinutes = 90,
                    weekStartDate = weekStart
                ),
                LocationSession(
                    userId = userId,
                    locationCategory = LocationCategory.GYM,
                    startTime = now - TimeUnit.DAYS.toMillis(1),
                    endTime = now - TimeUnit.DAYS.toMillis(1) + TimeUnit.MINUTES.toMillis(90),
                    durationMinutes = 90,
                    weekStartDate = weekStart
                )
            )

            val barSessions = listOf(
                LocationSession(
                    userId = userId,
                    locationCategory = LocationCategory.BAR,
                    startTime = now - TimeUnit.DAYS.toMillis(2),
                    endTime = now - TimeUnit.DAYS.toMillis(2) + TimeUnit.MINUTES.toMillis(90),
                    durationMinutes = 90,
                    weekStartDate = weekStart
                ),
                LocationSession(
                    userId = userId,
                    locationCategory = LocationCategory.BAR,
                    startTime = now - TimeUnit.HOURS.toMillis(6),
                    endTime = now - TimeUnit.HOURS.toMillis(5),
                    durationMinutes = 60,
                    weekStartDate = weekStart
                )
            )

            (librarySessions + gymSessions + barSessions).forEach { session ->
                locationDao.insertSession(session)
            }

            loadGoalsWithProgress(userId)
        }
    }

    fun clearDemoProgress(userId: Int) {
        viewModelScope.launch {
            val weekStart = getWeekStartDate()
            locationDao.clearSessionsForWeek(userId, weekStart)
            loadGoalsWithProgress(userId)
        }
    }
}