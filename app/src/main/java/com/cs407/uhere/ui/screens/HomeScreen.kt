package com.cs407.uhere.ui.screens

import androidx.compose.material3.AlertDialog
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cs407.uhere.R

@Composable
fun HomeScreen(){
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(0.dp, 16.dp)){
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = "Weekly Overview",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom =16.dp)
            )

            Column(modifier = Modifier.padding(16.dp).background(Color.White).fillMaxWidth()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(4.dp)){
                    var sliderPosition by remember { mutableFloatStateOf(1f) }
                    Column(){
                        Row(modifier = Modifier.padding(8.dp)){
                            Image(painter = painterResource(R.drawable.book),
                                contentDescription = "Library",
                                modifier = Modifier.size(78.dp).padding(4.dp),
                                contentScale = ContentScale.Fit)

                            Text("Library")
                        }
                        LinearProgressIndicator(
                            progress = { 1F },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(4.dp)){
                    var sliderPosition by remember { mutableFloatStateOf(1f) }
                    Column(){
                        Row(modifier = Modifier.padding(8.dp)){
                            Image(painter = painterResource(R.drawable.drink),
                                contentDescription = "Bar",
                                modifier = Modifier.size(78.dp).padding(4.dp),
                                contentScale = ContentScale.Fit)

                            Text("Bar")
                        }
                        LinearProgressIndicator(
                            progress = { 0.3F },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(4.dp)){
                    var sliderPosition by remember { mutableFloatStateOf(1f) }
                    Column(){
                        Row(modifier = Modifier.padding(8.dp)){
                            Image(painter = painterResource(R.drawable.barbell),
                                contentDescription = "Gym",
                                modifier = Modifier.size(78.dp).padding(4.dp),
                                contentScale = ContentScale.Fit)

                            Text("Gym")
                        }
                        LinearProgressIndicator(
                            progress = { 0.5F },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = {showDialog = true}
            ) {
                Text("View AI Weekly Summary")
            }
        }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Text(text = "AI Weekly Summary")
                },
                text = {
                    Text(
                        text = "This is a sample AI-generated summary of your week. " +
                                "You attended 8 classes, completed 5 assignments, and " +
                                "your most productive day was Tuesday with 6 hours of study time."
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