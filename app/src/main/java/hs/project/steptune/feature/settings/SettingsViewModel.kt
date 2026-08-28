package hs.project.steptune.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.api.ConflictException
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.MusicPreferenceRules
import hs.project.steptune.domain.usecase.ObserveUserPreferencesUseCase
import hs.project.steptune.domain.usecase.CheckNicknameAvailabilityUseCase
import hs.project.steptune.domain.usecase.DeleteAccountUseCase
import hs.project.steptune.domain.usecase.LogoutUseCase
import hs.project.steptune.domain.usecase.ObserveAuthSessionUseCase
import hs.project.steptune.domain.usecase.SetAutoStartTrackingEnabledUseCase
import hs.project.steptune.domain.usecase.SetReminderNotificationsEnabledUseCase
import hs.project.steptune.domain.usecase.UpdateMusicPreferencesUseCase
import hs.project.steptune.domain.usecase.UpdateProfileSettingsUseCase
import hs.project.steptune.domain.usecase.UpdateNicknameUseCase
import hs.project.steptune.domain.usecase.SyncCurrentUserUseCase
import hs.project.steptune.feature.musicpreference.MusicPreferenceSelectionUiState
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val updateProfileSettingsUseCase: UpdateProfileSettingsUseCase,
    private val setReminderNotificationsEnabledUseCase: SetReminderNotificationsEnabledUseCase,
    private val setAutoStartTrackingEnabledUseCase: SetAutoStartTrackingEnabledUseCase,
    private val updateMusicPreferencesUseCase: UpdateMusicPreferencesUseCase,
    private val observeAuthSessionUseCase: ObserveAuthSessionUseCase,
    private val syncCurrentUserUseCase: SyncCurrentUserUseCase,
    private val checkNicknameAvailabilityUseCase: CheckNicknameAvailabilityUseCase,
    private val updateNicknameUseCase: UpdateNicknameUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        loadPreferences()
        observeAuthSession()
        syncCurrentUser()
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

    fun onNicknameChanged(value: String) {
        val normalized = value.trim()
        _uiState.update {
            it.copy(
                nicknameInput = value,
                nicknameValidationState = if (
                    normalized.length !in 1..MAX_NICKNAME_LENGTH
                ) {
                    NicknameValidationState.INVALID
                } else {
                    NicknameValidationState.IDLE
                },
                nicknameUpdated = false
            )
        }
    }

    fun checkNicknameAvailability() {
        val requestedNickname = _uiState.value.normalizedNicknameInput
        if (_uiState.value.isCheckingNickname || _uiState.value.isUpdatingNickname) return
        if (!_uiState.value.isNicknameInputValid) {
            _uiState.update {
                it.copy(nicknameValidationState = NicknameValidationState.INVALID)
            }
            return
        }

        _uiState.update {
            it.copy(
                isCheckingNickname = true,
                nicknameValidationState = NicknameValidationState.IDLE,
                nicknameUpdated = false
            )
        }
        viewModelScope.launch {
            try {
                val result = checkNicknameAvailabilityUseCase(requestedNickname)
                _uiState.update {
                    it.copy(
                        nicknameInput = result.nickName,
                        isCheckingNickname = false,
                        nicknameValidationState = if (result.isAvailable) {
                            NicknameValidationState.AVAILABLE
                        } else {
                            NicknameValidationState.UNAVAILABLE
                        }
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isCheckingNickname = false,
                        nicknameValidationState = NicknameValidationState.ERROR
                    )
                }
            }
        }
    }

    fun updateNickname() {
        val requestedNickname = _uiState.value.normalizedNicknameInput
        if (!_uiState.value.canUpdateNickname) return
        _uiState.update {
            it.copy(isUpdatingNickname = true, nicknameUpdated = false)
        }
        viewModelScope.launch {
            try {
                updateNicknameUseCase(requestedNickname)
                _uiState.update {
                    it.copy(
                        isUpdatingNickname = false,
                        nicknameValidationState = NicknameValidationState.IDLE,
                        nicknameUpdated = true
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: ConflictException) {
                _uiState.update {
                    it.copy(
                        isUpdatingNickname = false,
                        nicknameValidationState = NicknameValidationState.UNAVAILABLE
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdatingNickname = false,
                        nicknameValidationState = NicknameValidationState.ERROR
                    )
                }
            }
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

    fun logout() {
        if (_uiState.value.isLoggingOut) return
        _uiState.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch {
            logoutUseCase()
            _uiState.update { it.copy(isLoggingOut = false) }
            _events.emit(SettingsEvent.LoggedOut)
        }
    }

    fun showDeleteAccountDialog() {
        if (_uiState.value.isDeletingAccount) return
        _uiState.update {
            it.copy(showDeleteAccountDialog = true, deleteAccountFailed = false)
        }
    }

    fun hideDeleteAccountDialog() {
        if (_uiState.value.isDeletingAccount) return
        _uiState.update {
            it.copy(showDeleteAccountDialog = false, deleteAccountFailed = false)
        }
    }

    fun deleteAccount() {
        if (_uiState.value.isDeletingAccount) return
        _uiState.update {
            it.copy(isDeletingAccount = true, deleteAccountFailed = false)
        }
        viewModelScope.launch {
            try {
                deleteAccountUseCase()
                _uiState.update {
                    it.copy(isDeletingAccount = false, showDeleteAccountDialog = false)
                }
                _events.emit(SettingsEvent.LoggedOut)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isDeletingAccount = false, deleteAccountFailed = true)
                }
            }
        }
    }

    private fun observeAuthSession() {
        viewModelScope.launch {
            observeAuthSessionUseCase().collect { session ->
                _uiState.update { current ->
                    val shouldUpdateInput = current.nicknameInput.isBlank() ||
                        current.nicknameInput == current.nickName
                    current.copy(
                        nickName = session.nickName,
                        nicknameInput = if (shouldUpdateInput) {
                            session.nickName
                        } else {
                            current.nicknameInput
                        }
                    )
                }
            }
        }
    }

    private fun syncCurrentUser() {
        viewModelScope.launch {
            try {
                syncCurrentUserUseCase()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // 설정 화면은 로컬 캐시된 사용자 정보를 계속 표시한다.
            }
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

    private companion object {
        const val MAX_NICKNAME_LENGTH = 30
    }
}

sealed interface SettingsEvent {
    data object LoggedOut : SettingsEvent
}



