package hs.project.steptune.domain.model

data class DailyStepRecordWrite(
    val recordDate: String,
    val stepCount: Int,
    val measuredAt: String
)

data class DailyStepRecord(
    val recordDate: String,
    val stepCount: Int,
    val measuredAt: String,
    val updatedAt: String
)

data class DailyStepRecordSyncResult(
    val records: List<DailyStepRecord>,
    val syncTime: String
)
