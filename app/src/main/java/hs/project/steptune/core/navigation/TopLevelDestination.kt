package hs.project.steptune.core.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import hs.project.steptune.R

sealed class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int
) {
    data object Progress : TopLevelDestination(
        route = "progress",
        labelRes = R.string.navigation_today,
        iconRes = R.drawable.ic_nav_today
    )

    data object Stats : TopLevelDestination(
        route = "stats",
        labelRes = R.string.navigation_stats,
        iconRes = R.drawable.ic_nav_stats
    )

    data object Settings : TopLevelDestination(
        route = "settings",
        labelRes = R.string.navigation_settings,
        iconRes = R.drawable.ic_nav_settings
    )

    companion object {
        val items: List<TopLevelDestination>
            get() = listOf(Progress, Stats, Settings)
    }
}
