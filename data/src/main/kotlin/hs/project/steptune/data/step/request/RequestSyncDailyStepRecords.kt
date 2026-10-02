package hs.project.steptune.data.step.request

data class RequestSyncDailyStepRecords(
    val records: List<RequestDailyStepRecord>
)

data class RequestDailyStepRecord(
    val recordDate: String,
    val stepCount: Int,
    val measuredAt: String
)
