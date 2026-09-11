package hs.project.steptune.feature.recommendation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hs.project.steptune.R
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.MusicRecommendation
import hs.project.steptune.domain.model.RecommendationActivityLevel
import hs.project.steptune.feature.musicpreference.MusicPreferenceSelector

@Composable
fun MusicRecommendationRoute(
    onBack: () -> Unit
) {
    val viewModel: MusicRecommendationViewModel = hiltViewModel()
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current

    MusicRecommendationScreen(
        uiState = uiState,
        onBack = onBack,
        onGenreToggled = viewModel::onGenreToggled,
        onMoodToggled = viewModel::onMoodToggled,
        onDurationSelected = viewModel::onDurationSelected,
        onGenerate = viewModel::generateRecommendation,
        onRequestAnother = viewModel::requestAnotherRecommendation,
        onOpenSearch = { searchQuery ->
            val intent = Intent(Intent.ACTION_VIEW, searchQuery.toYoutubeSearchUri())
            runCatching { context.startActivity(intent) }
        }
    )
}

@Composable
fun MusicRecommendationScreen(
    uiState: MusicRecommendationUiState,
    onBack: () -> Unit,
    onGenreToggled: (MusicGenre) -> Unit,
    onMoodToggled: (MusicMood) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onGenerate: () -> Unit,
    onRequestAnother: () -> Unit,
    onOpenSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        RecommendationTopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (uiState.recommendation == null) {
                RecommendationForm(
                    uiState = uiState,
                    onGenreToggled = onGenreToggled,
                    onMoodToggled = onMoodToggled,
                    onDurationSelected = onDurationSelected,
                    onGenerate = onGenerate
                )
            } else {
                RecommendationResult(
                    recommendation = uiState.recommendation,
                    onOpenSearch = onOpenSearch,
                    onRequestAnother = onRequestAnother
                )
            }
        }
    }
}

@Composable
private fun RecommendationTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.recommendation_back)
            )
        }
        Text(
            text = stringResource(R.string.recommendation_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecommendationForm(
    uiState: MusicRecommendationUiState,
    onGenreToggled: (MusicGenre) -> Unit,
    onMoodToggled: (MusicMood) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onGenerate: () -> Unit
) {
    Text(
        text = stringResource(R.string.recommendation_form_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = stringResource(R.string.recommendation_form_description),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (uiState.isLoadingPreferences) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            MusicPreferenceSelector(
                uiState = uiState.preferences,
                onGenreToggled = onGenreToggled,
                onMoodToggled = onMoodToggled,
                modifier = Modifier.padding(20.dp),
                enabled = !uiState.isGenerating
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.recommendation_duration_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MusicRecommendationUiState.DURATION_OPTIONS.forEach { duration ->
                FilterChip(
                    selected = uiState.durationMinutes == duration,
                    enabled = !uiState.isGenerating,
                    onClick = { onDurationSelected(duration) },
                    label = {
                        Text(stringResource(R.string.recommendation_minutes_format, duration))
                    }
                )
            }
        }
    }

    uiState.error?.let { error -> RecommendationErrorCard(error) }

    Button(
        onClick = onGenerate,
        enabled = uiState.canGenerate,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (uiState.isGenerating) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Text(
                text = stringResource(R.string.recommendation_generating),
                modifier = Modifier.padding(start = 10.dp)
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.recommendation_generate),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun RecommendationErrorCard(error: MusicRecommendationError) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = stringResource(error.messageResource),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun RecommendationResult(
    recommendation: MusicRecommendation,
    onOpenSearch: (String) -> Unit,
    onRequestAnother: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music_note),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(14.dp).size(28.dp)
        )
    }
    Text(
        text = stringResource(R.string.recommendation_result_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = stringResource(R.string.recommendation_result_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(13.dp).size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.recommendation_track_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                )
                Text(
                    text = recommendation.track.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = recommendation.track.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.recommendation_reason_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = recommendation.reason,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ResultInfoRow(
                label = stringResource(R.string.recommendation_activity_level),
                value = stringResource(recommendation.activityLevel.labelResource)
            )
            ResultInfoRow(
                label = stringResource(R.string.recommendation_today_steps),
                value = stringResource(
                    R.string.steps_format,
                    recommendation.stepSummary.todayStepCount
                )
            )
            ResultInfoRow(
                label = stringResource(R.string.recommendation_recent_average),
                value = stringResource(
                    R.string.steps_format,
                    recommendation.stepSummary.recent7DayAverage.toInt()
                )
            )
            ResultInfoRow(
                label = stringResource(R.string.recommendation_duration_title),
                value = stringResource(
                    R.string.recommendation_minutes_format,
                    recommendation.durationMinutes
                )
            )
        }
    }

    Button(
        onClick = { onOpenSearch(recommendation.track.searchQuery) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music_note),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(R.string.recommendation_open_youtube),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
    Text(
        text = stringResource(R.string.recommendation_track_search_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedButton(
        onClick = onRequestAnother,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(stringResource(R.string.recommendation_request_another))
    }
}

@Composable
private fun ResultInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun String.toYoutubeSearchUri(): Uri = Uri.parse("https://www.youtube.com/results")
    .buildUpon()
    .appendQueryParameter("search_query", this)
    .build()

private val MusicRecommendationError.messageResource: Int
    get() = when (this) {
        MusicRecommendationError.TODAY_RECORD_NOT_FOUND ->
            R.string.recommendation_error_today_record
        MusicRecommendationError.RATE_LIMIT -> R.string.recommendation_error_rate_limit
        MusicRecommendationError.SERVICE_UNAVAILABLE ->
            R.string.recommendation_error_service_unavailable
        MusicRecommendationError.INVALID_RESPONSE ->
            R.string.recommendation_error_invalid_response
        MusicRecommendationError.REQUEST_FAILED -> R.string.recommendation_error_request_failed
        MusicRecommendationError.NETWORK -> R.string.recommendation_error_network
    }

private val RecommendationActivityLevel.labelResource: Int
    get() = when (this) {
        RecommendationActivityLevel.LOW -> R.string.recommendation_activity_low
        RecommendationActivityLevel.MODERATE -> R.string.recommendation_activity_moderate
        RecommendationActivityLevel.HIGH -> R.string.recommendation_activity_high
    }
