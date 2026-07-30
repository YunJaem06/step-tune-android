package hs.project.steptune.feature.onboarding

data class OnboardingUiState(
    val activityRecognitionGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val saving: Boolean = false
) {
    val allRequiredGranted: Boolean
        get() = activityRecognitionGranted && notificationGranted
}

