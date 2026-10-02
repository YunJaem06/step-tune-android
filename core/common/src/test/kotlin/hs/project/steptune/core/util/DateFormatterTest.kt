package hs.project.steptune.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DateFormatterTest {
    @Test
    fun `measured time format preserves the stored epoch millis`() {
        val formatted = DateFormatter.measuredAt(TEST_EPOCH_MILLIS)

        assertEquals(TEST_EPOCH_MILLIS, DateFormatter.parseMeasuredAt(formatted))
    }

    @Test
    fun `server time with nanoseconds is parsed`() {
        val parsed = DateFormatter.parseMeasuredAt("2026-09-03T14:30:00.123456789+09:00")

        assertNotNull(parsed)
        assertEquals(123L, requireNotNull(parsed) % 1_000L)
    }

    private companion object {
        const val TEST_EPOCH_MILLIS = 1_788_405_600_123L
    }
}
