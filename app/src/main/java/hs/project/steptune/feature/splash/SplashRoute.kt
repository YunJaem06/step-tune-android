package hs.project.steptune.feature.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hs.project.steptune.R

@Composable
fun SplashRoute(
    onNavigateToLogin: () -> Unit,
    onNavigateToPostLogin: () -> Unit
) {
    val viewModel: SplashViewModel = hiltViewModel()
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(uiState.destination) {
        when (uiState.destination) {
            SplashDestination.Login -> onNavigateToLogin()
            SplashDestination.PostLogin -> onNavigateToPostLogin()
            null -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium
        )
        if (uiState.hasConnectionError) {
            Text(
                text = stringResource(R.string.splash_auto_login_failed),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = viewModel::retry) {
                Text(stringResource(R.string.common_retry))
            }
        } else {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.splash_checking_login),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
