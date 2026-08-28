package hs.project.steptune.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import hs.project.steptune.R
import hs.project.steptune.core.util.PermissionUtils
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.feature.musicpreference.MusicPreferenceSelector
import hs.project.steptune.service.StepTrackingServiceController
import hs.project.steptune.ui.theme.StepTuneTheme
import kotlinx.coroutines.CancellationException

@Composable
fun SettingsRoute(
    onLoggedOut: () -> Unit
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(viewModel, onLoggedOut) {
        viewModel.events.collect { event ->
            if (event == SettingsEvent.LoggedOut) {
                try {
                    CredentialManager.create(context).clearCredentialState(
                        ClearCredentialStateRequest()
                    )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    // 로컬 세션은 이미 삭제됐으므로 로그인 화면 이동은 계속한다.
                }
                onLoggedOut()
            }
        }
    }

    SettingScreen(
        uiState = uiState,
        activityRecognitionGranted = PermissionUtils.hasActivityRecognitionPermission(context),
        notificationPermissionGranted = PermissionUtils.hasNotificationPermission(context),
        onDailyGoalChanged = viewModel::onDailyGoalChanged,
        onNicknameChanged = viewModel::onNicknameChanged,
        onCheckNickname = viewModel::checkNicknameAvailability,
        onUpdateNickname = viewModel::updateNickname,
        onStepLengthChanged = viewModel::onStepLengthChanged,
        onHeightChanged = viewModel::onHeightChanged,
        onWeightChanged = viewModel::onWeightChanged,
        onReminderNotificationsChanged = viewModel::onReminderNotificationsChanged,
        onGenreToggled = viewModel::onGenreToggled,
        onMoodToggled = viewModel::onMoodToggled,
        onAutoStartTrackingChanged = { enabled ->
            viewModel.onAutoStartTrackingChanged(enabled)
            if (enabled && PermissionUtils.hasTrackingPermissions(context)) {
                StepTrackingServiceController.start(context)
            } else if (!enabled) {
                StepTrackingServiceController.stop(context)
            }
        },
        onSaveProfile = viewModel::saveProfile,
        onSaveMusicPreferences = viewModel::saveMusicPreferences,
        onLogout = viewModel::logout,
        onShowDeleteAccountDialog = viewModel::showDeleteAccountDialog,
        onHideDeleteAccountDialog = viewModel::hideDeleteAccountDialog,
        onDeleteAccount = viewModel::deleteAccount,
        onOpenAppSettings = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
            context.startActivity(intent)
        }
    )
}

