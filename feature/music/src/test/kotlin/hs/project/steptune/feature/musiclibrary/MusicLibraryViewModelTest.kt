package hs.project.steptune.feature.musiclibrary

import hs.project.steptune.domain.model.MusicRecommendation
import hs.project.steptune.domain.model.MusicRecommendationHistoryPage
import hs.project.steptune.domain.usecase.DeleteMusicRecommendationUseCase
import hs.project.steptune.domain.usecase.GetMusicRecommendationHistoryUseCase
import hs.project.steptune.domain.usecase.UpdateMusicRecommendationFavoriteUseCase
import hs.project.steptune.feature.music.FakeMusicRepository
import hs.project.steptune.feature.music.MainDispatcherRule
import hs.project.steptune.feature.music.historyPage
import hs.project.steptune.feature.music.recommendation
import hs.project.steptune.feature.musiclibrary.MusicLibraryContract.Action
import hs.project.steptune.feature.musiclibrary.MusicLibraryContract.Effect
import java.io.IOException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
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
class MusicLibraryViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val repository = FakeMusicRepository()

    @Test
    fun `loading more guards repeated actions and merges pages without duplicate IDs`() = runTest {
        repository.onHistory = { page, favoriteOnly ->
            val tracks = if (page == 0) listOf(recommendation("a"))
            else listOf(recommendation("a"), recommendation("b"))
            historyPage(tracks, page, favoriteOnly, hasNext = page == 0)
        }
        val viewModel = createViewModel()
        viewModel.onAction(Action.Refresh)
        assertTrue(viewModel.state.value.isLoading)
        runCurrent()
        viewModel.onAction(Action.LoadMore)
        viewModel.onAction(Action.LoadMore)
        assertTrue(viewModel.state.value.isLoadingMore)
        runCurrent()

        assertEquals(listOf(0 to false, 1 to false), repository.historyRequests)
        assertEquals(listOf("a", "b"), viewModel.state.value.recommendations.map { it.recommendationId })
        assertEquals(1, viewModel.state.value.currentPage)
        assertFalse(viewModel.state.value.hasNext)
        viewModel.onAction(Action.LoadMore)
        runCurrent()
        assertEquals(2, repository.historyRequests.size)
    }

    @Test
    fun `a late response from canceled refresh cannot replace the latest state`() = runTest {
        var firstResponse: Continuation<MusicRecommendationHistoryPage>? = null
        repository.onHistory = { _, _ -> suspendCoroutine { firstResponse = it } }
        val viewModel = createViewModel()
        viewModel.onAction(Action.Refresh)
        runCurrent()
        repository.onHistory = { _, _ -> historyPage(listOf(recommendation("new"))) }
        viewModel.onAction(Action.Refresh)
        runCurrent()
        // Simulate a data source that finishes even after cancellation.
        firstResponse!!.resume(historyPage(listOf(recommendation("old"))))
        runCurrent()
        assertEquals(listOf("new"), viewModel.state.value.recommendations.map { it.recommendationId })
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `changing filter cancels old loading and requests favorites from page zero`() = runTest {
        val oldResponse = CompletableDeferred<MusicRecommendationHistoryPage>()
        repository.onHistory = { page, favoriteOnly ->
            if (favoriteOnly) historyPage(listOf(recommendation("favorite", true)), page, true)
            else oldResponse.await()
        }
        val viewModel = createViewModel()
        viewModel.onAction(Action.Refresh)
        runCurrent()
        viewModel.onAction(Action.SelectFilter(MusicLibraryFilter.FAVORITES))
        runCurrent()
        oldResponse.complete(historyPage(listOf(recommendation("old"))))
        runCurrent()
        assertEquals(listOf(0 to false, 0 to true), repository.historyRequests)
        assertTrue(viewModel.state.value.favoriteOnly)
        assertEquals("favorite", viewModel.state.value.recommendations.single().recommendationId)
    }

    @Test
    fun `saving favorite blocks overlapping writes pagination and filter changes`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAction(Action.Refresh)
        runCurrent()
        val pending = CompletableDeferred<MusicRecommendation>()
        repository.onFavorite = { _, _ -> pending.await() }
        viewModel.onAction(Action.ToggleFavorite("track-1"))
        viewModel.onAction(Action.ToggleFavorite("track-1"))
        viewModel.onAction(Action.RequestDelete("track-1"))
        viewModel.onAction(Action.Refresh)
        viewModel.onAction(Action.LoadMore)
        viewModel.onAction(Action.SelectFilter(MusicLibraryFilter.FAVORITES))
        runCurrent()
        assertEquals(listOf("track-1" to true), repository.favoriteRequests)
        assertEquals(1, repository.historyRequests.size)
        assertNull(viewModel.state.value.deleteConfirmation)
        assertEquals(MusicLibraryFilter.ALL, viewModel.state.value.filter)
        pending.complete(recommendation(favorite = true))
        runCurrent()
        assertTrue(viewModel.state.value.recommendations.single().favorite)
        assertNull(viewModel.state.value.pendingFavoriteId)
    }

    @Test
    fun `removing favorite in favorites tab reloads list from page zero`() = runTest {
        repository.onHistory = { page, favoriteOnly ->
            historyPage(listOf(recommendation(favorite = true)), page, favoriteOnly)
        }
        val viewModel = createViewModel()
        viewModel.onAction(Action.SelectFilter(MusicLibraryFilter.FAVORITES))
        runCurrent()
        repository.onHistory = { page, favoriteOnly -> historyPage(emptyList(), page, favoriteOnly) }
        viewModel.onAction(Action.ToggleFavorite("track-1"))
        runCurrent()
        assertEquals(listOf("track-1" to false), repository.favoriteRequests)
        assertEquals(listOf(0 to true, 0 to true), repository.historyRequests)
        assertTrue(viewModel.state.value.recommendations.isEmpty())
        assertEquals(0L, viewModel.state.value.totalElements)
    }

    @Test
    fun `delete failure retains confirmation and retry deletes once then refreshes`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAction(Action.Refresh)
        runCurrent()
        repository.onDelete = { throw IOException("offline") }
        viewModel.onAction(Action.RequestDelete("missing"))
        assertNull(viewModel.state.value.deleteConfirmation)
        viewModel.onAction(Action.RequestDelete("track-1"))
        viewModel.onAction(Action.ConfirmDelete)
        runCurrent()
        assertEquals(MusicLibraryError.NETWORK, viewModel.state.value.error)
        assertNull(viewModel.state.value.pendingDeleteId)
        assertEquals("track-1", viewModel.state.value.deleteConfirmation?.recommendationId)

        val pending = CompletableDeferred<Unit>()
        repository.onDelete = { pending.await() }
        repository.onHistory = { page, favoriteOnly -> historyPage(emptyList(), page, favoriteOnly) }
        viewModel.onAction(Action.ConfirmDelete)
        viewModel.onAction(Action.ConfirmDelete)
        viewModel.onAction(Action.DismissDelete)
        runCurrent()
        assertEquals(listOf("track-1", "track-1"), repository.deletedIds)
        assertEquals("track-1", viewModel.state.value.pendingDeleteId)
        assertEquals("track-1", viewModel.state.value.deleteConfirmation?.recommendationId)
        pending.complete(Unit)
        runCurrent()
        assertNull(viewModel.state.value.deleteConfirmation)
        assertNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.recommendations.isEmpty())
    }

    @Test
    fun `failed next page preserves loaded items and can retry the same page`() = runTest {
        repository.onHistory = { page, favoriteOnly ->
            if (page > 0) throw IOException("offline")
            historyPage(listOf(recommendation()), page, favoriteOnly, hasNext = true)
        }
        val viewModel = createViewModel()
        viewModel.onAction(Action.Refresh)
        runCurrent()
        viewModel.onAction(Action.LoadMore)
        runCurrent()
        assertEquals(MusicLibraryError.NETWORK, viewModel.state.value.error)
        assertEquals(0, viewModel.state.value.currentPage)
        assertEquals(listOf(recommendation()), viewModel.state.value.recommendations)
        assertFalse(viewModel.state.value.isLoadingMore)
        repository.onHistory = { page, favoriteOnly -> historyPage(listOf(recommendation("next")), page, favoriteOnly) }
        viewModel.onAction(Action.LoadMore)
        runCurrent()
        assertEquals(listOf(0 to false, 1 to false, 1 to false), repository.historyRequests)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `favorite failure clears busy state without changing the previous favorite`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAction(Action.Refresh)
        runCurrent()
        repository.onFavorite = { _, _ -> throw IllegalStateException("server") }
        viewModel.onAction(Action.ToggleFavorite("track-1"))
        runCurrent()
        assertEquals(MusicLibraryError.REQUEST_FAILED, viewModel.state.value.error)
        assertNull(viewModel.state.value.pendingFavoriteId)
        assertFalse(viewModel.state.value.recommendations.single().favorite)
    }

    @Test
    fun `effects use current item IDs and are not replayed after consumption`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAction(Action.Refresh)
        runCurrent()
        viewModel.onAction(Action.OpenYouTube("missing"))
        assertNull(withTimeoutOrNull(1) { viewModel.effects.first() })
        viewModel.onAction(Action.OpenYouTube("track-1"))
        runCurrent()
        assertEquals(Effect.OpenYouTube("BTS Dynamite official audio"), viewModel.effects.first())
        assertNull(withTimeoutOrNull(1) { viewModel.effects.first() })
        viewModel.onAction(Action.RequestRecommendation)
        runCurrent()
        assertEquals(Effect.NavigateToRecommendation, viewModel.effects.first())
    }

    private fun createViewModel() = MusicLibraryViewModel(
        getHistoryUseCase = GetMusicRecommendationHistoryUseCase(repository),
        updateFavoriteUseCase = UpdateMusicRecommendationFavoriteUseCase(repository),
        deleteRecommendationUseCase = DeleteMusicRecommendationUseCase(repository)
    )
}
