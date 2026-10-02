package hs.project.steptune.api

import com.google.gson.Gson
import hs.project.steptune.data.step.request.RequestDailyStepRecord
import hs.project.steptune.data.step.request.RequestSyncDailyStepRecords
import org.junit.Assert.assertEquals
import org.junit.Test

class StepAPIRequestTest {
    @Test
    fun `daily step sync request matches server contract`() {
        val jsonObject = Gson().toJsonTree(
            RequestSyncDailyStepRecords(
                records = listOf(
                    RequestDailyStepRecord(
                        recordDate = "2026-09-03",
                        stepCount = 3_200,
                        measuredAt = "2026-09-03T14:30:00+09:00"
                    )
                )
            )
        ).asJsonObject

        assertEquals(setOf("records"), jsonObject.keySet())
        val record = jsonObject["records"].asJsonArray.single().asJsonObject
        assertEquals(setOf("recordDate", "stepCount", "measuredAt"), record.keySet())
        assertEquals("2026-09-03", record["recordDate"].asString)
        assertEquals(3_200, record["stepCount"].asInt)
        assertEquals("2026-09-03T14:30:00+09:00", record["measuredAt"].asString)
    }
}
