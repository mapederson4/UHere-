package com.cs407.uhere.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cs407.uhere.data.User
import com.cs407.uhere.viewmodel.UserViewModel
import com.cs407.uhere.viewmodel.GoalViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    userState: User?,
    userViewModel: UserViewModel,
    goalViewModel: GoalViewModel // Add this parameter
) {
    var tracking by remember { mutableStateOf(false) }
    var showDemoMessage by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // User Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("Name: ${userState?.displayName ?: "N/A"}")
                    Text("Email: ${userState?.email ?: "N/A"}")
                }
            }

            // Location Tracking Toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Location Tracking")
                    Switch(
                        checked = tracking,
                        onCheckedChange = { newValue ->
                            tracking = newValue
                            // TODO: Implement location tracking service start/stop
                        }
                    )
                }
            }

            // **DEMO DATA CARD**
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Demo Mode",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Load sample progress data for testing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                userState?.let { user ->
                                    goalViewModel.insertDemoProgress(user.id)
                                    showDemoMessage = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Load Demo Data")
                        }

                        OutlinedButton(
                            onClick = {
                                userState?.let { user ->
                                    goalViewModel.clearDemoProgress(user.id)
                                    showDemoMessage = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear Data")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = { userViewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Logout")
            }
        }

        // Show snackbar message
        if (showDemoMessage) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                showDemoMessage = false
            }
        }
    }
}