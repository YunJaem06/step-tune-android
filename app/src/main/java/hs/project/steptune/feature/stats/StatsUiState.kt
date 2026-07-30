package hs.project.steptune.feature.stats

import hs.project.steptune.domain.model.StatsOverview
import hs.project.steptune.domain.model.StatsPeriod

data class StatsUiState(
    val selectedPeriod: StatsPeriod = StatsPeriod.DAILY,
    val overview: StatsOverview = StatsOverview(),
    val isLoading: Boolean = true
)



