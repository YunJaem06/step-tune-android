package hs.project.steptune.data.repository

import hs.project.steptune.api.ServerException
import hs.project.steptune.api.StepAPI
import hs.project.steptune.api.UnauthorizedException
import hs.project.steptune.api.NotFoundException
import hs.project.steptune.data.ServerResponse
import hs.project.steptune.data.step.request.RequestDailyStepRecord
import hs.project.steptune.data.step.request.RequestSyncDailyStepRecords
import hs.project.steptune.data.step.response.ResponseDailyStepRecord
import hs.project.steptune.domain.model.DailyStepRecord
import hs.project.steptune.domain.model.DailyStepRecordSyncResult
import hs.project.steptune.domain.model.DailyStepRecordWrite
import hs.project.steptune.domain.model.WeeklyStepStatistics
import hs.project.steptune.domain.repository.StepRecordRepository
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class StepRecordRepositoryImpl @Inject constructor(
    private val stepAPI: StepAPI
) : StepRecordRepository {
    override suspend fun syncDailyRecords(
        records: List<DailyStepRecordWrite>
    ): DailyStepRecordSyncResult {
        require(records.isNotEmpty()) { "records must not be empty" }
        val response = stepAPI.requestSyncDailyRecords(
            RequestSyncDailyStepRecords(
                records = records.map { record ->
                    RequestDailyStepRecord(
                        recordDate = record.recordDate,
                        stepCount = record.stepCount,
                        measuredAt = record.measuredAt
                    )
                }
            )
        ).requireData()
        return DailyStepRecordSyncResult(
            records = response.records.map { record -> record.toDomain() },
            syncTime = response.syncTime
        )
    }

    override suspend fun getDailyRecord(recordDate: String): DailyStepRecord? =
        stepAPI.requestDailyRecord(recordDate)
            .requireData()
            .record
            ?.toDomain()

    override suspend fun getHistory(from: String, to: String): List<DailyStepRecord> =
        stepAPI.requestDailyRecordHistory(from = from, to = to)
            .requireData()
            .records
            .map { record -> record.toDomain() }

    override suspend fun getWeeklyStatistics(recordDate: String): WeeklyStepStatistics {
        val response = stepAPI.requestWeeklyStatistics(recordDate).requireData()
        return WeeklyStepStatistics(
            recordDate = response.recordDate,
            todayStepCount = response.todayStepCount,
            recent7DayAverage = response.recent7DayAverage.toDouble(),
            recordedDayCount = response.recordedDayCount,
            differenceFromAverage = response.differenceFromAverage.toDouble(),
            changeRatePercent = response.changeRatePercent?.toDouble()
        )
    }

    private fun ResponseDailyStepRecord.toDomain(): DailyStepRecord = DailyStepRecord(
        recordDate = recordDate,
        stepCount = stepCount,
        measuredAt = measuredAt,
        updatedAt = updatedAt
    )

    private fun <T> Response<ServerResponse<T>>.requireData(): T {
        if (code() == HTTP_UNAUTHORIZED) {
            throw UnauthorizedException()
        }
        if (code() == HTTP_NOT_FOUND) {
            throw NotFoundException(body()?.message ?: "걸음 기록을 찾을 수 없습니다.")
        }

        val responseBody = body()
        if (!isSuccessful || responseBody?.code != HTTP_OK) {
            throw ServerException(responseBody?.message ?: "걸음 기록 서버 요청에 실패했습니다.")
        }
        return responseBody.data
            ?: throw ServerException("서버 응답에 걸음 기록 데이터가 없습니다.")
    }

    private companion object {
        const val HTTP_OK = 200
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_NOT_FOUND = 404
    }
}
