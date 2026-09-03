package hs.project.steptune.domain.usecase

import hs.project.steptune.core.util.DateFormatter
import hs.project.steptune.domain.model.DailyProgress
import hs.project.steptune.domain.model.DailyStepRecordWrite
import hs.project.steptune.domain.repository.StepRecordRepository
import javax.inject.Inject

class SyncDailyStepRecordsUseCase @Inject constructor(
    private val stepRecordRepository: StepRecordRepository
) {
    suspend operator fun invoke(records: List<DailyProgress>) {
        if (records.isEmpty()) return
        require(records.size <= MAX_RECORDS_PER_REQUEST) {
            "A step sync request can contain at most $MAX_RECORDS_PER_REQUEST records"
        }
        stepRecordRepository.syncDailyRecords(
            records.map { progress ->
                val measuredAtEpochMillis = progress.measuredAtEpochMillis
                    .takeIf { it > 0L }
                    ?: DateFormatter.endOfDayEpochMillis(progress.date)
                    ?: error("Invalid daily step record date: ${progress.date}")
                DailyStepRecordWrite(
                    recordDate = progress.date,
                    stepCount = progress.steps.coerceAtLeast(0),
                    measuredAt = DateFormatter.measuredAt(measuredAtEpochMillis)
                )
            }
        )
    }

    private companion object {
        const val MAX_RECORDS_PER_REQUEST = 366
    }
}
