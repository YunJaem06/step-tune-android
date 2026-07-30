package hs.project.steptune.domain.repository

import hs.project.steptune.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observePreferences(): Flow<UserPreferences>
    suspend fun updateOnboardingCompleted(completed: Boolean)
    suspend fun updateProfileSettings(
        dailyGoal: Int,
        stepLengthCm: Int,
        heightCm: Int,
        weightKg: Int
    )
    suspend fun updateReminderNotificationsEnabled(enabled: Boolean)
    suspend fun updateAutoStartTrackingEnabled(enabled: Boolean)
}

