package hs.project.steptune.domain.model

data class UserPreferences(
    val dailyGoal: Int = 10_000,
    val stepLengthCm: Int = 72,
    val heightCm: Int = 170,
    val weightKg: Int = 65,
    val preferredGenres: Set<MusicGenre> = emptySet(),
    val preferredMoods: Set<MusicMood> = emptySet(),
    val reminderNotificationsEnabled: Boolean = true,
    val autoStartTrackingEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val musicPreferencesOnboardingCompleted: Boolean = false
)

