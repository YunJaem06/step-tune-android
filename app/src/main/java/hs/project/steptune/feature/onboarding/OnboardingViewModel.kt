package hs.project.steptune.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.domain.usecase.SetOnboardingCompletedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val setOnboardingCompletedUseCase: SetOnboardingCompletedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updatePermissionState(
        activityRecognitionGranted: Boolean,
        notificationGranted: Boolean
    ) {
        _uiState.update {
            it.copy(
                activityRecognitionGranted = activityRecognitionGranted,
                notificationGranted = notificationGranted
            )
        }
    }

    fun completeOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            setOnboardingCompletedUseCase(true)
            _uiState.update { it.copy(saving = false) }
            onFinished()
        }
    }
}

