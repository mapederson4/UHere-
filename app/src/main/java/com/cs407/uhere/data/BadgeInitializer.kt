package com.cs407.uhere.data

import android.content.Context
import com.cs407.uhere.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun initializeBadges(context: Context) {
    val badgeDao = UHereDatabase.getDatabase(context).badgeDao()

    CoroutineScope(Dispatchers.IO).launch {
        val badges = listOf(
            Badge(
                badgeName = "Bookworm",
                description = "Spend 10 hours at the library in one week",
                iconResource = R.drawable.book,
                category = LocationCategory.LIBRARY,
                requirement = "10 hours at library"
            ),
            Badge(
                badgeName = "Gym Rat",
                description = "Spend 5 hours at the gym in one week",
                iconResource = R.drawable.barbell,
                category = LocationCategory.GYM,
                requirement = "5 hours at gym"
            ),
            Badge(
                badgeName = "Social Butterfly",
                description = "Spend 3 hours at the bar in one week",
                iconResource = R.drawable.drink,
                category = LocationCategory.BAR,
                requirement = "3 hours at bar"
            )
        )

        badges.forEach { badge ->
            badgeDao.insertBadge(badge)
        }
    }
}