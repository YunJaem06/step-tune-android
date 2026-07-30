package hs.project.steptune.domain.model

data class DailyProgress(
    val date: String,
    val steps: Int,
    val goal: Int,
    val distanceMeters: Float,
    val calories: Float
) {
    val progressRatio: Float
        get() = if (goal == 0) 0f else (steps.toFloat() / goal).coerceIn(0f, 1f)
}

