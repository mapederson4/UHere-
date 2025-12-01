package com.cs407.uhere.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cs407.uhere.R

data class Reward(
    val id: Int,
    val drawableRes: Int,
    val name: String = "",
    val description: String = "",
    val isUnlocked: Boolean = false
)

@Composable
fun RewardScreen(modifier: Modifier = Modifier) {
    // Replace these with your actual drawable resource IDs
    val rewards = listOf(
        Reward(1, R.drawable.bar1, "Bar 1", "Meet bar goals once", true),
        Reward(2, R.drawable.bar2, "Bar 2", "Meet bar goals three times", true),
        Reward(3, R.drawable.bar3, "Bar 3", "Meet bar goals three weeks consecutively", false),

        Reward(4, R.drawable.gym1, "Gym 1", "Meet gym goals once", true),
        Reward(5, R.drawable.gym2, "Gym 2", "Meet gym goals three times", false),
        Reward(6, R.drawable.gym3, "Gym 3", "Meet gym goals three weeks consecutively", false),

        Reward(7, R.drawable.library1, "Library 1", "Meet library goals once", true),
        Reward(8, R.drawable.library2, "Library 2", "Meet library goals three times", true),
        Reward(9, R.drawable.library3, "Library 3", "Meet library goals three weeks consecutively", true),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Rewards Screen",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(rewards) { reward ->
                FlippableRewardCard(reward = reward)
            }
        }
    }
}

@Composable
fun FlippableRewardCard(reward: Reward) {
    var isFlipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "cardFlip"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { isFlipped = !isFlipped },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = if (rotation > 90f) 180f else 0f
                },
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // if unlocked show normal front side, if not show locked placeholder
                if (reward.isUnlocked){
                    // Front side
                    Image(
                        painter = painterResource(id = reward.drawableRes),
                        contentDescription = reward.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // show a locked placeholder
                    Image(
                        painter = painterResource(id = R.drawable.locked),
                        contentDescription = reward.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }

            } else {
                // Back side
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = reward.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = reward.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RewardCard(reward: Reward) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.reward),
                contentDescription = reward.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}