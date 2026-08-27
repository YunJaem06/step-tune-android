package hs.project.steptune.api.client

import hs.project.steptune.data.local.preferences.AuthPreferencesDataSource
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class BearerAuthInterceptor @Inject constructor(
    private val authPreferencesDataSource: AuthPreferencesDataSource
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath in UNAUTHENTICATED_PATHS) {
            return chain.proceed(request)
        }

        val accessToken = runBlocking {
            authPreferencesDataSource.currentSession().accessToken
        }
        return chain.proceed(request.withBearerAccessToken(accessToken))
    }
}

internal fun Request.withBearerAccessToken(accessToken: String): Request {
    if (accessToken.isBlank() || url.encodedPath in UNAUTHENTICATED_PATHS) {
        return this
    }

    return newBuilder()
        .header("Authorization", "Bearer $accessToken")
        .build()
}

private val UNAUTHENTICATED_PATHS = setOf(
    "/api/v1/auth/social",
    "/api/v1/auth/refresh",
    "/api/v1/auth/logout"
)
