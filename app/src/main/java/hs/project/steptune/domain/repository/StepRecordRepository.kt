package hs.project.steptune.domain.repository

import hs.project.steptune.domain.model.DailyStepRecord
import hs.project.steptune.domain.model.DailyStepRecordSyncResult
import hs.project.steptune.domain.model.DailyStepRecordWrite
import hs.project.steptune.domain.model.WeeklyStepStatistics

interface StepRecordRepository {
    suspend fun syncDailyRecords(records: List<DailyStepRecordWrite>): DailyStepRecordSyncResult
    suspend fun getDailyRecord(recordDate: String): DailyStepRecord?
    suspend fun getHistory(from: String, to: String): List<DailyStepRecord>
    suspend fun getWeeklyStatistics(recordDate: String): WeeklyStepStatistics
}
