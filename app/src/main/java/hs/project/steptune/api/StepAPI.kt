package hs.project.steptune.api

import hs.project.steptune.Config
import hs.project.steptune.data.ServerResponse
import hs.project.steptune.data.step.request.RequestSyncDailyStepRecords
import hs.project.steptune.data.step.response.ResponseDailyStepRecordHistory
import hs.project.steptune.data.step.response.ResponseDailyStepRecordLookup
import hs.project.steptune.data.step.response.ResponseDailyStepRecordSync
import hs.project.steptune.data.step.response.ResponseWeeklyStepStatistics
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface StepAPI {
    @PUT(Config.API.STEP_DAILY_RECORD_SYNC)
    suspend fun requestSyncDailyRecords(
        @Body request: RequestSyncDailyStepRecords
    ): Response<ServerResponse<ResponseDailyStepRecordSync>>

    @GET(Config.API.STEP_DAILY_RECORD_BY_DATE)
    suspend fun requestDailyRecord(
        @Query("recordDate") recordDate: String
    ): Response<ServerResponse<ResponseDailyStepRecordLookup>>

    @GET(Config.API.STEP_DAILY_RECORD_HISTORY)
    suspend fun requestDailyRecordHistory(
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<ServerResponse<ResponseDailyStepRecordHistory>>

    @GET(Config.API.STEP_WEEKLY_STATISTICS)
    suspend fun requestWeeklyStatistics(
        @Query("recordDate") recordDate: String
    ): Response<ServerResponse<ResponseWeeklyStepStatistics>>
}
