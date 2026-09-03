package hs.project.steptune.domain.model

data class WeeklyStepStatistics(
    val recordDate: String,
    val todayStepCount: Int,
    val recent7DayAverage: Double,
    val recordedDayCount: Int,
    val differenceFromAverage: Double,
    val changeRatePercent: Double?
)
