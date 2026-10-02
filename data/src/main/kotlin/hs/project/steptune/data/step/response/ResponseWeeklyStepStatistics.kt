package hs.project.steptune.data.step.response

import java.math.BigDecimal

data class ResponseWeeklyStepStatistics(
    val recordDate: String,
    val todayStepCount: Int,
    val recent7DayAverage: BigDecimal,
    val recordedDayCount: Int,
    val differenceFromAverage: BigDecimal,
    val changeRatePercent: BigDecimal?
)
