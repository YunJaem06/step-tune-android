package hs.project.steptune.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import hs.project.steptune.api.AuthAPI
import hs.project.steptune.api.UnauthorizedException
import hs.project.steptune.data.ServerResponse
import hs.project.steptune.data.auth.response.ResponseAuthLogin
import hs.project.steptune.data.auth.response.ResponseUserData
import hs.project.steptune.data.local.preferences.AuthPreferencesDataSource
import hs.project.steptune.domain.model.AuthSession
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response

class AuthRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `refresh replaces access token refresh token and user data`() = runBlocking {
        val dataSource = createDataSource()
        dataSource.saveSession(
            AuthSession(
                accessToken = "old-access",
                refreshToken = "old-refresh",
                userId = "old-user",
                nickName = "old-name"
            )
        )
        val api = FakeAuthAPI().apply {
            refreshResponse = Response.success(
                ServerResponse(
                    code = 200,
                    message = "success",
                    data = ResponseAuthLogin(
                        accessToken = "new-access",
                        accessTokenExpiresIn = 900,
                        refreshToken = "new-refresh",
                        userData = ResponseUserData("new-user", "new-name")
                    )
                )
            )
        }

        val session = AuthRepositoryImpl(api, dataSource).refreshSession()

        assertEquals("old-refresh", api.lastRefreshRequest?.refreshToken)
        assertEquals("new-access", session.accessToken)
        assertEquals("new-refresh", session.refreshToken)
        assertEquals(session, dataSource.currentSession())
    }

    @Test
    fun `refresh 401 clears saved authentication data`() {
        val dataSource = createDataSource()
        runBlocking {
            dataSource.saveSession(
                AuthSession(
                    accessToken = "access",
                    refreshToken = "refresh",
                    userId = "user",
                    nickName = "name"
                )
            )
        }
        val api = FakeAuthAPI().apply {
            refreshResponse = Response.error(
                401,
                "{}".toResponseBody("application/json".toMediaType())
            )
        }

        assertThrows(UnauthorizedException::class.java) {
            runBlocking {
                AuthRepositoryImpl(api, dataSource).refreshSession()
            }
        }
        assertEquals(AuthSession(), runBlocking { dataSource.currentSession() })
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
}

private class FakeAuthAPI : AuthAPI {
    var refreshResponse: Response<ServerResponse<ResponseAuthLogin>> = Response.success(
        ServerResponse(code = 500, message = "not configured", data = null)
    )
    var lastRefreshRequest: AuthAPI.RequestRefreshToken? = null

    override suspend fun requestSocialLogin(
        request: AuthAPI.RequestSocialLogin
    ): Response<ServerResponse<ResponseAuthLogin>> = error("not used")

    override suspend fun requestRefreshToken(
        request: AuthAPI.RequestRefreshToken
    ): Response<ServerResponse<ResponseAuthLogin>> {
        lastRefreshRequest = request
        return refreshResponse
    }

    override suspend fun requestMyInfo(): Response<ServerResponse<ResponseUserData>> =
        error("not used")

    override suspend fun requestLogout(
        request: AuthAPI.RequestLogout
    ): Response<ServerResponse<Any>> = error("not used")
}
