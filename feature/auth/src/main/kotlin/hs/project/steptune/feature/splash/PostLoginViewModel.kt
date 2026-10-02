package hs.project.steptune.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.domain.model.UserPreferences
import hs.project.steptune.domain.usecase.ObserveUserPreferencesUseCase
import hs.project.steptune.domain.usecase.SynchronizeLocalStepHistoryUseCase
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PostLoginViewModel @Inject constructor(
    observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val synchronizeLocalStepHistoryUseCase: SynchronizeLocalStepHistoryUseCase
) : ViewModel() {
    val preferences: Flow<UserPreferences> = observeUserPreferencesUseCase()

    private val _stepHistoryPrepared = MutableStateFlow(false)
    val stepHistoryPrepared: StateFlow<Boolean> = _stepHistoryPrepared.asStateFlow()

    init {
        synchronizeStepHistory()
    }

    private fun synchronizeStepHistory() {
        viewModelScope.launch {
            try {
                synchronizeLocalStepHistoryUseCase()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // 동기화 실패는 로컬 만보기 사용을 막지 않고 다음 실행이나 측정 주기에 재시도한다.
            } finally {
                _stepHistoryPrepared.value = true
            }
        }
    }
}
