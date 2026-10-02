package hs.project.steptune.core.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class AuthSessionEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<AuthSessionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthSessionEvent> = _events.asSharedFlow()

    fun notifySessionExpired() {
        _events.tryEmit(AuthSessionEvent.SessionExpired)
    }
}

sealed interface AuthSessionEvent {
    data object SessionExpired : AuthSessionEvent
}
