package hs.project.steptune.feature.musiclibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.domain.model.MusicRecommendation
import hs.project.steptune.domain.usecase.DeleteMusicRecommendationUseCase
import hs.project.steptune.domain.usecase.GetMusicRecommendationHistoryUseCase
import hs.project.steptune.domain.usecase.UpdateMusicRecommendationFavoriteUseCase
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    private val getHistoryUseCase: GetMusicRecommendationHistoryUseCase,
    private val updateFavoriteUseCase: UpdateMusicRecommendationFavoriteUseCase,
    private val deleteRecommendationUseCase: DeleteMusicRecommendationUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MusicLibraryUiState())
    val uiState: StateFlow<MusicLibraryUiState> = _uiState.asStateFlow()
    private var historyJob: Job? = null

    fun selectFilter(filter: MusicLibraryFilter) {
        val current = _uiState.value
        if (
            current.filter == filter ||
            current.pendingFavoriteId != null ||
            current.pendingDeleteId != null
        ) {
            return
        }
        historyJob?.cancel()
        _uiState.update {
            MusicLibraryUiState(filter = filter)
        }
        loadHistory(reset = true)
    }

    fun retry() {
        historyJob?.cancel()
        loadHistory(reset = true)
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isLoading || current.isLoadingMore || !current.hasNext) return
        loadHistory(reset = false)
    }

    fun toggleFavorite(recommendation: MusicRecommendation) {
        val current = _uiState.value
        if (current.pendingFavoriteId != null || current.pendingDeleteId != null) return
        val favoriteOnly = current.favoriteOnly
        _uiState.update {
            it.copy(pendingFavoriteId = recommendation.recommendationId, error = null)
        }
        viewModelScope.launch {
            try {
                val updated = updateFavoriteUseCase(
                    recommendationId = recommendation.recommendationId,
                    favorite = !recommendation.favorite
                )
                if (favoriteOnly && !updated.favorite) {
                    _uiState.update { it.copy(pendingFavoriteId = null, error = null) }
                    loadHistory(reset = true)
                } else {
                    _uiState.update { state ->
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
                _uiState.update {
                    it.copy(pendingFavoriteId = null, error = exception.toLibraryError())
                }
            }
        }
    }

    fun requestDelete(recommendation: MusicRecommendation) {
        if (_uiState.value.pendingDeleteId != null) return
        _uiState.update { it.copy(deleteConfirmation = recommendation) }
    }

    fun dismissDelete() {
        if (_uiState.value.pendingDeleteId != null) return
        _uiState.update { it.copy(deleteConfirmation = null) }
    }

    fun confirmDelete() {
        val recommendation = _uiState.value.deleteConfirmation ?: return
        if (_uiState.value.pendingDeleteId != null) return
        _uiState.update {
            it.copy(pendingDeleteId = recommendation.recommendationId, error = null)
        }
        viewModelScope.launch {
            try {
                deleteRecommendationUseCase(recommendation.recommendationId)
                _uiState.update {
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
                _uiState.update {
                    it.copy(pendingDeleteId = null, error = exception.toLibraryError())
                }
            }
        }
    }

    private fun loadHistory(reset: Boolean) {
        val current = _uiState.value
        val requestedPage = if (reset) 0 else current.currentPage + 1
        val requestedFilter = current.filter
        historyJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = reset,
                    isLoadingMore = !reset,
                    error = null
                )
            }
            try {
                val result = getHistoryUseCase(
                    page = requestedPage,
                    favoriteOnly = requestedFilter == MusicLibraryFilter.FAVORITES
                )
                _uiState.update { state ->
                    if (state.filter != requestedFilter) return@update state
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
                _uiState.update { state ->
                    if (state.filter != requestedFilter) return@update state
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
