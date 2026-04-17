package com.at.coba.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    companion object {
        val THEME_MODE_KEY = intPreferencesKey("theme_mode")
        val USER_AGREED_KEY = booleanPreferencesKey("user_agreed")
        val PERMISSIONS_SHOWN_KEY = booleanPreferencesKey("permissions_shown")
        val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        val IS_2FA_ENABLED_KEY = booleanPreferencesKey("is_2fa_enabled")
        val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        val COOKIES_KEY = stringPreferencesKey("cookies")
        const val DEVICE_TYPE = "web"
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

    fun getAuthToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTH_TOKEN_KEY]
        }
    }

    suspend fun getOrCreateDeviceId(): String {
        val currentId = context.dataStore.data.map { it[DEVICE_ID_KEY] }.first()
        if (currentId != null) return currentId

        val newId = UUID.randomUUID().toString().replace("-", "")
        context.dataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = newId
        }
        return newId
    }

    val cookies: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[COOKIES_KEY]
    }

    suspend fun setCookies(cookieString: String) {
        context.dataStore.edit { preferences ->
            preferences[COOKIES_KEY] = cookieString
        }
    }

    suspend fun setAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
        }
    }

    val is2FAEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_2FA_ENABLED_KEY] ?: false
    }

    suspend fun setIs2FAEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_2FA_ENABLED_KEY] = enabled
        }
    }

    suspend fun clearAuthData() {
        context.dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
            preferences.remove(IS_2FA_ENABLED_KEY)
            preferences.remove(COOKIES_KEY)
        }
    }

    val allData: Flow<Map<String, Any>> = context.dataStore.data.map { preferences ->
        preferences.asMap().mapKeys { it.key.name }
    }
}
