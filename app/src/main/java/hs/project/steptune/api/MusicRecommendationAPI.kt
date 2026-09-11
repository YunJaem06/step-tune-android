package hs.project.steptune.api

import hs.project.steptune.Config
import hs.project.steptune.data.ServerResponse
import hs.project.steptune.data.recommendation.request.RequestGenerateMusicRecommendation
import hs.project.steptune.data.recommendation.response.ResponseMusicRecommendation
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MusicRecommendationAPI {
    @POST(Config.API.MUSIC_RECOMMENDATION_GENERATE)
    suspend fun requestGenerateRecommendation(
        @Body request: RequestGenerateMusicRecommendation
    ): Response<ServerResponse<ResponseMusicRecommendation>>
}
