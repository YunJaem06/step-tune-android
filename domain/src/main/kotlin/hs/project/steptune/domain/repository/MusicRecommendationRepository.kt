package hs.project.steptune.domain.repository

import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.MusicRecommendation
import hs.project.steptune.domain.model.MusicRecommendationHistoryPage

interface MusicRecommendationRepository {
    suspend fun generate(
        recordDate: String,
        preferredMoods: Set<MusicMood>,
        preferredGenres: Set<MusicGenre>,
        durationMinutes: Int
    ): MusicRecommendation

    suspend fun getHistory(
        page: Int,
        size: Int,
        favoriteOnly: Boolean
    ): MusicRecommendationHistoryPage

    suspend fun updateFavorite(
        recommendationId: String,
        favorite: Boolean
    ): MusicRecommendation

    suspend fun delete(recommendationId: String)
}
