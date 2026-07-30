package hs.project.steptune.core.navigation

sealed class TopLevelDestination(
    val route: String,
    val label: String
) {
    data object Progress : TopLevelDestination("progress", "Progress")
    data object Stats : TopLevelDestination("stats", "Stats")
    data object Settings : TopLevelDestination("settings", "Settings")

    companion object {
        val items = listOf(Progress, Stats, Settings)
    }
}
