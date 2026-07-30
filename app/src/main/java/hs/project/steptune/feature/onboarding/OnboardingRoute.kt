package hs.project.steptune.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hs.project.steptune.core.util.PermissionUtils
import hs.project.steptune.ui.theme.StepTuneTheme

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.updatePermissionState(
            activityRecognitionGranted = PermissionUtils.hasActivityRecognitionPermission(context),
            notificationGranted = PermissionUtils.hasNotificationPermission(context)
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.updatePermissionState(
            activityRecognitionGranted = PermissionUtils.hasActivityRecognitionPermission(context),
            notificationGranted = PermissionUtils.hasNotificationPermission(context)
        )
    }

    LaunchedEffect(Unit) {
        viewModel.updatePermissionState(
            activityRecognitionGranted = PermissionUtils.hasActivityRecognitionPermission(context),
            notificationGranted = PermissionUtils.hasNotificationPermission(context)
        )
    }

    OnboardingScreen(
        uiState = uiState,
        onRequestActivityRecognition = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        },
        onRequestNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onContinue = {
            viewModel.completeOnboarding(onFinished)
        }
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onRequestActivityRecognition: () -> Unit,
    onRequestNotifications: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Welcome", style = MaterialTheme.typography.headlineMedium)
        Text("To count steps reliably, the app needs a couple of permissions before entering the main screen.")

        PermissionCard(
            title = "Activity recognition",
            granted = uiState.activityRecognitionGranted,
            actionLabel = "Grant permission",
            onAction = onRequestActivityRecognition
        )

        PermissionCard(
            title = "Notifications",
            granted = uiState.notificationGranted,
            actionLabel = "Allow notifications",
            onAction = onRequestNotifications
        )

        Button(
            onClick = onContinue,
            enabled = uiState.allRequiredGranted && !uiState.saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.saving) "Preparing..." else "Continue")
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(if (granted) "Granted" else "Required before entering the app")
            if (!granted) {
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    StepTuneTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(),
            onRequestActivityRecognition = {},
            onRequestNotifications = {},
            onContinue = {}
        )
    }
}

