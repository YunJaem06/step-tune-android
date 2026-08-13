package hs.project.steptune.feature.musicpreference

import androidx.compose.runtime.Immutable
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood

@Immutable
data class MusicPreferenceSelectionUiState(
    val selectedGenres: Set<MusicGenre> = emptySet(),
    val selectedMoods: Set<MusicMood> = emptySet()
)
