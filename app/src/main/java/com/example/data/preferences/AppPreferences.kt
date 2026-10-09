package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "transaction_mate_prefs")

class AppPreferences(private val context: Context) {
    companion object {
        val KEY_BASE_URL = stringPreferencesKey("backend_base_url")
        val KEY_ACTIVE_USERNAME = stringPreferencesKey("active_username")
        val KEY_ACTIVE_USER_NAME = stringPreferencesKey("active_user_name")
        val KEY_CURRENCY = stringPreferencesKey("currency_symbol")
        val KEY_BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val KEY_LOCK_TIMEOUT_SECONDS = intPreferencesKey("lock_timeout_seconds")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_THEME_COLOR = stringPreferencesKey("theme_color")

        // Configured default server URL
        const val DEFAULT_BASE_URL = "http://147.224.251.137:8001"
        const val DEFAULT_USERNAME = "demo_user"
        const val DEFAULT_CURRENCY = "₹"
        const val DEFAULT_LOCK_TIMEOUT_SECONDS = 15
    }

    val baseUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BASE_URL]?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL
    }

    val activeUsernameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_USERNAME]?.takeIf { it.isNotBlank() } ?: DEFAULT_USERNAME
    }

    val activeUserNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_USER_NAME]?.takeIf { it.isNotBlank() } ?: "User"
    }

    val currencyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURRENCY] ?: DEFAULT_CURRENCY
    }

    val biometricLockEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_LOCK_ENABLED] ?: true
    }

    val lockTimeoutSecondsFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOCK_TIMEOUT_SECONDS] ?: DEFAULT_LOCK_TIMEOUT_SECONDS
    }

    val themeModeFlow: Flow<com.example.ui.theme.AppThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_MODE] ?: com.example.ui.theme.AppThemeMode.SYSTEM.name
        try {
            com.example.ui.theme.AppThemeMode.valueOf(raw)
        } catch (_: Exception) {
            com.example.ui.theme.AppThemeMode.SYSTEM
        }
    }

    val themeColorFlow: Flow<com.example.ui.theme.AppThemeColor> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_COLOR] ?: com.example.ui.theme.AppThemeColor.GREEN.name
        try {
            com.example.ui.theme.AppThemeColor.valueOf(raw)
        } catch (_: Exception) {
            com.example.ui.theme.AppThemeColor.GREEN
        }
    }

    suspend fun setThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setThemeColor(color: com.example.ui.theme.AppThemeColor) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_COLOR] = color.name
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setLockTimeoutSeconds(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOCK_TIMEOUT_SECONDS] = seconds
        }
    }

    suspend fun setBaseUrl(url: String) {
        var cleanUrl = url.trim()
        if (!cleanUrl.endsWith("/")) {
            cleanUrl += "/"
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = cleanUrl
        }
    }

    suspend fun setActiveUser(username: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_USERNAME] = username.trim()
            if (name.isNotBlank()) {
                prefs[KEY_ACTIVE_USER_NAME] = name.trim()
            }
        }
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURRENCY] = currency.trim()
        }
    }
}
