package hs.project.steptune.domain.repository

import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observePreferences(): Flow<UserPreferences>
    suspend fun completeOnboarding(
        preferredGenres: Set<MusicGenre>,
        preferredMoods: Set<MusicMood>
    )
    suspend fun updateProfileSettings(
        dailyGoal: Int,
        stepLengthCm: Int,
        heightCm: Int,
        weightKg: Int
    )
    suspend fun updateReminderNotificationsEnabled(enabled: Boolean)
    suspend fun updateAutoStartTrackingEnabled(enabled: Boolean)
    suspend fun updateMusicPreferences(
        preferredGenres: Set<MusicGenre>,
        preferredMoods: Set<MusicMood>
    )
}

