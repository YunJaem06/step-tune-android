package hs.project.steptune.service

import hs.project.steptune.data.local.preferences.StepTrackingState
import org.junit.Assert.assertEquals
import org.junit.Test

class StepCountCalculatorTest {

    @Test
    fun `counter rollback above the old baseline preserves today's saved steps`() {
        val result = StepCountCalculator.calculate(
            date = "2026-10-02",
            rawSensorSteps = 1_500,
            previousState = StepTrackingState("2026-10-02", 1_000, 100),
            existingTodaySteps = 1_100
        )

        assertEquals(1_100, result.steps)
        assertEquals(StepTrackingState("2026-10-02", 1_500, 1_100), result.trackingState)
        val next = StepCountCalculator.calculate("2026-10-02", 1_520, result.trackingState, result.steps)
        assertEquals(1_120, next.steps)
    }

    @Test
    fun `repeated sensor values do not count the same steps twice`() {
        val state = StepTrackingState("2026-10-02", 1_000, 100)
        val first = StepCountCalculator.calculate("2026-10-02", 1_250, state, 100)
        val repeated = StepCountCalculator.calculate("2026-10-02", 1_250, first.trackingState, first.steps)

        assertEquals(350, first.steps)
        assertEquals(first, repeated)
    }

    @Test
    fun `very large sensor values do not overflow the daily step total`() {
        val result = StepCountCalculator.calculate(
            date = "2026-10-02",
            rawSensorSteps = Int.MAX_VALUE,
            previousState = StepTrackingState("2026-10-02", 0, 1_000),
            existingTodaySteps = 1_000
        )

        assertEquals(Int.MAX_VALUE, result.steps)
    }

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
