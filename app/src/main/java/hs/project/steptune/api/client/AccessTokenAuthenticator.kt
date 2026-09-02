package hs.project.steptune.api.client

import hs.project.steptune.api.AuthAPI
import hs.project.steptune.core.auth.AuthSessionEventBus
import hs.project.steptune.core.di.RefreshAuthClient
import hs.project.steptune.data.local.preferences.AuthPreferencesDataSource
import hs.project.steptune.domain.model.AuthSession
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class AccessTokenAuthenticator @Inject constructor(
    @param:RefreshAuthClient
    private val refreshAuthAPI: AuthAPI,
    private val authPreferencesDataSource: AuthPreferencesDataSource,
    private val authSessionEventBus: AuthSessionEventBus
) : Authenticator {
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.isUnauthenticatedPath()) return null
        if (response.responseCount >= MAX_AUTH_ATTEMPTS) {
            expireSession()
            return null
        }

        val failedAccessToken = response.request.bearerAccessToken()
        return synchronized(refreshLock) {
            val currentSession = runBlocking {
                authPreferencesDataSource.currentSession()
            }
            if (!currentSession.hasRefreshToken) {
                expireSession()
                return@synchronized null
            }

            if (
                currentSession.accessToken.isNotBlank() &&
                currentSession.accessToken != failedAccessToken
            ) {
                return@synchronized response.request.withBearerAccessToken(
                    currentSession.accessToken
                )
            }

            val refreshedSession = refreshSession(currentSession.refreshToken)
                ?: return@synchronized null
            response.request.withBearerAccessToken(refreshedSession.accessToken)
        }
    }

    private fun refreshSession(refreshToken: String): AuthSession? {
        return try {
            val response = runBlocking {
                refreshAuthAPI.requestRefreshToken(
                    AuthAPI.RequestRefreshToken(refreshToken)
                )
            }
            val responseBody = response.body()
            val authData = responseBody?.data
            if (!response.isSuccessful || responseBody?.code != HTTP_OK || authData == null) {
                expireSession()
                return null
            }

            AuthSession(
                accessToken = authData.accessToken,
                refreshToken = authData.refreshToken,
                userId = authData.userData.userId.toString(),
                nickName = authData.userData.nickName
            ).also { session ->
                runBlocking {
                    authPreferencesDataSource.saveSession(session)
                }
            }
        } catch (_: Exception) {
            expireSession()
            null
        }
    }

    private fun expireSession() {
        runCatching {
            runBlocking {
                authPreferencesDataSource.clearSession()
            }
        }
        authSessionEventBus.notifySessionExpired()
    }

    private fun Request.bearerAccessToken(): String =
        header(AUTHORIZATION_HEADER)
            ?.removePrefix(BEARER_PREFIX)
            .orEmpty()

    private val Response.responseCount: Int
        get() {
            var count = 1
            var priorResponse = priorResponse
            while (priorResponse != null) {
                count++
                priorResponse = priorResponse.priorResponse
            }
            return count
        }

    private companion object {
        const val HTTP_OK = 200
        const val MAX_AUTH_ATTEMPTS = 2
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
