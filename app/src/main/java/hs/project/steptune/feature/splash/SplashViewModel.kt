package hs.project.steptune.feature.splash

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.domain.model.UserPreferences
import hs.project.steptune.domain.usecase.ObserveUserPreferencesUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    observeUserPreferencesUseCase: ObserveUserPreferencesUseCase
) : ViewModel() {

    val preferences: Flow<UserPreferences> = observeUserPreferencesUseCase()
}
