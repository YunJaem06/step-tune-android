package hs.project.steptune.feature.recommendation

import androidx.compose.runtime.Immutable
import hs.project.steptune.domain.model.MusicRecommendation
import hs.project.steptune.feature.musicpreference.MusicPreferenceSelectionUiState

enum class MusicRecommendationError {
    TODAY_RECORD_NOT_FOUND,
    RATE_LIMIT,
    SERVICE_UNAVAILABLE,
    INVALID_RESPONSE,
    REQUEST_FAILED,
    NETWORK
}

@Immutable
data class MusicRecommendationUiState(
    val preferences: MusicPreferenceSelectionUiState = MusicPreferenceSelectionUiState(),
    val durationMinutes: Int = DEFAULT_DURATION_MINUTES,
    val isLoadingPreferences: Boolean = true,
    val isGenerating: Boolean = false,
    val isUpdatingFavorite: Boolean = false,
    val recommendation: MusicRecommendation? = null,
    val error: MusicRecommendationError? = null
) {
    val canGenerate: Boolean
        get() = !isLoadingPreferences && !isGenerating && !isUpdatingFavorite

    companion object {
        const val DEFAULT_DURATION_MINUTES = 30
        val DURATION_OPTIONS = listOf(15, 30, 45, 60)
    }
}
