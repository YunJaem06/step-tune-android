package hs.project.steptune.api

import hs.project.steptune.Config
import hs.project.steptune.data.ServerResponse
import hs.project.steptune.data.recommendation.request.RequestGenerateMusicRecommendation
import hs.project.steptune.data.recommendation.request.RequestUpdateMusicRecommendationFavorite
import hs.project.steptune.data.recommendation.response.ResponseMusicRecommendationHistory
import hs.project.steptune.data.recommendation.response.ResponseMusicRecommendation
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MusicRecommendationAPI {
    @POST(Config.API.MUSIC_RECOMMENDATION_GENERATE)
    suspend fun requestGenerateRecommendation(
        @Body request: RequestGenerateMusicRecommendation
    ): Response<ServerResponse<ResponseMusicRecommendation>>

    @GET(Config.API.MUSIC_RECOMMENDATION_HISTORY)
    suspend fun requestRecommendationHistory(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("favoriteOnly") favoriteOnly: Boolean
    ): Response<ServerResponse<ResponseMusicRecommendationHistory>>

    @PATCH(Config.API.MUSIC_RECOMMENDATION_FAVORITE)
    suspend fun requestUpdateFavorite(
        @Path("recommendationId") recommendationId: String,
        @Body request: RequestUpdateMusicRecommendationFavorite
    ): Response<ServerResponse<ResponseMusicRecommendation>>

    @DELETE(Config.API.MUSIC_RECOMMENDATION)
    suspend fun requestDeleteRecommendation(
        @Path("recommendationId") recommendationId: String
    ): Response<ServerResponse<Any?>>
}
