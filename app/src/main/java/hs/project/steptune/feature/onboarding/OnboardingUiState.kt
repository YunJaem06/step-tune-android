package hs.project.steptune.feature.onboarding

import androidx.compose.runtime.Immutable
import hs.project.steptune.feature.musicpreference.MusicPreferenceSelectionUiState

enum class OnboardingStep {
    PERMISSIONS,
    MUSIC_PREFERENCES
}

@Immutable
data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.PERMISSIONS,
    val activityRecognitionGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val musicPreferences: MusicPreferenceSelectionUiState = MusicPreferenceSelectionUiState(),
    val musicPreferencesOnboardingCompleted: Boolean = false,
    val autoStartTrackingEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val saving: Boolean = false,
    val saveFailed: Boolean = false
) {
    val allRequiredGranted: Boolean
        get() = activityRecognitionGranted && notificationGranted
}
