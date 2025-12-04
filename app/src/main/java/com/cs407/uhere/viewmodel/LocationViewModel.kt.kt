package com.cs407.uhere.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.uhere.data.LocationCategory
import com.cs407.uhere.data.Place
import com.cs407.uhere.data.SettingsDataStore
import com.cs407.uhere.data.UHereDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val placeDao = UHereDatabase.getDatabase(application).placeDao()
    private val settingsDataStore = SettingsDataStore(application)

    private val _userPlaces = MutableStateFlow<List<Place>>(emptyList())
    val userPlaces: StateFlow<List<Place>> = _userPlaces.asStateFlow()

    private val _isTrackingEnabled = MutableStateFlow(false)
    val isTrackingEnabled: StateFlow<Boolean> = _isTrackingEnabled.asStateFlow()

    private var currentUserId: Int? = null
    private var loadJob: Job? = null

    init {
        // Load the persisted tracking state when ViewModel is created
        viewModelScope.launch {
            settingsDataStore.isTrackingEnabled.collect { enabled ->
                _isTrackingEnabled.value = enabled
            }
        }
    }

    fun loadUserPlaces(userId: Int) {
        if (currentUserId != userId) {
            loadJob?.cancel()
            currentUserId = userId
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            placeDao.getUserPlaces(userId)
                .flowOn(Dispatchers.IO)
                .collect { places ->
                    _userPlaces.value = places
                }
        }
    }

    fun addPlace(
        userId: Int,
        name: String,
        latitude: Double,
        longitude: Double,
        category: LocationCategory,
        radius: Double = 100.0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val place = Place(
                name = name,
                latitude = latitude,
                longitude = longitude,
                category = category,
                radius = radius,
                userId = userId
            )
            placeDao.insertPlace(place)
        }
    }

    fun deletePlace(place: Place) {
        viewModelScope.launch(Dispatchers.IO) {
            placeDao.deletePlace(place)
        }
    }

    fun setTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setTrackingEnabled(enabled)
            // The init block's collector will automatically update _isTrackingEnabled
        }
    }

    fun clearState() {
        loadJob?.cancel()
        _userPlaces.value = emptyList()
        currentUserId = null
    }
}