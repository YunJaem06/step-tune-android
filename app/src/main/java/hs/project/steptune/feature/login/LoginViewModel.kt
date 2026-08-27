package hs.project.steptune.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.domain.usecase.LoginWithGoogleUseCase
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    fun onCredentialRequestStarted() {
        _uiState.update { it.copy(isLoading = true) }
    }

    fun onCredentialRequestFailed() {
        _uiState.update { it.copy(isLoading = false) }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                loginWithGoogleUseCase(idToken)
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(LoginEvent.LoginSucceeded)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(LoginEvent.LoginFailed)
            }
        }
    }
}

sealed interface LoginEvent {
    data object LoginSucceeded : LoginEvent

    data object LoginFailed : LoginEvent
}
