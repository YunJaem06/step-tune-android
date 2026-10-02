package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.repository.SettingsRepository
import javax.inject.Inject

class ObserveUserPreferencesUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke() = repository.observePreferences()
}

