package hs.project.steptune.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.api.UnauthorizedException
import hs.project.steptune.domain.usecase.GetCurrentAuthSessionUseCase
import hs.project.steptune.domain.usecase.RefreshAuthSessionUseCase
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getCurrentAuthSessionUseCase: GetCurrentAuthSessionUseCase,
    private val refreshAuthSessionUseCase: RefreshAuthSessionUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        attemptAutoLogin()
    }

    fun retry() {
        if (!_uiState.value.hasConnectionError) return
        attemptAutoLogin()
    }

    private fun attemptAutoLogin() {
        _uiState.update { SplashUiState() }
        viewModelScope.launch {
            try {
                val currentSession = getCurrentAuthSessionUseCase()
                val destination = if (currentSession.hasRefreshToken) {
                    refreshAuthSessionUseCase()
                    SplashDestination.PostLogin
                } else {
                    SplashDestination.Login
                }
                _uiState.update { it.copy(destination = destination) }
            } catch (_: UnauthorizedException) {
                _uiState.update { it.copy(destination = SplashDestination.Login) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { it.copy(hasConnectionError = true) }
            }
        }
    }
}
