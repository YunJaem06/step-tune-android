package hs.project.steptune.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PedometerPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val preferences: Flow<PedometerPreferences> = dataStore.data.map { prefs ->
        PedometerPreferences(
            dailyGoal = prefs[DAILY_GOAL] ?: 10_000,
            stepLengthCm = prefs[STEP_LENGTH_CM] ?: 72,
            heightCm = prefs[HEIGHT_CM] ?: 170,
            weightKg = prefs[WEIGHT_KG] ?: 65,
            preferredGenreKeys = prefs[PREFERRED_GENRE_KEYS].orEmpty().toSet(),
            preferredMoodKeys = prefs[PREFERRED_MOOD_KEYS].orEmpty().toSet(),
            reminderNotificationsEnabled = prefs[REMINDER_NOTIFICATIONS_ENABLED] ?: true,
            autoStartTrackingEnabled = prefs[AUTO_START_TRACKING_ENABLED] ?: true,
            onboardingCompleted = prefs[ONBOARDING_COMPLETED] ?: false,
            musicPreferencesOnboardingCompleted =
                prefs[MUSIC_PREFERENCES_ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun completeOnboarding(
        preferredGenreKeys: Set<String>,
        preferredMoodKeys: Set<String>
    ) {
        dataStore.edit { prefs ->
            prefs[PREFERRED_GENRE_KEYS] = preferredGenreKeys.toSet()
            prefs[PREFERRED_MOOD_KEYS] = preferredMoodKeys.toSet()
            prefs[ONBOARDING_COMPLETED] = true
            prefs[MUSIC_PREFERENCES_ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun updateProfileSettings(
        dailyGoal: Int,
        stepLengthCm: Int,
        heightCm: Int,
        weightKg: Int
    ) {
        dataStore.edit { prefs ->
            prefs[DAILY_GOAL] = dailyGoal
            prefs[STEP_LENGTH_CM] = stepLengthCm
            prefs[HEIGHT_CM] = heightCm
            prefs[WEIGHT_KG] = weightKg
        }
    }

    suspend fun updateReminderNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[REMINDER_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateMusicPreferences(
        preferredGenreKeys: Set<String>,
        preferredMoodKeys: Set<String>
    ) {
        dataStore.edit { prefs ->
            prefs[PREFERRED_GENRE_KEYS] = preferredGenreKeys.toSet()
            prefs[PREFERRED_MOOD_KEYS] = preferredMoodKeys.toSet()
        }
    }

    suspend fun updateAutoStartTrackingEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_START_TRACKING_ENABLED] = enabled
        }
    }

    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    companion object {
        private val DAILY_GOAL = intPreferencesKey("daily_goal")
        private val STEP_LENGTH_CM = intPreferencesKey("step_length_cm")
        private val HEIGHT_CM = intPreferencesKey("height_cm")
        private val WEIGHT_KG = intPreferencesKey("weight_kg")
        private val PREFERRED_GENRE_KEYS = stringSetPreferencesKey("preferred_genre_keys")
        private val PREFERRED_MOOD_KEYS = stringSetPreferencesKey("preferred_mood_keys")
        private val REMINDER_NOTIFICATIONS_ENABLED = booleanPreferencesKey("reminder_notifications_enabled")
        private val AUTO_START_TRACKING_ENABLED = booleanPreferencesKey("auto_start_tracking_enabled")
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val MUSIC_PREFERENCES_ONBOARDING_COMPLETED =
            booleanPreferencesKey("music_preferences_onboarding_completed")
    }
}

