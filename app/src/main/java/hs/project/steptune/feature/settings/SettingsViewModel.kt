package hs.project.steptune.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.MusicPreferenceRules
import hs.project.steptune.domain.usecase.ObserveUserPreferencesUseCase
import hs.project.steptune.domain.usecase.SetAutoStartTrackingEnabledUseCase
import hs.project.steptune.domain.usecase.SetReminderNotificationsEnabledUseCase
import hs.project.steptune.domain.usecase.UpdateMusicPreferencesUseCase
import hs.project.steptune.domain.usecase.UpdateProfileSettingsUseCase
import hs.project.steptune.feature.musicpreference.MusicPreferenceSelectionUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val updateProfileSettingsUseCase: UpdateProfileSettingsUseCase,
    private val setReminderNotificationsEnabledUseCase: SetReminderNotificationsEnabledUseCase,
    private val setAutoStartTrackingEnabledUseCase: SetAutoStartTrackingEnabledUseCase,
    private val updateMusicPreferencesUseCase: UpdateMusicPreferencesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    fun onDailyGoalChanged(value: String) {
        updateNumericField(value) { current, sanitized ->
            current.copy(dailyGoalInput = sanitized, hasInvalidNumberError = false, profileSaved = false)
        }
    }

    fun onStepLengthChanged(value: String) {
        updateNumericField(value) { current, sanitized ->
            current.copy(stepLengthInput = sanitized, hasInvalidNumberError = false, profileSaved = false)
        }
    }

    fun onHeightChanged(value: String) {
        updateNumericField(value) { current, sanitized ->
            current.copy(heightInput = sanitized, hasInvalidNumberError = false, profileSaved = false)
        }
    }

    fun onWeightChanged(value: String) {
        updateNumericField(value) { current, sanitized ->
            current.copy(weightInput = sanitized, hasInvalidNumberError = false, profileSaved = false)
        }
    }

    fun onReminderNotificationsChanged(enabled: Boolean) {
        _uiState.update {
            it.copy(reminderNotificationsEnabled = enabled)
        }
        viewModelScope.launch {
            setReminderNotificationsEnabledUseCase(enabled)
        }
    }

    fun onAutoStartTrackingChanged(enabled: Boolean) {
        _uiState.update {
            it.copy(autoStartTrackingEnabled = enabled)
        }
        viewModelScope.launch {
            setAutoStartTrackingEnabledUseCase(enabled)
        }
    }

    fun onGenreToggled(genre: MusicGenre) {
        if (_uiState.value.isSavingMusicPreferences) return
        _uiState.update { current ->
            current.copy(
                musicPreferences = current.musicPreferences.copy(
                    selectedGenres = MusicPreferenceRules.toggleGenre(
                        selectedGenres = current.musicPreferences.selectedGenres,
                        genre = genre
                    )
                ),
                musicPreferencesSaved = false,
                musicPreferencesSaveFailed = false
            )
        }
    }

    fun onMoodToggled(mood: MusicMood) {
        if (_uiState.value.isSavingMusicPreferences) return
        _uiState.update { current ->
            current.copy(
                musicPreferences = current.musicPreferences.copy(
                    selectedMoods = MusicPreferenceRules.toggleMood(
                        selectedMoods = current.musicPreferences.selectedMoods,
                        mood = mood
                    )
                ),
                musicPreferencesSaved = false,
                musicPreferencesSaveFailed = false
            )
        }
    }

    fun saveMusicPreferences() {
        val selection = _uiState.value.musicPreferences
        if (_uiState.value.isSavingMusicPreferences) return
        _uiState.update {
            it.copy(
                isSavingMusicPreferences = true,
                musicPreferencesSaved = false,
                musicPreferencesSaveFailed = false
            )
        }
        viewModelScope.launch {
            runCatching {
                updateMusicPreferencesUseCase(
                    preferredGenres = selection.selectedGenres,
                    preferredMoods = selection.selectedMoods
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSavingMusicPreferences = false,
                        musicPreferencesSaved = true
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isSavingMusicPreferences = false,
                        musicPreferencesSaveFailed = true
                    )
                }
            }
        }
    }

    fun saveProfile() {
        val current = uiState.value
        val dailyGoal = current.dailyGoalInput.toIntOrNull()
        val stepLength = current.stepLengthInput.toIntOrNull()
        val height = current.heightInput.toIntOrNull()
        val weight = current.weightInput.toIntOrNull()

        if (dailyGoal == null || stepLength == null || height == null || weight == null) {
            _uiState.update {
                it.copy(hasInvalidNumberError = true, profileSaved = false)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true, hasInvalidNumberError = false, profileSaved = false) }
            updateProfileSettingsUseCase(
                dailyGoal = dailyGoal,
                stepLengthCm = stepLength,
                heightCm = height,
                weightKg = weight
            )
            _uiState.update { it.copy(isSavingProfile = false, profileSaved = true) }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            val preferences = observeUserPreferencesUseCase().first()
            _uiState.update {
                it.copy(
                    dailyGoalInput = preferences.dailyGoal.toString(),
                    stepLengthInput = preferences.stepLengthCm.toString(),
                    heightInput = preferences.heightCm.toString(),
                    weightInput = preferences.weightKg.toString(),
                    musicPreferences = MusicPreferenceSelectionUiState(
                        selectedGenres = preferences.preferredGenres,
                        selectedMoods = preferences.preferredMoods
                    ),
                    reminderNotificationsEnabled = preferences.reminderNotificationsEnabled,
                    autoStartTrackingEnabled = preferences.autoStartTrackingEnabled,
                    isLoading = false,
                    hasInvalidNumberError = false
                )
            }
        }
    }

    private fun updateNumericField(
        value: String,
        reducer: (SettingsUiState, String) -> SettingsUiState
    ) {
        val sanitized = value.filter(Char::isDigit)
        _uiState.update { current -> reducer(current, sanitized) }
    }
}



