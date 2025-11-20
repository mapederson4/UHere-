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
import com.cs407.uhere.BuildConfig
import com.cs407.uhere.R
import com.cs407.uhere.data.LocationCategory
import com.cs407.uhere.data.User
import com.cs407.uhere.viewmodel.GoalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

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
                goalsWithProgress.forEach { goalWithProgress ->
                    CategoryProgressCard(
                        category = goalWithProgress.category,
                        progress = goalWithProgress.progressPercentage,
                        currentMinutes = goalWithProgress.currentMinutes,
                        targetHours = goalWithProgress.targetHours
                    )
                }

                if (goalsWithProgress.isEmpty()) {
                    Text(
                        text = "No goals set yet. Go to Goals tab to set your weekly targets!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

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
                            aiError = null
                        } catch (e: Exception) {
                            android.util.Log.e("HomeScreen", "AI Error", e)
                            aiError = "Couldn't reach AI service. Showing simple summary."
                            aiSummary = generateSimpleSummary(goalsWithProgress)
                        } finally {
                            aiLoading = false
                        }
                    }
                },
                enabled = goalsWithProgress.isNotEmpty()
            ) {
                Text("View AI Weekly Summary")
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "Weekly Summary") },
                text = {
                    when {
                        aiLoading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Generating your personalized summary...")
                            }
                        }
                        aiError != null -> {
                            Column {
                                Text(
                                    aiError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(aiSummary ?: "", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        aiSummary != null -> {
                            Text(
                                aiSummary ?: "",
                                style = MaterialTheme.typography.bodyMedium
                            )
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
            The user hasn't set any weekly goals yet.
            Write 1-2 encouraging sentences (max 100 words) motivating them to set goals and start tracking their time.
        """.trimIndent()
    }

    val goalsText = goalsWithProgress.joinToString("\n") { g ->
        val hours = g.currentMinutes / 60
        val mins = g.currentMinutes % 60
        val categoryName = when (g.category) {
            LocationCategory.LIBRARY -> "studying at the library"
            LocationCategory.GYM -> "working out at the gym"
            LocationCategory.BAR -> "socializing at bars"
        }
        val progressPercent = (g.progressPercentage * 100).toInt()
        val status = when {
            progressPercent >= 100 -> "✓ COMPLETED"
            progressPercent >= 75 -> "almost there"
            progressPercent >= 50 -> "halfway"
            progressPercent >= 25 -> "started"
            else -> "just beginning"
        }
        "- $categoryName: ${hours}h ${mins}m out of ${g.targetHours.toInt()}h ($status - $progressPercent%)"
    }

    val totalProgress = goalsWithProgress.map { it.progressPercentage }.average()
    val overallStatus = when {
        totalProgress >= 0.9 -> "crushing it"
        totalProgress >= 0.7 -> "doing great"
        totalProgress >= 0.5 -> "making steady progress"
        totalProgress >= 0.3 -> "off to a good start"
        else -> "just getting started"
    }

    return """
        You are a friendly, motivating college productivity coach. Keep your response SHORT (max 150 words).
        
        The user is $overallStatus this week with their time goals. Here's their progress:
        
        $goalsText
        
        Write a personalized summary with:
        1. One sentence acknowledging their overall progress
        2. One specific callout (praise what they're doing well OR encourage where they're behind)
        3. One actionable tip for next week
        
        Be casual, positive, and concise. Use "you" not "the user". NO bullet points.
    """.trimIndent()
}

suspend fun callOpenAI(prompt: String): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.OPENAI_API_KEY

    if (apiKey.isBlank()) {
        throw IOException("OpenAI API key not configured")
    }

    val mediaType = "application/json; charset=utf-8".toMediaType()

    val requestBody = JSONObject().apply {
        put("model", "gpt-4o-mini")
        put("messages", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are a supportive college productivity coach. Keep responses under 150 words and conversational.")
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        })
        put("max_tokens", 200)
        put("temperature", 0.7)
    }

    val body = requestBody.toString().toRequestBody(mediaType)

    val request = Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .addHeader("Authorization", "Bearer $apiKey")
        .addHeader("Content-Type", "application/json")
        .post(body)
        .build()

    val response = httpClient.newCall(request).execute()

    if (!response.isSuccessful) {
        val errorBody = response.body?.string()
        android.util.Log.e("OpenAI", "Error: ${response.code} - $errorBody")
        throw IOException("OpenAI API error: ${response.code}")
    }

    val responseBody = response.body?.string()
        ?: throw IOException("Empty response from OpenAI")

    val json = JSONObject(responseBody)
    val choices = json.getJSONArray("choices")
    val firstChoice = choices.getJSONObject(0)
    val message = firstChoice.getJSONObject("message")

    message.getString("content").trim()
}

fun generateSimpleSummary(
    goalsWithProgress: List<com.cs407.uhere.viewmodel.GoalWithProgress>
): String {
    if (goalsWithProgress.isEmpty()) {
        return "Ready to make this week count? Head to the Goals tab to set your weekly targets and start tracking your time!"
    }

    val totalProgress = goalsWithProgress.map { it.progressPercentage }.average()
    val progressPercent = (totalProgress * 100).toInt()

    val bestCategory = goalsWithProgress.maxByOrNull { it.progressPercentage }
    val weakestCategory = goalsWithProgress.minByOrNull { it.progressPercentage }

    return buildString {
        append("You're $progressPercent% towards your weekly goals. ")

        bestCategory?.let {
            val categoryName = when (it.category) {
                LocationCategory.LIBRARY -> "library time"
                LocationCategory.GYM -> "gym sessions"
                LocationCategory.BAR -> "social time"
            }
            val percent = (it.progressPercentage * 100).toInt()
            append("Great work on $categoryName ($percent%)! ")
        }

        weakestCategory?.let {
            if (it.progressPercentage < 0.5) {
                val categoryName = when (it.category) {
                    LocationCategory.LIBRARY -> "studying"
                    LocationCategory.GYM -> "working out"
                    LocationCategory.BAR -> "socializing"
                }
                append("Try to spend a bit more time $categoryName this week. ")
            }
        }

        append("Keep it up!")
    }
}