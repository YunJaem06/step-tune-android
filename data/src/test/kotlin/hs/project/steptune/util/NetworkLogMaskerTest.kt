package hs.project.steptune.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NetworkLogMaskerTest {
    @Test
    fun `token values are masked while ordinary response fields remain visible`() {
        val message = """{"code":200,"data":{"token":"google-id-token","accessToken":"access-value","refreshToken":"refresh-value","nickName":"스텝러너"}}"""

        val masked = NetworkLogMasker.mask(message)

        assertFalse(masked.contains("google-id-token"))
        assertFalse(masked.contains("access-value"))
        assertFalse(masked.contains("refresh-value"))
        assertEquals(
            """{"code":200,"data":{"token":"██","accessToken":"██","refreshToken":"██","nickName":"스텝러너"}}""",
            masked
        )
    }
}
