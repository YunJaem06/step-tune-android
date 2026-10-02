package hs.project.steptune.data.local.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PedometerPreferencesDataSourceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `complete onboarding stores music preferences and both completion flags`() = runBlocking {
        val dataSource = createDataSource()

        dataSource.completeOnboarding(
            preferredGenreKeys = setOf("rnb", "indie"),
            preferredMoodKeys = setOf("calm")
        )

        val preferences = dataSource.preferences.first()
        assertEquals(setOf("rnb", "indie"), preferences.preferredGenreKeys)
        assertEquals(setOf("calm"), preferences.preferredMoodKeys)
        assertTrue(preferences.onboardingCompleted)
        assertTrue(preferences.musicPreferencesOnboardingCompleted)
    }

    @Test
    fun `complete onboarding supports skipping with empty preferences`() = runBlocking {
        val dataSource = createDataSource()

        dataSource.completeOnboarding(
            preferredGenreKeys = emptySet(),
            preferredMoodKeys = emptySet()
        )

        val preferences = dataSource.preferences.first()
        assertTrue(preferences.preferredGenreKeys.isEmpty())
        assertTrue(preferences.preferredMoodKeys.isEmpty())
        assertTrue(preferences.onboardingCompleted)
        assertTrue(preferences.musicPreferencesOnboardingCompleted)
    }

    private fun createDataSource(): PedometerPreferencesDataSource {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = {
                File(temporaryFolder.root, "pedometer.preferences_pb")
            }
        )
        return PedometerPreferencesDataSource(dataStore)
    }
}
