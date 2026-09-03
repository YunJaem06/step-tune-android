package hs.project.steptune.data.repository

import hs.project.steptune.api.StepAPI
import hs.project.steptune.data.ServerResponse
import hs.project.steptune.data.step.request.RequestSyncDailyStepRecords
import hs.project.steptune.data.step.response.ResponseDailyStepRecord
import hs.project.steptune.data.step.response.ResponseDailyStepRecordHistory
import hs.project.steptune.data.step.response.ResponseDailyStepRecordLookup
import hs.project.steptune.data.step.response.ResponseDailyStepRecordSync
import hs.project.steptune.data.step.response.ResponseWeeklyStepStatistics
import hs.project.steptune.domain.model.DailyStepRecordWrite
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class StepRecordRepositoryImplTest {
    @Test
    fun `sync maps request and response without exposing user id`() = runBlocking {
        val responseRecord = responseRecord(stepCount = 4_500)
        val api = FakeStepAPI().apply {
            syncResponse = Response.success(
                ServerResponse(
                    code = 200,
                    message = "success",
                    data = ResponseDailyStepRecordSync(
                        records = listOf(responseRecord),
                        syncTime = "2026-09-03T05:30:01Z"
                    )
                )
            )
        }

        val result = StepRecordRepositoryImpl(api).syncDailyRecords(
            listOf(
                DailyStepRecordWrite(
                    recordDate = "2026-09-03",
                    stepCount = 4_500,
                    measuredAt = "2026-09-03T14:30:00+09:00"
                )
            )
        )

        assertEquals(1, api.lastSyncRequest?.records?.size)
        assertEquals("2026-09-03", api.lastSyncRequest?.records?.single()?.recordDate)
        assertEquals(4_500, api.lastSyncRequest?.records?.single()?.stepCount)
        assertEquals(4_500, result.records.single().stepCount)
        assertEquals("2026-09-03T05:30:01Z", result.syncTime)
    }

    @Test
    fun `history and missing daily record follow server response`() = runBlocking {
        val api = FakeStepAPI().apply {
            historyResponse = Response.success(
                ServerResponse(
                    code = 200,
                    message = "success",
                    data = ResponseDailyStepRecordHistory(
                        from = "2026-09-01",
                        to = "2026-09-03",
                        records = listOf(responseRecord(stepCount = 7_000))
                    )
                )
            )
            lookupResponse = Response.success(
                ServerResponse(
                    code = 200,
                    message = "success",
                    data = ResponseDailyStepRecordLookup(record = null)
                )
            )
        }
        val repository = StepRecordRepositoryImpl(api)

        val history = repository.getHistory("2026-09-01", "2026-09-03")
        val missingRecord = repository.getDailyRecord("2026-09-02")

        assertEquals("2026-09-01" to "2026-09-03", api.lastHistoryRange)
        assertEquals(7_000, history.single().stepCount)
        assertEquals("2026-09-02", api.lastLookupDate)
        assertEquals(null, missingRecord)
    }

    @Test
    fun `weekly statistics maps decimal values and nullable change rate`() = runBlocking {
        val api = FakeStepAPI().apply {
            weeklyStatisticsResponse = Response.success(
                ServerResponse(
                    code = 200,
                    message = "success",
                    data = ResponseWeeklyStepStatistics(
                        recordDate = "2026-09-03",
                        todayStepCount = 5_000,
                        recent7DayAverage = BigDecimal("3000.00"),
                        recordedDayCount = 3,
                        differenceFromAverage = BigDecimal("2000.00"),
                        changeRatePercent = BigDecimal("66.67")
                    )
                )
            )
        }

        val statistics = StepRecordRepositoryImpl(api)
            .getWeeklyStatistics("2026-09-03")

        assertEquals("2026-09-03", api.lastWeeklyStatisticsDate)
        assertEquals(5_000, statistics.todayStepCount)
        assertEquals(3_000.0, statistics.recent7DayAverage, 0.0)
        assertEquals(66.67, statistics.changeRatePercent ?: 0.0, 0.0)
    }

    private fun responseRecord(stepCount: Int) = ResponseDailyStepRecord(
        recordDate = "2026-09-03",
        stepCount = stepCount,
        measuredAt = "2026-09-03T05:30:00Z",
        updatedAt = "2026-09-03T05:30:01Z"
    )
}

private class FakeStepAPI : StepAPI {
    lateinit var syncResponse: Response<ServerResponse<ResponseDailyStepRecordSync>>
    lateinit var lookupResponse: Response<ServerResponse<ResponseDailyStepRecordLookup>>
    lateinit var historyResponse: Response<ServerResponse<ResponseDailyStepRecordHistory>>
    lateinit var weeklyStatisticsResponse: Response<ServerResponse<ResponseWeeklyStepStatistics>>
    var lastSyncRequest: RequestSyncDailyStepRecords? = null
    var lastLookupDate: String? = null
    var lastHistoryRange: Pair<String, String>? = null
    var lastWeeklyStatisticsDate: String? = null

    override suspend fun requestSyncDailyRecords(
        request: RequestSyncDailyStepRecords
    ): Response<ServerResponse<ResponseDailyStepRecordSync>> {
        lastSyncRequest = request
        return syncResponse
    }

    override suspend fun requestDailyRecord(
        recordDate: String
    ): Response<ServerResponse<ResponseDailyStepRecordLookup>> {
        lastLookupDate = recordDate
        return lookupResponse
    }

    override suspend fun requestDailyRecordHistory(
        from: String,
        to: String
    ): Response<ServerResponse<ResponseDailyStepRecordHistory>> {
        lastHistoryRange = from to to
        return historyResponse
    }

    override suspend fun requestWeeklyStatistics(
        recordDate: String
    ): Response<ServerResponse<ResponseWeeklyStepStatistics>> {
        lastWeeklyStatisticsDate = recordDate
        return weeklyStatisticsResponse
    }
}
