package hs.project.steptune.data.repository

import hs.project.steptune.api.MusicRecommendationAPI
import hs.project.steptune.api.RecommendationUnavailableException
import hs.project.steptune.data.ServerResponse
import hs.project.steptune.data.local.database.MusicRecommendationDao
import hs.project.steptune.data.local.database.MusicRecommendationEntity
import hs.project.steptune.data.recommendation.request.RequestGenerateMusicRecommendation
import hs.project.steptune.data.recommendation.response.ResponseMusicRecommendation
import hs.project.steptune.data.recommendation.response.ResponseRecommendedTrack
import hs.project.steptune.data.recommendation.response.ResponseRecommendationStepSummary
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Response

class MusicRecommendationRepositoryImplTest {
    @Test
    fun `successful response is mapped and stored in Room`() = runBlocking {
        val api = FakeMusicRecommendationAPI(
            response = Response.success(
                ServerResponse(code = 200, message = "success", data = recommendationResponse())
            )
        )
        val dao = RecordingMusicRecommendationDao()
        val repository = MusicRecommendationRepositoryImpl(api, dao)

        val result = repository.generate(
            recordDate = "2026-09-08",
            preferredMoods = setOf(MusicMood.CALM),
            preferredGenres = setOf(MusicGenre.RNB),
            durationMinutes = 30
        )

        assertEquals(listOf("CALM"), api.lastRequest?.preferredMoods)
        assertEquals(listOf("RNB"), api.lastRequest?.preferredGenres)
        assertEquals("recommendation-id", result.recommendationId)
        assertEquals("차분한 산책 음악을 추천해요.", result.reason)
        assertEquals("Dynamite", result.track.title)
        assertEquals("BTS", dao.saved?.trackArtist)
        assertEquals("BTS Dynamite official audio", dao.saved?.trackSearchQuery)
    }

    @Test
    fun `503 response is exposed as unavailable recommendation service`() {
        val repository = MusicRecommendationRepositoryImpl(
            recommendationAPI = FakeMusicRecommendationAPI(
                response = Response.error(503, "".toResponseBody())
            ),
            recommendationDao = RecordingMusicRecommendationDao()
        )

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

    private fun recommendationResponse() = ResponseMusicRecommendation(
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
        generatedAt = "2026-09-08T05:00:00Z"
    )
}

private class FakeMusicRecommendationAPI(
    private val response: Response<ServerResponse<ResponseMusicRecommendation>>
) : MusicRecommendationAPI {
    var lastRequest: RequestGenerateMusicRecommendation? = null

    override suspend fun requestGenerateRecommendation(
        request: RequestGenerateMusicRecommendation
    ): Response<ServerResponse<ResponseMusicRecommendation>> {
        lastRequest = request
        return response
    }
}

private class RecordingMusicRecommendationDao : MusicRecommendationDao {
    var saved: MusicRecommendationEntity? = null

    override suspend fun upsert(recommendation: MusicRecommendationEntity) {
        saved = recommendation
    }

    override fun observeAll(): Flow<List<MusicRecommendationEntity>> =
        flowOf(listOfNotNull(saved))

    override suspend fun deleteAll() {
        saved = null
    }
}
