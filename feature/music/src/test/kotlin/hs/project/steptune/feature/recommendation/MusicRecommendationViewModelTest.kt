package hs.project.steptune.feature.recommendation

import hs.project.steptune.domain.error.InvalidRecommendationResponseException
import hs.project.steptune.domain.error.RecommendationUnavailableException
import hs.project.steptune.domain.error.ResourceNotFoundException
import hs.project.steptune.domain.error.ServerException
import hs.project.steptune.domain.error.TooManyRequestsException
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.usecase.ObserveUserPreferencesUseCase
import hs.project.steptune.domain.usecase.UpdateMusicRecommendationFavoriteUseCase
import hs.project.steptune.feature.music.FakeMusicRepository
import hs.project.steptune.feature.music.FakeSettingsRepository
import hs.project.steptune.feature.music.MainDispatcherRule
import hs.project.steptune.feature.music.generateUseCase
import hs.project.steptune.feature.music.recommendation
import hs.project.steptune.feature.recommendation.MusicRecommendationContract.Action
import hs.project.steptune.feature.recommendation.MusicRecommendationContract.Effect
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MusicRecommendationViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val repository = FakeMusicRepository()
    private val settings = FakeSettingsRepository()

    @Test
    fun `preferences load and actions enforce selection limits and duration options`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAction(Action.Generate)
        assertTrue(repository.generateRequests.isEmpty())
        runCurrent()
        assertEquals(setOf(MusicGenre.INDIE), viewModel.state.value.preferences.selectedGenres)
        assertTrue(viewModel.state.value.canGenerate)

        viewModel.onAction(Action.ToggleGenre(MusicGenre.POP))
        viewModel.onAction(Action.ToggleGenre(MusicGenre.JAZZ))
        viewModel.onAction(Action.ToggleGenre(MusicGenre.ROCK))
        viewModel.onAction(Action.ToggleMood(MusicMood.EMOTIONAL))
        viewModel.onAction(Action.ToggleMood(MusicMood.LIVELY))
        viewModel.onAction(Action.SelectDuration(45))
        viewModel.onAction(Action.SelectDuration(500))

        assertEquals(setOf(MusicGenre.INDIE, MusicGenre.POP, MusicGenre.JAZZ), viewModel.state.value.preferences.selectedGenres)
        assertEquals(setOf(MusicMood.CALM, MusicMood.EMOTIONAL), viewModel.state.value.preferences.selectedMoods)
        assertEquals(45, viewModel.state.value.durationMinutes)
    }

    @Test
    fun `generation captures input and ignores repeated generate and input while busy`() = runTest {
        val pending = CompletableDeferred<hs.project.steptune.domain.model.MusicRecommendation>()
        repository.onGenerate = { pending.await() }
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onAction(Action.SelectDuration(45))
        viewModel.onAction(Action.Generate)
        viewModel.onAction(Action.Generate)
        viewModel.onAction(Action.ToggleGenre(MusicGenre.POP))
        viewModel.onAction(Action.RequestAnother)
        assertTrue(viewModel.state.value.isGenerating)
        runCurrent()
        assertEquals(listOf(FakeMusicRepository.GenerateRequest(setOf(MusicMood.CALM), setOf(MusicGenre.INDIE), 45)), repository.generateRequests)

        pending.complete(recommendation())
        runCurrent()
        assertEquals(recommendation(), viewModel.state.value.recommendation)
        assertFalse(viewModel.state.value.isGenerating)
        viewModel.onAction(Action.RequestAnother)
        assertNull(viewModel.state.value.recommendation)
        assertEquals(45, viewModel.state.value.durationMinutes)
    }

    @Test
    fun `API errors become state and generating can be retried`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        val failures = listOf(
            ResourceNotFoundException() to MusicRecommendationError.TODAY_RECORD_NOT_FOUND,
            TooManyRequestsException() to MusicRecommendationError.RATE_LIMIT,
            RecommendationUnavailableException() to MusicRecommendationError.SERVICE_UNAVAILABLE,
            InvalidRecommendationResponseException() to MusicRecommendationError.INVALID_RESPONSE,
            ServerException("server") to MusicRecommendationError.REQUEST_FAILED,
            IOException("offline") to MusicRecommendationError.NETWORK
        )
        failures.forEach { (exception, expected) ->
            repository.onGenerate = { throw exception }
            viewModel.onAction(Action.Generate)
            runCurrent()
            assertEquals(expected, viewModel.state.value.error)
            assertTrue(viewModel.state.value.canGenerate)
        }
        repository.onGenerate = { recommendation() }
        viewModel.onAction(Action.Generate)
        runCurrent()
        assertNull(viewModel.state.value.error)
        assertEquals(recommendation(), viewModel.state.value.recommendation)
    }

    @Test
    fun `preference read failure blocks generation until retry succeeds`() = runTest {
        settings.readFailure = IOException("storage")
        val viewModel = createViewModel()
        runCurrent()
        assertFalse(viewModel.state.value.isLoadingPreferences)
        assertFalse(viewModel.state.value.canGenerate)
        assertEquals(MusicRecommendationError.REQUEST_FAILED, viewModel.state.value.error)
        viewModel.onAction(Action.Generate)
        settings.readFailure = null
        viewModel.onAction(Action.RetryPreferences)
        runCurrent()
        assertTrue(viewModel.state.value.canGenerate)
        assertNull(viewModel.state.value.error)
        assertTrue(repository.generateRequests.isEmpty())
    }

    @Test
    fun `favorite action ignores repeated clicks and prevents replacing result while saving`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onAction(Action.Generate)
        runCurrent()
        val pending = CompletableDeferred<hs.project.steptune.domain.model.MusicRecommendation>()
        repository.onFavorite = { _, _ -> pending.await() }
        viewModel.onAction(Action.ToggleFavorite)
        viewModel.onAction(Action.ToggleFavorite)
        viewModel.onAction(Action.RequestAnother)
        runCurrent()
        assertEquals(listOf("track-1" to true), repository.favoriteRequests)
        assertTrue(viewModel.state.value.isUpdatingFavorite)
        assertEquals("track-1", viewModel.state.value.recommendation?.recommendationId)
        pending.complete(recommendation(favorite = true))
        runCurrent()
        assertTrue(viewModel.state.value.recommendation!!.favorite)
        assertFalse(viewModel.state.value.isUpdatingFavorite)
    }

    @Test
    fun `YouTube and navigation effects are buffered once and never replayed`() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onAction(Action.OpenYouTube)
        assertNull(withTimeoutOrNull(1) { viewModel.effects.first() })
        viewModel.onAction(Action.Generate)
        runCurrent()
        viewModel.onAction(Action.OpenYouTube)
        runCurrent()
        assertEquals(Effect.OpenYouTube("BTS Dynamite official audio"), viewModel.effects.first())
        assertNull(withTimeoutOrNull(1) { viewModel.effects.first() })
        viewModel.onAction(Action.Back)
        runCurrent()
        assertEquals(Effect.NavigateBack, viewModel.effects.first())
    }

    private fun createViewModel() = MusicRecommendationViewModel(
        observeUserPreferencesUseCase = ObserveUserPreferencesUseCase(settings),
        generateMusicRecommendationUseCase = generateUseCase(repository, settings),
        updateFavoriteUseCase = UpdateMusicRecommendationFavoriteUseCase(repository)
    )
}
