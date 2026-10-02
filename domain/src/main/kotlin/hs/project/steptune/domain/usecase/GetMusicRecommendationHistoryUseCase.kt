package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.model.MusicRecommendationHistoryPage
import hs.project.steptune.domain.repository.MusicRecommendationRepository
import javax.inject.Inject

class GetMusicRecommendationHistoryUseCase @Inject constructor(
    private val repository: MusicRecommendationRepository
) {
    suspend operator fun invoke(
        page: Int,
        favoriteOnly: Boolean,
        size: Int = DEFAULT_PAGE_SIZE
    ): MusicRecommendationHistoryPage {
        require(page >= 0)
        require(size in 1..MAX_PAGE_SIZE)
        return repository.getHistory(page, size, favoriteOnly)
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 50
    }
}
