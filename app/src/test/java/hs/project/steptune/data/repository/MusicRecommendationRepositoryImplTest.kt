package hs.project.steptune.data.repository

import hs.project.steptune.api.MusicRecommendationAPI
import hs.project.steptune.api.RecommendationUnavailableException
import hs.project.steptune.data.ServerResponse
import hs.project.steptune.data.recommendation.request.RequestGenerateMusicRecommendation
import hs.project.steptune.data.recommendation.request.RequestUpdateMusicRecommendationFavorite
import hs.project.steptune.data.recommendation.response.ResponseMusicRecommendation
import hs.project.steptune.data.recommendation.response.ResponseMusicRecommendationHistory
import hs.project.steptune.data.recommendation.response.ResponseRecommendedTrack
import hs.project.steptune.data.recommendation.response.ResponseRecommendationStepSummary
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class MusicRecommendationRepositoryImplTest {
    @Test
    fun `successful generation response is mapped from server`() = runBlocking {
        val api = FakeMusicRecommendationAPI()
        val repository = MusicRecommendationRepositoryImpl(api)

        val result = repository.generate(
            recordDate = "2026-09-08",
            preferredMoods = setOf(MusicMood.CALM),
            preferredGenres = setOf(MusicGenre.RNB),
            durationMinutes = 30
        )

        assertEquals(listOf("CALM"), api.lastGenerateRequest?.preferredMoods)
        assertEquals(listOf("RNB"), api.lastGenerateRequest?.preferredGenres)
        assertEquals("recommendation-id", result.recommendationId)
        assertEquals("Dynamite", result.track.title)
        assertFalse(result.favorite)
    }

    @Test
    fun `history response keeps server pagination and favorite state`() = runBlocking {
        val api = FakeMusicRecommendationAPI().apply {
            historyResponse = Response.success(
                ServerResponse(
                    code = 200,
                    message = "success",
                    data = ResponseMusicRecommendationHistory(
                        userId = 1L,
                        recommendations = listOf(recommendationResponse(favorite = true)),
                        favoriteOnly = true,
                        page = 0,
                        size = 20,
                        totalElements = 1,
                        totalPages = 1,
                        hasNext = false
                    )
                )
            )
        }
        val result = MusicRecommendationRepositoryImpl(api).getHistory(
            page = 0,
            size = 20,
            favoriteOnly = true
        )

        assertEquals(0, api.lastHistoryPage)
        assertTrue(api.lastFavoriteOnly == true)
        assertTrue(result.favoriteOnly)
        assertTrue(result.recommendations.single().favorite)
        assertFalse(result.hasNext)
    }

    @Test
    fun `favorite update and delete call the matching server endpoints`() = runBlocking {
        val api = FakeMusicRecommendationAPI().apply {
            favoriteResponse = Response.success(
                ServerResponse(
                    code = 200,
                    message = "success",
                    data = recommendationResponse(favorite = true)
                )
            )
        }
        val repository = MusicRecommendationRepositoryImpl(api)

        val updated = repository.updateFavorite("recommendation-id", favorite = true)
        repository.delete("recommendation-id")

        assertEquals("recommendation-id", api.lastFavoriteRecommendationId)
        assertTrue(api.lastFavoriteRequest?.favorite == true)
        assertTrue(updated.favorite)
        assertEquals("recommendation-id", api.lastDeletedRecommendationId)
    }

    @Test
    fun `503 response is exposed as unavailable recommendation service`() {
        val api = FakeMusicRecommendationAPI().apply {
            generateResponse = Response.error(503, "".toResponseBody())
        }
        val repository = MusicRecommendationRepositoryImpl(api)

        assertThrows(RecommendationUnavailableException::class.java) {
            runBlocking {
                repository.generate(
                    recordDate = "2026-09-08",
                    preferredMoods = emptySet(),
                    preferredGenres = emptySet(),
                    durationMinutes = 30
                )
            }
        }
    }
}

private fun recommendationResponse(
    favorite: Boolean = false
) = ResponseMusicRecommendation(
    recommendationId = "recommendation-id",
    recordDate = "2026-09-08",
    stepSummary = ResponseRecommendationStepSummary(
        todayStepCount = 3_200,
        recent7DayAverage = BigDecimal("2800.00"),
        recordedDayCount = 5,
        differenceFromAverage = BigDecimal("400.00"),
        changeRatePercent = BigDecimal("14.29")
    ),
    activityLevel = "MODERATE",
    durationMinutes = 30,
    reason = "차분한 산책 음악을 추천해요.",
    track = ResponseRecommendedTrack(
        title = "Dynamite",
        artist = "BTS",
        searchQuery = "BTS Dynamite official audio"
    ),
    favorite = favorite,
    generatedAt = "2026-09-08T05:00:00Z"
)

private class FakeMusicRecommendationAPI : MusicRecommendationAPI {
    var generateResponse: Response<ServerResponse<ResponseMusicRecommendation>> =
        Response.success(
            ServerResponse(200, "success", recommendationResponse())
        )
    var historyResponse: Response<ServerResponse<ResponseMusicRecommendationHistory>> =
        Response.success(
            ServerResponse(
                200,
                "success",
                ResponseMusicRecommendationHistory(
                    userId = 1L,
                    recommendations = emptyList(),
                    favoriteOnly = false,
                    page = 0,
                    size = 20,
                    totalElements = 0,
                    totalPages = 0,
                    hasNext = false
                )
            )
        )
    var favoriteResponse: Response<ServerResponse<ResponseMusicRecommendation>> =
        Response.success(ServerResponse(200, "success", recommendationResponse()))
    var deleteResponse: Response<ServerResponse<Any?>> =
        Response.success(ServerResponse<Any?>(200, "success", null))

    var lastGenerateRequest: RequestGenerateMusicRecommendation? = null
    var lastHistoryPage: Int? = null
    var lastFavoriteOnly: Boolean? = null
    var lastFavoriteRecommendationId: String? = null
    var lastFavoriteRequest: RequestUpdateMusicRecommendationFavorite? = null
    var lastDeletedRecommendationId: String? = null

    override suspend fun requestGenerateRecommendation(
        request: RequestGenerateMusicRecommendation
    ): Response<ServerResponse<ResponseMusicRecommendation>> {
        lastGenerateRequest = request
        return generateResponse
    }

    override suspend fun requestRecommendationHistory(
        page: Int,
        size: Int,
        favoriteOnly: Boolean
    ): Response<ServerResponse<ResponseMusicRecommendationHistory>> {
        lastHistoryPage = page
        lastFavoriteOnly = favoriteOnly
        return historyResponse
    }

    override suspend fun requestUpdateFavorite(
        recommendationId: String,
        request: RequestUpdateMusicRecommendationFavorite
    ): Response<ServerResponse<ResponseMusicRecommendation>> {
        lastFavoriteRecommendationId = recommendationId
        lastFavoriteRequest = request
        return favoriteResponse
    }

    override suspend fun requestDeleteRecommendation(
        recommendationId: String
    ): Response<ServerResponse<Any?>> {
        lastDeletedRecommendationId = recommendationId
        return deleteResponse
    }
}
