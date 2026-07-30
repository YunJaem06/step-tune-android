package hs.project.steptune.feature.settings

data class SettingsUiState(
    val dailyGoalInput: String = "",
    val stepLengthInput: String = "",
    val heightInput: String = "",
    val weightInput: String = "",
    val reminderNotificationsEnabled: Boolean = true,
    val autoStartTrackingEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val isSavingProfile: Boolean = false,
    val profileSaved: Boolean = false,
    val hasInvalidNumberError: Boolean = false
) {
    val canSaveProfile: Boolean
        get() = listOf(dailyGoalInput, stepLengthInput, heightInput, weightInput)
            .all { input -> input.toIntOrNull()?.let { value -> value > 0 } == true } && !isSavingProfile
}



