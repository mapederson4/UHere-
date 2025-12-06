package com.cs407.uhere.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cs407.uhere.R
import com.cs407.uhere.data.*
import kotlinx.coroutines.launch

data class Reward(
    val id: Int,
    val drawableRes: Int,
    val name: String = "",
    val description: String = "",
    val isUnlocked: Boolean = false
)

@Composable
fun RewardScreen(
    modifier: Modifier = Modifier,
    userId: Int?,
    database: UHereDatabase
) {
    // INSTANT COMPLETIONS - for badge unlocking
    var completions by remember { mutableStateOf<List<GoalCompletion>>(emptyList()) }

    // WEEKLY PROGRESS - for streaks and historical data
    var weeklyProgress by remember { mutableStateOf<List<WeeklyProgress>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()

    // Load both instant completions AND weekly progress
    LaunchedEffect(userId) {
        userId?.let {
            // Load instant completions
            launch {
                database.goalCompletionDao().getAllCompletions(it).collect { allCompletions ->
                    completions = allCompletions
                }
            }

            // Load weekly progress for streaks
            launch {
                database.weeklyProgressDao().getAllWeeklyProgress(it).collect { progress ->
                    weeklyProgress = progress
                }
            }
        }
    }

    // Calculate badges based on INSTANT completions (unlocks when hitting 100%)
    val barCompletions = completions.filter { it.category == LocationCategory.BAR }
    val gymCompletions = completions.filter { it.category == LocationCategory.GYM }
    val libraryCompletions = completions.filter { it.category == LocationCategory.LIBRARY }

    // Calculate streaks from WEEKLY progress (for streak display)
    val libraryStreak = StreakCalculator.calculateCategoryStreak(
        weeklyProgress,
        LocationCategory.LIBRARY
    )

    val barStreak = StreakCalculator.calculateCategoryStreak(
        weeklyProgress,
        LocationCategory.BAR
    )

    val gymStreak = StreakCalculator.calculateCategoryStreak(
        weeklyProgress,
        LocationCategory.GYM
    )
    val allGoalsStreak = StreakCalculator.calculateAllGoalsStreak(
        weeklyProgress
    )

    val rewards = listOf(
        // Bar badges - INSTANT UNLOCK based on completions
        Reward(
            id = 1,
            drawableRes = R.drawable.bar1,
            name = "Social Butterfly",
            description = "Complete bar goal once",
            isUnlocked = barCompletions.size >= 1
        ),
        Reward(
            id = 2,
            drawableRes = R.drawable.bar2,
            name = "Party Expert",
            description = "Complete bar goal three times",
            isUnlocked = barCompletions.size >= 3
        ),
        Reward(
            id = 3,
            drawableRes = R.drawable.bar3,
            name = "Social Legend",
            description = "Complete bar goal three weeks consecutively",
            isUnlocked = calculateConsecutiveWeeks(barCompletions) >= 3
        ),
        // Gym badges
        Reward(
            id = 4,
            drawableRes = R.drawable.gym1,
            name = "Fitness Starter",
            description = "Complete gym goal once",
            isUnlocked = gymCompletions.size >= 1
        ),
        Reward(
            id = 5,
            drawableRes = R.drawable.gym2,
            name = "Gym Regular",
            description = "Complete gym goal three times",
            isUnlocked = gymCompletions.size >= 3
        ),
        Reward(
            id = 6,
            drawableRes = R.drawable.gym3,
            name = "Fitness Champion",
            description = "Complete gym goal three weeks consecutively",
            isUnlocked = calculateConsecutiveWeeks(gymCompletions) >= 3
        ),
        // Library badges
        Reward(
            id = 7,
            drawableRes = R.drawable.library1,
            name = "Study Beginner",
            description = "Complete library goal once",
            isUnlocked = libraryCompletions.size >= 1
        ),
        Reward(
            id = 8,
            drawableRes = R.drawable.library2,
            name = "Dedicated Scholar",
            description = "Complete library goal three times",
            isUnlocked = libraryCompletions.size >= 3
        ),
        Reward(
            id = 9,
            drawableRes = R.drawable.library3,
            name = "Academic Master",
            description = "Complete library goal three weeks consecutively",
            isUnlocked = calculateConsecutiveWeeks(libraryCompletions) >= 3
        ),
    )

    val unlockedCount = rewards.count { it.isUnlocked }
    val totalCount = rewards.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "Your Rewards",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .size(34.dp)
                    )
                }
                Text(
                    text = "Track your streaks and unlock badges",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // Streak section (using WeeklyProgress)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Text(
                    text = "Your Streaks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                )
                Text(
                    text = " 🔥",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 26.sp
                )
            }

            if (weeklyProgress.isNotEmpty()) {
                // All goals streak
                if (allGoalsStreak.totalWeeksCompleted > 0) {
                    StreakCard(
                        title = "All Goals Complete",
                        icon = Icons.Default.EmojiEvents,
                        iconColor = Color(0xFFFFD700),
                        labelFontSize = 20.sp,
                        labelFontColor = MaterialTheme.colorScheme.primary,
                        progressFontSize = 16.sp,
                        progressFontColor = Color(0xFF000000),
                        bestFontSize = 14.sp,
                        bestFontColor = Color(0xFF000000),
                        streakInfo = allGoalsStreak,
                        isPrimary = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Individual category streaks
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (libraryStreak.totalWeeksCompleted > 0) {
                        CategoryStreakCard(
                            title = "Library",
                            iconRes = R.drawable.book,
                            streakInfo = libraryStreak,
                            color = Color(0xFF1E88E5),
                            modifier = Modifier.weight(1f),
                            labelFontSize = 20.sp,
                            labelFontColor = Color(0xFF000000),
                            bestFontSize = 15.sp,
                            bestFontColor = Color(0xFF121212)
                        )
                    }

                    if (gymStreak.totalWeeksCompleted > 0) {
                        CategoryStreakCard(
                            title = "Gym",
                            iconRes = R.drawable.barbell,
                            streakInfo = gymStreak,
                            color = Color(0xFF43A047),
                            modifier = Modifier.weight(1f),
                            labelFontSize = 20.sp,
                            labelFontColor = Color(0xFF000000),
                            bestFontSize = 15.sp,
                            bestFontColor = Color(0xFF121212)
                        )
                    }

                    if (barStreak.totalWeeksCompleted > 0) {
                        CategoryStreakCard(
                            title = "Bar",
                            iconRes = R.drawable.drink,
                            streakInfo = barStreak,
                            color = Color(0xFFFB8C00),
                            modifier = Modifier.weight(1f),
                            labelFontSize = 20.sp,
                            labelFontColor = Color(0xFF000000),
                            bestFontSize = 15.sp,
                            bestFontColor = Color(0xFF121212)
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = "Complete your first week to start tracking streaks!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 16.sp,
                        color = Color(0xFF000000),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Badges section (using GoalCompletions - instant unlock)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Collectible Badges",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Text(
                    text = " 🏆",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 28.sp
                )
            }

            // Progress indicator
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$unlockedCount of $totalCount unlocked",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${((unlockedCount.toFloat() / totalCount) * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 24.sp
                    )
                }
            }

            // Badges grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(
                    min = 350.dp,
                    max = 900.dp
                ),
                userScrollEnabled = false
            ) {
                items(rewards) { reward ->
                    FlippableRewardCard(reward = reward)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
fun StreakCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    streakInfo: StreakInfo,
    labelFontColor: Color = MaterialTheme.colorScheme.primary,
    labelFontSize: TextUnit = 16.sp,
    progressFontColor: Color = MaterialTheme.colorScheme.primary,
    progressFontSize: TextUnit = 12.sp,
    bestFontSize: TextUnit = 12.sp,
    bestFontColor: Color = MaterialTheme.colorScheme.primary,
    isPrimary: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(34.dp)
                )

                Column (
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ){
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = labelFontSize,
                        color = labelFontColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${streakInfo.totalWeeksCompleted} " +
                                    if (streakInfo.totalWeeksCompleted == 1) "week completed" else "weeks completed"

                        ,
                        style = MaterialTheme.typography.bodySmall,
                        color = progressFontColor,
                        fontSize = progressFontSize
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "${streakInfo.currentStreak}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35),
                        fontSize = 28.sp
                    )
                }
                Text(
                    text = "Best: ${streakInfo.bestStreak}",
                    style = MaterialTheme.typography.bodySmall,
                    color = bestFontColor,
                    fontSize = bestFontSize
                )
            }
        }
    }
}

