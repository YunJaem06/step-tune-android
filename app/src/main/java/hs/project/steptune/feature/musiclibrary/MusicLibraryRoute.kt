package hs.project.steptune.feature.musiclibrary

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hs.project.steptune.R
import hs.project.steptune.domain.model.MusicRecommendation

@Composable
fun MusicLibraryRoute(
    onRecommendationClick: () -> Unit
) {
    val viewModel: MusicLibraryViewModel = hiltViewModel()
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.retry()
    }

    MusicLibraryScreen(
        uiState = uiState,
        onFilterSelected = viewModel::selectFilter,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        onFavoriteToggle = viewModel::toggleFavorite,
        onDeleteRequested = viewModel::requestDelete,
        onDeleteDismissed = viewModel::dismissDelete,
        onDeleteConfirmed = viewModel::confirmDelete,
        onRecommendationClick = onRecommendationClick,
        onOpenSearch = { query ->
            val intent = Intent(Intent.ACTION_VIEW, query.toYoutubeSearchUri())
            runCatching { context.startActivity(intent) }
        }
    )
}

@Composable
fun MusicLibraryScreen(
    uiState: MusicLibraryUiState,
    onFilterSelected: (MusicLibraryFilter) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onFavoriteToggle: (MusicRecommendation) -> Unit,
    onDeleteRequested: (MusicRecommendation) -> Unit,
    onDeleteDismissed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onRecommendationClick: () -> Unit,
    onOpenSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    uiState.deleteConfirmation?.let { recommendation ->
        DeleteRecommendationDialog(
            recommendation = recommendation,
            isDeleting = uiState.pendingDeleteId == recommendation.recommendationId,
            onDismiss = onDeleteDismissed,
            onConfirm = onDeleteConfirmed
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.music_library_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.music_library_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MusicLibraryFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        enabled = uiState.pendingFavoriteId == null &&
                            uiState.pendingDeleteId == null,
                        onClick = { onFilterSelected(filter) },
                        label = {
                            Text(
                                stringResource(
                                    if (filter == MusicLibraryFilter.ALL) {
                                        R.string.music_library_filter_all
                                    } else {
                                        R.string.music_library_filter_favorites
                                    }
                                )
                            )
                        }
                    )
                }
            }
        }

        if (!uiState.isLoading && uiState.error == null) {
            item {
                Text(
                    text = stringResource(
                        if (uiState.favoriteOnly) {
                            R.string.music_library_favorite_count
                        } else {
                            R.string.music_library_total_count
                        },
                        uiState.totalElements
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            uiState.error?.let { error ->
                item {
                    MusicLibraryErrorCard(error = error, onRetry = onRetry)
                }
            }

            if (uiState.recommendations.isEmpty() && uiState.error == null) {
                item {
                    EmptyMusicLibrary(
                        favoriteOnly = uiState.favoriteOnly,
                        onRecommendationClick = onRecommendationClick
                    )
                }
            }

            items(
                items = uiState.recommendations,
                key = MusicRecommendation::recommendationId
            ) { recommendation ->
                MusicRecommendationHistoryCard(
                    recommendation = recommendation,
                    isUpdatingFavorite = uiState.pendingFavoriteId ==
                        recommendation.recommendationId,
                    isDeleting = uiState.pendingDeleteId == recommendation.recommendationId,
                    onOpenSearch = { onOpenSearch(recommendation.track.searchQuery) },
                    onFavoriteToggle = { onFavoriteToggle(recommendation) },
                    onDelete = { onDeleteRequested(recommendation) }
                )
            }

            if (uiState.hasNext || uiState.isLoadingMore) {
                item {
                    OutlinedButton(
                        onClick = onLoadMore,
                        enabled = !uiState.isLoadingMore,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (uiState.isLoadingMore) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = stringResource(R.string.music_library_loading_more),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        } else {
                            Text(stringResource(R.string.music_library_load_more))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicRecommendationHistoryCard(
    recommendation: MusicRecommendation,
    isUpdatingFavorite: Boolean,
    isDeleting: Boolean,
    onOpenSearch: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(11.dp).size(22.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = recommendation.track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = recommendation.track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onFavoriteToggle,
                    enabled = !isUpdatingFavorite && !isDeleting
                ) {
                    if (isUpdatingFavorite) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                if (recommendation.favorite) {
                                    R.drawable.ic_favorite
                                } else {
                                    R.drawable.ic_favorite_border
                                }
                            ),
                            contentDescription = stringResource(
                                if (recommendation.favorite) {
                                    R.string.recommendation_remove_favorite
                                } else {
                                    R.string.recommendation_add_favorite
                                }
                            ),
                            tint = if (recommendation.favorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    enabled = !isDeleting && !isUpdatingFavorite
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.music_library_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = recommendation.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecommendationMetadata(
                    text = recommendation.recordDate,
                    modifier = Modifier.weight(1f)
                )
                RecommendationMetadata(
                    text = stringResource(
                        R.string.recommendation_minutes_format,
                        recommendation.durationMinutes
                    ),
                    modifier = Modifier.weight(1f)
                )
                RecommendationMetadata(
                    text = stringResource(
                        R.string.steps_format,
                        recommendation.stepSummary.todayStepCount
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = onOpenSearch,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play),
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = stringResource(R.string.music_library_open_youtube),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun RecommendationMetadata(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyMusicLibrary(
    favoriteOnly: Boolean,
    onRecommendationClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (favoriteOnly) R.drawable.ic_favorite_border else R.drawable.ic_music_note
                ),
                contentDescription = null,
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(
                    if (favoriteOnly) {
                        R.string.music_library_favorites_empty_title
                    } else {
                        R.string.music_library_empty_title
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    if (favoriteOnly) {
                        R.string.music_library_favorites_empty_description
                    } else {
                        R.string.music_library_empty_description
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!favoriteOnly) {
                Button(onClick = onRecommendationClick) {
                    Text(stringResource(R.string.music_library_request_recommendation))
                }
            }
        }
    }
}

@Composable
private fun MusicLibraryErrorCard(
    error: MusicLibraryError,
    onRetry: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(
                    if (error == MusicLibraryError.NETWORK) {
                        R.string.music_library_error_network
                    } else {
                        R.string.music_library_error_request
                    }
                ),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.common_retry))
            }
        }
    }
}

@Composable
private fun DeleteRecommendationDialog(
    recommendation: MusicRecommendation,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.music_library_delete_title)) },
        text = {
            Text(
                stringResource(
                    R.string.music_library_delete_description,
                    recommendation.track.artist,
                    recommendation.track.title
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                Text(
                    stringResource(
                        if (isDeleting) {
                            R.string.music_library_deleting
                        } else {
                            R.string.music_library_delete_confirm
                        }
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(stringResource(R.string.music_library_delete_cancel))
            }
        }
    )
}

private fun String.toYoutubeSearchUri(): Uri = Uri.parse("https://www.youtube.com/results")
    .buildUpon()
    .appendQueryParameter("search_query", this)
    .build()
