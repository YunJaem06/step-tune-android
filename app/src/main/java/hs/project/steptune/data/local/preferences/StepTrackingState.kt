package hs.project.steptune.data.local.preferences

data class StepTrackingState(
    val trackingDate: String? = null,
    val baselineSensorSteps: Int? = null,
    val offsetSteps: Int = 0
)
