package hs.project.steptune.domain.model

import androidx.compose.runtime.Immutable

enum class RecommendationActivityLevel {
    LOW,
    MODERATE,
    HIGH
}

@Immutable
data class RecommendationStepSummary(
    val todayStepCount: Int,
    val recent7DayAverage: Double,
    val recordedDayCount: Int,
    val differenceFromAverage: Double,
    val changeRatePercent: Double?
)

@Immutable
data class RecommendedTrack(
    val title: String,
    val artist: String,
    val searchQuery: String
)

@Immutable
data class MusicRecommendation(
    val recommendationId: String,
    val recordDate: String,
    val stepSummary: RecommendationStepSummary,
    val activityLevel: RecommendationActivityLevel,
    val durationMinutes: Int,
    val reason: String,
    val track: RecommendedTrack,
    val favorite: Boolean = false,
    val generatedAt: String
)

@Immutable
data class MusicRecommendationHistoryPage(
    val recommendations: List<MusicRecommendation>,
    val favoriteOnly: Boolean,
    val page: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
)
