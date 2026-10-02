package hs.project.steptune.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.core.util.DateFormatter
import hs.project.steptune.domain.usecase.GetTodayProgressUseCase
import hs.project.steptune.domain.model.StatsPeriod
import hs.project.steptune.domain.usecase.ObserveStatsOverviewUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodayProgressUseCase: GetTodayProgressUseCase,
    private val observeStatsOverviewUseCase: ObserveStatsOverviewUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeTodayProgress()
    }

    private fun observeTodayProgress() {
        viewModelScope.launch {
            currentDateFlow()
                .flatMapLatest { date ->
                    combine(
                        getTodayProgressUseCase(date),
                        observeStatsOverviewUseCase(StatsPeriod.DAILY)
                    ) { progress, overview ->
                        progress to overview.records
                    }
                }
                .collect { (progress, weeklyRecords) ->
                    _uiState.update {
                        it.copy(
                            date = progress.date,
                            steps = progress.steps,
                            goal = progress.goal,
                            distanceMeters = progress.distanceMeters,
                            calories = progress.calories,
                            weeklyRecords = weeklyRecords,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun currentDateFlow() = flow {
        while (currentCoroutineContext().isActive) {
            emit(DateFormatter.today())
            delay(DateFormatter.millisUntilNextDay())
        }
    }.distinctUntilChanged()
}


