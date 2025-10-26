package com.cs407.uhere.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cs407.uhere.R

@Composable
fun GoalScreen(){
    Box(modifier = Modifier.fillMaxSize().padding(0.dp, 48.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Weekly Time Goals",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
                fontSize = 36.sp)

            Text("Select how many hours you would like to spend in each place per week",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp, start = 16.dp, end = 16.dp))

            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent)){
                var sliderPosition by remember { mutableFloatStateOf(1f) }
                Column(){
                    Row(modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween){
                        Image(painter = painterResource(R.drawable.book),
                            contentDescription = "Library",
                            modifier = Modifier.size(78.dp).padding(4.dp),
                            contentScale = ContentScale.Fit)

                        Text("Library")

                        CircularProgressIndicator(progress = {1.0F})
                    }
                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it }
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent)){
                var sliderPosition by remember { mutableFloatStateOf(1f) }
                Column(){
                    Row(modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween){
                        Image(painter = painterResource(R.drawable.drink),
                            contentDescription = "Bar",
                            modifier = Modifier.size(78.dp).padding(4.dp),
                            contentScale = ContentScale.Fit)

                        Text("Bar")

                        CircularProgressIndicator(progress = {0.3F})
                    }
                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it }
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent)){
                var sliderPosition by remember { mutableFloatStateOf(1f) }
                Column(){
                    Row(modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween){
                        Image(painter = painterResource(R.drawable.barbell),
                            contentDescription = "Gym",
                            modifier = Modifier.size(78.dp).padding(4.dp),
                            contentScale = ContentScale.Fit)

                        Text("Gym")

                        CircularProgressIndicator(progress = {0.5F})
                    }
                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it }
                    )
                }
            }


        }
    }
}