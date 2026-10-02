package hs.project.steptune.data.step.response

data class ResponseDailyStepRecord(
    val recordDate: String,
    val stepCount: Int,
    val measuredAt: String,
    val updatedAt: String
)

data class ResponseDailyStepRecordSync(
    val records: List<ResponseDailyStepRecord>,
    val syncTime: String
)

data class ResponseDailyStepRecordLookup(
    val record: ResponseDailyStepRecord?
)

data class ResponseDailyStepRecordHistory(
    val from: String,
    val to: String,
    val records: List<ResponseDailyStepRecord>
)
