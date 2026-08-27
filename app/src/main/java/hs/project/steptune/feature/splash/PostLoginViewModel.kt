package hs.project.steptune.feature.splash

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.domain.model.UserPreferences
import hs.project.steptune.domain.usecase.ObserveUserPreferencesUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class PostLoginViewModel @Inject constructor(
    observeUserPreferencesUseCase: ObserveUserPreferencesUseCase
) : ViewModel() {
    val preferences: Flow<UserPreferences> = observeUserPreferencesUseCase()
}
