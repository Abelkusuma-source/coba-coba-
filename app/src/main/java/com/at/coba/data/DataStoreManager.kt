package com.at.coba.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    companion object {
        val THEME_MODE_KEY = intPreferencesKey("theme_mode")
        val USER_AGREED_KEY = booleanPreferencesKey("user_agreed")
        val PERMISSIONS_SHOWN_KEY = booleanPreferencesKey("permissions_shown")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        ThemeMode.fromStorageValue(preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM_DEFAULT.storageValue)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.storageValue
        }
    }

    val hasUserAgreed: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USER_AGREED_KEY] ?: false
    }

    suspend fun setUserAgreed(agreed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USER_AGREED_KEY] = agreed
        }
    }

    val hasPermissionsShown: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PERMISSIONS_SHOWN_KEY] ?: false
    }

    suspend fun setPermissionsShown(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PERMISSIONS_SHOWN_KEY] = shown
        }
    }

    val allData: Flow<Map<String, Any>> = context.dataStore.data.map { preferences ->
        preferences.asMap().mapKeys { it.key.name }
    }
}
