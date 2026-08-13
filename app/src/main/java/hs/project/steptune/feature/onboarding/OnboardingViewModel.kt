package hs.project.steptune.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.MusicPreferenceRules
import hs.project.steptune.domain.usecase.CompleteOnboardingUseCase
import hs.project.steptune.domain.usecase.ObserveUserPreferencesUseCase
import hs.project.steptune.feature.musicpreference.MusicPreferenceSelectionUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadMusicPreferences()
    }

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

    fun showMusicPreferences() {
        if (!_uiState.value.allRequiredGranted) return
        _uiState.update { it.copy(step = OnboardingStep.MUSIC_PREFERENCES, saveFailed = false) }
    }

    fun continueFromPermissions(onFinished: () -> Unit) {
        if (!_uiState.value.allRequiredGranted) return
        if (_uiState.value.musicPreferencesOnboardingCompleted) {
            completeOnboarding(onFinished)
        } else {
            showMusicPreferences()
        }
    }

    fun showPermissions() {
        _uiState.update { it.copy(step = OnboardingStep.PERMISSIONS, saveFailed = false) }
    }

    fun toggleGenre(genre: MusicGenre) {
        if (_uiState.value.isLoading || _uiState.value.saving) return
        _uiState.update { current ->
            current.copy(
                musicPreferences = current.musicPreferences.copy(
                    selectedGenres = MusicPreferenceRules.toggleGenre(
                        selectedGenres = current.musicPreferences.selectedGenres,
                        genre = genre
                    )
                ),
                saveFailed = false
            )
        }
    }

    fun toggleMood(mood: MusicMood) {
        if (_uiState.value.isLoading || _uiState.value.saving) return
        _uiState.update { current ->
            current.copy(
                musicPreferences = current.musicPreferences.copy(
                    selectedMoods = MusicPreferenceRules.toggleMood(
                        selectedMoods = current.musicPreferences.selectedMoods,
                        mood = mood
                    )
                ),
                saveFailed = false
            )
        }
    }

    fun completeOnboarding(onFinished: () -> Unit) {
        val selection = _uiState.value.musicPreferences
        saveAndFinish(
            preferredGenres = selection.selectedGenres,
            preferredMoods = selection.selectedMoods,
            onFinished = onFinished
        )
    }

    fun skipMusicPreferences(onFinished: () -> Unit) {
        saveAndFinish(
            preferredGenres = emptySet(),
            preferredMoods = emptySet(),
            onFinished = onFinished
        )
    }

    private fun loadMusicPreferences() {
        viewModelScope.launch {
            val preferences = observeUserPreferencesUseCase().first()
            _uiState.update { current ->
                current.copy(
                    musicPreferences = MusicPreferenceSelectionUiState(
                        selectedGenres = preferences.preferredGenres,
                        selectedMoods = preferences.preferredMoods
                    ),
                    musicPreferencesOnboardingCompleted =
                        preferences.musicPreferencesOnboardingCompleted,
                    autoStartTrackingEnabled = preferences.autoStartTrackingEnabled,
                    isLoading = false
                )
            }
        }
    }

    private fun saveAndFinish(
        preferredGenres: Set<MusicGenre>,
        preferredMoods: Set<MusicMood>,
        onFinished: () -> Unit
    ) {
        if (_uiState.value.isLoading || _uiState.value.saving) return
        _uiState.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching {
                completeOnboardingUseCase(preferredGenres, preferredMoods)
            }.onSuccess {
                _uiState.update { it.copy(saving = false) }
                onFinished()
            }.onFailure {
                _uiState.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }
}
