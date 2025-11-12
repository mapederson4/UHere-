package com.cs407.uhere.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cs407.uhere.data.LocationCategory
import com.cs407.uhere.data.Place
import com.cs407.uhere.data.User
import com.cs407.uhere.service.LocationTrackingService
import com.cs407.uhere.viewmodel.LocationViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(
    modifier: Modifier = Modifier,
    userState: User?,
    locationViewModel: LocationViewModel
) {
    val context = LocalContext.current
    val userPlaces by locationViewModel.userPlaces.collectAsState()
    val isTracking by locationViewModel.isTrackingEnabled.collectAsState()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showAddPlaceDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    // Load places when user changes
    LaunchedEffect(userState) {
        userState?.let { locationViewModel.loadUserPlaces(it.id) }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(43.0731, -89.4012), // Madison, WI
            12f
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!hasLocationPermission) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Location permission is required")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }) {
                    Text("Grant Permission")
                }
            }
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(myLocationButtonEnabled = true),
                onMapClick = { latLng ->
                    selectedLocation = latLng
                    showAddPlaceDialog = true
                }
            ) {
                // Draw circles for each place
                userPlaces.forEach { place ->
                    Marker(
                        state = MarkerState(position = LatLng(place.latitude, place.longitude)),
                        title = place.name,
                        snippet = place.category.name
                    )

                    Circle(
                        center = LatLng(place.latitude, place.longitude),
                        radius = place.radius,
                        fillColor = androidx.compose.ui.graphics.Color.Blue.copy(alpha = 0.2f),
                        strokeColor = androidx.compose.ui.graphics.Color.Blue,
                        strokeWidth = 2f
                    )
                }
            }

            // Tracking toggle
            FloatingActionButton(
                onClick = {
                    userState?.let { user ->
                        if (isTracking) {
                            LocationTrackingService.stop(context)
                            locationViewModel.setTrackingEnabled(false)
                        } else {
                            LocationTrackingService.start(context, user.id)
                            locationViewModel.setTrackingEnabled(true)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(if (isTracking) "Stop" else "Start")
            }

            FloatingActionButton(
                onClick = { showAddPlaceDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, "Add Place")
            }
        }
    }

    if (showAddPlaceDialog && selectedLocation != null) {
        AddPlaceDialog(
            location = selectedLocation!!,
            onDismiss = { showAddPlaceDialog = false },
            onConfirm = { name, category, radius ->
                userState?.let { user ->
                    locationViewModel.addPlace(
                        userId = user.id,
                        name = name,
                        latitude = selectedLocation!!.latitude,
                        longitude = selectedLocation!!.longitude,
                        category = category,
                        radius = radius
                    )
                }
                showAddPlaceDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaceDialog(
    location: LatLng,
    onDismiss: () -> Unit,
    onConfirm: (String, LocationCategory, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(LocationCategory.LIBRARY) }
    var radius by remember { mutableFloatStateOf(100f) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Place") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Place Name") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = category.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        LocationCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Radius: ${radius.toInt()}m")
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..500f
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, category, radius.toDouble()) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}