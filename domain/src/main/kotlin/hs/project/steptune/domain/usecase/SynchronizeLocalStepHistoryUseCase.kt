package hs.project.steptune.domain.usecase

import hs.project.steptune.core.util.DateFormatter
import hs.project.steptune.domain.model.DailyProgress
import hs.project.steptune.domain.model.StepMetricsCalculator
import hs.project.steptune.domain.repository.PedometerRepository
import hs.project.steptune.domain.repository.SettingsRepository
import hs.project.steptune.domain.repository.StepRecordRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SynchronizeLocalStepHistoryUseCase @Inject constructor(
    private val pedometerRepository: PedometerRepository,
    private val settingsRepository: SettingsRepository,
    private val stepRecordRepository: StepRecordRepository,
    private val syncDailyStepRecordsUseCase: SyncDailyStepRecordsUseCase
) {
    suspend operator fun invoke(
        from: String = DateFormatter.daysAgo(HISTORY_DAYS - 1),
        to: String = DateFormatter.today()
    ) {
        val localRecords = pedometerRepository.observeDailyProgressRange(from, to).first()
        val mergedRecords = localRecords.associateByTo(linkedMapOf(), DailyProgress::date)
        val preferences = settingsRepository.observePreferences().first()

        stepRecordRepository.getHistory(from = from, to = to).forEach { remoteRecord ->
            val localRecord = mergedRecords[remoteRecord.recordDate]
            val remoteMeasuredAtEpochMillis = DateFormatter.parseMeasuredAt(remoteRecord.measuredAt)
                ?: DateFormatter.endOfDayEpochMillis(remoteRecord.recordDate)
                ?: 0L
            val shouldRestoreRemoteRecord = localRecord == null ||
                remoteRecord.stepCount > localRecord.steps ||
                (
                    remoteRecord.stepCount == localRecord.steps &&
                        remoteMeasuredAtEpochMillis > localRecord.measuredAtEpochMillis
                )
            if (shouldRestoreRemoteRecord) {
                val steps = remoteRecord.stepCount.coerceAtLeast(0)
                val distanceMeters = StepMetricsCalculator.distanceMeters(
                    steps = steps,
                    stepLengthCm = preferences.stepLengthCm
                )
                val restoredRecord = DailyProgress(
                    date = remoteRecord.recordDate,
                    steps = steps,
                    goal = localRecord?.goal ?: preferences.dailyGoal,
                    distanceMeters = distanceMeters,
                    calories = StepMetricsCalculator.calories(
                        distanceMeters = distanceMeters,
                        weightKg = preferences.weightKg
                    ),
                    measuredAtEpochMillis = remoteMeasuredAtEpochMillis
                )
                pedometerRepository.upsertDailyProgress(restoredRecord)
                mergedRecords[restoredRecord.date] = restoredRecord
            }
        }

        syncDailyStepRecordsUseCase(mergedRecords.values.sortedBy(DailyProgress::date))
    }

    private companion object {
        const val HISTORY_DAYS = 366
    }
}
