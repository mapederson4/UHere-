package com.cs407.uhere.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

    // Start auto-refresh when screen opens
    LaunchedEffect(userState) {
        userState?.let { user ->
            goalViewModel.startAutoRefresh(user.id)
        }
    }

    // Stop refresh when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            goalViewModel.stopAutoRefresh()
        }
    }

    // Update sliders with loaded goals
    LaunchedEffect(goalsWithProgress) {
        goalsWithProgress.forEach { goal ->
            when (goal.category) {
                LocationCategory.LIBRARY -> libraryHours = goal.targetHours
                LocationCategory.BAR -> barHours = goal.targetHours
                LocationCategory.GYM -> gymHours = goal.targetHours
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().padding(0.dp, 16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Weekly Time Goals",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
                fontSize = 36.sp
            )

            Text(
                "Select how many hours you would like to spend in each place per week",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
            )

            GoalCard(
                iconRes = R.drawable.book,
                label = "Library",
                hours = libraryHours,
                onHoursChange = { libraryHours = it },
                progress = goalsWithProgress.find { it.category == LocationCategory.LIBRARY }?.progressPercentage ?: 0f,
                currentHours = goalsWithProgress.find { it.category == LocationCategory.LIBRARY }?.let { it.currentMinutes / 60f } ?: 0f,
                enabled = !slidersLocked
            )

            GoalCard(
                iconRes = R.drawable.drink,
                label = "Bar",
                hours = barHours,
                onHoursChange = { barHours = it },
                progress = goalsWithProgress.find { it.category == LocationCategory.BAR }?.progressPercentage ?: 0f,
                currentHours = goalsWithProgress.find { it.category == LocationCategory.BAR }?.let { it.currentMinutes / 60f } ?: 0f,
                enabled = !slidersLocked
            )

            GoalCard(
                iconRes = R.drawable.barbell,
                label = "Gym",
                hours = gymHours,
                onHoursChange = { gymHours = it },
                progress = goalsWithProgress.find { it.category == LocationCategory.GYM }?.progressPercentage ?: 0f,
                currentHours = goalsWithProgress.find { it.category == LocationCategory.GYM }?.let { it.currentMinutes / 60f } ?: 0f,
                enabled = !slidersLocked
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { slidersLocked = false },
                    enabled = slidersLocked,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Text("Edit")
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
fun GoalCard(
    iconRes: Int,
    label: String,
    hours: Float,
    onHoursChange: (Float) -> Unit,
    progress: Float,
    currentHours: Float,
    enabled: Boolean
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Column {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    modifier = Modifier
                        .size(78.dp)
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )

                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(label)

                    Text(
                        text = if (hours.toInt() == 1) {
                            "${currentHours.toInt()}/${hours.toInt()} Hour"
                        } else {
                            "${currentHours.toInt()}/${hours.toInt()} Hours"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(64.dp),
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp
                    )
                }
            }
            Slider(
                value = hours,
                onValueChange = { onHoursChange(it.roundToInt().toFloat()) },
                valueRange = 0F..20F,
                enabled = enabled
            )
        }
    }
}