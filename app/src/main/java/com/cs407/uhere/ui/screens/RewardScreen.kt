package com.cs407.uhere.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cs407.uhere.R
import com.cs407.uhere.data.*
import kotlinx.coroutines.launch

data class Reward(
    val id: Int,
    val drawableRes: Int,
    val name: String = "",
    val description: String = ""
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

    val rewards = listOf(
        Reward(1, R.drawable.reward, "Reward 1", "This is the first reward"),
        Reward(2, R.drawable.reward, "Reward 2", "This is the second reward"),
        Reward(3, R.drawable.reward, "Reward 3", "This is the third reward"),
        Reward(4, R.drawable.reward, "Reward 4", "This is the fourth reward"),
        Reward(5, R.drawable.reward, "Reward 5", "This is the fifth reward"),
        Reward(6, R.drawable.reward, "Reward 6", "This is the sixth reward"),
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Rewards & Achievements",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Streaks Section
        item {
            Text(
                text = "Your Streaks",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
            )
        }

        item {
            streakInfo?.let { streaks ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    // All Goals Streak (most prominent)
                    streaks["all_goals"]?.let { allGoalsStreak ->
                        StreakCard(
                            title = "All Goals Complete",
                            icon = Icons.Default.EmojiEvents,
                            iconColor = Color(0xFFFFD700), // Gold
                            streakInfo = allGoalsStreak,
                            isPrimary = true
                        )
                    }

                    // Individual category streaks
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                }
            } ?: run {
                Text(
                    text = "Complete your first goal to start tracking streaks!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }

        // Rewards Grid Section
        item {
            Text(
                text = "Collectible Rewards",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp, top = 16.dp)
            )
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(600.dp), // Fixed height for nested scroll
                userScrollEnabled = false
            ) {
                items(rewards) { reward ->
                    FlippableRewardCard(reward = reward)
                }
            }
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
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${streakInfo.totalWeeksCompleted} weeks completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
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
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "${streakInfo.currentStreak}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )
                }
                Text(
                    text = "Best: ${streakInfo.bestStreak}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
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
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

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
                    color = color
                )
            }

            Text(
                text = "Best: ${streakInfo.bestStreak}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Image(
                    painter = painterResource(id = reward.drawableRes),
                    contentDescription = reward.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = reward.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = reward.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}