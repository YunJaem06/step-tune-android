package hs.project.steptune.service

import hs.project.steptune.data.local.preferences.StepTrackingState

internal data class StepCountCalculation(
    val steps: Int,
    val trackingState: StepTrackingState
)

internal object StepCountCalculator {

    fun calculate(
        date: String,
        rawSensorSteps: Int,
        previousState: StepTrackingState,
        existingTodaySteps: Int
    ): StepCountCalculation {
        val safeRawSensorSteps = rawSensorSteps.coerceAtLeast(0)
        val safeExistingSteps = existingTodaySteps.coerceAtLeast(0)
        val previousBaseline = previousState.baselineSensorSteps

        if (previousState.trackingDate != date || previousBaseline == null) {
            val initializedState = StepTrackingState(
                trackingDate = date,
                baselineSensorSteps = safeRawSensorSteps,
                offsetSteps = safeExistingSteps
            )
            return StepCountCalculation(
                steps = safeExistingSteps,
                trackingState = initializedState
            )
        }

        if (safeRawSensorSteps < previousBaseline) {
            val preservedSteps = maxOf(previousState.offsetSteps, safeExistingSteps)
            val resetState = StepTrackingState(
                trackingDate = date,
                baselineSensorSteps = safeRawSensorSteps,
                offsetSteps = preservedSteps
            )
            return StepCountCalculation(
                steps = preservedSteps,
                trackingState = resetState
            )
        }

        val calculatedSteps = (
            previousState.offsetSteps.toLong() +
                safeRawSensorSteps.toLong() -
                previousBaseline.toLong()
            )
            .coerceIn(previousState.offsetSteps.toLong(), Int.MAX_VALUE.toLong())
            .toInt()

        return StepCountCalculation(
            steps = calculatedSteps,
            trackingState = previousState
        )
    }
}
