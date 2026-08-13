package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.MusicPreferenceRules
import hs.project.steptune.domain.repository.SettingsRepository
import javax.inject.Inject

class CompleteOnboardingUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(
        preferredGenres: Set<MusicGenre>,
        preferredMoods: Set<MusicMood>
    ) {
        require(preferredGenres.size <= MusicPreferenceRules.MAX_GENRES)
        require(preferredMoods.size <= MusicPreferenceRules.MAX_MOODS)
        repository.completeOnboarding(preferredGenres, preferredMoods)
    }
}