@Composable
fun CategoryStreakCard(
    title: String,
    iconRes: Int,
    streakInfo: StreakInfo,
    color: Color,
    modifier: Modifier = Modifier,
    labelFontColor: Color = MaterialTheme.colorScheme.primary,
    labelFontSize: TextUnit = 16.sp,
    bestFontColor: Color = MaterialTheme.colorScheme.primary,
    bestFontSize: TextUnit = 16.sp,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontSize = labelFontSize,
                color = labelFontColor
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "${streakInfo.currentStreak}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B35),
                    fontSize = 28.sp
                )
            }

            Text(
                text = "Best: ${streakInfo.bestStreak}",
                style = MaterialTheme.typography.bodySmall,
                color = bestFontColor,
                fontSize = bestFontSize
            )
        }
    }
}

/**
 * Calculate consecutive weeks from completion records
 */
private fun calculateConsecutiveWeeks(completions: List<GoalCompletion>): Int {
    if (completions.isEmpty()) return 0

    val sortedWeeks = completions.map { it.weekStartDate }.distinct().sorted()
    var maxStreak = 1
    var currentStreak = 1

    for (i in 1 until sortedWeeks.size) {
        val weekDiff = (sortedWeeks[i] - sortedWeeks[i - 1]) / (7 * 24 * 60 * 60 * 1000)
        if (weekDiff == 1L) {
            currentStreak++
            maxStreak = maxOf(maxStreak, currentStreak)
        } else {
            currentStreak = 1
        }
    }

    return maxStreak
}

@Composable
fun FlippableRewardCard(reward: Reward) {
    var isFlipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "cardFlip"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .aspectRatio(1f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { isFlipped = !isFlipped },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
                .graphicsLayer {
                    rotationY = if (rotation > 90f) 180f else 0f
                },
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Front side
                if (reward.isUnlocked) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = reward.drawableRes),
                            contentDescription = reward.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    // Locked state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Locked",
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(bottom = 4.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Locked",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF000000),
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            } else {
                // Back side
                if (reward.isUnlocked){
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (reward.isUnlocked) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(bottom = 3.dp)
                            )
                            Text(
                                text = reward.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(bottom = 3.dp),
                                fontSize = 13.sp
                            )
                            Text(
                                text = reward.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 7.sp
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (reward.isUnlocked) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(bottom = 3.dp)
                            )
                            Text(
                                text = reward.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF000000),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(bottom = 3.dp),
                                fontSize = 13.sp
                            )
                            Text(
                                text = reward.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF000000),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 7.sp
                            )
                        }
                    }
                }

            }
        }
    }
}