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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hs.project.steptune.core.util.PermissionUtils
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext

@Composable
fun SplashRoute(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val onboardingCompleted = viewModel.onboardingCompleted.collectAsStateWithLifecycle(initialValue = false).value
    val hasActivityPermission = PermissionUtils.hasActivityRecognitionPermission(context)
    val hasNotificationPermission = PermissionUtils.hasNotificationPermission(context)

    LaunchedEffect(onboardingCompleted, hasActivityPermission, hasNotificationPermission) {
        delay(400)
        if (onboardingCompleted && hasActivityPermission && hasNotificationPermission) {
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

