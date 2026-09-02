package hs.project.steptune.api.client

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import hs.project.steptune.api.AuthAPI
import hs.project.steptune.core.auth.AuthSessionEventBus
import hs.project.steptune.data.ServerResponse
import hs.project.steptune.data.auth.response.ResponseAuthLogin
import hs.project.steptune.data.auth.response.ResponseUserData
import hs.project.steptune.data.local.preferences.AuthPreferencesDataSource
import hs.project.steptune.data.user.request.RequestUpdateNickname
import hs.project.steptune.data.user.response.ResponseNicknameAvailability
import hs.project.steptune.domain.model.AuthSession
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response as RetrofitResponse

class AccessTokenAuthenticatorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `401 refreshes tokens and retries with the new access token`() = runBlocking {
        val dataSource = createDataSource()
        dataSource.saveSession(
            AuthSession(
                accessToken = "old-access",
                refreshToken = "old-refresh",
                userId = "1",
                nickName = "old-name"
            )
        )
        val api = FakeRefreshAuthAPI().apply {
            refreshResponse = RetrofitResponse.success(
                ServerResponse(
                    code = 200,
                    message = "success",
                    data = ResponseAuthLogin(
                        accessToken = "new-access",
                        accessTokenExpiresIn = 900,
                        refreshToken = "new-refresh",
                        userData = ResponseUserData(1L, "new-name")
                    )
                )
            )
        }
        val authenticator = AccessTokenAuthenticator(
            refreshAuthAPI = api,
            authPreferencesDataSource = dataSource,
            authSessionEventBus = AuthSessionEventBus()
        )

        val retriedRequest = authenticator.authenticate(
            route = null,
            response = unauthorizedResponse("old-access")
        )

        assertEquals("old-refresh", api.lastRefreshToken)
        assertEquals("Bearer new-access", retriedRequest?.header("Authorization"))
        assertEquals("new-access", dataSource.currentSession().accessToken)
        assertEquals("new-refresh", dataSource.currentSession().refreshToken)
        assertEquals("new-name", dataSource.currentSession().nickName)
    }

    @Test
    fun `failed refresh clears the session and stops retrying`() = runBlocking {
        val dataSource = createDataSource()
        dataSource.saveSession(
            AuthSession(
                accessToken = "expired-access",
                refreshToken = "invalid-refresh",
                userId = "1",
                nickName = "name"
            )
        )
        val api = FakeRefreshAuthAPI().apply {
            refreshResponse = RetrofitResponse.error(
                401,
                "{}".toResponseBody("application/json".toMediaType())
            )
        }
        val authenticator = AccessTokenAuthenticator(
            refreshAuthAPI = api,
            authPreferencesDataSource = dataSource,
            authSessionEventBus = AuthSessionEventBus()
        )

        val retriedRequest = authenticator.authenticate(
            route = null,
            response = unauthorizedResponse("expired-access")
        )

        assertNull(retriedRequest)
        assertEquals(AuthSession(), dataSource.currentSession())
    }

    private fun createDataSource(): AuthPreferencesDataSource {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = {
                File(temporaryFolder.root, "auth.preferences_pb")
            }
        )
        return AuthPreferencesDataSource(dataStore)
    }

    private fun unauthorizedResponse(accessToken: String): Response =
        Response.Builder()
            .request(
                Request.Builder()
                    .url("http://10.0.2.2:8080/api/v1/me/profile")
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            )
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()
}

private class FakeRefreshAuthAPI : AuthAPI {
    var refreshResponse: RetrofitResponse<ServerResponse<ResponseAuthLogin>> =
        RetrofitResponse.success(
            ServerResponse(code = 500, message = "not configured", data = null)
        )
    var lastRefreshToken: String? = null

    override suspend fun requestSocialLogin(
        request: AuthAPI.RequestSocialLogin
    ): RetrofitResponse<ServerResponse<ResponseAuthLogin>> = error("not used")

    override suspend fun requestRefreshToken(
        request: AuthAPI.RequestRefreshToken
    ): RetrofitResponse<ServerResponse<ResponseAuthLogin>> {
        lastRefreshToken = request.refreshToken
        return refreshResponse
    }

    override suspend fun requestMyInfo(): RetrofitResponse<ServerResponse<ResponseUserData>> =
        error("not used")

    override suspend fun requestNicknameAvailability(
        nickName: String
    ): RetrofitResponse<ServerResponse<ResponseNicknameAvailability>> = error("not used")

    override suspend fun requestUpdateNickname(
        request: RequestUpdateNickname
    ): RetrofitResponse<ServerResponse<ResponseUserData>> = error("not used")

    override suspend fun requestDeleteAccount(): RetrofitResponse<ServerResponse<Any>> =
        error("not used")

    override suspend fun requestLogout(
        request: AuthAPI.RequestLogout
    ): RetrofitResponse<ServerResponse<Any>> = error("not used")
}
