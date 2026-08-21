package hs.project.steptune.core.navigation

sealed class AppDestination(val route: String) {
    data object Splash : AppDestination("splash")
    data object Login : AppDestination("login")
    data object PostLogin : AppDestination("post_login")
    data object Onboarding : AppDestination("onboarding")
}
