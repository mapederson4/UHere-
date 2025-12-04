package com.cs407.uhere.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
    val focusManager = LocalFocusManager.current

    var mapLoaded by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var searchedLocation by remember { mutableStateOf<LatLng?>(null) }
    var showPermissionExplanation by remember { mutableStateOf(false) }

    /*var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }*/

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

    LaunchedEffect(Unit) {
        val apiKey = context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .metaData
            .getString("com.google.android.geo.API_KEY")
        android.util.Log.d("MapsScreen", "API Key loaded: ${apiKey?.take(10)}...")
    }

    var showAddPlaceDialog by remember { mutableStateOf(false) }
    var showDeletePlaceDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var toDelete by remember { mutableStateOf<LatLng?>(null) }

    /*val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }*/

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
        if (!(hasForegroundPermission && hasBackgroundPermission)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Location permission is required")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    /*permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )*/
                    showPermissionExplanation = true
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
                        focusManager.clearFocus()
                        selectedLocation = latLng
                        showAddPlaceDialog = true
                    },
                    onMapLoaded = {
                        mapLoaded = true
                        android.util.Log.d("MapsScreen", "Map loaded successfully")
                    }
                ) {
                    LaunchedEffect(cameraPositionState.isMoving) {
                        if (cameraPositionState.isMoving) {
                            focusManager.clearFocus()       // clears while dragging/panning
                        }
                    }

                    searchedLocation?.let { latLng ->
                        key(latLng.latitude to latLng.longitude) {
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
                    }

                    // Use memoized data to prevent recomposition
                    markersAndCircles.forEach { (place, latLng) ->
                        key(place.id) {
                            Marker(
                                state = rememberMarkerState(position = latLng),
                                title = place.name,
                                snippet = place.category.displayName,
                                onInfoWindowClick = { marker ->
                                    toDelete = marker.position
                                    showDeletePlaceDialog = true
                                }
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
                onClick = {
                    focusManager.clearFocus()
                    showAddPlaceDialog = true },
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

    if(showDeletePlaceDialog && toDelete != null){
        val placeToDelete = userPlaces.find{
            it.latitude == toDelete!!.latitude && it.longitude== toDelete!!.longitude
        }
        DeletePlaceDialog(
            onDismiss = {
                showDeletePlaceDialog = false
                toDelete = null
            },
            onConfirm = {
                locationViewModel.deletePlace(placeToDelete!!)
                showDeletePlaceDialog = false
                toDelete = null
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
                        value = category.displayName,
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
                                text = { Text(cat.displayName) },
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
fun DeletePlaceDialog(onDismiss: () -> Unit, onConfirm: () -> Unit){
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Place") },
        confirmButton = {
            TextButton(
                onClick = { onConfirm() }
            ) {
                Text("Delete")
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

                if (it.length < 3) {
                    predictions = emptyList()
                    return@OutlinedTextField
                }

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
            },
            textStyle = TextStyle(color = Color(0xFF1A1A1A)),
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