package hs.project.steptune.feature.stats

import androidx.compose.runtime.Immutable
import hs.project.steptune.domain.model.StatsOverview
import hs.project.steptune.domain.model.StatsPeriod
import hs.project.steptune.domain.model.WeeklyStepStatistics

@Immutable
data class StatsUiState(
    val selectedPeriod: StatsPeriod = StatsPeriod.DAILY,
    val overview: StatsOverview = StatsOverview(),
    val isLoading: Boolean = true,
    val weeklyStatistics: WeeklyStepStatistics? = null,
    val isWeeklyStatisticsLoading: Boolean = true,
    val weeklyStatisticsLoadFailed: Boolean = false
)



