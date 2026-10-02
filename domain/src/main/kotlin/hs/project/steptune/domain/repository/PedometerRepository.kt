package hs.project.steptune.domain.repository

import hs.project.steptune.domain.model.DailyProgress
import kotlinx.coroutines.flow.Flow

interface PedometerRepository {
    fun observeDailyProgress(date: String): Flow<DailyProgress>
    fun observeDailyProgressRange(startDate: String, endDate: String): Flow<List<DailyProgress>>
    suspend fun getDailyProgress(date: String): DailyProgress?
    suspend fun upsertDailyProgress(progress: DailyProgress)
}
