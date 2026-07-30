package hs.project.steptune.service

import hs.project.steptune.data.local.preferences.StepTrackingState
import org.junit.Assert.assertEquals
import org.junit.Test

class StepCountCalculatorTest {

    @Test
    fun `first reading initializes baseline and preserves an existing record`() {
        val result = StepCountCalculator.calculate(
            date = "2026-07-30",
            rawSensorSteps = 1_000,
            previousState = StepTrackingState(),
            existingTodaySteps = 320
        )

        assertEquals(320, result.steps)
        assertEquals(
            StepTrackingState(
                trackingDate = "2026-07-30",
                baselineSensorSteps = 1_000,
                offsetSteps = 320
            ),
            result.trackingState
        )
    }

    @Test
    fun `same day reading adds the sensor delta to the offset`() {
        val result = StepCountCalculator.calculate(
            date = "2026-07-30",
            rawSensorSteps = 1_250,
            previousState = StepTrackingState(
                trackingDate = "2026-07-30",
                baselineSensorSteps = 1_000,
                offsetSteps = 320
            ),
            existingTodaySteps = 0
        )

        assertEquals(570, result.steps)
    }

    @Test
    fun `sensor reset preserves saved steps and starts a new baseline`() {
        val result = StepCountCalculator.calculate(
            date = "2026-07-30",
            rawSensorSteps = 12,
            previousState = StepTrackingState(
                trackingDate = "2026-07-30",
                baselineSensorSteps = 1_000,
                offsetSteps = 100
            ),
            existingTodaySteps = 640
        )

        assertEquals(640, result.steps)
        assertEquals(
            StepTrackingState(
                trackingDate = "2026-07-30",
                baselineSensorSteps = 12,
                offsetSteps = 640
            ),
            result.trackingState
        )
    }

    @Test
    fun `new day starts from today's existing record instead of yesterday's offset`() {
        val result = StepCountCalculator.calculate(
            date = "2026-07-31",
            rawSensorSteps = 1_500,
            previousState = StepTrackingState(
                trackingDate = "2026-07-30",
                baselineSensorSteps = 1_000,
                offsetSteps = 700
            ),
            existingTodaySteps = 25
        )

        assertEquals(25, result.steps)
        assertEquals(
            StepTrackingState(
                trackingDate = "2026-07-31",
                baselineSensorSteps = 1_500,
                offsetSteps = 25
            ),
            result.trackingState
        )
    }
}
