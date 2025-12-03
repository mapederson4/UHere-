package com.cs407.uhere

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cs407.uhere.data.initializeBadges
import com.cs407.uhere.data.WeeklyProgressManager
import com.cs407.uhere.ui.screens.*
import com.cs407.uhere.ui.theme.UHereTheme
import com.cs407.uhere.viewmodel.GoalViewModel
import com.cs407.uhere.viewmodel.LocationViewModel
import com.cs407.uhere.viewmodel.UserViewModel
import com.google.android.libraries.places.api.Places
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val userViewModel: UserViewModel by viewModels()
    private val goalViewModel: GoalViewModel by viewModels()
    private val locationViewModel: LocationViewModel by viewModels()

    // Notification permission launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "✅ Notification permission granted")
        } else {
            android.util.Log.d("MainActivity", "❌ Notification permission denied")
            Toast.makeText(
                this,
                "Notification permission is required for check-in alerts",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        checkNotificationPermission()

        try {
            val apiKey = applicationContext.packageManager
                .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                .metaData
                ?.getString("com.google.android.geo.API_KEY")

            if (apiKey != null && apiKey.isNotEmpty() && !Places.isInitialized()) {
                Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize Places API", e)
        }

        initializeBadges(this)
        enableEdgeToEdge()
        setContent {
            UHereTheme {
                AppNavigation(
                    userViewModel = userViewModel,
                    goalViewModel = goalViewModel,
                    locationViewModel = locationViewModel
                )
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    android.util.Log.d("MainActivity", "Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show explanation to user
                    Toast.makeText(
                        this,
                        "Notification permission is needed for check-in alerts",
                        Toast.LENGTH_LONG
                    ).show()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request permission directly
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            android.util.Log.d("MainActivity", "Notification permission not required (Android < 13)")
        }
    }
}

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    @DrawableRes val res: Int? = null
) {
    object Login : Screen("login", "Login")
    object SignUp : Screen("signup", "SignUp")
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Goal : Screen("goals", "Goal", res = R.drawable.baseline_alarm_24)
    object Reward : Screen("rewards", "Reward", Icons.Filled.Star)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object Maps : Screen("maps", "Maps", Icons.Filled.LocationOn)
}

@Composable
fun AppNavigation(
    userViewModel: UserViewModel,
    goalViewModel: GoalViewModel,
    locationViewModel: LocationViewModel
) {
    val navController = rememberNavController()
    val userState by userViewModel.userState.collectAsState()
    val items = listOf(Screen.Home, Screen.Goal, Screen.Maps, Screen.Reward, Screen.Settings)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ✅ CRITICAL: Check for week transitions when user logs in or app starts
    LaunchedEffect(userState) {
        userState?.let { user ->
            coroutineScope.launch {
                try {
                    android.util.Log.d("MainActivity", "Checking week transition for user ${user.id}")
                    val weeklyProgressManager = WeeklyProgressManager(context)
                    weeklyProgressManager.checkAndHandleWeekTransition(user.id)
                    android.util.Log.d("MainActivity", "Week transition check completed")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error checking week transition", e)
                }
            }
        }
    }

    LaunchedEffect(userState) {
        if (userState == null) {
            goalViewModel.clearState()
            locationViewModel.clearState()
        }
    }

    LaunchedEffect(userState) {
        if (userState != null) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        } else {
            if (navController.currentDestination?.route != Screen.Login.route &&
                navController.currentDestination?.route != Screen.SignUp.route) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val startDestination = if (userState != null) Screen.Home.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            val context = LocalContext.current
            LoginScreen(
                onLoginClick = { email, password, setLoading ->
                    com.cs407.uhere.auth.signIn(
                        email = email,
                        password = password,
                        onSuccess = { firebaseUser ->
                            val user = com.cs407.uhere.data.User(
                                firebaseUid = firebaseUser.uid,
                                displayName = firebaseUser.displayName ?: "User",
                                email = firebaseUser.email ?: email
                            )
                            userViewModel.setUser(user)
                        },
                        onError = { error ->
                            setLoading(false)
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onSignUpClick = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        composable(Screen.SignUp.route) {
            val context = LocalContext.current
            SignUpScreen(
                onSignUpClick = { name, email, password, setLoading ->
                    com.cs407.uhere.auth.createAccount(
                        email = email,
                        password = password,
                        onSuccess = { firebaseUser ->
                            com.cs407.uhere.auth.updateDisplayName(name) { success, _ ->
                                if (success) {
                                    val user = com.cs407.uhere.data.User(
                                        firebaseUid = firebaseUser.uid,
                                        displayName = name,
                                        email = email
                                    )
                                    userViewModel.setUser(user)
                                } else {
                                    setLoading(false)
                                    Toast.makeText(context, "Failed to update display name", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onError = { error ->
                            setLoading(false)
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onLoginClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            Scaffold(
                bottomBar = { BottomNavigationBar(navController, items) }
            ) { innerPadding ->
                HomeScreen(
                    modifier = Modifier.padding(innerPadding),
                    userState = userState,
                    goalViewModel = goalViewModel
                )
            }
        }

        composable(Screen.Goal.route) {
            Scaffold(
                bottomBar = { BottomNavigationBar(navController, items) }
            ) { innerPadding ->
                GoalScreen(
                    modifier = Modifier.padding(innerPadding),
                    userState = userState,
                    goalViewModel = goalViewModel
                )
            }
        }

        composable(Screen.Maps.route) {
            Scaffold(
                bottomBar = { BottomNavigationBar(navController, items) }
            ) { innerPadding ->
                MapsScreen(
                    modifier = Modifier.padding(innerPadding),
                    userState = userState,
                    locationViewModel = locationViewModel
                )
            }
        }

        composable(Screen.Reward.route) {
            val context = LocalContext.current
            val database = remember { com.cs407.uhere.data.UHereDatabase.getDatabase(context) }

            Scaffold(
                bottomBar = { BottomNavigationBar(navController, items) }
            ) { innerPadding ->
                RewardScreen(
                    modifier = Modifier.padding(innerPadding),
                    userId = userState?.id,
                    database = database
                )
            }
        }

        composable(Screen.Settings.route) {
            Scaffold(
                bottomBar = { BottomNavigationBar(navController, items) }
            ) { innerPadding ->
                SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    userState = userState,
                    userViewModel = userViewModel,
                    goalViewModel = goalViewModel,
                    locationViewModel = locationViewModel
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: androidx.navigation.NavHostController,
    items: List<Screen>
) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp
    ) {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    if (screen.icon != null)
                        Icon(screen.icon, contentDescription = screen.label)
                    else
                        Icon(painterResource(screen.res!!), contentDescription = screen.label)
                },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            )
        }
    }
}