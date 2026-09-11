package hs.project.steptune.domain.usecase

import hs.project.steptune.api.NotFoundException
import hs.project.steptune.core.util.DateFormatter
import hs.project.steptune.domain.model.DailyProgress
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.MusicPreferenceRules
import hs.project.steptune.domain.model.MusicRecommendation
import hs.project.steptune.domain.repository.MusicRecommendationRepository
import hs.project.steptune.domain.repository.PedometerRepository
import hs.project.steptune.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class GenerateMusicRecommendationUseCase @Inject constructor(
    private val pedometerRepository: PedometerRepository,
    private val settingsRepository: SettingsRepository,
    private val musicRecommendationRepository: MusicRecommendationRepository,
    private val syncDailyStepRecordsUseCase: SyncDailyStepRecordsUseCase
) {
    suspend operator fun invoke(
        preferredMoods: Set<MusicMood>,
        preferredGenres: Set<MusicGenre>,
        durationMinutes: Int,
        recordDate: String = DateFormatter.today()
    ): MusicRecommendation {
        require(preferredMoods.size <= MusicPreferenceRules.MAX_MOODS)
        require(preferredGenres.size <= MusicPreferenceRules.MAX_GENRES)
        require(durationMinutes in MIN_DURATION_MINUTES..MAX_DURATION_MINUTES)

        val progress = getOrCreateProgress(recordDate)
        syncDailyStepRecordsUseCase(listOf(progress))
        return try {
            generate(recordDate, preferredMoods, preferredGenres, durationMinutes)
        } catch (_: NotFoundException) {
            syncDailyStepRecordsUseCase(listOf(progress))
            generate(recordDate, preferredMoods, preferredGenres, durationMinutes)
        }
    }

    private suspend fun generate(
        recordDate: String,
        preferredMoods: Set<MusicMood>,
        preferredGenres: Set<MusicGenre>,
        durationMinutes: Int
    ): MusicRecommendation = musicRecommendationRepository.generate(
        recordDate = recordDate,
        preferredMoods = preferredMoods,
        preferredGenres = preferredGenres,
        durationMinutes = durationMinutes
    )

    private suspend fun getOrCreateProgress(recordDate: String): DailyProgress {
        pedometerRepository.getDailyProgress(recordDate)?.let { return it }
        val preferences = settingsRepository.observePreferences().first()
        return DailyProgress(
            date = recordDate,
            steps = 0,
            goal = preferences.dailyGoal,
            distanceMeters = 0f,
            calories = 0f,
            measuredAtEpochMillis = System.currentTimeMillis()
        ).also { progress -> pedometerRepository.upsertDailyProgress(progress) }
    }

    companion object {
        const val MIN_DURATION_MINUTES = 10
        const val MAX_DURATION_MINUTES = 120
    }
}
