package com.cs407.uhere.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.cs407.uhere.data.User
import com.cs407.uhere.data.WeeklyProgressManager
import com.cs407.uhere.service.LocationTrackingService
import com.cs407.uhere.viewmodel.UserViewModel
import com.cs407.uhere.viewmodel.GoalViewModel
import com.cs407.uhere.viewmodel.LocationViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    userState: User?,
    userViewModel: UserViewModel,
    goalViewModel: GoalViewModel,
    locationViewModel: LocationViewModel
) {
    val context = LocalContext.current
    val isTracking by locationViewModel.isTrackingEnabled.collectAsState()
    // Check permissions
    var hasForegroundPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasBackgroundPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Not needed on Android 9 and below
            }
        )
    }

    // Step 2: Request background location permission (separate launcher)
    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBackgroundPermission = isGranted

        if (isGranted && hasForegroundPermission) {
            // All permissions granted, start tracking
            userState?.let { user ->
                LocationTrackingService.start(context, user.id)
                locationViewModel.setTrackingEnabled(true)
            }
        } else if (!isGranted) {
            // Background permission denied - still allow foreground tracking
            userState?.let { user ->
                LocationTrackingService.start(context, user.id)
                locationViewModel.setTrackingEnabled(true)
            }
        }
    }

    // Step 1: Request foreground location permissions
    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        hasForegroundPermission = fineGranted || coarseGranted

        if (hasForegroundPermission) {
            // Step 2: Request background permission (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundPermission) {
                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                // All permissions granted, start tracking
                userState?.let { user ->
                    LocationTrackingService.start(context, user.id)
                    locationViewModel.setTrackingEnabled(true)
                }
            }
        } else {
            locationViewModel.setTrackingEnabled(false)
        }
    }



    var showDemoMessage by remember { mutableStateOf(false) }
    var showWeekMessage by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPermissionExplanation by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Beautiful header with gradient
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
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(30.dp)
                    )
                }
                Text(
                    text = "Manage your account and preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 20.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Account section
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(
                    bottom = 8.dp,
                    start = 4.dp
                )
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(end = 18.dp)
                    )
                    Column {
                        Text(
                            text = userState?.displayName ?: "User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 30.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userState?.email ?: "email@example.com",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Location Tracking Section
            Text(
                text = "Tracking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(
                    bottom = 8.dp,
                    start = 4.dp
                )
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isTracking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(56.dp)
                                .padding(end = 16.dp)
                        )
                        Column {
                            Text(
                                text = "Location Tracking",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isTracking) "Currently active" else "Paused",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Switch(
                        checked = isTracking,
                        onCheckedChange = { newValue ->
                            userState?.let { user ->
                                if (newValue) {
                                    if (!hasForegroundPermission) {
                                        // Show explanation before requesting
                                        showPermissionExplanation = true
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundPermission) {
                                        // Request background permission
                                        backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                    } else {
                                        // Already have all permissions
                                        userState?.let { user ->
                                            LocationTrackingService.start(context, user.id)
                                            locationViewModel.setTrackingEnabled(true)
                                        }
                                    }
                                } else {
                                    LocationTrackingService.stop(context)
                                    locationViewModel.setTrackingEnabled(false)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            // Week and streak force
            Text(
                text = "Week & Streak Testing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(
                    bottom = 8.dp,
                    start = 4.dp
                )
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Force End of Week",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFFFFF),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Manually end week to test streak tracking.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFFFFF),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Text(
                        text = "Save current week's progress and start a new week.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFFFFF),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                userState?.let { user ->
                                    coroutineScope.launch {
                                        try {
                                            val database = com.cs407.uhere.data.UHereDatabase.getDatabase(context)
                                            val goals = database.goalDao().getActiveGoals(user.id).first()
                                            val weekStart = com.cs407.uhere.data.getWeekStartDate()

                                            var debugInfo = "Current Week Goals:\n"
                                            goals.filter { goal -> goal.weekStartDate == weekStart }.forEach { goal ->
                                                val minutes = database.locationDao().getTotalMinutesForCategory(
                                                    user.id, goal.locationCategory, weekStart
                                                ).first() ?: 0
                                                val target = (goal.targetHours * 60).toInt()
                                                val percent = if (target > 0) (minutes * 100 / target) else 0
                                                debugInfo += "${goal.locationCategory}: $minutes/$target min (${percent}%)\n"
                                            }

                                            showWeekMessage = debugInfo
                                        } catch (e: Exception) {
                                            showWeekMessage = "Error: ${e.message}"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFFFFF)
                            )
                        ) {
                            Text(
                                "Check Progress",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = {
                                userState?.let { user ->
                                    coroutineScope.launch {
                                        try {
                                            val database = com.cs407.uhere.data.UHereDatabase.getDatabase(context)
                                            val goalDao = database.goalDao()
                                            val locationDao = database.locationDao()
                                            val weeklyProgressDao = database.weeklyProgressDao()

                                            // Get current week goals
                                            val weekStart = com.cs407.uhere.data.getWeekStartDate()
                                            val goals = goalDao.getActiveGoals(user.id).first()
                                                .filter { it.weekStartDate == weekStart }

                                            if (goals.isEmpty()) {
                                                showWeekMessage = "No active goals found for current week"
                                                return@launch
                                            }

                                            // Calculate completions for current week
                                            val completions = mutableMapOf<com.cs407.uhere.data.LocationCategory, Boolean>()
                                            for (goal in goals) {
                                                val totalMinutes = locationDao.getTotalMinutesForCategory(
                                                    user.id, goal.locationCategory, weekStart
                                                ).first() ?: 0
                                                val targetMinutes = (goal.targetHours * 60).toInt()
                                                completions[goal.locationCategory] = totalMinutes >= targetMinutes
                                            }

                                            // Check if any goal was completed
                                            val anyCompleted = completions.values.any { it }

                                            if (anyCompleted) {
                                                // Create WeeklyProgress record
                                                val weekEndDate = weekStart + (7 * 24 * 60 * 60 * 1000) - 1
                                                val allCompleted = completions.values.all { it } && completions.size == 3

                                                val weeklyProgress = com.cs407.uhere.data.WeeklyProgress(
                                                    userId = user.id,
                                                    weekStartDate = weekStart,
                                                    weekEndDate = weekEndDate,
                                                    libraryCompleted = completions[com.cs407.uhere.data.LocationCategory.LIBRARY] ?: false,
                                                    barCompleted = completions[com.cs407.uhere.data.LocationCategory.BAR] ?: false,
                                                    gymCompleted = completions[com.cs407.uhere.data.LocationCategory.GYM] ?: false,
                                                    allGoalsCompleted = allCompleted
                                                )

                                                weeklyProgressDao.insertWeeklyProgress(weeklyProgress)

                                                // Create GoalCompletions for completed goals
                                                val goalCompletionDao = database.goalCompletionDao()
                                                for ((category, completed) in completions) {
                                                    if (completed) {
                                                        val goal = goals.first { it.locationCategory == category }
                                                        val minutes = locationDao.getTotalMinutesForCategory(
                                                            user.id, category, weekStart
                                                        ).first() ?: 0

                                                        val completion = com.cs407.uhere.data.GoalCompletion(
                                                            userId = user.id,
                                                            category = category,
                                                            weekStartDate = weekStart,
                                                            completedAt = System.currentTimeMillis(),
                                                            targetHours = goal.targetHours,
                                                            completedMinutes = minutes
                                                        )
                                                        goalCompletionDao.insertCompletion(completion)
                                                    }
                                                }

                                                // Move goals to next week
                                                val nextWeekStart = weekStart + (7 * 24 * 60 * 60 * 1000)
                                                goalDao.deactivateAllGoals(user.id)

                                                goals.forEach { oldGoal ->
                                                    val newGoal = oldGoal.copy(
                                                        id = 0,
                                                        weekStartDate = nextWeekStart,
                                                        isActive = true
                                                    )
                                                    goalDao.insertGoal(newGoal)
                                                }

                                                showWeekMessage = "✓ Week saved! ${completions.count { it.value }} goals completed. Check Rewards tab!"
                                            } else {
                                                showWeekMessage = "⚠ No goals were completed (0% progress)"
                                            }
                                        } catch (e: Exception) {
                                            showWeekMessage = "✗ Error: ${e.message}"
                                            android.util.Log.e("Settings", "Week transition error", e)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFFFFF)
                            )
                        ) {
                            Text(
                                "Force Week End",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (showWeekMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = showWeekMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFFFFF),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Demo Mode Section
            Text(
                text = "Developer Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Demo Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFFFFF),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Load sample progress data for testing and demo purposes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFFFFF),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                userState?.let { user ->
                                    goalViewModel.insertDemoProgress(user.id)
                                    showDemoMessage = true
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                "Load Demo",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                userState?.let { user ->
                                    goalViewModel.clearDemoProgress(user.id)
                                    showDemoMessage = true
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                "Clear Data",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                                )
                        }
                    }

                    if (showDemoMessage) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "✓ Data updated successfully",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFFFFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Logout Button
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    "Logout",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Permission Explanation Dialog
    if (showPermissionExplanation) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanation = false },
            title = {
                Text(
                    text = "Location Permission Required",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "UHere needs location access to track your progress:\n\n" +
                                "• Foreground: While using the app\n" +
                                "• Background: Even when the app is closed\n\n" +
                                "Your location data is private and used only for your goals."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "You'll need to grant 'Allow all the time' in the next screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionExplanation = false
                        foregroundPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text("Allow", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionExplanation = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Logout",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to logout? Location tracking will be stopped.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        LocationTrackingService.stop(context)
                        locationViewModel.setTrackingEnabled(false)
                        userViewModel.logout()
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Logout", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Auto-hide messages
    if (showDemoMessage) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(3000)
            showDemoMessage = false
        }
    }

    if (showWeekMessage.isNotEmpty()) {
        LaunchedEffect(showWeekMessage) {
            kotlinx.coroutines.delay(5000)
            showWeekMessage = ""
        }
    }
}