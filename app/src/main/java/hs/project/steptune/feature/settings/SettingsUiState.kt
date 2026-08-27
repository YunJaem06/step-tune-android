package hs.project.steptune.feature.settings

import androidx.compose.runtime.Immutable
import hs.project.steptune.feature.musicpreference.MusicPreferenceSelectionUiState

@Immutable
data class SettingsUiState(
    val nickName: String = "",
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
    val isLoggingOut: Boolean = false
) {
    val canSaveProfile: Boolean
        get() = listOf(dailyGoalInput, stepLengthInput, heightInput, weightInput)
            .all { input -> input.toIntOrNull()?.let { value -> value > 0 } == true } &&
            !isSavingProfile

    val canSaveMusicPreferences: Boolean
        get() = !isSavingMusicPreferences
}
