package com.cs407.uhere.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        private val IS_TRACKING_ENABLED = booleanPreferencesKey("is_tracking_enabled")
    }

    val isTrackingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_TRACKING_ENABLED] ?: false
        }

    suspend fun setTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_TRACKING_ENABLED] = enabled
        }
    }
}