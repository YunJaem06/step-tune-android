package hs.project.steptune.data.repository

import hs.project.steptune.data.local.preferences.PedometerPreferencesDataSource
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.UserPreferences
import hs.project.steptune.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PedometerPreferencesDataSource
) : SettingsRepository {

    override fun observePreferences(): Flow<UserPreferences> {
        return preferencesDataSource.preferences.map {
            UserPreferences(
                dailyGoal = it.dailyGoal,
                stepLengthCm = it.stepLengthCm,
                heightCm = it.heightCm,
                weightKg = it.weightKg,
                preferredGenres = it.preferredGenreKeys
                    .mapNotNull { storageKey -> MusicGenre.fromStorageKey(storageKey) }
                    .toSet(),
                preferredMoods = it.preferredMoodKeys
                    .mapNotNull { storageKey -> MusicMood.fromStorageKey(storageKey) }
                    .toSet(),
                reminderNotificationsEnabled = it.reminderNotificationsEnabled,
                autoStartTrackingEnabled = it.autoStartTrackingEnabled,
                onboardingCompleted = it.onboardingCompleted,
                musicPreferencesOnboardingCompleted = it.musicPreferencesOnboardingCompleted
            )
        }
    }

    override suspend fun completeOnboarding(
        preferredGenres: Set<MusicGenre>,
        preferredMoods: Set<MusicMood>
    ) {
        preferencesDataSource.completeOnboarding(
            preferredGenreKeys = preferredGenres.map { genre -> genre.storageKey }.toSet(),
            preferredMoodKeys = preferredMoods.map { mood -> mood.storageKey }.toSet()
        )
    }

    override suspend fun updateProfileSettings(
        dailyGoal: Int,
        stepLengthCm: Int,
        heightCm: Int,
        weightKg: Int
    ) {
        preferencesDataSource.updateProfileSettings(
            dailyGoal = dailyGoal,
            stepLengthCm = stepLengthCm,
            heightCm = heightCm,
            weightKg = weightKg
        )
    }

    override suspend fun updateReminderNotificationsEnabled(enabled: Boolean) {
        preferencesDataSource.updateReminderNotificationsEnabled(enabled)
    }

    override suspend fun updateAutoStartTrackingEnabled(enabled: Boolean) {
        preferencesDataSource.updateAutoStartTrackingEnabled(enabled)
    }

    override suspend fun updateMusicPreferences(
        preferredGenres: Set<MusicGenre>,
        preferredMoods: Set<MusicMood>
    ) {
        preferencesDataSource.updateMusicPreferences(
            preferredGenreKeys = preferredGenres.map { genre -> genre.storageKey }.toSet(),
            preferredMoodKeys = preferredMoods.map { mood -> mood.storageKey }.toSet()
        )
    }
}

