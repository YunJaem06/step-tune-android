package hs.project.steptune.data.recommendation.request

data class RequestGenerateMusicRecommendation(
    val recordDate: String,
    val preferredMoods: List<String>,
    val preferredGenres: List<String>,
    val durationMinutes: Int
)
