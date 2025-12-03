package com.cs407.uhere.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Manager class for handling weekly progress tracking and transitions
 */
class WeeklyProgressManager(private val context: Context) {

    private val database = UHereDatabase.getDatabase(context)
    private val goalDao = database.goalDao()
    private val locationDao = database.locationDao()
    private val weeklyProgressDao = database.weeklyProgressDao()

    companion object {
        private const val TAG = "WeeklyProgressManager"
    }

    /**
     * Check if a new week has started and handle the transition
     */
    suspend fun checkAndHandleWeekTransition(userId: Int) {
        try {
            Log.d(TAG, "Checking week transition for user $userId")

            val currentWeekStart = getWeekStartDate()
            val activeGoals = goalDao.getActiveGoals(userId).first()

            if (activeGoals.isEmpty()) {
                Log.d(TAG, "No active goals found, nothing to transition")
                return
            }

            val previousWeekGoals = activeGoals.filter { it.weekStartDate < currentWeekStart }

            if (previousWeekGoals.isEmpty()) {
                Log.d(TAG, "All goals are current, no transition needed")
                return
            }

            val previousWeekStart = previousWeekGoals.first().weekStartDate
            Log.d(TAG, "New week detected! Processing week starting at $previousWeekStart")

            processCompletedWeek(userId, previousWeekStart, currentWeekStart, previousWeekGoals)

            Log.d(TAG, "Week transition completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error during week transition", e)
            throw e
        }
    }

    /**
     * Process a completed week
     */
    private suspend fun processCompletedWeek(
        userId: Int,
        previousWeekStart: Long,
        currentWeekStart: Long,
        previousWeekGoals: List<Goal>
    ) {
        val weekEndDate = getWeekEndDate(previousWeekStart)
        val completionStatus = checkGoalCompletions(userId, previousWeekStart)

        Log.d(TAG, "Completion status: $completionStatus")

        val anyGoalCompleted = completionStatus.values.any { it }

        if (anyGoalCompleted) {
            val allGoalsCompleted = completionStatus.values.all { it } && completionStatus.size == 3

            val weeklyProgress = WeeklyProgress(
                userId = userId,
                weekStartDate = previousWeekStart,
                weekEndDate = weekEndDate,
                libraryCompleted = completionStatus[LocationCategory.LIBRARY] ?: false,
                barCompleted = completionStatus[LocationCategory.BAR] ?: false,
                gymCompleted = completionStatus[LocationCategory.GYM] ?: false,
                allGoalsCompleted = allGoalsCompleted
            )

            weeklyProgressDao.insertWeeklyProgress(weeklyProgress)
            Log.d(TAG, "Saved weekly progress: $weeklyProgress")
        } else {
            Log.d(TAG, "No goals completed this week, not saving progress")
        }

        locationDao.clearSessionsForWeek(userId, previousWeekStart)
        Log.d(TAG, "Cleared location sessions for week $previousWeekStart")

        goalDao.deactivateAllGoals(userId)
        Log.d(TAG, "Deactivated old goals")

        previousWeekGoals.forEach { oldGoal ->
            val newGoal = oldGoal.copy(
                id = 0,
                weekStartDate = currentWeekStart,
                isActive = true
            )
            goalDao.insertGoal(newGoal)
            Log.d(TAG, "Created new goal for ${newGoal.locationCategory}: ${newGoal.targetHours} hours")
        }
    }

    /**
     * Check which goals were completed in a given week
     */
    private suspend fun checkGoalCompletions(
        userId: Int,
        weekStart: Long
    ): Map<LocationCategory, Boolean> {
        val goals = goalDao.getActiveGoals(userId).first()
            .filter { it.weekStartDate == weekStart }

        val completions = mutableMapOf<LocationCategory, Boolean>()

        for (goal in goals) {
            val totalMinutes = locationDao.getTotalMinutesForCategory(
                userId,
                goal.locationCategory,
                weekStart
            ).first() ?: 0

            val targetMinutes = (goal.targetHours * 60).toInt()
            val isCompleted = totalMinutes >= targetMinutes

            completions[goal.locationCategory] = isCompleted

            Log.d(TAG, "${goal.locationCategory}: $totalMinutes/$targetMinutes minutes - " +
                    if (isCompleted) "COMPLETED" else "incomplete")
        }

        return completions
    }

    /**
     * Get all streaks for a user
     */
    suspend fun getAllStreaks(userId: Int): Map<String, StreakInfo> {
        val allProgress = weeklyProgressDao.getAllWeeklyProgress(userId).first()

        return mapOf(
            "library" to StreakCalculator.calculateCategoryStreak(allProgress, LocationCategory.LIBRARY),
            "bar" to StreakCalculator.calculateCategoryStreak(allProgress, LocationCategory.BAR),
            "gym" to StreakCalculator.calculateCategoryStreak(allProgress, LocationCategory.GYM),
            "all_goals" to StreakCalculator.calculateAllGoalsStreak(allProgress)
        )
    }

    /**
     * Get the end date of a week (Saturday 11:59:59 PM)
     */
    private fun getWeekEndDate(weekStartDate: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = weekStartDate
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}