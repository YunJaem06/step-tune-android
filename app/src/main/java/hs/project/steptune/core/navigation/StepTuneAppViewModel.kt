package hs.project.steptune.core.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.core.auth.AuthSessionEvent
import hs.project.steptune.core.auth.AuthSessionEventBus
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow

@HiltViewModel
class StepTuneAppViewModel @Inject constructor(
    authSessionEventBus: AuthSessionEventBus
) : ViewModel() {
    val authSessionEvents: SharedFlow<AuthSessionEvent> = authSessionEventBus.events
}
