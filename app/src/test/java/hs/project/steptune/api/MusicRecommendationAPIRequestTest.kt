package hs.project.steptune.api

import com.google.gson.Gson
import hs.project.steptune.data.recommendation.request.RequestGenerateMusicRecommendation
import hs.project.steptune.data.recommendation.request.RequestUpdateMusicRecommendationFavorite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MusicRecommendationAPIRequestTest {
    @Test
    fun `recommendation request matches server contract without user or step fields`() {
        val json = Gson().toJsonTree(
            RequestGenerateMusicRecommendation(
                recordDate = "2026-09-08",
                preferredMoods = listOf("CALM", "EMOTIONAL"),
                preferredGenres = listOf("RNB", "INDIE"),
                durationMinutes = 30
            )
        ).asJsonObject

        assertEquals(
            setOf("recordDate", "preferredMoods", "preferredGenres", "durationMinutes"),
            json.keySet()
        )
        assertEquals("2026-09-08", json["recordDate"].asString)
        assertEquals("CALM", json["preferredMoods"].asJsonArray.first().asString)
        assertEquals("RNB", json["preferredGenres"].asJsonArray.first().asString)
        assertEquals(30, json["durationMinutes"].asInt)
        assertFalse(json.has("userId"))
        assertFalse(json.has("stepCount"))
    }

    @Test
    fun `favorite request contains only the target state`() {
        val json = Gson().toJsonTree(
            RequestUpdateMusicRecommendationFavorite(favorite = true)
        ).asJsonObject

        assertEquals(setOf("favorite"), json.keySet())
        assertEquals(true, json["favorite"].asBoolean)
    }
}
