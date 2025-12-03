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
    weeklyProgressManager: WeeklyProgressManager
) {
    var streakInfo by remember { mutableStateOf<Map<String, StreakInfo>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Load streak information
    LaunchedEffect(userId) {
        userId?.let {
            coroutineScope.launch {
                streakInfo = weeklyProgressManager.getAllStreaks(it)
            }
        }
    }

    // Calculate which badges should be unlocked based on streak data
    val barStreak = streakInfo?.get("bar")
    val gymStreak = streakInfo?.get("gym")
    val libraryStreak = streakInfo?.get("library")

    val rewards = listOf(
        // Bar badges
        Reward(
            id = 1,
            drawableRes = R.drawable.bar1,
            name = "Social Butterfly",
            description = "Meet bar goals once",
            isUnlocked = (barStreak?.totalWeeksCompleted ?: 0) >= 1
        ),
        Reward(
            id = 2,
            drawableRes = R.drawable.bar2,
            name = "Party Expert",
            description = "Meet bar goals three times",
            isUnlocked = (barStreak?.totalWeeksCompleted ?: 0) >= 3
        ),
        Reward(
            id = 3,
            drawableRes = R.drawable.bar3,
            name = "Social Legend",
            description = "Meet bar goals three weeks consecutively",
            isUnlocked = (barStreak?.bestStreak ?: 0) >= 3
        ),
        // Gym badges
        Reward(
            id = 4,
            drawableRes = R.drawable.gym1,
            name = "Fitness Starter",
            description = "Meet gym goals once",
            isUnlocked = (gymStreak?.totalWeeksCompleted ?: 0) >= 1
        ),
        Reward(
            id = 5,
            drawableRes = R.drawable.gym2,
            name = "Gym Regular",
            description = "Meet gym goals three times",
            isUnlocked = (gymStreak?.totalWeeksCompleted ?: 0) >= 3
        ),
        Reward(
            id = 6,
            drawableRes = R.drawable.gym3,
            name = "Fitness Champion",
            description = "Meet gym goals three weeks consecutively",
            isUnlocked = (gymStreak?.bestStreak ?: 0) >= 3
        ),
        // Library badges
        Reward(
            id = 7,
            drawableRes = R.drawable.library1,
            name = "Study Beginner",
            description = "Meet library goals once",
            isUnlocked = (libraryStreak?.totalWeeksCompleted ?: 0) >= 1
        ),
        Reward(
            id = 8,
            drawableRes = R.drawable.library2,
            name = "Dedicated Scholar",
            description = "Meet library goals three times",
            isUnlocked = (libraryStreak?.totalWeeksCompleted ?: 0) >= 3
        ),
        Reward(
            id = 9,
            drawableRes = R.drawable.library3,
            name = "Academic Master",
            description = "Meet library goals three weeks consecutively",
            isUnlocked = (libraryStreak?.bestStreak ?: 0) >= 3
        ),
    )

    val unlockedCount = rewards.count { it.isUnlocked }
    val totalCount = rewards.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Section - UNIFIED STYLE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "Your Rewards",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(28.dp)
                    )
                }
                Text(
                    text = "Track your streaks and unlock badges",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Streaks Section Header - UNIFIED STYLE
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Your Streaks",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Text(
                    text = " 🔥",
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 28.sp
                )
            }

            streakInfo?.let { streaks ->
                // All Goals Streak
                streaks["all_goals"]?.let { allGoalsStreak ->
                    StreakCard(
                        title = "All Goals Complete",
                        icon = Icons.Default.EmojiEvents,
                        iconColor = Color(0xFFFFD700),
                        streakInfo = allGoalsStreak,
                        isPrimary = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Individual category streaks
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    streaks["library"]?.let { libraryStreak ->
                        CategoryStreakCard(
                            title = "Library",
                            iconRes = R.drawable.book,
                            streakInfo = libraryStreak,
                            color = Color(0xFF1E88E5),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    streaks["gym"]?.let { gymStreak ->
                        CategoryStreakCard(
                            title = "Gym",
                            iconRes = R.drawable.barbell,
                            streakInfo = gymStreak,
                            color = Color(0xFF43A047),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    streaks["bar"]?.let { barStreak ->
                        CategoryStreakCard(
                            title = "Bar",
                            iconRes = R.drawable.drink,
                            streakInfo = barStreak,
                            color = Color(0xFFFB8C00),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } ?: run {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "Complete your first goal to start tracking streaks!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Badges Section Header - UNIFIED STYLE
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Collectible Badges",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Text(
                    text = " 🏆",
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 28.sp
                )
            }

            // Badges Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(min = 350.dp, max = 900.dp),
                userScrollEnabled = false
            ) {
                items(rewards) { reward ->
                    FlippableRewardCard(reward = reward)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StreakCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    streakInfo: StreakInfo,
    isPrimary: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                .padding(12.dp),
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
                    modifier = Modifier.size(28.dp)
                )

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${streakInfo.totalWeeksCompleted} weeks completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
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
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${streakInfo.currentStreak}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )
                }
                Text(
                    text = "Best: ${streakInfo.bestStreak}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${streakInfo.currentStreak}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 20.sp
                )
            }

            Text(
                text = "Best: ${streakInfo.bestStreak}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun FlippableRewardCard(reward: Reward) {
    var isFlipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "cardFlip"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            } else {
                // Back side
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
                                .size(24.dp)
                                .padding(bottom = 4.dp)
                        )
                        Text(
                            text = reward.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(bottom = 3.dp),
                            fontSize = 11.sp
                        )
                        Text(
                            text = reward.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}