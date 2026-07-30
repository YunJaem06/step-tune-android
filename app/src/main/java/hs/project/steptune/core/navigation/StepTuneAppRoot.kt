package hs.project.steptune.core.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import hs.project.steptune.feature.home.HomeRoute
import hs.project.steptune.feature.onboarding.OnboardingRoute
import hs.project.steptune.feature.stats.StatsRoute
import hs.project.steptune.feature.settings.SettingsRoute
import hs.project.steptune.feature.splash.SplashRoute

@Composable
fun StepTuneAppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoutes = currentDestination?.hierarchy?.mapNotNull { it.route }?.toSet().orEmpty()
    val homeRoute = TopLevelDestination.Progress.route
    val isHomeDestination = homeRoute in currentRoutes
    val isMainDestination = TopLevelDestination.items.any { destination ->
        destination.route in currentRoutes
    }

    BackHandler(enabled = isMainDestination && !isHomeDestination) {
        navController.navigate(homeRoute) {
            popUpTo(homeRoute)
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (isMainDestination) {
                NavigationBar {
                    TopLevelDestination.items.forEach { destination ->
                        val selected = destination.route in currentRoutes
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        popUpTo(homeRoute) {
                                            this.saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {},
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Splash.route) {
                SplashRoute(
                    onNavigateToOnboarding = {
                        navController.navigate(AppDestination.Onboarding.route) {
                            popUpTo(AppDestination.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToMain = {
                        navController.navigate(homeRoute) {
                            popUpTo(AppDestination.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(AppDestination.Onboarding.route) {
                OnboardingRoute(
                    onFinished = {
                        navController.navigate(homeRoute) {
                            popUpTo(AppDestination.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(TopLevelDestination.Progress.route) {
                HomeRoute()
            }
            composable(TopLevelDestination.Stats.route) {
                StatsRoute()
            }
            composable(TopLevelDestination.Settings.route) {
                SettingsRoute()
            }
        }
    }
}
