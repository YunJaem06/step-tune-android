package hs.project.steptune.domain.usecase

import hs.project.steptune.api.NotFoundException
import hs.project.steptune.domain.model.DailyProgress
import hs.project.steptune.domain.model.DailyStepRecord
import hs.project.steptune.domain.model.DailyStepRecordSyncResult
import hs.project.steptune.domain.model.DailyStepRecordWrite
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.UserPreferences
import hs.project.steptune.domain.model.WeeklyStepStatistics
import hs.project.steptune.domain.repository.PedometerRepository
import hs.project.steptune.domain.repository.SettingsRepository
import hs.project.steptune.domain.repository.StepRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetWeeklyStepStatisticsUseCaseTest {
    @Test
    fun `missing local day is stored and synced as zero before statistics request`() = runBlocking {
        val pedometerRepository = WeeklyFakePedometerRepository()
        val stepRecordRepository = WeeklyFakeStepRecordRepository()
        val useCase = createUseCase(pedometerRepository, stepRecordRepository)

        val result = useCase(TEST_DATE)

        assertEquals(0, pedometerRepository.records.getValue(TEST_DATE).steps)
        assertTrue(pedometerRepository.records.getValue(TEST_DATE).measuredAtEpochMillis > 0L)
        assertEquals(listOf(0), stepRecordRepository.syncedRequests.map { it.single().stepCount })
        assertEquals(TEST_DATE, stepRecordRepository.requestedStatisticsDates.single())
        assertEquals(3_000.0, result.recent7DayAverage, 0.0)
    }

    @Test
    fun `not found statistics retries sync and request once`() = runBlocking {
        val pedometerRepository = WeeklyFakePedometerRepository(
            initialRecord = dailyProgress(2_000)
        )
        val stepRecordRepository = WeeklyFakeStepRecordRepository(
            remainingNotFoundResponses = 1
        )
        val useCase = createUseCase(pedometerRepository, stepRecordRepository)

        useCase(TEST_DATE)

        assertEquals(2, stepRecordRepository.syncedRequests.size)
        assertEquals(2, stepRecordRepository.requestedStatisticsDates.size)
    }

    private fun createUseCase(
        pedometerRepository: WeeklyFakePedometerRepository,
        stepRecordRepository: WeeklyFakeStepRecordRepository
    ): GetWeeklyStepStatisticsUseCase = GetWeeklyStepStatisticsUseCase(
        pedometerRepository = pedometerRepository,
        settingsRepository = WeeklyFakeSettingsRepository(),
        stepRecordRepository = stepRecordRepository,
        syncDailyStepRecordsUseCase = SyncDailyStepRecordsUseCase(stepRecordRepository)
    )

    private companion object {
        const val TEST_DATE = "2026-09-03"

        fun dailyProgress(steps: Int) = DailyProgress(
            date = TEST_DATE,
            steps = steps,
            goal = 10_000,
            distanceMeters = 0f,
            calories = 0f,
            measuredAtEpochMillis = TEST_MEASURED_AT_EPOCH_MILLIS
        )

        const val TEST_MEASURED_AT_EPOCH_MILLIS = 1_788_405_600_000L
    }
}

private class WeeklyFakePedometerRepository(
    initialRecord: DailyProgress? = null
) : PedometerRepository {
    val records = initialRecord?.let { linkedMapOf(it.date to it) } ?: linkedMapOf()

    override fun observeDailyProgress(date: String): Flow<DailyProgress> =
        flowOf(records[date] ?: error("record not found"))

    override fun observeDailyProgressRange(
        startDate: String,
        endDate: String
    ): Flow<List<DailyProgress>> = flowOf(
        records.values.filter { it.date in startDate..endDate }
    )

    override suspend fun getDailyProgress(date: String): DailyProgress? = records[date]

    override suspend fun upsertDailyProgress(progress: DailyProgress) {
        records[progress.date] = progress
    }
}

private class WeeklyFakeSettingsRepository : SettingsRepository {
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

private class WeeklyFakeStepRecordRepository(
    private var remainingNotFoundResponses: Int = 0
) : StepRecordRepository {
    val syncedRequests = mutableListOf<List<DailyStepRecordWrite>>()
    val requestedStatisticsDates = mutableListOf<String>()

    override suspend fun syncDailyRecords(
        records: List<DailyStepRecordWrite>
    ): DailyStepRecordSyncResult {
        syncedRequests += records
        return DailyStepRecordSyncResult(records = emptyList(), syncTime = "")
    }

    override suspend fun getDailyRecord(recordDate: String): DailyStepRecord? = null
    override suspend fun getHistory(from: String, to: String): List<DailyStepRecord> = emptyList()

    override suspend fun getWeeklyStatistics(recordDate: String): WeeklyStepStatistics {
        requestedStatisticsDates += recordDate
        if (remainingNotFoundResponses > 0) {
            remainingNotFoundResponses--
            throw NotFoundException()
        }
        return WeeklyStepStatistics(
            recordDate = recordDate,
            todayStepCount = 0,
            recent7DayAverage = 3_000.0,
            recordedDayCount = 3,
            differenceFromAverage = -3_000.0,
            changeRatePercent = -100.0
        )
    }
}
