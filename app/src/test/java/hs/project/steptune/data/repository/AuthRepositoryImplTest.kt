package hs.project.steptune.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import hs.project.steptune.api.AuthAPI
import hs.project.steptune.api.UnauthorizedException
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
                        userData = ResponseUserData(2L, "new-name")
                    )
                )
            )
        }

        val session = AuthRepositoryImpl(api, dataSource).refreshSession()

        assertEquals("old-refresh", api.lastRefreshRequest?.refreshToken)
        assertEquals("new-access", session.accessToken)
        assertEquals("new-refresh", session.refreshToken)
        assertEquals("2", session.userId)
        assertEquals(session, dataSource.currentSession())
    }

    @Test
    fun `nickname availability and update follow the server response`() = runBlocking {
        val dataSource = createDataSource()
        dataSource.saveSession(
            AuthSession(
                accessToken = "access",
                refreshToken = "refresh",
                userId = "1",
                nickName = "old-name"
            )
        )
        val api = FakeAuthAPI().apply {
            nicknameAvailabilityResponse = Response.success(
                ServerResponse(
                    code = 200,
                    message = "success",
                    data = ResponseNicknameAvailability("new-name", true)
                )
            )
            updateNicknameResponse = Response.success(
                ServerResponse(
                    code = 200,
                    message = "success",
                    data = ResponseUserData(1L, "new-name")
                )
            )
        }
        val repository = AuthRepositoryImpl(api, dataSource)

        val availability = repository.checkNicknameAvailability("  new-name  ")
        val session = repository.updateNickname("new-name")

        assertEquals("  new-name  ", api.lastNicknameAvailabilityRequest)
        assertEquals("new-name", availability.nickName)
        assertEquals(true, availability.isAvailable)
        assertEquals("new-name", api.lastUpdateNicknameRequest?.nickName)
        assertEquals("new-name", session.nickName)
        assertEquals(session, dataSource.currentSession())
    }

    @Test
    fun `delete account clears saved authentication data after server success`() = runBlocking {
        val dataSource = createDataSource()
        dataSource.saveSession(
            AuthSession(
                accessToken = "access",
                refreshToken = "refresh",
                userId = "1",
                nickName = "name"
            )
        )
        val api = FakeAuthAPI().apply {
            deleteAccountResponse = Response.success(
                ServerResponse(code = 200, message = "success", data = null)
            )
        }

        AuthRepositoryImpl(api, dataSource).deleteAccount()

        assertEquals(AuthSession(), dataSource.currentSession())
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
    var nicknameAvailabilityResponse: Response<ServerResponse<ResponseNicknameAvailability>> =
        Response.success(ServerResponse(code = 500, message = "not configured", data = null))
    var updateNicknameResponse: Response<ServerResponse<ResponseUserData>> =
        Response.success(ServerResponse(code = 500, message = "not configured", data = null))
    var deleteAccountResponse: Response<ServerResponse<Any>> =
        Response.success(ServerResponse(code = 500, message = "not configured", data = null))
    var lastNicknameAvailabilityRequest: String? = null
    var lastUpdateNicknameRequest: RequestUpdateNickname? = null

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

    override suspend fun requestNicknameAvailability(
        nickName: String
    ): Response<ServerResponse<ResponseNicknameAvailability>> {
        lastNicknameAvailabilityRequest = nickName
        return nicknameAvailabilityResponse
    }

    override suspend fun requestUpdateNickname(
        request: RequestUpdateNickname
    ): Response<ServerResponse<ResponseUserData>> {
        lastUpdateNicknameRequest = request
        return updateNicknameResponse
    }

    override suspend fun requestDeleteAccount(): Response<ServerResponse<Any>> =
        deleteAccountResponse

    override suspend fun requestLogout(
        request: AuthAPI.RequestLogout
    ): Response<ServerResponse<Any>> = error("not used")
}
