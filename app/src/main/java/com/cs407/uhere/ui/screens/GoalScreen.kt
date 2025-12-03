package com.cs407.uhere.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cs407.uhere.R
import com.cs407.uhere.data.LocationCategory
import com.cs407.uhere.data.User
import com.cs407.uhere.viewmodel.GoalViewModel
import kotlin.math.roundToInt

@Composable
fun GoalScreen(
    modifier: Modifier = Modifier,
    userState: User?,
    goalViewModel: GoalViewModel
) {
    var slidersLocked by remember { mutableStateOf(true) }
    var libraryHours by remember { mutableFloatStateOf(0f) }
    var barHours by remember { mutableFloatStateOf(0f) }
    var gymHours by remember { mutableFloatStateOf(0f) }

    val goalsWithProgress by goalViewModel.goalsWithProgress.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(userState) {
        userState?.let { user ->
            goalViewModel.startAutoRefresh(user.id)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            goalViewModel.stopAutoRefresh()
        }
    }

    LaunchedEffect(goalsWithProgress) {
        goalsWithProgress.forEach { goal ->
            when (goal.category) {
                LocationCategory.LIBRARY -> libraryHours = goal.targetHours
                LocationCategory.BAR -> barHours = goal.targetHours
                LocationCategory.GYM -> gymHours = goal.targetHours
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
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
                Text(
                    text = "Weekly Goals",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Text(
                    text = "Set your time targets for the week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ImprovedGoalCard(
                iconRes = R.drawable.book,
                label = "Library",
                hours = libraryHours,
                onHoursChange = { libraryHours = it },
                progress = goalsWithProgress.find { it.category == LocationCategory.LIBRARY }?.progressPercentage
                    ?: 0f,
                currentHours = goalsWithProgress.find { it.category == LocationCategory.LIBRARY }?.let { it.currentMinutes / 60f }
                    ?: 0f,
                enabled = !slidersLocked,
                color = Color(0xFF1976D2)
            )

            Spacer(modifier = Modifier.height(12.dp))

            ImprovedGoalCard(
                iconRes = R.drawable.drink,
                label = "Social Time",
                hours = barHours,
                onHoursChange = { barHours = it },
                progress = goalsWithProgress.find { it.category == LocationCategory.BAR }?.progressPercentage
                    ?: 0f,
                currentHours = goalsWithProgress.find { it.category == LocationCategory.BAR }?.let { it.currentMinutes / 60f }
                    ?: 0f,
                enabled = !slidersLocked,
                color = Color(0xFFFF6F00)
            )

            Spacer(modifier = Modifier.height(12.dp))

            ImprovedGoalCard(
                iconRes = R.drawable.barbell,
                label = "Fitness",
                hours = gymHours,
                onHoursChange = { gymHours = it },
                progress = goalsWithProgress.find { it.category == LocationCategory.GYM }?.progressPercentage
                    ?: 0f,
                currentHours = goalsWithProgress.find { it.category == LocationCategory.GYM }?.let { it.currentMinutes / 60f }
                    ?: 0f,
                enabled = !slidersLocked,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { slidersLocked = false },
                    enabled = slidersLocked,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Goals", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        slidersLocked = true
                        userState?.let { user ->
                            val goals = mapOf(
                                LocationCategory.LIBRARY to libraryHours,
                                LocationCategory.BAR to barHours,
                                LocationCategory.GYM to gymHours
                            )
                            goalViewModel.saveGoals(user.id, goals)
                        }
                    },
                    enabled = !slidersLocked,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Goals", fontWeight = FontWeight.SemiBold)
                }
            }

            // Extra spacing at bottom to ensure buttons are always visible
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ImprovedGoalCard(
    iconRes: Int,
    label: String,
    hours: Float,
    onHoursChange: (Float) -> Unit,
    progress: Float,
    currentHours: Float,
    enabled: Boolean,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(iconRes),
                            contentDescription = label,
                            modifier = Modifier.size(30.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (hours.toInt() == 1) {
                                "${currentHours.toInt()} / ${hours.toInt()} Hour"
                            } else {
                                "${currentHours.toInt()} / ${hours.toInt()} Hours"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Circular Progress
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(52.dp),
                        color = color,
                        trackColor = color.copy(alpha = 0.2f),
                        strokeWidth = 5.dp
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Slider
            Column {
                Text(
                    text = "Weekly Target: ${hours.toInt()} hours",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = hours,
                    onValueChange = { onHoursChange(it.roundToInt().toFloat()) },
                    valueRange = 0F..20F,
                    enabled = enabled,
                    colors = SliderDefaults.colors(
                        thumbColor = color,
                        activeTrackColor = color,
                        inactiveTrackColor = color.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}