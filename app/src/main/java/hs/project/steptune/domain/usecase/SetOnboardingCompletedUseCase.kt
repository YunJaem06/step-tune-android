package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.repository.SettingsRepository
import javax.inject.Inject

class SetOnboardingCompletedUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(completed: Boolean) {
        repository.updateOnboardingCompleted(completed)
    }
}

