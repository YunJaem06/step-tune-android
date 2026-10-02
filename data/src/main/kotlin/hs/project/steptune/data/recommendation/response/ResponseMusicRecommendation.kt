package hs.project.steptune.data.recommendation.response

import java.math.BigDecimal

data class ResponseRecommendationStepSummary(
    val todayStepCount: Int,
    val recent7DayAverage: BigDecimal,
    val recordedDayCount: Int,
    val differenceFromAverage: BigDecimal,
    val changeRatePercent: BigDecimal?
)

data class ResponseRecommendedTrack(
    val title: String,
    val artist: String,
    val searchQuery: String
)

data class ResponseMusicRecommendation(
    val recommendationId: String,
    val recordDate: String,
    val stepSummary: ResponseRecommendationStepSummary,
    val activityLevel: String,
    val durationMinutes: Int,
    val reason: String,
    val track: ResponseRecommendedTrack,
    val favorite: Boolean = false,
    val generatedAt: String
)

data class ResponseMusicRecommendationHistory(
    val userId: Long,
    val recommendations: List<ResponseMusicRecommendation>,
    val favoriteOnly: Boolean,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
)
