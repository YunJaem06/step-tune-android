package hs.project.steptune.feature.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hs.project.steptune.core.util.PermissionUtils
import hs.project.steptune.service.StepTrackingServiceController
import kotlinx.coroutines.delay

@Composable
fun SplashRoute(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val viewModel: SplashViewModel = hiltViewModel()
    val context = LocalContext.current
    val preferences = viewModel.preferences.collectAsStateWithLifecycle(initialValue = null).value
    val hasActivityPermission = PermissionUtils.hasActivityRecognitionPermission(context)
    val hasNotificationPermission = PermissionUtils.hasNotificationPermission(context)

    LaunchedEffect(preferences, hasActivityPermission, hasNotificationPermission) {
        val currentPreferences = preferences ?: return@LaunchedEffect
        delay(400)
        if (
            currentPreferences.onboardingCompleted &&
            currentPreferences.musicPreferencesOnboardingCompleted &&
            hasActivityPermission &&
            hasNotificationPermission
        ) {
            if (currentPreferences.autoStartTrackingEnabled) {
                StepTrackingServiceController.start(context)
            }
            onNavigateToMain()
        } else {
            onNavigateToOnboarding()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "StepTune",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
