package hs.project.steptune.data.local.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import hs.project.steptune.domain.model.AuthSession
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AuthPreferencesDataSourceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `save session replaces both tokens and user data`() = runBlocking {
        val dataSource = createDataSource()
        dataSource.saveSession(
            AuthSession(
                accessToken = "old-access",
                refreshToken = "old-refresh",
                userId = "old-user",
                nickName = "old-name"
            )
        )

        val rotatedSession = AuthSession(
            accessToken = "new-access",
            refreshToken = "new-refresh",
            userId = "new-user",
            nickName = "new-name"
        )
        dataSource.saveSession(rotatedSession)

        assertEquals(rotatedSession, dataSource.currentSession())
    }

    @Test
    fun `update user preserves tokens`() = runBlocking {
        val dataSource = createDataSource()
        dataSource.saveSession(
            AuthSession(
                accessToken = "access",
                refreshToken = "refresh",
                userId = "old-user",
                nickName = "old-name"
            )
        )

        dataSource.updateUser(userId = "new-user", nickName = "new-name")

        assertEquals(
            AuthSession(
                accessToken = "access",
                refreshToken = "refresh",
                userId = "new-user",
                nickName = "new-name"
            ),
            dataSource.currentSession()
        )
    }

    @Test
    fun `clear session removes all authentication data`() = runBlocking {
        val dataSource = createDataSource()
        dataSource.saveSession(
            AuthSession(
                accessToken = "access",
                refreshToken = "refresh",
                userId = "user",
                nickName = "name"
            )
        )

        dataSource.clearSession()

        val clearedSession = dataSource.currentSession()
        assertEquals(AuthSession(), clearedSession)
        assertFalse(clearedSession.hasRefreshToken)
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
