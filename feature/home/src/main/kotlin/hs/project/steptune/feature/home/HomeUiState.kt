package hs.project.steptune.feature.home

import androidx.compose.runtime.Immutable
import hs.project.steptune.domain.model.StatsRecord

@Immutable
data class HomeUiState(
    val date: String = "",
    val steps: Int = 0,
    val goal: Int = 10_000,
    val distanceMeters: Float = 0f,
    val calories: Float = 0f,
    val weeklyRecords: List<StatsRecord> = emptyList(),
    val isLoading: Boolean = true
) {
    val progressRatio: Float
        get() = if (goal == 0) 0f else (steps.toFloat() / goal).coerceIn(0f, 1f)

    val remainingSteps: Int
        get() = (goal - steps).coerceAtLeast(0)

    val progressPercent: Int
        get() = (progressRatio * 100).toInt()
}


