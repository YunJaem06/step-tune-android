package hs.project.steptune.feature.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.domain.error.InvalidRecommendationResponseException
import hs.project.steptune.domain.error.RecommendationUnavailableException
import hs.project.steptune.domain.error.ServerException
import hs.project.steptune.domain.error.TooManyRequestsException
import hs.project.steptune.domain.error.ResourceNotFoundException
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.MusicPreferenceRules
import hs.project.steptune.domain.usecase.GenerateMusicRecommendationUseCase
import hs.project.steptune.domain.usecase.ObserveUserPreferencesUseCase
import hs.project.steptune.domain.usecase.UpdateMusicRecommendationFavoriteUseCase
import hs.project.steptune.core.ui.musicpreference.MusicPreferenceSelectionUiState
import hs.project.steptune.feature.recommendation.MusicRecommendationContract.Action
import hs.project.steptune.feature.recommendation.MusicRecommendationContract.Effect
import hs.project.steptune.feature.recommendation.MusicRecommendationContract.State
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MusicRecommendationViewModel @Inject constructor(
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val generateMusicRecommendationUseCase: GenerateMusicRecommendationUseCase,
    private val updateFavoriteUseCase: UpdateMusicRecommendationFavoriteUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private val effectChannel = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = effectChannel.receiveAsFlow()

    init {
        loadPreferences()
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.ToggleGenre -> toggleGenre(action.genre)
            is Action.ToggleMood -> toggleMood(action.mood)
            is Action.SelectDuration -> selectDuration(action.minutes)
            Action.RetryPreferences -> {
                if (!_state.value.isLoadingPreferences && !_state.value.hasLoadedPreferences) {
                    loadPreferences()
                }
            }
            Action.Generate -> generateRecommendation()
            Action.RequestAnother -> requestAnotherRecommendation()
            Action.ToggleFavorite -> toggleFavorite()
            Action.OpenYouTube -> _state.value.recommendation?.let {
                emitEffect(Effect.OpenYouTube(it.track.searchQuery))
            }
            Action.Back -> emitEffect(Effect.NavigateBack)
        }
    }

    private fun emitEffect(effect: Effect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }

    private fun toggleGenre(genre: MusicGenre) {
        if (!_state.value.canGenerate) return
        _state.update { current ->
            current.copy(
                preferences = current.preferences.copy(
                    selectedGenres = MusicPreferenceRules.toggleGenre(
                        current.preferences.selectedGenres,
                        genre
                    )
                ),
                error = null
            )
        }
    }

    private fun toggleMood(mood: MusicMood) {
        if (!_state.value.canGenerate) return
        _state.update { current ->
            current.copy(
                preferences = current.preferences.copy(
                    selectedMoods = MusicPreferenceRules.toggleMood(
                        current.preferences.selectedMoods,
                        mood
                    )
                ),
                error = null
            )
        }
    }

    private fun selectDuration(durationMinutes: Int) {
        if (!_state.value.canGenerate || durationMinutes !in State.DURATION_OPTIONS) {
            return
        }
        _state.update { it.copy(durationMinutes = durationMinutes, error = null) }
    }

    private fun generateRecommendation() {
        val current = _state.value
        if (!current.canGenerate || current.recommendation != null) return
        _state.update { it.copy(isGenerating = true, error = null) }
        viewModelScope.launch {
            try {
                val result = generateMusicRecommendationUseCase(
                    preferredMoods = current.preferences.selectedMoods,
                    preferredGenres = current.preferences.selectedGenres,
                    durationMinutes = current.durationMinutes
                )
                _state.update {
                    it.copy(
                        isGenerating = false,
                        recommendation = result,
                        error = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.update {
                    it.copy(
                        isGenerating = false,
                        error = exception.toUiError()
                    )
                }
            }
        }
    }

    private fun requestAnotherRecommendation() {
        if (_state.value.isGenerating || _state.value.isUpdatingFavorite) return
        _state.update { it.copy(recommendation = null, error = null) }
    }

    private fun toggleFavorite() {
        val recommendation = _state.value.recommendation ?: return
        if (_state.value.isUpdatingFavorite || _state.value.isGenerating) return
        _state.update { it.copy(isUpdatingFavorite = true, error = null) }
        viewModelScope.launch {
            try {
                val updated = updateFavoriteUseCase(
                    recommendationId = recommendation.recommendationId,
                    favorite = !recommendation.favorite
                )
                _state.update {
                    it.copy(
                        isUpdatingFavorite = false,
                        recommendation = updated,
                        error = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.update {
                    it.copy(
                        isUpdatingFavorite = false,
                        error = if (exception is IOException) {
                            MusicRecommendationError.NETWORK
                        } else {
                            MusicRecommendationError.REQUEST_FAILED
                        }
                    )
                }
            }
        }
    }

    private fun loadPreferences() {
        _state.update { it.copy(isLoadingPreferences = true, error = null) }
        viewModelScope.launch {
            try {
                val preferences = observeUserPreferencesUseCase().first()
                _state.update { current ->
                    current.copy(
                        preferences = MusicPreferenceSelectionUiState(
                            selectedGenres = preferences.preferredGenres,
                            selectedMoods = preferences.preferredMoods
                        ),
                        isLoadingPreferences = false,
                        hasLoadedPreferences = true
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.update {
                    it.copy(isLoadingPreferences = false, error = MusicRecommendationError.REQUEST_FAILED)
                }
            }
        }
    }

    private fun Exception.toUiError(): MusicRecommendationError = when (this) {
        is ResourceNotFoundException -> MusicRecommendationError.TODAY_RECORD_NOT_FOUND
        is TooManyRequestsException -> MusicRecommendationError.RATE_LIMIT
        is RecommendationUnavailableException -> MusicRecommendationError.SERVICE_UNAVAILABLE
        is InvalidRecommendationResponseException -> MusicRecommendationError.INVALID_RESPONSE
        is ServerException -> MusicRecommendationError.REQUEST_FAILED
        is IOException -> MusicRecommendationError.NETWORK
        else -> MusicRecommendationError.NETWORK
    }
}
