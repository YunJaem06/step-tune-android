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

object MusicLibraryContract {
    @Immutable
    data class State(
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

        val isMutating: Boolean
            get() = pendingFavoriteId != null || pendingDeleteId != null

        val canModify: Boolean
            get() = !isLoading && !isLoadingMore && !isMutating
    }

    sealed interface Action {
        data object Refresh : Action
        data class SelectFilter(val filter: MusicLibraryFilter) : Action
        data object LoadMore : Action
        data class ToggleFavorite(val recommendationId: String) : Action
        data class RequestDelete(val recommendationId: String) : Action
        data object DismissDelete : Action
        data object ConfirmDelete : Action
        data class OpenYouTube(val recommendationId: String) : Action
        data object RequestRecommendation : Action
    }

    sealed interface Effect {
        data class OpenYouTube(val searchQuery: String) : Effect
        data object NavigateToRecommendation : Effect
    }
}
