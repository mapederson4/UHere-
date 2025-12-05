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
import androidx.compose.ui.unit.TextUnit
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
                .padding(28.dp)
        ) {
            Column {
                Text(
                    text = "Weekly Goals",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                )
                Text(
                    text = "Set your time goals for the week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 18.sp
                )
            }
        }

        Column(
            modifier = Modifier.padding(18.dp),
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
                color = Color(0xFF64B5F6),
                fontColor = Color(0xFF121212),
                labelFontSize = 24.sp,
                progressFontSize = 16.sp,
                targetFontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

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
                color = Color(0xFFEF6C00),
                fontColor = Color(0xFF121212),
                labelFontSize = 24.sp,
                progressFontSize = 16.sp,
                targetFontSize = 18.sp
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
                color = Color(0xFF388E3C),
                fontColor = Color(0xFF121212),
                labelFontSize = 24.sp,
                progressFontSize = 16.sp,
                targetFontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { slidersLocked = false },
                    enabled = slidersLocked,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary, // baby blue when enabled
                        contentColor = Color.White,         // text color when enabled
                        disabledContainerColor = Color(0xFFB0BEC5), // muted gray when disabled
                        disabledContentColor = Color.White.copy(alpha = 0.5f) // slightly faded text
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Goals", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Goals", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
    color: Color,
    fontColor: Color = MaterialTheme.colorScheme.onSurface,
    labelFontSize: TextUnit = 16.sp,
    progressFontSize: TextUnit = 10.sp,
    targetFontSize: TextUnit = 10.sp
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.35f)),
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
                            fontSize = labelFontSize,
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
                            fontSize = progressFontSize,
                            style = MaterialTheme.typography.bodySmall,
                            color = fontColor
                        )
                    }
                }

                // Circular Progress
                Box(
                    modifier = Modifier.size(62.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(62.dp),
                        color = color,
                        trackColor = color.copy(alpha = 0.3f),
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Slider
            Column {
                Text(
                    text = "Weekly Target: ${hours.toInt()} hours",
                    fontSize = targetFontSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = fontColor,
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
                        inactiveTrackColor = color.copy(alpha = 0.3f),

                        disabledThumbColor = color.copy(alpha = 0.5f),
                        disabledActiveTrackColor = color.copy(alpha = 0.3f),
                        disabledInactiveTrackColor = color.copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
}