package hs.project.steptune.feature.musiclibrary

import androidx.compose.runtime.Immutable
import hs.project.steptune.domain.model.MusicRecommendation

enum class MusicLibraryFilter {
    ALL,
    FAVORITES
}

enum class MusicLibraryError {
    NETWORK,
    REQUEST_FAILED
}

@Immutable
data class MusicLibraryUiState(
    val filter: MusicLibraryFilter = MusicLibraryFilter.ALL,
    val recommendations: List<MusicRecommendation> = emptyList(),
    val totalElements: Long = 0,
    val currentPage: Int = 0,
    val hasNext: Boolean = false,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val pendingFavoriteId: String? = null,
    val pendingDeleteId: String? = null,
    val deleteConfirmation: MusicRecommendation? = null,
    val error: MusicLibraryError? = null
) {
    val favoriteOnly: Boolean
        get() = filter == MusicLibraryFilter.FAVORITES
}
