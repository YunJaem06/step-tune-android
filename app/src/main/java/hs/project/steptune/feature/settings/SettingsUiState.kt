package hs.project.steptune.feature.settings

import androidx.compose.runtime.Immutable
import hs.project.steptune.feature.musicpreference.MusicPreferenceSelectionUiState

@Immutable
data class SettingsUiState(
    val nickName: String = "",
    val nicknameInput: String = "",
    val nicknameValidationState: NicknameValidationState = NicknameValidationState.IDLE,
    val isCheckingNickname: Boolean = false,
    val isUpdatingNickname: Boolean = false,
    val nicknameUpdated: Boolean = false,
    val dailyGoalInput: String = "",
    val stepLengthInput: String = "",
    val heightInput: String = "",
    val weightInput: String = "",
    val musicPreferences: MusicPreferenceSelectionUiState = MusicPreferenceSelectionUiState(),
    val reminderNotificationsEnabled: Boolean = true,
    val autoStartTrackingEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val isSavingProfile: Boolean = false,
    val profileSaved: Boolean = false,
    val hasInvalidNumberError: Boolean = false,
    val isSavingMusicPreferences: Boolean = false,
    val musicPreferencesSaved: Boolean = false,
    val musicPreferencesSaveFailed: Boolean = false,
    val isLoggingOut: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val deleteAccountFailed: Boolean = false
) {
    val normalizedNicknameInput: String
        get() = nicknameInput.trim()

    val isNicknameInputValid: Boolean
        get() = normalizedNicknameInput.length in 1..MAX_NICKNAME_LENGTH

    val canCheckNickname: Boolean
        get() = isNicknameInputValid &&
            normalizedNicknameInput != nickName &&
            !isCheckingNickname &&
            !isUpdatingNickname

    val canUpdateNickname: Boolean
        get() = nicknameValidationState == NicknameValidationState.AVAILABLE &&
            normalizedNicknameInput != nickName &&
            !isCheckingNickname &&
            !isUpdatingNickname

    val canSaveProfile: Boolean
        get() = listOf(dailyGoalInput, stepLengthInput, heightInput, weightInput)
            .all { input -> input.toIntOrNull()?.let { value -> value > 0 } == true } &&
            !isSavingProfile

    val canSaveMusicPreferences: Boolean
        get() = !isSavingMusicPreferences

    private companion object {
        const val MAX_NICKNAME_LENGTH = 30
    }
}

enum class NicknameValidationState {
    IDLE,
    INVALID,
    AVAILABLE,
    UNAVAILABLE,
    ERROR
}
