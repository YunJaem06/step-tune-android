package hs.project.steptune.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AuthAPIRequestTest {
    @Test
    fun `google login request contains only provider and token`() {
        val jsonObject = Gson().toJsonTree(
            AuthAPI.RequestSocialLogin(
                provider = "google",
                token = "google-id-token"
            )
        ).asJsonObject

        assertEquals(setOf("provider", "token"), jsonObject.keySet())
        assertEquals("google", jsonObject["provider"].asString)
        assertEquals("google-id-token", jsonObject["token"].asString)
        assertFalse(jsonObject.has("providerId"))
        assertFalse(jsonObject.has("deviceType"))
        assertFalse(jsonObject.has("deviceFingerprint"))
    }
}
