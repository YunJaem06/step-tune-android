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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hs.project.steptune.R
import hs.project.steptune.core.util.PermissionUtils
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.feature.musicpreference.MusicPreferenceSelector
import hs.project.steptune.service.StepTrackingServiceController
import hs.project.steptune.ui.theme.StepTuneTheme

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit
) {
    val viewModel: OnboardingViewModel = hiltViewModel()
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

    when (uiState.step) {
        OnboardingStep.PERMISSIONS -> PermissionOnboardingScreen(
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
                viewModel.continueFromPermissions {
                    if (uiState.autoStartTrackingEnabled) {
                        StepTrackingServiceController.start(context)
                    }
                    onFinished()
                }
            }
        )

        OnboardingStep.MUSIC_PREFERENCES -> MusicPreferencesOnboardingScreen(
            uiState = uiState,
            onGenreToggled = viewModel::toggleGenre,
            onMoodToggled = viewModel::toggleMood,
            onPrevious = viewModel::showPermissions,
            onComplete = {
                viewModel.completeOnboarding {
                    if (uiState.autoStartTrackingEnabled) {
                        StepTrackingServiceController.start(context)
                    }
                    onFinished()
                }
            },
            onSkip = {
                viewModel.skipMusicPreferences {
                    if (uiState.autoStartTrackingEnabled) {
                        StepTrackingServiceController.start(context)
                    }
                    onFinished()
                }
            }
        )
    }
}

@Composable
fun PermissionOnboardingScreen(
    uiState: OnboardingUiState,
    onRequestActivityRecognition: () -> Unit,
    onRequestNotifications: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.onboarding_permission_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PermissionCard(
            title = stringResource(R.string.onboarding_activity_permission),
            granted = uiState.activityRecognitionGranted,
            actionLabel = stringResource(R.string.onboarding_grant_permission),
            onAction = onRequestActivityRecognition
        )

        PermissionCard(
            title = stringResource(R.string.onboarding_notification_permission),
            granted = uiState.notificationGranted,
            actionLabel = stringResource(R.string.onboarding_allow_notifications),
            onAction = onRequestNotifications
        )

        if (uiState.saveFailed) {
            Text(
                text = stringResource(R.string.onboarding_save_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onContinue,
            enabled = uiState.allRequiredGranted && !uiState.isLoading && !uiState.saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (uiState.saving) {
                    stringResource(R.string.onboarding_saving)
                } else {
                    stringResource(R.string.onboarding_continue)
                }
            )
        }
    }
}

@Composable
fun MusicPreferencesOnboardingScreen(
    uiState: OnboardingUiState,
    onGenreToggled: (MusicGenre) -> Unit,
    onMoodToggled: (MusicMood) -> Unit,
    onPrevious: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_music_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.onboarding_music_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            MusicPreferenceSelector(
                uiState = uiState.musicPreferences,
                onGenreToggled = onGenreToggled,
                onMoodToggled = onMoodToggled,
                enabled = !uiState.saving
            )
        }

        if (uiState.saveFailed) {
            Text(
                text = stringResource(R.string.onboarding_save_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onComplete,
            enabled = !uiState.isLoading && !uiState.saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (uiState.saving) {
                    stringResource(R.string.onboarding_saving)
                } else {
                    stringResource(R.string.onboarding_complete)
                }
            )
        }
        OutlinedButton(
            onClick = onSkip,
            enabled = !uiState.isLoading && !uiState.saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.onboarding_skip))
        }
        TextButton(
            onClick = onPrevious,
            enabled = !uiState.saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.onboarding_previous))
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
            Text(
                if (granted) {
                    stringResource(R.string.onboarding_permission_granted)
                } else {
                    stringResource(R.string.onboarding_permission_required)
                }
            )
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
private fun PermissionOnboardingScreenPreview() {
    StepTuneTheme {
        PermissionOnboardingScreen(
            uiState = OnboardingUiState(isLoading = false),
            onRequestActivityRecognition = {},
            onRequestNotifications = {},
            onContinue = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MusicPreferencesOnboardingScreenPreview() {
    StepTuneTheme {
        MusicPreferencesOnboardingScreen(
            uiState = OnboardingUiState(
                step = OnboardingStep.MUSIC_PREFERENCES,
                isLoading = false
            ),
            onGenreToggled = {},
            onMoodToggled = {},
            onPrevious = {},
            onComplete = {},
            onSkip = {}
        )
    }
}
