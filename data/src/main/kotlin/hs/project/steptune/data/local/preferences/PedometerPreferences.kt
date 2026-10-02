package hs.project.steptune.data.local.preferences

data class PedometerPreferences(
    val dailyGoal: Int = 10_000,
    val stepLengthCm: Int = 72,
    val heightCm: Int = 170,
    val weightKg: Int = 65,
    val preferredGenreKeys: Set<String> = emptySet(),
    val preferredMoodKeys: Set<String> = emptySet(),
    val reminderNotificationsEnabled: Boolean = true,
    val autoStartTrackingEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val musicPreferencesOnboardingCompleted: Boolean = false
)

