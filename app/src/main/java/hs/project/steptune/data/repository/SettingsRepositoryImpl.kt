package hs.project.steptune.data.repository

import hs.project.steptune.data.local.preferences.PedometerPreferencesDataSource
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
                reminderNotificationsEnabled = it.reminderNotificationsEnabled,
                autoStartTrackingEnabled = it.autoStartTrackingEnabled,
                onboardingCompleted = it.onboardingCompleted
            )
        }
    }

    override suspend fun updateOnboardingCompleted(completed: Boolean) {
        preferencesDataSource.updateOnboardingCompleted(completed)
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
}

