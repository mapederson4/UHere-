package com.cs407.uhere.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cs407.uhere.data.LocationCategory
import com.cs407.uhere.data.User
import com.cs407.uhere.service.LocationTrackingService
import com.cs407.uhere.viewmodel.LocationViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    var mapLoaded by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var searchedLocation by remember { mutableStateOf<LatLng?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(Unit) {
        val apiKey = context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .metaData
            .getString("com.google.android.geo.API_KEY")
        android.util.Log.d("MapsScreen", "API Key loaded: ${apiKey?.take(10)}...")
    }

    var showAddPlaceDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    // Load places when user changes - with IO dispatcher
    LaunchedEffect(userState) {
        userState?.let {
            locationViewModel.loadUserPlaces(it.id)  //
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(43.0731, -89.4012), // Madison, WI
            12f
        )
    }

    // Memoize markers to prevent recomposition lag
    val markersAndCircles = remember(userPlaces) {
        userPlaces.map { place ->
            place to LatLng(place.latitude, place.longitude)
        }
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
            // Show loading while map initializes
            Box(modifier = Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = true,
                        // Reduce map features to speed up loading
                        isTrafficEnabled = false,
                        isBuildingEnabled = false
                    ),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = true,
                        mapToolbarEnabled = false, // Disable toolbar for faster loading
                        zoomControlsEnabled = false
                    ),
                    onMapClick = { latLng ->
                        selectedLocation = latLng
                        showAddPlaceDialog = true
                    },
                    onMapLoaded = {
                        mapLoaded = true
                        android.util.Log.d("MapsScreen", "Map loaded successfully")
                    }
                ) {
                    searchedLocation?.let { latLng ->
                        Marker(
                            state = rememberMarkerState(position = latLng),
                            title = "Selected Location",
                            onClick = {
                                selectedLocation = latLng
                                showAddPlaceDialog = true
                                true
                            },
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }

                    // Use memoized data to prevent recomposition
                    markersAndCircles.forEach { (place, latLng) ->
                        Marker(
                            state = rememberMarkerState(position = latLng),
                            title = place.name,
                            snippet = place.category.name
                        )

                        Circle(
                            center = latLng,
                            radius = place.radius,
                            fillColor = androidx.compose.ui.graphics.Color.Blue.copy(alpha = 0.2f),
                            strokeColor = androidx.compose.ui.graphics.Color.Blue,
                            strokeWidth = 2f
                        )
                    }


                }
                PlacesSearchBar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(4.dp, 4.dp, 64.dp, 0.dp),
                    onPlaceSelected = { place ->
                        place.latLng?.let { latLng ->
                            searchedLocation = latLng
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                                )
                            }
                        }
                    }
                )

                // Loading indicator
                if (!mapLoaded && mapError == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Error message
                mapError?.let { error ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Map Error",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { mapError = null }) {
                                Text("Retry")
                            }
                        }
                    }
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

    // Timeout detector for map loading
    LaunchedEffect(Unit) {
        delay(10000) // 10 seconds
        if (!mapLoaded && mapError == null) {
            mapError = "Map taking too long to load. Check your internet connection and API key."
            android.util.Log.e("MapsScreen", "Map load timeout")
        }
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

@Composable
fun PlacesSearchBar(
    onPlaceSelected: (Place) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val placesClient = remember { Places.createClient(context) }

    var query by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.length > 2) {
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(it)
                        .build()

                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            predictions = response.autocompletePredictions
                        }
                        .addOnFailureListener {
                            predictions = emptyList()
                        }
                } else {
                    predictions = emptyList()
                }
            },
            placeholder = { Text("Search location...") },
            singleLine = true,
            trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.7f), shape = RoundedCornerShape(12.dp))
        )

        if (predictions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column {
                    predictions.forEach { prediction ->
                        DropdownMenuItem(
                            text = { Text(prediction.getFullText(null).toString()) },
                            onClick = {
                                val placeId = prediction.placeId
                                val placeRequest = FetchPlaceRequest.newInstance(
                                    placeId,
                                    listOf(Place.Field.LAT_LNG, Place.Field.NAME)
                                )
                                placesClient.fetchPlace(placeRequest)
                                    .addOnSuccessListener { response ->
                                        onPlaceSelected(response.place)
                                        query = response.place.name ?: ""
                                        predictions = emptyList()
                                    }
                            }
                        )
                    }
                }
            }
        }
    }
}