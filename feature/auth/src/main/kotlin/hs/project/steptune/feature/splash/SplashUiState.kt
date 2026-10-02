package hs.project.steptune.feature.splash

import androidx.compose.runtime.Immutable

@Immutable
data class SplashUiState(
    val destination: SplashDestination? = null,
    val hasConnectionError: Boolean = false
)

enum class SplashDestination {
    Login,
    PostLogin
}
