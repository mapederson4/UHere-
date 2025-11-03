package com.cs407.uhere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cs407.uhere.data.initializeBadges
import com.cs407.uhere.ui.screens.*
import com.cs407.uhere.ui.theme.UHereTheme
import com.cs407.uhere.viewmodel.GoalViewModel
import com.cs407.uhere.viewmodel.UserViewModel

class MainActivity : ComponentActivity() {
    private val userViewModel: UserViewModel by viewModels()
    private val goalViewModel: GoalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeBadges(this) // Initialize badges (only runs once)

        enableEdgeToEdge()
        setContent {
            UHereTheme {
                AppNavigation(
                    userViewModel = userViewModel,
                    goalViewModel = goalViewModel
                )
            }
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
}

@Composable
fun AppNavigation(
    userViewModel: UserViewModel,
    goalViewModel: GoalViewModel
) {
    val navController = rememberNavController()
    val userState by userViewModel.userState.collectAsState()
    val items = listOf(Screen.Home, Screen.Goal, Screen.Reward, Screen.Settings)

    // Navigate based on authentication state
    LaunchedEffect(userState) {
        if (userState != null) {
            // User is logged in, navigate to home
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        } else {
            // User is logged out, navigate to login
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
        // Auth Screens (no bottom bar)
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginClick = { email, password ->
                    com.cs407.uhere.auth.signIn(
                        email = email,
                        password = password,
                        onSuccess = { firebaseUser ->
                            // Save user to database
                            val user = com.cs407.uhere.data.User(
                                firebaseUid = firebaseUser.uid,
                                displayName = firebaseUser.displayName ?: "User",
                                email = firebaseUser.email ?: email
                            )
                            userViewModel.setUser(user)
                        },
                        onError = { error ->
                            // Show error (you can add a snackbar here)
                            println("Login error: $error")
                        }
                    )
                },
                onSignUpClick = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpClick = { name, email, password ->
                    com.cs407.uhere.auth.createAccount(
                        email = email,
                        password = password,
                        onSuccess = { firebaseUser ->
                            // Update display name
                            com.cs407.uhere.auth.updateDisplayName(name) { success, _ ->
                                if (success) {
                                    val user = com.cs407.uhere.data.User(
                                        firebaseUid = firebaseUser.uid,
                                        displayName = name,
                                        email = email
                                    )
                                    userViewModel.setUser(user)
                                }
                            }
                        },
                        onError = { error ->
                            println("Sign up error: $error")
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

        // Main App Screens (with bottom bar)
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

        composable(Screen.Reward.route) {
            Scaffold(
                bottomBar = { BottomNavigationBar(navController, items) }
            ) { innerPadding ->
                RewardScreen(
                    modifier = Modifier.padding(innerPadding)
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
                    userViewModel = userViewModel
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
    NavigationBar {
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
                }
            )
        }
    }
}