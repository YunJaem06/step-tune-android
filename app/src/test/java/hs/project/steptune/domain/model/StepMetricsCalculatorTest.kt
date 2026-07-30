package hs.project.steptune.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StepMetricsCalculatorTest {

    @Test
    fun `distance and calories are calculated from current preferences`() {
        val distanceMeters = StepMetricsCalculator.distanceMeters(
            steps = 5_000,
            stepLengthCm = 70
        )
        val calories = StepMetricsCalculator.calories(
            distanceMeters = distanceMeters,
            weightKg = 65
        )

        assertEquals(3_500f, distanceMeters, 0.001f)
        assertEquals(129.675f, calories, 0.001f)
    }
}
