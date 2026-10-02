package hs.project.steptune.domain.model

object StepMetricsCalculator {

    fun distanceMeters(
        steps: Int,
        stepLengthCm: Int
    ): Float {
        return steps.coerceAtLeast(0) * (stepLengthCm.coerceAtLeast(0) / 100f)
    }

    fun calories(
        distanceMeters: Float,
        weightKg: Int
    ): Float {
        val distanceKm = distanceMeters.coerceAtLeast(0f) / 1_000f
        return distanceKm * weightKg.coerceAtLeast(0) * 0.57f
    }
}
