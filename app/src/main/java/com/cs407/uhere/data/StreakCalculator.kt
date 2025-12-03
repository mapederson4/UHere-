package com.cs407.uhere.data

import java.util.Calendar

/**
 * Utility object for calculating streaks from weekly progress data
 * Handles both category-specific and all-goals streaks
 */
object StreakCalculator {

    /**
     * Calculate current and best streak for a specific category
     *
     * @param completedWeeks All weekly progress records for the user
     * @param category The category to calculate streaks for
     * @return StreakInfo containing current streak, best streak, and total weeks
     */
    fun calculateCategoryStreak(
        completedWeeks: List<WeeklyProgress>,
        category: LocationCategory
    ): StreakInfo {
        val sortedWeeks = completedWeeks.sortedByDescending { it.weekStartDate }

        // Filter to only weeks where this category was completed
        val isCompleted: (WeeklyProgress) -> Boolean = when (category) {
            LocationCategory.LIBRARY -> { w -> w.libraryCompleted }
            LocationCategory.BAR -> { w -> w.barCompleted }
            LocationCategory.GYM -> { w -> w.gymCompleted }
        }

        val relevantWeeks = sortedWeeks.filter(isCompleted)

        if (relevantWeeks.isEmpty()) {
            return StreakInfo(
                category = category,
                currentStreak = 0,
                bestStreak = 0,
                totalWeeksCompleted = 0
            )
        }

        val currentStreak = calculateCurrentStreak(relevantWeeks)
        val bestStreak = calculateBestStreak(relevantWeeks)

        return StreakInfo(
            category = category,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            totalWeeksCompleted = relevantWeeks.size
        )
    }

    /**
     * Calculate streak for completing ALL goals in a week
     * Only counts weeks where library, bar, AND gym goals were all completed
     *
     * @param completedWeeks All weekly progress records for the user
     * @return StreakInfo with category = null to indicate "all goals"
     */
    fun calculateAllGoalsStreak(completedWeeks: List<WeeklyProgress>): StreakInfo {
        val sortedWeeks = completedWeeks
            .filter { it.allGoalsCompleted }
            .sortedByDescending { it.weekStartDate }

        if (sortedWeeks.isEmpty()) {
            return StreakInfo(
                category = null,
                currentStreak = 0,
                bestStreak = 0,
                totalWeeksCompleted = 0
            )
        }

        val currentStreak = calculateCurrentStreak(sortedWeeks)
        val bestStreak = calculateBestStreak(sortedWeeks)

        return StreakInfo(
            category = null,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            totalWeeksCompleted = sortedWeeks.size
        )
    }

    /**
     * Calculate current active streak
     * A streak is active if it includes the current or most recent week
     * and continues backwards without gaps
     *
     * @param sortedWeeks Weeks sorted by date descending (most recent first)
     * @return Number of consecutive weeks
     */
    private fun calculateCurrentStreak(sortedWeeks: List<WeeklyProgress>): Int {
        if (sortedWeeks.isEmpty()) return 0

        val currentWeekStart = getWeekStartDate()
        val lastWeekStart = getPreviousWeekStart(currentWeekStart)

        var streak = 0
        var expectedWeekStart = currentWeekStart

        // Check if the most recent completed week is current week or last week
        val mostRecentWeek = sortedWeeks.first().weekStartDate

        if (mostRecentWeek != currentWeekStart && mostRecentWeek != lastWeekStart) {
            // Streak is broken - most recent completion is too old
            return 0
        }

        // Start from the most recent week
        if (mostRecentWeek == currentWeekStart) {
            expectedWeekStart = currentWeekStart
        } else {
            expectedWeekStart = lastWeekStart
        }

        // Count consecutive weeks
        for (week in sortedWeeks) {
            if (week.weekStartDate == expectedWeekStart) {
                streak++
                expectedWeekStart = getPreviousWeekStart(expectedWeekStart)
            } else if (week.weekStartDate < expectedWeekStart) {
                // Gap found - streak is broken
                break
            }
        }

        return streak
    }

    /**
     * Calculate the best (longest) streak ever achieved
     * Looks through entire history to find longest consecutive run
     *
     * @param sortedWeeks Weeks sorted by date descending (most recent first)
     * @return Longest consecutive streak
     */
    private fun calculateBestStreak(sortedWeeks: List<WeeklyProgress>): Int {
        if (sortedWeeks.isEmpty()) return 0
        if (sortedWeeks.size == 1) return 1

        var bestStreak = 1
        var currentStreak = 1

        // Iterate through sorted weeks and check for consecutive weeks
        for (i in 0 until sortedWeeks.size - 1) {
            val currentWeek = sortedWeeks[i].weekStartDate
            val nextWeek = sortedWeeks[i + 1].weekStartDate
            val expectedPreviousWeek = getPreviousWeekStart(currentWeek)

            if (nextWeek == expectedPreviousWeek) {
                // Consecutive week found
                currentStreak++
                bestStreak = maxOf(bestStreak, currentStreak)
            } else {
                // Gap found - reset current streak
                currentStreak = 1
            }
        }

        return bestStreak
    }

    /**
     * Get the start timestamp of the previous week
     *
     * @param currentWeekStart Timestamp of current week's start (Sunday 12:00 AM)
     * @return Timestamp of previous week's start
     */
    private fun getPreviousWeekStart(currentWeekStart: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentWeekStart
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        return calendar.timeInMillis
    }

    /**
     * Get the start timestamp of the next week
     *
     * @param currentWeekStart Timestamp of current week's start (Sunday 12:00 AM)
     * @return Timestamp of next week's start
     */
    private fun getNextWeekStart(currentWeekStart: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentWeekStart
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        return calendar.timeInMillis
    }

    /**
     * Check if two weeks are consecutive
     *
     * @param week1 First week timestamp
     * @param week2 Second week timestamp
     * @return True if week2 is immediately after week1
     */
    fun areWeeksConsecutive(week1: Long, week2: Long): Boolean {
        return getNextWeekStart(week1) == week2
    }

    /**
     * Get number of weeks between two dates
     *
     * @param startWeek Earlier week timestamp
     * @param endWeek Later week timestamp
     * @return Number of weeks between the dates
     */
    fun getWeeksBetween(startWeek: Long, endWeek: Long): Int {
        if (endWeek <= startWeek) return 0

        val weekInMillis = 7 * 24 * 60 * 60 * 1000L
        return ((endWeek - startWeek) / weekInMillis).toInt()
    }
}