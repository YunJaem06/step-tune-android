package hs.project.steptune.feature.music

import hs.project.steptune.domain.model.DailyProgress
import hs.project.steptune.domain.model.DailyStepRecord
import hs.project.steptune.domain.model.DailyStepRecordSyncResult
import hs.project.steptune.domain.model.DailyStepRecordWrite
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.MusicRecommendation
import hs.project.steptune.domain.model.MusicRecommendationHistoryPage
import hs.project.steptune.domain.model.RecommendationActivityLevel
import hs.project.steptune.domain.model.RecommendationStepSummary
import hs.project.steptune.domain.model.RecommendedTrack
import hs.project.steptune.domain.model.UserPreferences
import hs.project.steptune.domain.model.WeeklyStepStatistics
import hs.project.steptune.domain.repository.MusicRecommendationRepository
import hs.project.steptune.domain.repository.PedometerRepository
import hs.project.steptune.domain.repository.SettingsRepository
import hs.project.steptune.domain.repository.StepRecordRepository
import hs.project.steptune.domain.usecase.GenerateMusicRecommendationUseCase
import hs.project.steptune.domain.usecase.SyncDailyStepRecordsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

internal fun recommendation(id: String = "track-1", favorite: Boolean = false) = MusicRecommendation(
    recommendationId = id,
    recordDate = "2026-10-02",
    stepSummary = RecommendationStepSummary(3_200, 3_000.0, 7, 200.0, 6.67),
    activityLevel = RecommendationActivityLevel.MODERATE,
    durationMinutes = 30,
    reason = "가볍게 산책해 보세요.",
    track = RecommendedTrack("Dynamite", "BTS", "BTS Dynamite official audio"),
    favorite = favorite,
    generatedAt = "2026-10-02T00:00:00Z"
)

internal fun historyPage(
    tracks: List<MusicRecommendation>,
    page: Int = 0,
    favoriteOnly: Boolean = false,
    hasNext: Boolean = false
) = MusicRecommendationHistoryPage(
    recommendations = tracks,
    favoriteOnly = favoriteOnly,
    page = page,
    totalElements = tracks.size.toLong(),
    totalPages = if (hasNext) page + 2 else page + 1,
    hasNext = hasNext
)

internal class FakeMusicRepository : MusicRecommendationRepository {
    data class GenerateRequest(val moods: Set<MusicMood>, val genres: Set<MusicGenre>, val minutes: Int)
    val generateRequests = mutableListOf<GenerateRequest>()
    val historyRequests = mutableListOf<Pair<Int, Boolean>>()
    val favoriteRequests = mutableListOf<Pair<String, Boolean>>()
    val deletedIds = mutableListOf<String>()

    var onGenerate: suspend () -> MusicRecommendation = { recommendation() }
    var onHistory: suspend (Int, Boolean) -> MusicRecommendationHistoryPage = { page, favoriteOnly ->
        historyPage(listOf(recommendation()), page, favoriteOnly)
    }
    var onFavorite: suspend (String, Boolean) -> MusicRecommendation = { id, favorite ->
        recommendation(id, favorite)
    }
    var onDelete: suspend (String) -> Unit = {}

    override suspend fun generate(
        recordDate: String,
        preferredMoods: Set<MusicMood>,
        preferredGenres: Set<MusicGenre>,
        durationMinutes: Int
    ): MusicRecommendation {
        generateRequests += GenerateRequest(preferredMoods, preferredGenres, durationMinutes)
        return onGenerate()
    }

    override suspend fun getHistory(page: Int, size: Int, favoriteOnly: Boolean): MusicRecommendationHistoryPage {
        historyRequests += page to favoriteOnly
        return onHistory(page, favoriteOnly)
    }

    override suspend fun updateFavorite(recommendationId: String, favorite: Boolean): MusicRecommendation {
        favoriteRequests += recommendationId to favorite
        return onFavorite(recommendationId, favorite)
    }

    override suspend fun delete(recommendationId: String) {
        deletedIds += recommendationId
        onDelete(recommendationId)
    }
}

internal class FakeSettingsRepository : SettingsRepository {
    var preferences = UserPreferences(
        preferredGenres = setOf(MusicGenre.INDIE),
        preferredMoods = setOf(MusicMood.CALM)
    )
    var readFailure: Exception? = null

    override fun observePreferences(): Flow<UserPreferences> = flow {
        readFailure?.let { throw it }
        emit(preferences)
    }
    override suspend fun completeOnboarding(preferredGenres: Set<MusicGenre>, preferredMoods: Set<MusicMood>) = Unit
    override suspend fun updateProfileSettings(dailyGoal: Int, stepLengthCm: Int, heightCm: Int, weightKg: Int) = Unit
    override suspend fun updateReminderNotificationsEnabled(enabled: Boolean) = Unit
    override suspend fun updateAutoStartTrackingEnabled(enabled: Boolean) = Unit
    override suspend fun updateMusicPreferences(preferredGenres: Set<MusicGenre>, preferredMoods: Set<MusicMood>) = Unit
}

internal fun generateUseCase(
    musicRepository: FakeMusicRepository,
    settingsRepository: FakeSettingsRepository
) = GenerateMusicRecommendationUseCase(
    pedometerRepository = object : PedometerRepository {
        override fun observeDailyProgress(date: String): Flow<DailyProgress> = flowOf(progress(date))
        override fun observeDailyProgressRange(startDate: String, endDate: String): Flow<List<DailyProgress>> =
            flowOf(emptyList())
        override suspend fun getDailyProgress(date: String): DailyProgress = progress(date)
        override suspend fun upsertDailyProgress(progress: DailyProgress) = Unit
        private fun progress(date: String) = DailyProgress(date, 3_200, 10_000, 0f, 0f, 1_791_000_000_000L)
    },
    settingsRepository = settingsRepository,
    musicRecommendationRepository = musicRepository,
    syncDailyStepRecordsUseCase = SyncDailyStepRecordsUseCase(object : StepRecordRepository {
        override suspend fun syncDailyRecords(records: List<DailyStepRecordWrite>) =
            DailyStepRecordSyncResult(emptyList(), "")
        override suspend fun getDailyRecord(recordDate: String): DailyStepRecord? = null
        override suspend fun getHistory(from: String, to: String): List<DailyStepRecord> = emptyList()
        override suspend fun getWeeklyStatistics(recordDate: String): WeeklyStepStatistics = error("Not used")
    })
)
