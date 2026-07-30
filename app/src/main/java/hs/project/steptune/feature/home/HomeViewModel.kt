package hs.project.steptune.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.core.util.DateFormatter
import hs.project.steptune.domain.usecase.GetTodayProgressUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        val today = DateFormatter.today()
        viewModelScope.launch {
            getTodayProgressUseCase(today).collect { progress ->
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
}


