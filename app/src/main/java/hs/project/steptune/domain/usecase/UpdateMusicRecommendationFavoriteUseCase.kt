package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.model.MusicRecommendation
import hs.project.steptune.domain.repository.MusicRecommendationRepository
import javax.inject.Inject

class UpdateMusicRecommendationFavoriteUseCase @Inject constructor(
    private val repository: MusicRecommendationRepository
) {
    suspend operator fun invoke(
        recommendationId: String,
        favorite: Boolean
    ): MusicRecommendation {
        require(recommendationId.isNotBlank())
        return repository.updateFavorite(recommendationId, favorite)
    }
}
