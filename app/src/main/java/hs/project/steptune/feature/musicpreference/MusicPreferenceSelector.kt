package hs.project.steptune.feature.musicpreference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hs.project.steptune.R
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.MusicPreferenceRules

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MusicPreferenceSelector(
    uiState: MusicPreferenceSelectionUiState,
    onGenreToggled: (MusicGenre) -> Unit,
    onMoodToggled: (MusicMood) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PreferenceGroupHeader(
            title = stringResource(R.string.music_preferences_genre_title),
            description = stringResource(
                R.string.music_preferences_selection_count,
                uiState.selectedGenres.size,
                MusicPreferenceRules.MAX_GENRES
            )
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MusicGenre.entries.forEach { genre ->
                FilterChip(
                    selected = genre in uiState.selectedGenres,
                    enabled = enabled && (
                        genre in uiState.selectedGenres ||
                            uiState.selectedGenres.size < MusicPreferenceRules.MAX_GENRES
                    ),
                    onClick = { onGenreToggled(genre) },
                    label = { Text(stringResource(genre.labelResource)) }
                )
            }
        }

        PreferenceGroupHeader(
            title = stringResource(R.string.music_preferences_mood_title),
            description = stringResource(
                R.string.music_preferences_selection_count,
                uiState.selectedMoods.size,
                MusicPreferenceRules.MAX_MOODS
            )
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MusicMood.entries.forEach { mood ->
                FilterChip(
                    selected = mood in uiState.selectedMoods,
                    enabled = enabled && (
                        mood in uiState.selectedMoods ||
                            uiState.selectedMoods.size < MusicPreferenceRules.MAX_MOODS
                    ),
                    onClick = { onMoodToggled(mood) },
                    label = { Text(stringResource(mood.labelResource)) }
                )
            }
        }
    }
}

@Composable
private fun PreferenceGroupHeader(
    title: String,
    description: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val MusicGenre.labelResource: Int
    get() = when (this) {
        MusicGenre.BALLAD -> R.string.music_genre_ballad
        MusicGenre.HIP_HOP -> R.string.music_genre_hip_hop
        MusicGenre.RNB -> R.string.music_genre_rnb
        MusicGenre.POP -> R.string.music_genre_pop
        MusicGenre.ROCK -> R.string.music_genre_rock
        MusicGenre.INDIE -> R.string.music_genre_indie
        MusicGenre.JAZZ -> R.string.music_genre_jazz
        MusicGenre.CLASSICAL -> R.string.music_genre_classical
    }

private val MusicMood.labelResource: Int
    get() = when (this) {
        MusicMood.CALM -> R.string.music_mood_calm
        MusicMood.ENERGETIC -> R.string.music_mood_energetic
        MusicMood.EMOTIONAL -> R.string.music_mood_emotional
        MusicMood.FOCUSED -> R.string.music_mood_focused
        MusicMood.LIVELY -> R.string.music_mood_lively
    }
