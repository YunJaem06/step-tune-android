package hs.project.steptune.feature.auth

import hs.project.steptune.domain.error.UnauthorizedException
import hs.project.steptune.domain.model.AuthSession
import hs.project.steptune.domain.model.NicknameAvailability
import hs.project.steptune.domain.repository.AuthRepository
import hs.project.steptune.domain.usecase.GetCurrentAuthSessionUseCase
import hs.project.steptune.domain.usecase.LoginWithGoogleUseCase
import hs.project.steptune.domain.usecase.RefreshAuthSessionUseCase
import hs.project.steptune.feature.login.LoginEvent
import hs.project.steptune.feature.login.LoginViewModel
import hs.project.steptune.feature.splash.SplashDestination
import hs.project.steptune.feature.splash.SplashViewModel
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthStartupRegressionTest {
    private val repository = StartupAuthRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `first launch without refresh token goes to login`() = runTest {
        val viewModel = createSplash()
        runCurrent()
        assertEquals(SplashDestination.Login, viewModel.uiState.value.destination)
        assertEquals(0, repository.refreshCalls)
    }

    @Test
    fun `saved refresh token completes auto login before post login navigation`() = runTest {
        repository.session = savedSession()
        val viewModel = createSplash()
        assertNull(viewModel.uiState.value.destination)
        runCurrent()
        assertEquals(1, repository.refreshCalls)
        assertEquals(SplashDestination.PostLogin, viewModel.uiState.value.destination)
        assertEquals("new-refresh", repository.session.refreshToken)
    }

    @Test
    fun `rejected refresh moves to login`() = runTest {
        repository.session = savedSession()
        repository.onRefresh = { throw UnauthorizedException() }
        val viewModel = createSplash()
        runCurrent()
        assertEquals(SplashDestination.Login, viewModel.uiState.value.destination)
        assertFalse(viewModel.uiState.value.hasConnectionError)
    }

    @Test
    fun `network failure remains retryable without losing saved session`() = runTest {
        repository.session = savedSession()
        repository.onRefresh = { throw IOException("offline") }
        val viewModel = createSplash()
        runCurrent()
        assertTrue(viewModel.uiState.value.hasConnectionError)
        assertNull(viewModel.uiState.value.destination)
        assertEquals(savedSession(), repository.session)
        repository.onRefresh = { savedSession().copy(accessToken = "new-access", refreshToken = "new-refresh") }
        viewModel.retry()
        viewModel.retry()
        runCurrent()
        assertEquals(2, repository.refreshCalls)
        assertEquals(SplashDestination.PostLogin, viewModel.uiState.value.destination)
    }

    @Test
    fun `retry before a connection error does not duplicate auto login`() = runTest {
        repository.session = savedSession()
        val viewModel = createSplash()
        viewModel.retry()
        runCurrent()
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `Google token login ends loading and emits success`() = runTest {
        val viewModel = LoginViewModel(LoginWithGoogleUseCase(repository))
        val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }
        viewModel.onCredentialRequestStarted()
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.loginWithGoogle("google-id-token")
        runCurrent()
        assertEquals("google-id-token", repository.lastGoogleToken)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(LoginEvent.LoginSucceeded, event.await())
    }

    @Test
    fun `Google server failure ends loading and emits failure`() = runTest {
        repository.loginFailure = IOException("offline")
        val viewModel = LoginViewModel(LoginWithGoogleUseCase(repository))
        val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }
        viewModel.onCredentialRequestStarted()
        viewModel.loginWithGoogle("google-id-token")
        runCurrent()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(LoginEvent.LoginFailed, event.await())
        assertEquals(AuthSession(), repository.session)
    }

    @Test
    fun `canceling credential selection allows trying again without server login`() = runTest {
        val viewModel = LoginViewModel(LoginWithGoogleUseCase(repository))
        viewModel.onCredentialRequestStarted()
        viewModel.onCredentialRequestFailed()
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(repository.lastGoogleToken)
    }

    private fun createSplash() = SplashViewModel(
        GetCurrentAuthSessionUseCase(repository), RefreshAuthSessionUseCase(repository)
    )

    private fun savedSession() = AuthSession("old-access", "old-refresh", "1", "name")
}

private class StartupAuthRepository : AuthRepository {
    var session = AuthSession()
    var refreshCalls = 0
    var lastGoogleToken: String? = null
    var loginFailure: Exception? = null
    var onRefresh: suspend () -> AuthSession = {
        session.copy(accessToken = "new-access", refreshToken = "new-refresh")
    }
    override fun observeSession(): Flow<AuthSession> = flowOf(session)
    override suspend fun getCurrentSession(): AuthSession = session
    override suspend fun refreshSession(): AuthSession {
        refreshCalls++
        return onRefresh().also { session = it }
    }
    override suspend fun loginWithGoogle(idToken: String): AuthSession {
        lastGoogleToken = idToken
        loginFailure?.let { throw it }
        return AuthSession("access", "refresh", "1", "name").also { session = it }
    }
    override suspend fun syncCurrentUser(): AuthSession = session
    override suspend fun checkNicknameAvailability(nickName: String): NicknameAvailability = error("Not used")
    override suspend fun updateNickname(nickName: String): AuthSession = error("Not used")
    override suspend fun deleteAccount() = error("Not used")
    override suspend fun logout() = error("Not used")
    override suspend fun clearSession() { session = AuthSession() }
}