@Composable
fun SettingScreen(
    uiState: SettingsUiState,
    activityRecognitionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    onDailyGoalChanged: (String) -> Unit,
    onNicknameChanged: (String) -> Unit,
    onCheckNickname: () -> Unit,
    onUpdateNickname: () -> Unit,
    onStepLengthChanged: (String) -> Unit,
    onHeightChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onReminderNotificationsChanged: (Boolean) -> Unit,
    onGenreToggled: (MusicGenre) -> Unit,
    onMoodToggled: (MusicMood) -> Unit,
    onAutoStartTrackingChanged: (Boolean) -> Unit,
    onSaveProfile: () -> Unit,
    onSaveMusicPreferences: () -> Unit,
    onLogout: () -> Unit,
    onShowDeleteAccountDialog: () -> Unit,
    onHideDeleteAccountDialog: () -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.padding(horizontal = 24.dp))
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.settings_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_account_section),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = uiState.nickName.ifBlank {
                        stringResource(R.string.settings_account_loading)
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedTextField(
                    value = uiState.nicknameInput,
                    onValueChange = onNicknameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isCheckingNickname && !uiState.isUpdatingNickname,
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_nickname)) },
                    supportingText = {
                        Text(
                            stringResource(
                                R.string.settings_nickname_length,
                                uiState.nicknameInput.length
                            )
                        )
                    },
                    isError = uiState.nicknameValidationState == NicknameValidationState.INVALID ||
                        uiState.nicknameValidationState == NicknameValidationState.UNAVAILABLE ||
                        uiState.nicknameValidationState == NicknameValidationState.ERROR
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCheckNickname,
                        enabled = uiState.canCheckNickname,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (uiState.isCheckingNickname) {
                                stringResource(R.string.settings_nickname_checking)
                            } else {
                                stringResource(R.string.settings_nickname_check)
                            }
                        )
                    }
                    Button(
                        onClick = onUpdateNickname,
                        enabled = uiState.canUpdateNickname,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (uiState.isUpdatingNickname) {
                                stringResource(R.string.settings_nickname_updating)
                            } else {
                                stringResource(R.string.settings_nickname_update)
                            }
                        )
                    }
                }
                when (uiState.nicknameValidationState) {
                    NicknameValidationState.INVALID -> AccountStatusText(
                        text = stringResource(R.string.settings_nickname_invalid),
                        isError = true
                    )
                    NicknameValidationState.AVAILABLE -> AccountStatusText(
                        text = stringResource(R.string.settings_nickname_available),
                        isError = false
                    )
                    NicknameValidationState.UNAVAILABLE -> AccountStatusText(
                        text = stringResource(R.string.settings_nickname_unavailable),
                        isError = true
                    )
                    NicknameValidationState.ERROR -> AccountStatusText(
                        text = stringResource(R.string.settings_nickname_request_failed),
                        isError = true
                    )
                    NicknameValidationState.IDLE -> Unit
                }
                if (uiState.nicknameUpdated) {
                    AccountStatusText(
                        text = stringResource(R.string.settings_nickname_updated),
                        isError = false
                    )
                }
                OutlinedButton(
                    onClick = onLogout,
                    enabled = !uiState.isLoggingOut && !uiState.isDeletingAccount,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (uiState.isLoggingOut) {
                            stringResource(R.string.settings_logging_out)
                        } else {
                            stringResource(R.string.settings_logout)
                        }
                    )
                }
                TextButton(
                    onClick = onShowDeleteAccountDialog,
                    enabled = !uiState.isLoggingOut && !uiState.isDeletingAccount,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.settings_delete_account),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.settings_profile_section), style = MaterialTheme.typography.titleMedium)
                NumericSettingField(
                    label = stringResource(R.string.settings_daily_goal),
                    value = uiState.dailyGoalInput,
                    suffix = stringResource(R.string.settings_steps_suffix),
                    onValueChange = onDailyGoalChanged
                )
                NumericSettingField(
                    label = stringResource(R.string.settings_step_length),
                    value = uiState.stepLengthInput,
                    suffix = stringResource(R.string.settings_cm_suffix),
                    onValueChange = onStepLengthChanged
                )
                NumericSettingField(
                    label = stringResource(R.string.settings_height),
                    value = uiState.heightInput,
                    suffix = stringResource(R.string.settings_cm_suffix),
                    onValueChange = onHeightChanged
                )
                NumericSettingField(
                    label = stringResource(R.string.settings_weight),
                    value = uiState.weightInput,
                    suffix = stringResource(R.string.settings_kg_suffix),
                    onValueChange = onWeightChanged
                )
                if (uiState.hasInvalidNumberError) {
                    Text(
                        text = stringResource(R.string.settings_invalid_number),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (uiState.profileSaved) {
                    Text(
                        text = stringResource(R.string.settings_saved),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(
                    onClick = onSaveProfile,
                    enabled = uiState.canSaveProfile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (uiState.isSavingProfile) {
                            stringResource(R.string.settings_saving)
                        } else {
                            stringResource(R.string.settings_save_profile)
                        }
                    )
                }
            }
        }

        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_music_section),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.settings_music_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MusicPreferenceSelector(
                    uiState = uiState.musicPreferences,
                    onGenreToggled = onGenreToggled,
                    onMoodToggled = onMoodToggled,
                    enabled = !uiState.isSavingMusicPreferences
                )
                if (uiState.musicPreferencesSaved) {
                    Text(
                        text = stringResource(R.string.settings_music_preferences_saved),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (uiState.musicPreferencesSaveFailed) {
                    Text(
                        text = stringResource(R.string.settings_music_preferences_save_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(
                    onClick = onSaveMusicPreferences,
                    enabled = uiState.canSaveMusicPreferences,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (uiState.isSavingMusicPreferences) {
                            stringResource(R.string.settings_saving)
                        } else {
                            stringResource(R.string.settings_save_music_preferences)
                        }
                    )
                }
            }
        }

        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.settings_permission_section), style = MaterialTheme.typography.titleMedium)
                PermissionStatusRow(
                    title = stringResource(R.string.settings_activity_permission),
                    granted = activityRecognitionGranted
                )
                PermissionStatusRow(
                    title = stringResource(R.string.settings_notification_permission),
                    granted = notificationPermissionGranted
                )
                if (!activityRecognitionGranted || !notificationPermissionGranted) {
                    Button(
                        onClick = onOpenAppSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_open_app_settings))
                    }
                }
            }
        }

        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.settings_options_section), style = MaterialTheme.typography.titleMedium)
                ToggleSettingRow(
                    title = stringResource(R.string.settings_reminder_toggle),
                    subtitle = stringResource(R.string.settings_reminder_toggle_description),
                    checked = uiState.reminderNotificationsEnabled,
                    onCheckedChange = onReminderNotificationsChanged
                )
                ToggleSettingRow(
                    title = stringResource(R.string.settings_auto_start_toggle),
                    subtitle = stringResource(R.string.settings_auto_start_toggle_description),
                    checked = uiState.autoStartTrackingEnabled,
                    onCheckedChange = onAutoStartTrackingChanged
                )
            }
        }
    }

    if (uiState.showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = onHideDeleteAccountDialog,
            title = { Text(stringResource(R.string.settings_delete_account_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_delete_account_description))
                    if (uiState.deleteAccountFailed) {
                        Text(
                            text = stringResource(R.string.settings_delete_account_failed),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onDeleteAccount,
                    enabled = !uiState.isDeletingAccount
                ) {
                    Text(
                        text = if (uiState.isDeletingAccount) {
                            stringResource(R.string.settings_delete_account_deleting)
                        } else {
                            stringResource(R.string.settings_delete_account_confirm)
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onHideDeleteAccountDialog,
                    enabled = !uiState.isDeletingAccount
                ) {
                    Text(stringResource(R.string.settings_delete_account_cancel))
                }
            }
        )
    }
}

@Composable
private fun AccountStatusText(
    text: String,
    isError: Boolean
) {
    Text(
        text = text,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun NumericSettingField(
    label: String,
    value: String,
    suffix: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        suffix = { Text(suffix) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun PermissionStatusRow(
    title: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = if (granted) stringResource(R.string.settings_permission_granted) else stringResource(R.string.settings_permission_required),
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingScreenPreview() {
    StepTuneTheme {
        SettingScreen(
            uiState = SettingsUiState(
                dailyGoalInput = "10000",
                stepLengthInput = "72",
                heightInput = "170",
                weightInput = "65",
                nickName = "스텝러너12345678",
                nicknameInput = "스텝러너12345678",
                reminderNotificationsEnabled = true,
                autoStartTrackingEnabled = true,
                isLoading = false,
                profileSaved = true
            ),
            activityRecognitionGranted = true,
            notificationPermissionGranted = false,
            onDailyGoalChanged = {},
            onNicknameChanged = {},
            onCheckNickname = {},
            onUpdateNickname = {},
            onStepLengthChanged = {},
            onHeightChanged = {},
            onWeightChanged = {},
            onReminderNotificationsChanged = {},
            onGenreToggled = {},
            onMoodToggled = {},
            onAutoStartTrackingChanged = {},
            onSaveProfile = {},
            onSaveMusicPreferences = {},
            onLogout = {},
            onShowDeleteAccountDialog = {},
            onHideDeleteAccountDialog = {},
            onDeleteAccount = {},
            onOpenAppSettings = {}
        )
    }
}



