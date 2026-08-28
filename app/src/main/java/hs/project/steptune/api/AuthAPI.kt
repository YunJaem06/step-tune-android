package hs.project.steptune.api

import hs.project.steptune.Config
import hs.project.steptune.data.ServerResponse
import hs.project.steptune.data.auth.response.ResponseAuthLogin
import hs.project.steptune.data.auth.response.ResponseUserData
import hs.project.steptune.data.user.request.RequestUpdateNickname
import hs.project.steptune.data.user.response.ResponseNicknameAvailability
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthAPI {
    data class RequestSocialLogin(
        val provider: String,
        val token: String
    )

    @POST(Config.API.AUTH_SOCIAL)
    suspend fun requestSocialLogin(
        @Body request: RequestSocialLogin
    ): Response<ServerResponse<ResponseAuthLogin>>

    data class RequestRefreshToken(
        val refreshToken: String
    )

    @POST(Config.API.AUTH_REFRESH)
    suspend fun requestRefreshToken(
        @Body request: RequestRefreshToken
    ): Response<ServerResponse<ResponseAuthLogin>>

    @GET(Config.API.USER_PROFILE)
    suspend fun requestMyInfo(): Response<ServerResponse<ResponseUserData>>

    @GET(Config.API.USER_NICKNAME_AVAILABILITY)
    suspend fun requestNicknameAvailability(
        @Query("nickName") nickName: String
    ): Response<ServerResponse<ResponseNicknameAvailability>>

    @PATCH(Config.API.USER_NICKNAME)
    suspend fun requestUpdateNickname(
        @Body request: RequestUpdateNickname
    ): Response<ServerResponse<ResponseUserData>>

    @DELETE(Config.API.USER_ACCOUNT)
    suspend fun requestDeleteAccount(): Response<ServerResponse<Any>>

    data class RequestLogout(
        val refreshToken: String
    )

    @POST(Config.API.AUTH_LOGOUT)
    suspend fun requestLogout(
        @Body request: RequestLogout
    ): Response<ServerResponse<Any>>
}
