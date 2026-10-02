package hs.project.steptune

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import hs.project.steptune.domain.model.MusicRecommendation
import hs.project.steptune.domain.model.RecommendationActivityLevel
import hs.project.steptune.domain.model.RecommendationStepSummary
import hs.project.steptune.domain.model.RecommendedTrack
import hs.project.steptune.feature.home.HomeScreen
import hs.project.steptune.feature.home.HomeUiState
import hs.project.steptune.feature.login.LoginScreen
import hs.project.steptune.feature.login.LoginUiState
import hs.project.steptune.feature.musiclibrary.MusicLibraryContract
import hs.project.steptune.feature.musiclibrary.MusicLibraryScreen
import hs.project.steptune.feature.onboarding.OnboardingUiState
import hs.project.steptune.feature.onboarding.PermissionOnboardingScreen
import hs.project.steptune.feature.recommendation.MusicRecommendationContract
import hs.project.steptune.feature.recommendation.MusicRecommendationScreen
import hs.project.steptune.ui.theme.StepTuneTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Stateless screens only: never selects a Google account or deletes real data. */
@RunWith(AndroidJUnit4::class)
class FeatureScreenRegressionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun googleLoginButtonRendersAndDispatchesClick() {
        var clicks = 0
        composeRule.setContent {
            StepTuneTheme { LoginScreen(LoginUiState(), { clicks++ }) }
        }
        composeRule.onNodeWithText("Google로 계속하기").performClick()
        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun onboardingCannotContinueWithoutRequiredPermissions() {
        composeRule.setContent {
            StepTuneTheme { PermissionOnboardingScreen(OnboardingUiState(isLoading = false), {}, {}, {}) }
        }
        composeRule.onNodeWithText("계속").assertIsNotEnabled()
    }

    @Test
    fun homeRendersGoalProgressAndOpensRecommendation() {
        var clicks = 0
        composeRule.setContent {
            StepTuneTheme {
                HomeScreen(HomeUiState(date = "2026-10-02", steps = 3_200, goal = 10_000, isLoading = false), { clicks++ })
            }
        }
        composeRule.onNodeWithText("32% 달성").assertIsDisplayed()
        composeRule.onNodeWithText("추천받기").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun recommendationRendersOneTrackAndDispatchesYouTubeAction() {
        val actions = mutableListOf<MusicRecommendationContract.Action>()
        val track = MusicRecommendation(
            "recommendation-1", "2026-10-02", RecommendationStepSummary(3200, 3000.0, 7, 200.0, 6.67),
            RecommendationActivityLevel.MODERATE, 30, "가벼운 산책에 어울려요.",
            RecommendedTrack("Dynamite", "BTS", "BTS Dynamite official audio"), generatedAt = "2026-10-02T00:00:00Z"
        )
        composeRule.setContent {
            StepTuneTheme {
                MusicRecommendationScreen(
                    MusicRecommendationContract.State(isLoadingPreferences = false, hasLoadedPreferences = true, recommendation = track),
                    actions::add
                )
            }
        }
        composeRule.onNodeWithText("Dynamite").assertIsDisplayed()
        composeRule.onNodeWithText("YouTube에서 검색하기").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(listOf(MusicRecommendationContract.Action.OpenYouTube), actions) }
    }

    @Test
    fun emptyLibraryDispatchesRecommendationNavigation() {
        val actions = mutableListOf<MusicLibraryContract.Action>()
        composeRule.setContent {
            StepTuneTheme { MusicLibraryScreen(MusicLibraryContract.State(isLoading = false), actions::add) }
        }
        composeRule.onNodeWithText("음악 추천받기").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(listOf(MusicLibraryContract.Action.RequestRecommendation), actions) }
    }
}
