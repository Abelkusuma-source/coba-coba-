package com.at.coba.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class TradingConfig(
    val rsiPeriod: Int = 14,
    val macdFast: Int = 12,
    val macdSlow: Int = 26,
    val macdSignal: Int = 9,
    val bbPeriod: Int = 20,
    val bbStdDevMultiplier: Float = 2.0f,
    val strategy: TradingStrategy = TradingStrategy.MACD_RSI
)

class DataStoreManager(private val context: Context) {

    private val dataStore = context.applicationContext.dataStore

    companion object {
        val THEME_MODE_KEY = intPreferencesKey("theme_mode")
        val USER_AGREED_KEY = booleanPreferencesKey("user_agreed")
        val PERMISSIONS_SHOWN_KEY = booleanPreferencesKey("permissions_shown")
        val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        val IS_2FA_ENABLED_KEY = booleanPreferencesKey("is_2fa_enabled")
        val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        val COOKIES_KEY = stringPreferencesKey("cookies")
        val TWO_FA_TOKEN_KEY = stringPreferencesKey("two_fa_token")
        /** @deprecated legacy key; migrated into [COOKIES_KEY] via [migrateSessionCookieIntoUnifiedIfNeeded] */
        private val LEGACY_SESSION_COOKIE_KEY = stringPreferencesKey("session_cookie")
        
        val RSI_PERIOD_KEY = intPreferencesKey("rsi_period")
        val MACD_FAST_KEY = intPreferencesKey("macd_fast")
        val MACD_SLOW_KEY = intPreferencesKey("macd_slow")
        val MACD_SIGNAL_KEY = intPreferencesKey("macd_signal")
        val BB_PERIOD_KEY = intPreferencesKey("bb_period")
        val BB_STDDEV_KEY = floatPreferencesKey("bb_stddev")
        val TRADING_STRATEGY_KEY = stringPreferencesKey("trading_strategy")
        val PROFILE_IMAGE_URI_KEY = stringPreferencesKey("profile_image_uri")
        val PROFILE_REMOTE_AVATAR_URL_KEY = stringPreferencesKey("profile_remote_avatar_url")
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        val USER_PHONE_KEY = stringPreferencesKey("user_phone")

        /** Internal file name after copying picker content into app storage. */
        const val PROFILE_IMAGE_INTERNAL_FILE = "profile_image.jpg"

        const val DEVICE_TYPE = "web"
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        ThemeMode.fromStorageValue(preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM_DEFAULT.storageValue)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.storageValue
        }
    }

    val hasUserAgreed: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[USER_AGREED_KEY] ?: false
    }

    suspend fun setUserAgreed(agreed: Boolean) {
        dataStore.edit { preferences ->
            preferences[USER_AGREED_KEY] = agreed
        }
    }

    val hasPermissionsShown: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PERMISSIONS_SHOWN_KEY] ?: false
    }

    suspend fun setPermissionsShown(shown: Boolean) {
        dataStore.edit { preferences ->
            preferences[PERMISSIONS_SHOWN_KEY] = shown
        }
    }

    val authToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN_KEY]
    }

    suspend fun getOrCreateDeviceId(): String {
        val currentId = dataStore.data.map { it[DEVICE_ID_KEY] }.first()
        if (currentId != null) return currentId

        val newId = UUID.randomUUID().toString().replace("-", "")
        dataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = newId
        }
        return newId
    }

    val cookies: Flow<String?> = dataStore.data.map { preferences ->
        preferences[COOKIES_KEY]
    }

    val twoFaToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[TWO_FA_TOKEN_KEY]
    }

    suspend fun setCookies(cookieString: String) {
        dataStore.edit { preferences ->
            preferences[COOKIES_KEY] = cookieString
        }
    }

    suspend fun setTwoFaToken(token: String) {
        dataStore.edit { preferences ->
            preferences[TWO_FA_TOKEN_KEY] = token
        }
    }

    /**
     * One-time migration: merge legacy `session_cookie` into the unified [COOKIES_KEY] and remove the old key.
     */
    suspend fun migrateSessionCookieIntoUnifiedIfNeeded() {
        dataStore.edit { preferences ->
            val legacy = preferences[LEGACY_SESSION_COOKIE_KEY]
            if (legacy.isNullOrEmpty()) return@edit
            val current = (preferences[COOKIES_KEY] ?: "").trim()
            val merged = mergeCookieHeaderParts(listOf(legacy, current).filter { it.isNotEmpty() })
            if (merged.isNotEmpty()) {
                preferences[COOKIES_KEY] = merged
            }
            preferences.remove(LEGACY_SESSION_COOKIE_KEY)
        }
    }

    private fun mergeCookieHeaderParts(parts: List<String>): String {
        val map = linkedMapOf<String, String>()
        for (p in parts) {
            p.split(';').forEach { seg ->
                val s = seg.trim()
                if (s.isEmpty()) return@forEach
                val kv = s.split('=', limit = 2)
                if (kv.size == 2) {
                    val k = kv[0].trim()
                    if (k.isNotEmpty()) map[k] = kv[1].trim()
                }
            }
        }
        return map.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    suspend fun setAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
        }
    }

    val is2FAEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_2FA_ENABLED_KEY] ?: false
    }

    val profileImageUri: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PROFILE_IMAGE_URI_KEY]
    }

    val profileRemoteAvatarUrl: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PROFILE_REMOTE_AVATAR_URL_KEY]
    }

    val userEmail: Flow<String?> = dataStore.data.map { it[USER_EMAIL_KEY] }
    val userPhone: Flow<String?> = dataStore.data.map { it[USER_PHONE_KEY] }

    suspend fun getStoredUserId(): String? = dataStore.data.map { it[USER_ID_KEY] }.first()

    suspend fun setUserId(userId: String) {
        dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }

    suspend fun setUserProfileInfo(email: String?, phone: String?) {
        dataStore.edit { preferences ->
            if (email != null) preferences[USER_EMAIL_KEY] = email
            if (phone != null) preferences[USER_PHONE_KEY] = phone
        }
    }

    suspend fun setProfileRemoteAvatarUrl(url: String?) {
        dataStore.edit { preferences ->
            if (url.isNullOrBlank()) {
                preferences.remove(PROFILE_REMOTE_AVATAR_URL_KEY)
            } else {
                preferences[PROFILE_REMOTE_AVATAR_URL_KEY] = url
            }
        }
    }

    suspend fun setProfileImageUri(uri: String) {
        dataStore.edit { preferences ->
            preferences[PROFILE_IMAGE_URI_KEY] = uri
        }
    }

    /**
     * Removes cached profile file and profile-related keys (not [USER_ID_KEY]).
     */
    suspend fun clearProfileImageStorage() {
        withContext(Dispatchers.IO) {
            File(context.filesDir, PROFILE_IMAGE_INTERNAL_FILE).delete()
        }
        dataStore.edit { preferences ->
            preferences.remove(PROFILE_IMAGE_URI_KEY)
            preferences.remove(PROFILE_REMOTE_AVATAR_URL_KEY)
        }
    }

    /**
     * Copies the user-picked image into app-internal storage and stores a stable file [Uri] string.
     * Avoids broken [content://] links after process death or provider revocation.
     */
    suspend fun persistProfileImageFromPicker(source: Uri?) {
        if (source == null) {
            clearProfileImageStorage()
            return
        }
        try {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(source)?.use { input ->
                    val dest = File(context.filesDir, PROFILE_IMAGE_INTERNAL_FILE)
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Unable to open image stream")
            }
            val persisted = Uri.fromFile(File(context.filesDir, PROFILE_IMAGE_INTERNAL_FILE)).toString()
            setProfileImageUri(persisted)
        } catch (_: Exception) {
            clearProfileImageStorage()
        }
    }

    val tradingConfig: Flow<TradingConfig> = dataStore.data.map { preferences ->
        TradingConfig(
            rsiPeriod = preferences[RSI_PERIOD_KEY] ?: 14,
            macdFast = preferences[MACD_FAST_KEY] ?: 12,
            macdSlow = preferences[MACD_SLOW_KEY] ?: 26,
            macdSignal = preferences[MACD_SIGNAL_KEY] ?: 9,
            bbPeriod = preferences[BB_PERIOD_KEY] ?: 20,
            bbStdDevMultiplier = preferences[BB_STDDEV_KEY] ?: 2.0f,
            strategy = TradingStrategy.fromStorageKey(preferences[TRADING_STRATEGY_KEY])
        )
    }

    suspend fun updateTradingConfig(config: TradingConfig) {
        dataStore.edit { preferences ->
            preferences[RSI_PERIOD_KEY] = config.rsiPeriod
            preferences[MACD_FAST_KEY] = config.macdFast
            preferences[MACD_SLOW_KEY] = config.macdSlow
            preferences[MACD_SIGNAL_KEY] = config.macdSignal
            preferences[BB_PERIOD_KEY] = config.bbPeriod.coerceIn(5, 200)
            preferences[BB_STDDEV_KEY] = config.bbStdDevMultiplier.coerceIn(0.5f, 4f)
            preferences[TRADING_STRATEGY_KEY] = config.strategy.storageKey
        }
    }

    suspend fun setIs2FAEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_2FA_ENABLED_KEY] = enabled
        }
    }

    suspend fun clearAuthData() {
        clearProfileImageStorage()
        dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
            preferences.remove(COOKIES_KEY)
            preferences.remove(TWO_FA_TOKEN_KEY)
            preferences.remove(LEGACY_SESSION_COOKIE_KEY)
            preferences.remove(IS_2FA_ENABLED_KEY)
            preferences.remove(USER_ID_KEY)
        }
    }

    val allData: Flow<Map<String, Any>> = dataStore.data.map { preferences ->
        preferences.asMap().mapKeys { it.key.name }
    }
}
