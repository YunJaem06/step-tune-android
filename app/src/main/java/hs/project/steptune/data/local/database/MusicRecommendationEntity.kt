package hs.project.steptune.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music_recommendation")
data class MusicRecommendationEntity(
    @PrimaryKey val recommendationId: String,
    val recordDate: String,
    val todayStepCount: Int,
    val recent7DayAverage: Double,
    val recordedDayCount: Int,
    val differenceFromAverage: Double,
    val changeRatePercent: Double?,
    val activityLevel: String,
    val musicMoods: String,
    val genres: String,
    val durationMinutes: Int,
    val reason: String,
    val youtubeSearchQuery: String?,
    val spotifySearchQuery: String?,
    val trackTitle: String?,
    val trackArtist: String?,
    val trackSearchQuery: String?,
    val generatedAt: String
)
