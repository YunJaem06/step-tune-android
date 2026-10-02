package hs.project.steptune.api.client

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BearerAuthInterceptorTest {
    @Test
    fun `authenticated request receives bearer access token`() {
        val request = Request.Builder()
            .url("http://10.0.2.2:8080/api/v1/me")
            .build()

        val authenticatedRequest = request.withBearerAccessToken("access-token")

        assertEquals(
            "Bearer access-token",
            authenticatedRequest.header("Authorization")
        )
    }

    @Test
    fun `authentication endpoints do not receive bearer header`() {
        val request = Request.Builder()
            .url("http://10.0.2.2:8080/api/v1/auth/refresh")
            .build()

        val unauthenticatedRequest = request.withBearerAccessToken("access-token")

        assertNull(unauthenticatedRequest.header("Authorization"))
    }
}
