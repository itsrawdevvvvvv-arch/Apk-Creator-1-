package com.rawedit.app.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "raw_edit_prefs")

@Immutable
data class UserPrefsState(
    val userName: String = "CREATOR_01",
    val avatarPath: String = "",
    val joinDateMs: Long = 0L,
    val onboardingDone: Boolean = false,
    val sampleIntervalMs: Long = 500L,
    val reducedMotion: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val ultraLowEndMode: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val AVATAR_PATH = stringPreferencesKey("avatar_path")
        val JOIN_DATE_MS = longPreferencesKey("join_date_ms")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SAMPLE_INTERVAL_MS = longPreferencesKey("sample_interval_ms")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val ULTRA_LOW_END_MODE = booleanPreferencesKey("ultra_low_end_mode")
    }

    val prefsFlow: Flow<UserPrefsState> = context.dataStore.data.map { prefs ->
        UserPrefsState(
            userName = prefs[Keys.USER_NAME] ?: "CREATOR_01",
            avatarPath = prefs[Keys.AVATAR_PATH] ?: "",
            joinDateMs = prefs[Keys.JOIN_DATE_MS] ?: System.currentTimeMillis(),
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
            sampleIntervalMs = prefs[Keys.SAMPLE_INTERVAL_MS] ?: 500L,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            ultraLowEndMode = prefs[Keys.ULTRA_LOW_END_MODE] ?: false
        )
    }

    suspend fun completeOnboarding(userName: String, avatarPath: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = userName.ifBlank { "CREATOR_01" }
            if (avatarPath.isNotBlank()) {
                prefs[Keys.AVATAR_PATH] = avatarPath
            }
            if ((prefs[Keys.JOIN_DATE_MS] ?: 0L) == 0L) {
                prefs[Keys.JOIN_DATE_MS] = System.currentTimeMillis()
            }
            prefs[Keys.ONBOARDING_DONE] = true
        }
    }

    suspend fun updateProfile(userName: String, avatarPath: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = userName.ifBlank { "CREATOR_01" }
            prefs[Keys.AVATAR_PATH] = avatarPath
        }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REDUCED_MOTION] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setSampleIntervalMs(intervalMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SAMPLE_INTERVAL_MS] = intervalMs.coerceIn(33L, 5000L)
        }
    }

    suspend fun setUltraLowEndMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ULTRA_LOW_END_MODE] = enabled
        }
    }
}
