package com.at.coba.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    companion object {
        // 0: System Default, 1: Light Mode, 2: Dark Mode
        val THEME_MODE_KEY = intPreferencesKey("theme_mode")
        
        const val MODE_SYSTEM_DEFAULT = 0
        const val MODE_LIGHT = 1
        const val MODE_DARK = 2
    }

    val themeMode: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY] ?: MODE_SYSTEM_DEFAULT
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }

    val allData: Flow<Map<String, Any>> = context.dataStore.data.map { preferences ->
        preferences.asMap().mapKeys { it.key.name }
    }
}
