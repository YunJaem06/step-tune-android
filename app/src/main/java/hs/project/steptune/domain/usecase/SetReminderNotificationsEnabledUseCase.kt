package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.repository.SettingsRepository
import javax.inject.Inject

class SetReminderNotificationsEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.updateReminderNotificationsEnabled(enabled)
    }
}

