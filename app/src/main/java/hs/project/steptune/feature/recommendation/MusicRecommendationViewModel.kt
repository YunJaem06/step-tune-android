package hs.project.steptune.feature.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hs.project.steptune.api.InvalidRecommendationResponseException
import hs.project.steptune.api.NotFoundException
import hs.project.steptune.api.RecommendationUnavailableException
import hs.project.steptune.api.ServerException
import hs.project.steptune.api.TooManyRequestsException
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.MusicPreferenceRules
import hs.project.steptune.domain.usecase.GenerateMusicRecommendationUseCase
import hs.project.steptune.domain.usecase.ObserveUserPreferencesUseCase
import hs.project.steptune.domain.usecase.UpdateMusicRecommendationFavoriteUseCase
import hs.project.steptune.feature.musicpreference.MusicPreferenceSelectionUiState
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MusicRecommendationViewModel @Inject constructor(
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val generateMusicRecommendationUseCase: GenerateMusicRecommendationUseCase,
    private val updateFavoriteUseCase: UpdateMusicRecommendationFavoriteUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MusicRecommendationUiState())
    val uiState: StateFlow<MusicRecommendationUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    fun onGenreToggled(genre: MusicGenre) {
        if (!_uiState.value.canGenerate) return
        _uiState.update { current ->
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

    fun onMoodToggled(mood: MusicMood) {
        if (!_uiState.value.canGenerate) return
        _uiState.update { current ->
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

    fun onDurationSelected(durationMinutes: Int) {
        if (!_uiState.value.canGenerate || durationMinutes !in MusicRecommendationUiState.DURATION_OPTIONS) {
            return
        }
        _uiState.update { it.copy(durationMinutes = durationMinutes, error = null) }
    }

    fun generateRecommendation() {
        val current = _uiState.value
        if (!current.canGenerate) return
        _uiState.update { it.copy(isGenerating = true, error = null) }
        viewModelScope.launch {
            try {
                val result = generateMusicRecommendationUseCase(
                    preferredMoods = current.preferences.selectedMoods,
                    preferredGenres = current.preferences.selectedGenres,
                    durationMinutes = current.durationMinutes
                )
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        recommendation = result,
                        error = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        error = exception.toUiError()
                    )
                }
            }
        }
    }

    fun requestAnotherRecommendation() {
        if (_uiState.value.isGenerating || _uiState.value.isUpdatingFavorite) return
        _uiState.update { it.copy(recommendation = null, error = null) }
    }

    fun toggleFavorite() {
        val recommendation = _uiState.value.recommendation ?: return
        if (_uiState.value.isUpdatingFavorite) return
        _uiState.update { it.copy(isUpdatingFavorite = true, error = null) }
        viewModelScope.launch {
            try {
                val updated = updateFavoriteUseCase(
                    recommendationId = recommendation.recommendationId,
                    favorite = !recommendation.favorite
                )
                _uiState.update {
                    it.copy(
                        isUpdatingFavorite = false,
                        recommendation = updated,
                        error = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
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
        viewModelScope.launch {
            val preferences = observeUserPreferencesUseCase().first()
            _uiState.update { current ->
                current.copy(
                    preferences = MusicPreferenceSelectionUiState(
                        selectedGenres = preferences.preferredGenres,
                        selectedMoods = preferences.preferredMoods
                    ),
                    isLoadingPreferences = false
                )
            }
        }
    }

    private fun Exception.toUiError(): MusicRecommendationError = when (this) {
        is NotFoundException -> MusicRecommendationError.TODAY_RECORD_NOT_FOUND
        is TooManyRequestsException -> MusicRecommendationError.RATE_LIMIT
        is RecommendationUnavailableException -> MusicRecommendationError.SERVICE_UNAVAILABLE
        is InvalidRecommendationResponseException -> MusicRecommendationError.INVALID_RESPONSE
        is ServerException -> MusicRecommendationError.REQUEST_FAILED
        is IOException -> MusicRecommendationError.NETWORK
        else -> MusicRecommendationError.NETWORK
    }
}
