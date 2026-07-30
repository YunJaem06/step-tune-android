package hs.project.steptune.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.core.util.DateFormatter
import hs.project.steptune.domain.usecase.GetTodayProgressUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
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
    private val getTodayProgressUseCase: GetTodayProgressUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeTodayProgress()
    }

    private fun observeTodayProgress() {
        viewModelScope.launch {
            currentDateFlow()
                .flatMapLatest(getTodayProgressUseCase::invoke)
                .collect { progress ->
                    _uiState.update {
                        it.copy(
                            date = progress.date,
                            steps = progress.steps,
                            goal = progress.goal,
                            distanceMeters = progress.distanceMeters,
                            calories = progress.calories,
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


