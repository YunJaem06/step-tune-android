package hs.project.steptune.api

import hs.project.steptune.Config
import hs.project.steptune.data.ServerResponse
import hs.project.steptune.data.auth.response.ResponseAuthLogin
import hs.project.steptune.data.auth.response.ResponseUserData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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

    @GET(Config.API.MY_INFO)
    suspend fun requestMyInfo(): Response<ServerResponse<ResponseUserData>>

    data class RequestLogout(
        val refreshToken: String
    )

    @POST(Config.API.AUTH_LOGOUT)
    suspend fun requestLogout(
        @Body request: RequestLogout
    ): Response<ServerResponse<Any>>
}
