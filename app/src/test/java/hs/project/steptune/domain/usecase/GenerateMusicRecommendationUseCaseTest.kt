package hs.project.steptune.domain.usecase

import hs.project.steptune.api.NotFoundException
import hs.project.steptune.domain.model.DailyProgress
import hs.project.steptune.domain.model.DailyStepRecord
import hs.project.steptune.domain.model.DailyStepRecordSyncResult
import hs.project.steptune.domain.model.DailyStepRecordWrite
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.MusicRecommendation
import hs.project.steptune.domain.model.RecommendedTrack
import hs.project.steptune.domain.model.RecommendationActivityLevel
import hs.project.steptune.domain.model.RecommendationStepSummary
import hs.project.steptune.domain.model.UserPreferences
import hs.project.steptune.domain.model.WeeklyStepStatistics
import hs.project.steptune.domain.repository.MusicRecommendationRepository
import hs.project.steptune.domain.repository.PedometerRepository
import hs.project.steptune.domain.repository.SettingsRepository
import hs.project.steptune.domain.repository.StepRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateMusicRecommendationUseCaseTest {
    @Test
    fun `missing today record is created and synced before recommendation`() = runBlocking {
        val pedometerRepository = RecommendationFakePedometerRepository()
        val stepRecordRepository = RecommendationFakeStepRecordRepository()
        val recommendationRepository = RecommendationFakeRepository()
        val useCase = createUseCase(
            pedometerRepository,
            stepRecordRepository,
            recommendationRepository
        )

        useCase(
            preferredMoods = setOf(MusicMood.CALM),
            preferredGenres = setOf(MusicGenre.INDIE),
            durationMinutes = 30,
            recordDate = TEST_DATE
        )

        assertEquals(0, pedometerRepository.records.getValue(TEST_DATE).steps)
        assertTrue(pedometerRepository.records.getValue(TEST_DATE).measuredAtEpochMillis > 0L)
        assertEquals(1, stepRecordRepository.syncedRequests.size)
        assertEquals(0, stepRecordRepository.syncedRequests.single().single().stepCount)
        assertEquals(1, recommendationRepository.generateCallCount)
    }

    @Test
    fun `missing server record retries sync and recommendation once`() = runBlocking {
        val pedometerRepository = RecommendationFakePedometerRepository(
            initialRecord = progress(3_200)
        )
        val stepRecordRepository = RecommendationFakeStepRecordRepository()
        val recommendationRepository = RecommendationFakeRepository(notFoundResponses = 1)
        val useCase = createUseCase(
            pedometerRepository,
            stepRecordRepository,
            recommendationRepository
        )

        useCase(
            preferredMoods = emptySet(),
            preferredGenres = emptySet(),
            durationMinutes = 30,
            recordDate = TEST_DATE
        )

        assertEquals(2, stepRecordRepository.syncedRequests.size)
        assertEquals(2, recommendationRepository.generateCallCount)
    }

    private fun createUseCase(
        pedometerRepository: RecommendationFakePedometerRepository,
        stepRecordRepository: RecommendationFakeStepRecordRepository,
        recommendationRepository: RecommendationFakeRepository
    ) = GenerateMusicRecommendationUseCase(
        pedometerRepository = pedometerRepository,
        settingsRepository = RecommendationFakeSettingsRepository(),
        musicRecommendationRepository = recommendationRepository,
        syncDailyStepRecordsUseCase = SyncDailyStepRecordsUseCase(stepRecordRepository)
    )

    private companion object {
        const val TEST_DATE = "2026-09-08"

        fun progress(steps: Int) = DailyProgress(
            date = TEST_DATE,
            steps = steps,
            goal = 10_000,
            distanceMeters = 0f,
            calories = 0f,
            measuredAtEpochMillis = 1_788_837_600_000L
        )
    }
}

private class RecommendationFakePedometerRepository(
    initialRecord: DailyProgress? = null
) : PedometerRepository {
    val records = initialRecord?.let { linkedMapOf(it.date to it) } ?: linkedMapOf()

    override fun observeDailyProgress(date: String): Flow<DailyProgress> =
        flowOf(records[date] ?: error("record not found"))

    override fun observeDailyProgressRange(
        startDate: String,
        endDate: String
    ): Flow<List<DailyProgress>> = flowOf(
        records.values.filter { record -> record.date in startDate..endDate }
    )

    override suspend fun getDailyProgress(date: String): DailyProgress? = records[date]

    override suspend fun upsertDailyProgress(progress: DailyProgress) {
        records[progress.date] = progress
    }
}

private class RecommendationFakeSettingsRepository : SettingsRepository {
    override fun observePreferences(): Flow<UserPreferences> = flowOf(UserPreferences())
    override suspend fun completeOnboarding(
        preferredGenres: Set<MusicGenre>,
        preferredMoods: Set<MusicMood>
    ) = Unit
    override suspend fun updateProfileSettings(
        dailyGoal: Int,
        stepLengthCm: Int,
        heightCm: Int,
        weightKg: Int
    ) = Unit
    override suspend fun updateReminderNotificationsEnabled(enabled: Boolean) = Unit
    override suspend fun updateAutoStartTrackingEnabled(enabled: Boolean) = Unit
    override suspend fun updateMusicPreferences(
        preferredGenres: Set<MusicGenre>,
        preferredMoods: Set<MusicMood>
    ) = Unit
}

private class RecommendationFakeStepRecordRepository : StepRecordRepository {
    val syncedRequests = mutableListOf<List<DailyStepRecordWrite>>()

    override suspend fun syncDailyRecords(
        records: List<DailyStepRecordWrite>
    ): DailyStepRecordSyncResult {
        syncedRequests += records
        return DailyStepRecordSyncResult(records = emptyList(), syncTime = "")
    }

    override suspend fun getDailyRecord(recordDate: String): DailyStepRecord? = null
    override suspend fun getHistory(from: String, to: String): List<DailyStepRecord> = emptyList()
    override suspend fun getWeeklyStatistics(recordDate: String): WeeklyStepStatistics =
        error("not used")
}

private class RecommendationFakeRepository(
    private var notFoundResponses: Int = 0
) : MusicRecommendationRepository {
    var generateCallCount: Int = 0

    override suspend fun generate(
        recordDate: String,
        preferredMoods: Set<MusicMood>,
        preferredGenres: Set<MusicGenre>,
        durationMinutes: Int
    ): MusicRecommendation {
        generateCallCount++
        if (notFoundResponses > 0) {
            notFoundResponses--
            throw NotFoundException()
        }
        return MusicRecommendation(
            recommendationId = "recommendation-id",
            recordDate = recordDate,
            stepSummary = RecommendationStepSummary(0, 0.0, 1, 0.0, null),
            activityLevel = RecommendationActivityLevel.LOW,
            durationMinutes = durationMinutes,
            reason = "추천 이유",
            track = RecommendedTrack(
                title = "Dynamite",
                artist = "BTS",
                searchQuery = "BTS Dynamite official audio"
            ),
            generatedAt = "2026-09-08T00:00:00Z"
        )
    }
}
