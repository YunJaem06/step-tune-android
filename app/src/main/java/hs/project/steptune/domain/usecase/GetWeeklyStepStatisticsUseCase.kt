package hs.project.steptune.domain.usecase

import hs.project.steptune.api.NotFoundException
import hs.project.steptune.core.util.DateFormatter
import hs.project.steptune.domain.model.DailyProgress
import hs.project.steptune.domain.model.WeeklyStepStatistics
import hs.project.steptune.domain.repository.PedometerRepository
import hs.project.steptune.domain.repository.SettingsRepository
import hs.project.steptune.domain.repository.StepRecordRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class GetWeeklyStepStatisticsUseCase @Inject constructor(
    private val pedometerRepository: PedometerRepository,
    private val settingsRepository: SettingsRepository,
    private val stepRecordRepository: StepRecordRepository,
    private val syncDailyStepRecordsUseCase: SyncDailyStepRecordsUseCase
) {
    suspend operator fun invoke(
        recordDate: String = DateFormatter.today()
    ): WeeklyStepStatistics {
        val todayProgress = getOrCreateTodayProgress(recordDate)
        syncDailyStepRecordsUseCase(listOf(todayProgress))

        return try {
            stepRecordRepository.getWeeklyStatistics(recordDate)
        } catch (_: NotFoundException) {
            syncDailyStepRecordsUseCase(listOf(todayProgress))
            stepRecordRepository.getWeeklyStatistics(recordDate)
        }
    }

    private suspend fun getOrCreateTodayProgress(recordDate: String): DailyProgress {
        pedometerRepository.getDailyProgress(recordDate)?.let { return it }

        val preferences = settingsRepository.observePreferences().first()
        return DailyProgress(
            date = recordDate,
            steps = 0,
            goal = preferences.dailyGoal,
            distanceMeters = 0f,
            calories = 0f,
            measuredAtEpochMillis = System.currentTimeMillis()
        ).also { progress ->
            pedometerRepository.upsertDailyProgress(progress)
        }
    }
}
