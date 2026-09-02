package hs.project.steptune.core.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import hs.project.steptune.core.auth.AuthSessionEvent
import hs.project.steptune.feature.home.HomeRoute
import hs.project.steptune.feature.login.LoginRoute
import hs.project.steptune.feature.onboarding.OnboardingRoute
import hs.project.steptune.feature.settings.SettingsRoute
import hs.project.steptune.feature.splash.PostLoginRoute
import hs.project.steptune.feature.splash.SplashRoute
import hs.project.steptune.feature.stats.StatsRoute
import hs.project.steptune.service.StepTrackingServiceController

@Composable
fun StepTuneAppRoot() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val viewModel: StepTuneAppViewModel = hiltViewModel()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoutes = currentDestination?.hierarchy?.mapNotNull { it.route }?.toSet().orEmpty()
    val homeRoute = TopLevelDestination.Progress.route
    val isHomeDestination = homeRoute in currentRoutes
    val isMainDestination = TopLevelDestination.items.any { destination ->
        destination.route in currentRoutes
    }

    LaunchedEffect(viewModel, navController) {
        viewModel.authSessionEvents.collect { event ->
            if (event == AuthSessionEvent.SessionExpired) {
                StepTrackingServiceController.stop(context)
                navController.navigate(AppDestination.Login.route) {
                    popUpTo(homeRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    BackHandler(enabled = isMainDestination && !isHomeDestination) {
        navController.navigate(homeRoute) {
            popUpTo(homeRoute)
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isMainDestination) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    TopLevelDestination.items.forEach { destination ->
                        val selected = destination.route in currentRoutes
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        popUpTo(homeRoute) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(destination.iconRes),
                                    contentDescription = stringResource(destination.labelRes)
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    onNavigateToLogin = {
                        StepTrackingServiceController.stop(context)
                        navController.navigate(AppDestination.Login.route) {
                            popUpTo(AppDestination.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToPostLogin = {
                        navController.navigate(AppDestination.PostLogin.route) {
                            popUpTo(AppDestination.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(AppDestination.Login.route) {
                LoginRoute(
                    onLoginSucceeded = {
                        navController.navigate(AppDestination.PostLogin.route) {
                            popUpTo(AppDestination.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(AppDestination.PostLogin.route) {
                PostLoginRoute(
                    onNavigateToOnboarding = {
                        navController.navigate(AppDestination.Onboarding.route) {
                            popUpTo(AppDestination.PostLogin.route) { inclusive = true }
                        }
                    },
                    onNavigateToMain = {
                        navController.navigate(homeRoute) {
                            popUpTo(AppDestination.PostLogin.route) { inclusive = true }
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
                SettingsRoute(
                    onLoggedOut = {
                        navController.navigate(AppDestination.Login.route) {
                            popUpTo(homeRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
