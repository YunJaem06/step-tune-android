package hs.project.steptune.feature.recommendation

import androidx.compose.runtime.Immutable
import hs.project.steptune.core.ui.musicpreference.MusicPreferenceSelectionUiState
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.MusicRecommendation

enum class MusicRecommendationError {
    TODAY_RECORD_NOT_FOUND,
    RATE_LIMIT,
    SERVICE_UNAVAILABLE,
    INVALID_RESPONSE,
    REQUEST_FAILED,
    NETWORK
}

object MusicRecommendationContract {
    @Immutable
    data class State(
        val preferences: MusicPreferenceSelectionUiState = MusicPreferenceSelectionUiState(),
        val durationMinutes: Int = DEFAULT_DURATION_MINUTES,
        val isLoadingPreferences: Boolean = true,
        val hasLoadedPreferences: Boolean = false,
        val isGenerating: Boolean = false,
        val isUpdatingFavorite: Boolean = false,
        val recommendation: MusicRecommendation? = null,
        val error: MusicRecommendationError? = null
    ) {
        val canGenerate: Boolean
            get() = hasLoadedPreferences && !isLoadingPreferences &&
                !isGenerating && !isUpdatingFavorite

        companion object {
            const val DEFAULT_DURATION_MINUTES = 30
            val DURATION_OPTIONS = listOf(15, 30, 45, 60)
        }
    }

    sealed interface Action {
        data class ToggleGenre(val genre: MusicGenre) : Action
        data class ToggleMood(val mood: MusicMood) : Action
        data class SelectDuration(val minutes: Int) : Action
        data object RetryPreferences : Action
        data object Generate : Action
        data object RequestAnother : Action
        data object ToggleFavorite : Action
        data object OpenYouTube : Action
        data object Back : Action
    }

    sealed interface Effect {
        data class OpenYouTube(val searchQuery: String) : Effect
        data object NavigateBack : Effect
    }
}
