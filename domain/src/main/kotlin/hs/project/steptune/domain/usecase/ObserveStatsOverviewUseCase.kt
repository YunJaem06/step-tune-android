package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.model.StatsOverview
import hs.project.steptune.domain.model.StatsPeriod
import hs.project.steptune.domain.repository.PedometerRepository
import hs.project.steptune.domain.util.StatsDateUtils
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveStatsOverviewUseCase @Inject constructor(
    private val repository: PedometerRepository
) {
    operator fun invoke(period: StatsPeriod): Flow<StatsOverview> {
        val (startDate, endDate) = StatsDateUtils.queryRange(period)
        return repository.observeDailyProgressRange(startDate, endDate)
            .map { records ->
                StatsDateUtils.buildOverview(period, records)
            }
    }
}

