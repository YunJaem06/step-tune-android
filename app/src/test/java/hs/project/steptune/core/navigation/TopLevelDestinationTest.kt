package hs.project.steptune.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelDestinationTest {

    @Test
    fun `items are initialized after progress route is accessed first`() {
        assertEquals("progress", TopLevelDestination.Progress.route)

        assertEquals(
            listOf("progress", "stats", "settings"),
            TopLevelDestination.items.map { destination -> destination.route }
        )
    }
}
