package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.repository.MusicRecommendationRepository
import javax.inject.Inject

class DeleteMusicRecommendationUseCase @Inject constructor(
    private val repository: MusicRecommendationRepository
) {
    suspend operator fun invoke(recommendationId: String) {
        require(recommendationId.isNotBlank())
        repository.delete(recommendationId)
    }
}
