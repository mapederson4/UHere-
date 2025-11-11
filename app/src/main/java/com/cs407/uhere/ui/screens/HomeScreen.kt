package com.cs407.uhere.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cs407.uhere.R
import com.cs407.uhere.data.LocationCategory
import com.cs407.uhere.data.User
import com.cs407.uhere.viewmodel.GoalViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userState: User?,
    goalViewModel: GoalViewModel
) {
    var showDialog by remember { mutableStateOf(false) }
    val goalsWithProgress by goalViewModel.goalsWithProgress.collectAsState()

    // Load goals when user state changes
    LaunchedEffect(userState) {
        userState?.let { user ->
            goalViewModel.loadGoalsWithProgress(user.id)
        }
    }

    Box(modifier = modifier.fillMaxSize().padding(0.dp, 16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Welcome, ${userState?.displayName ?: "User"}!",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Weekly Overview",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color.White)
                    .fillMaxWidth()
            ) {
                // Display progress for each category
                goalsWithProgress.forEach { goalWithProgress ->
                    CategoryProgressCard(
                        category = goalWithProgress.category,
                        progress = goalWithProgress.progressPercentage,
                        currentMinutes = goalWithProgress.currentMinutes,
                        targetHours = goalWithProgress.targetHours
                    )
                }

                // Show placeholder if no goals
                if (goalsWithProgress.isEmpty()) {
                    Text(
                        text = "No goals set yet. Go to Goals tab to set your weekly targets!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            OutlinedButton(
                onClick = { showDialog = true }
            ) {
                Text("View AI Weekly Summary")
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "AI Weekly Summary") },
                text = {
                    Text(
                        text = generateAISummary(goalsWithProgress)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun CategoryProgressCard(
    category: LocationCategory,
    progress: Float,
    currentMinutes: Int,
    targetHours: Float
) {
    val (iconRes, label) = when (category) {
        LocationCategory.LIBRARY -> R.drawable.book to "Library"
        LocationCategory.BAR -> R.drawable.drink to "Bar"
        LocationCategory.GYM -> R.drawable.barbell to "Gym"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.padding(4.dp)
    ) {
        Column {
            Row(modifier = Modifier.padding(8.dp)) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    modifier = Modifier
                        .size(78.dp)
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )

                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${currentMinutes / 60}h ${currentMinutes % 60}m / ${targetHours.toInt()}h",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

fun generateAISummary(goalsWithProgress: List<com.cs407.uhere.viewmodel.GoalWithProgress>): String {
    if (goalsWithProgress.isEmpty()) {
        return "No data available yet. Set your goals and start tracking your time!"
    }

    val totalProgress = goalsWithProgress.map { it.progressPercentage }.average()
    val bestCategory = goalsWithProgress.maxByOrNull { it.progressPercentage }

    return buildString {
        append("This week you're ${(totalProgress * 100).toInt()}% towards your goals. ")
        bestCategory?.let {
            append("Great job on ${it.category.name.lowercase()} time! ")
        }
        append("Keep up the good work!")
    }
}