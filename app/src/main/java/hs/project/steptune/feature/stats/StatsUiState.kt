package hs.project.steptune.feature.stats

import androidx.compose.runtime.Immutable
import hs.project.steptune.domain.model.StatsOverview
import hs.project.steptune.domain.model.StatsPeriod

@Immutable
data class StatsUiState(
    val selectedPeriod: StatsPeriod = StatsPeriod.DAILY,
    val overview: StatsOverview = StatsOverview(),
    val isLoading: Boolean = true
)



