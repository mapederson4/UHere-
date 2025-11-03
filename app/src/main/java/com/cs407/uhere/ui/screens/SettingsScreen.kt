package com.cs407.uhere.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cs407.uhere.data.User
import com.cs407.uhere.viewmodel.UserViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    userState: User?,
    userViewModel: UserViewModel
) {
    var tracking by remember { mutableStateOf(false) }

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
    }
}