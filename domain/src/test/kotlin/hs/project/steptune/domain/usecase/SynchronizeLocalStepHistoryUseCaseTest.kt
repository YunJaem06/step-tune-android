package hs.project.steptune.domain.usecase

import hs.project.steptune.core.util.DateFormatter
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
import org.junit.Test

class SynchronizeLocalStepHistoryUseCaseTest {
    @Test
    fun `history sync restores larger server totals and uploads merged local records`() = runBlocking {
        val pedometerRepository = FakePedometerRepository(
            listOf(progress(date = "2026-09-02", steps = 1_000))
        )
        val stepRecordRepository = FakeStepRecordRepository(
            history = listOf(
                remoteRecord(date = "2026-09-01", steps = 8_000),
                remoteRecord(date = "2026-09-02", steps = 1_500)
            )
        )
        val syncUseCase = SyncDailyStepRecordsUseCase(stepRecordRepository)
        val useCase = SynchronizeLocalStepHistoryUseCase(
            pedometerRepository = pedometerRepository,
            settingsRepository = FakeSettingsRepository(),
            stepRecordRepository = stepRecordRepository,
            syncDailyStepRecordsUseCase = syncUseCase
        )

        useCase(from = "2026-09-01", to = "2026-09-03")

        assertEquals(8_000, pedometerRepository.records.getValue("2026-09-01").steps)
        assertEquals(1_500, pedometerRepository.records.getValue("2026-09-02").steps)
        assertEquals(
            DateFormatter.parseMeasuredAt("2026-09-02T14:00:00Z"),
            pedometerRepository.records.getValue("2026-09-02").measuredAtEpochMillis
        )
        assertEquals(
            listOf("2026-09-01" to 8_000, "2026-09-02" to 1_500),
            stepRecordRepository.lastSyncedRecords.map { it.recordDate to it.stepCount }
        )
    }

    @Test
    fun `history sync keeps a larger local total`() = runBlocking {
        val pedometerRepository = FakePedometerRepository(
            listOf(progress(date = "2026-09-03", steps = 5_000))
        )
        val stepRecordRepository = FakeStepRecordRepository(
            history = listOf(remoteRecord(date = "2026-09-03", steps = 4_000))
        )
        val useCase = SynchronizeLocalStepHistoryUseCase(
            pedometerRepository = pedometerRepository,
            settingsRepository = FakeSettingsRepository(),
            stepRecordRepository = stepRecordRepository,
            syncDailyStepRecordsUseCase = SyncDailyStepRecordsUseCase(stepRecordRepository)
        )

        useCase(from = "2026-09-01", to = "2026-09-03")

        assertEquals(5_000, pedometerRepository.records.getValue("2026-09-03").steps)
        assertEquals(5_000, stepRecordRepository.lastSyncedRecords.single().stepCount)
        assertEquals(
            LOCAL_MEASURED_AT_EPOCH_MILLIS,
            DateFormatter.parseMeasuredAt(stepRecordRepository.lastSyncedRecords.single().measuredAt)
        )
    }

    @Test
    fun `history sync keeps the newest measurement time when totals are equal`() = runBlocking {
        val pedometerRepository = FakePedometerRepository(
            listOf(progress(date = "2026-09-03", steps = 5_000))
        )
        val stepRecordRepository = FakeStepRecordRepository(
            history = listOf(remoteRecord(date = "2026-09-03", steps = 5_000))
        )
        val useCase = SynchronizeLocalStepHistoryUseCase(
            pedometerRepository = pedometerRepository,
            settingsRepository = FakeSettingsRepository(),
            stepRecordRepository = stepRecordRepository,
            syncDailyStepRecordsUseCase = SyncDailyStepRecordsUseCase(stepRecordRepository)
        )

        useCase(from = "2026-09-01", to = "2026-09-03")

        assertEquals(
            DateFormatter.parseMeasuredAt("2026-09-03T14:00:00Z"),
            pedometerRepository.records.getValue("2026-09-03").measuredAtEpochMillis
        )
    }

    private fun progress(date: String, steps: Int) = DailyProgress(
        date = date,
        steps = steps,
        goal = 10_000,
        distanceMeters = 0f,
        calories = 0f,
        measuredAtEpochMillis = LOCAL_MEASURED_AT_EPOCH_MILLIS
    )

    private fun remoteRecord(date: String, steps: Int) = DailyStepRecord(
        recordDate = date,
        stepCount = steps,
        measuredAt = "${date}T14:00:00Z",
        updatedAt = "${date}T14:00:01Z"
    )

    private companion object {
        const val LOCAL_MEASURED_AT_EPOCH_MILLIS = 1_788_405_600_000L
    }
}

private class FakePedometerRepository(
    initialRecords: List<DailyProgress>
) : PedometerRepository {
    val records = initialRecords.associateByTo(linkedMapOf(), DailyProgress::date)

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

private class FakeSettingsRepository : SettingsRepository {
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

private class FakeStepRecordRepository(
    private val history: List<DailyStepRecord>
) : StepRecordRepository {
    var lastSyncedRecords: List<DailyStepRecordWrite> = emptyList()

    override suspend fun syncDailyRecords(
        records: List<DailyStepRecordWrite>
    ): DailyStepRecordSyncResult {
        lastSyncedRecords = records
        return DailyStepRecordSyncResult(records = emptyList(), syncTime = "")
    }

    override suspend fun getDailyRecord(recordDate: String): DailyStepRecord? =
        history.firstOrNull { it.recordDate == recordDate }

    override suspend fun getHistory(from: String, to: String): List<DailyStepRecord> =
        history.filter { it.recordDate in from..to }

    override suspend fun getWeeklyStatistics(recordDate: String): WeeklyStepStatistics =
        error("not used")
}
