package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.model.StepMetricsCalculator
import hs.project.steptune.domain.repository.PedometerRepository
import hs.project.steptune.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.combine

class GetTodayProgressUseCase @Inject constructor(
    private val pedometerRepository: PedometerRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(date: String) = combine(
        pedometerRepository.observeDailyProgress(date),
        settingsRepository.observePreferences()
    ) { progress, preferences ->
        val distanceMeters = StepMetricsCalculator.distanceMeters(
            steps = progress.steps,
            stepLengthCm = preferences.stepLengthCm
        )
        progress.copy(
            goal = preferences.dailyGoal,
            distanceMeters = distanceMeters,
            calories = StepMetricsCalculator.calories(
                distanceMeters = distanceMeters,
                weightKg = preferences.weightKg
            )
        )
    }
}
