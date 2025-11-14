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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import com.cs407.uhere.BuildConfig

private val httpClient = OkHttpClient()

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userState: User?,
    goalViewModel: GoalViewModel
) {
    var showDialog by remember { mutableStateOf(false) }
    val goalsWithProgress by goalViewModel.goalsWithProgress.collectAsState()

    var aiSummary by remember { mutableStateOf<String?>(null) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

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

            // AI summary button
            OutlinedButton(
                onClick = {
                    showDialog = true
                    aiLoading = true
                    aiError = null
                    aiSummary = null

                    coroutineScope.launch {
                        try {
                            val prompt = buildPromptFromGoals(goalsWithProgress)
                            val result = callOpenAI(prompt)
                            aiSummary = result
                        } catch (e: Exception) {
                            e.printStackTrace()
                            aiError = "Could not generate summary. Showing simple summary instead."
                            aiSummary = generateAISummaryFallback(goalsWithProgress)
                        } finally {
                            aiLoading = false
                        }
                    }
                }
            ) {
                Text("View AI Weekly Summary")
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "AI Weekly Summary") },
                text = {
                    when {
                        aiLoading -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Generating your summary...")
                            }
                        }
                        aiError != null -> {
                            Column {
                                Text(aiError ?: "")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(aiSummary ?: "")
                            }
                        }
                        aiSummary != null -> {
                            Text(aiSummary ?: "")
                        }
                        else -> {
                            Text("No data available yet. Set your goals and start tracking your time!")
                        }
                    }
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

fun buildPromptFromGoals(
    goalsWithProgress: List<com.cs407.uhere.viewmodel.GoalWithProgress>
): String {
    if (goalsWithProgress.isEmpty()) {
        return """
            The user has not tracked any goals this week.
            Write one short, encouraging sentence telling them to set some goals and start tracking.
        """.trimIndent()
    }

    val goalsText = goalsWithProgress.joinToString("\n") { g ->
        val hours = g.currentMinutes / 60
        val mins = g.currentMinutes % 60
        val categoryName = g.category.name.lowercase().replaceFirstChar { it.uppercase() }
        "$categoryName: $hours h $mins m out of ${g.targetHours} h " +
                "(progress ${(g.progressPercentage * 100).toInt()}%)"
    }

    return """
        You are a friendly productivity coach for a college student.

        Given this weekly time-tracking data, write:
        - 2–4 sentences of encouraging summary
        - 1 concrete suggestion for next week

        Keep it casual, positive, and short.

        Weekly data:
        $goalsText
    """.trimIndent()
}

suspend fun callOpenAI(prompt: String): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.OPENAI_API_KEY

    print(apiKey)

    val mediaType = "application/json; charset=utf-8".toMediaType()

    val root = JSONObject().apply {
        put("model", "gpt-4.1-mini")
        put("messages", JSONArray().apply {
            put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                }
            )
        })
    }

    val body = root.toString().toRequestBody(mediaType)

    val request = Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .addHeader("Authorization", "Bearer $apiKey")
        .addHeader("Content-Type", "application/json")
        .post(body)
        .build()

    val response = httpClient.newCall(request).execute()

    if (!response.isSuccessful) {
        throw IOException("Unexpected code $response")
    }

    val responseBody = response.body?.string() ?: throw IOException("Empty response body")

    val json = JSONObject(responseBody)
    val choices = json.getJSONArray("choices")
    val first = choices.getJSONObject(0)
    val message = first.getJSONObject("message")
    message.getString("content")
}


fun generateAISummaryFallback(
    goalsWithProgress: List<com.cs407.uhere.viewmodel.GoalWithProgress>
): String {
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