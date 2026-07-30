package hs.project.steptune.data.repository

import hs.project.steptune.data.local.database.DayRecordDao
import hs.project.steptune.data.local.database.DayRecordEntity
import hs.project.steptune.domain.model.DailyProgress
import hs.project.steptune.domain.repository.PedometerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PedometerRepositoryImpl @Inject constructor(
    private val dayRecordDao: DayRecordDao
) : PedometerRepository {

    override fun observeDailyProgress(date: String): Flow<DailyProgress> {
        return dayRecordDao.observeDayRecord(date).map { entity ->
            entity?.toDailyProgress() ?: DailyProgress(
                date = date,
                steps = 0,
                goal = 10_000,
                distanceMeters = 0f,
                calories = 0f
            )
        }
    }

    override fun observeDailyProgressRange(startDate: String, endDate: String): Flow<List<DailyProgress>> {
        return dayRecordDao.observeDayRecordsBetween(startDate, endDate).map { entities ->
            entities.map { entity -> entity.toDailyProgress() }
        }
    }

    override suspend fun getDailyProgress(date: String): DailyProgress? {
        return dayRecordDao.getDayRecord(date)?.toDailyProgress()
    }

    override suspend fun upsertDailyProgress(progress: DailyProgress) {
        dayRecordDao.upsert(
            DayRecordEntity(
                date = progress.date,
                steps = progress.steps,
                goal = progress.goal,
                distanceMeters = progress.distanceMeters,
                calories = progress.calories
            )
        )
    }

    private fun DayRecordEntity.toDailyProgress(): DailyProgress {
        return DailyProgress(
            date = date,
            steps = steps,
            goal = goal,
            distanceMeters = distanceMeters,
            calories = calories
        )
    }
}
