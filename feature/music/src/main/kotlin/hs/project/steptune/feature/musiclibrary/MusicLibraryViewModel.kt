package hs.project.steptune.feature.musiclibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.domain.model.MusicRecommendation
import hs.project.steptune.domain.usecase.DeleteMusicRecommendationUseCase
import hs.project.steptune.domain.usecase.GetMusicRecommendationHistoryUseCase
import hs.project.steptune.domain.usecase.UpdateMusicRecommendationFavoriteUseCase
import hs.project.steptune.feature.musiclibrary.MusicLibraryContract.Action
import hs.project.steptune.feature.musiclibrary.MusicLibraryContract.Effect
import hs.project.steptune.feature.musiclibrary.MusicLibraryContract.State
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    private val getHistoryUseCase: GetMusicRecommendationHistoryUseCase,
    private val updateFavoriteUseCase: UpdateMusicRecommendationFavoriteUseCase,
    private val deleteRecommendationUseCase: DeleteMusicRecommendationUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private val effectChannel = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = effectChannel.receiveAsFlow()
    private var historyJob: Job? = null
    private var historyRequestId = 0L

    fun onAction(action: Action) {
        when (action) {
            Action.Refresh -> refresh()
            is Action.SelectFilter -> selectFilter(action.filter)
            Action.LoadMore -> loadMore()
            is Action.ToggleFavorite -> toggleFavorite(action.recommendationId)
            is Action.RequestDelete -> requestDelete(action.recommendationId)
            Action.DismissDelete -> dismissDelete()
            Action.ConfirmDelete -> confirmDelete()
            is Action.OpenYouTube -> findRecommendation(action.recommendationId)?.let {
                emitEffect(Effect.OpenYouTube(it.track.searchQuery))
            }
            Action.RequestRecommendation -> emitEffect(Effect.NavigateToRecommendation)
        }
    }

    private fun emitEffect(effect: Effect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }

    private fun findRecommendation(id: String): MusicRecommendation? =
        _state.value.recommendations.find { it.recommendationId == id }

    private fun selectFilter(filter: MusicLibraryFilter) {
        val current = _state.value
        if (
            current.filter == filter ||
            current.isMutating ||
            current.deleteConfirmation != null
        ) {
            return
        }
        _state.update {
            State(filter = filter)
        }
        loadHistory(reset = true)
    }

    private fun refresh() {
        if (_state.value.isMutating || _state.value.deleteConfirmation != null) return
        loadHistory(reset = true)
    }

    private fun loadMore() {
        val current = _state.value
        if (!current.canModify || current.deleteConfirmation != null || !current.hasNext) return
        loadHistory(reset = false)
    }

    private fun toggleFavorite(recommendationId: String) {
        val current = _state.value
        if (!current.canModify || current.deleteConfirmation != null) return
        val recommendation = findRecommendation(recommendationId) ?: return
        val favoriteOnly = current.favoriteOnly
        _state.update {
            it.copy(pendingFavoriteId = recommendation.recommendationId, error = null)
        }
        viewModelScope.launch {
            try {
                val updated = updateFavoriteUseCase(
                    recommendationId = recommendation.recommendationId,
                    favorite = !recommendation.favorite
                )
                if (favoriteOnly && !updated.favorite) {
                    _state.update { it.copy(pendingFavoriteId = null, error = null) }
                    loadHistory(reset = true)
                } else {
                    _state.update { state ->
                        state.copy(
                            recommendations = state.recommendations.map {
                                if (it.recommendationId == updated.recommendationId) updated else it
                            },
                            pendingFavoriteId = null,
                            error = null
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.update {
                    it.copy(pendingFavoriteId = null, error = exception.toLibraryError())
                }
            }
        }
    }

    private fun requestDelete(recommendationId: String) {
        if (!_state.value.canModify || _state.value.deleteConfirmation != null) return
        val recommendation = findRecommendation(recommendationId) ?: return
        _state.update { it.copy(deleteConfirmation = recommendation) }
    }

    private fun dismissDelete() {
        if (_state.value.pendingDeleteId != null) return
        _state.update { it.copy(deleteConfirmation = null) }
    }

    private fun confirmDelete() {
        val recommendation = _state.value.deleteConfirmation ?: return
        if (!_state.value.canModify) return
        _state.update {
            it.copy(pendingDeleteId = recommendation.recommendationId, error = null)
        }
        viewModelScope.launch {
            try {
                deleteRecommendationUseCase(recommendation.recommendationId)
                _state.update {
                    it.copy(
                        pendingDeleteId = null,
                        deleteConfirmation = null,
                        error = null
                    )
                }
                loadHistory(reset = true)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.update {
                    it.copy(pendingDeleteId = null, error = exception.toLibraryError())
                }
            }
        }
    }

    private fun loadHistory(reset: Boolean) {
        historyJob?.cancel()
        val requestId = ++historyRequestId
        val current = _state.value
        val requestedPage = if (reset) 0 else current.currentPage + 1
        val requestedFilter = current.filter
        // Set loading before launching so consecutive Actions cannot request the same page twice.
        _state.update {
            it.copy(isLoading = reset, isLoadingMore = !reset, error = null)
        }
        historyJob = viewModelScope.launch {
            try {
                val result = getHistoryUseCase(
                    page = requestedPage,
                    favoriteOnly = requestedFilter == MusicLibraryFilter.FAVORITES
                )
                _state.update { state ->
                    if (requestId != historyRequestId || state.filter != requestedFilter) {
                        return@update state
                    }
                    state.copy(
                        recommendations = if (reset) {
                            result.recommendations
                        } else {
                            (state.recommendations + result.recommendations)
                                .distinctBy(MusicRecommendation::recommendationId)
                        },
                        totalElements = result.totalElements,
                        currentPage = result.page,
                        hasNext = result.hasNext,
                        isLoading = false,
                        isLoadingMore = false,
                        error = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.update { state ->
                    if (requestId != historyRequestId || state.filter != requestedFilter) {
                        return@update state
                    }
                    state.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = exception.toLibraryError()
                    )
                }
            }
        }
    }

    private fun Exception.toLibraryError(): MusicLibraryError = when (this) {
        is IOException -> MusicLibraryError.NETWORK
        else -> MusicLibraryError.REQUEST_FAILED
    }
}
