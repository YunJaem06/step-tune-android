package hs.project.steptune.data.repository

import hs.project.steptune.api.InvalidRecommendationResponseException
import hs.project.steptune.api.MusicRecommendationAPI
import hs.project.steptune.api.NotFoundException
import hs.project.steptune.api.RecommendationUnavailableException
import hs.project.steptune.api.ServerException
import hs.project.steptune.api.TooManyRequestsException
import hs.project.steptune.api.UnauthorizedException
import hs.project.steptune.data.ServerResponse
import hs.project.steptune.data.recommendation.request.RequestGenerateMusicRecommendation
import hs.project.steptune.data.recommendation.request.RequestUpdateMusicRecommendationFavorite
import hs.project.steptune.data.recommendation.response.ResponseMusicRecommendation
import hs.project.steptune.data.recommendation.response.ResponseMusicRecommendationHistory
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.model.MusicRecommendation
import hs.project.steptune.domain.model.MusicRecommendationHistoryPage
import hs.project.steptune.domain.model.RecommendedTrack
import hs.project.steptune.domain.model.RecommendationActivityLevel
import hs.project.steptune.domain.model.RecommendationStepSummary
import hs.project.steptune.domain.repository.MusicRecommendationRepository
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class MusicRecommendationRepositoryImpl @Inject constructor(
    private val recommendationAPI: MusicRecommendationAPI
) : MusicRecommendationRepository {
    override suspend fun generate(
        recordDate: String,
        preferredMoods: Set<MusicMood>,
        preferredGenres: Set<MusicGenre>,
        durationMinutes: Int
    ): MusicRecommendation = recommendationAPI.requestGenerateRecommendation(
        RequestGenerateMusicRecommendation(
            recordDate = recordDate,
            preferredMoods = preferredMoods.map(MusicMood::name),
            preferredGenres = preferredGenres.map(MusicGenre::name),
            durationMinutes = durationMinutes
        )
    ).requireRecommendationData(
        notFoundMessage = "오늘 걸음 기록을 서버에서 찾을 수 없습니다."
    ).toDomain()

    override suspend fun getHistory(
        page: Int,
        size: Int,
        favoriteOnly: Boolean
    ): MusicRecommendationHistoryPage = recommendationAPI.requestRecommendationHistory(
        page = page,
        size = size,
        favoriteOnly = favoriteOnly
    ).requireRecommendationData().toDomain()

    override suspend fun updateFavorite(
        recommendationId: String,
        favorite: Boolean
    ): MusicRecommendation = recommendationAPI.requestUpdateFavorite(
        recommendationId = recommendationId,
        request = RequestUpdateMusicRecommendationFavorite(favorite)
    ).requireRecommendationData().toDomain()

    override suspend fun delete(recommendationId: String) {
        recommendationAPI.requestDeleteRecommendation(recommendationId)
            .requireRecommendationSuccess()
    }

    private fun ResponseMusicRecommendationHistory.toDomain(): MusicRecommendationHistoryPage {
        if (page < 0 || size !in 1..MAX_PAGE_SIZE || totalElements < 0 || totalPages < 0) {
            throw InvalidRecommendationResponseException()
        }
        return MusicRecommendationHistoryPage(
            recommendations = recommendations.map { response -> response.toDomain() },
            favoriteOnly = favoriteOnly,
            page = page,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = hasNext
        )
    }

    private fun ResponseMusicRecommendation.toDomain(): MusicRecommendation {
        val activity = RecommendationActivityLevel.entries
            .firstOrNull { value -> value.name.equals(activityLevel, ignoreCase = true) }
            ?: throw InvalidRecommendationResponseException()
        if (
            recommendationId.isBlank() ||
            recordDate.isBlank() ||
            reason.isBlank() ||
            track.title.isBlank() ||
            track.artist.isBlank() ||
            track.searchQuery.isBlank() ||
            generatedAt.isBlank()
        ) {
            throw InvalidRecommendationResponseException()
        }
        return MusicRecommendation(
            recommendationId = recommendationId,
            recordDate = recordDate,
            stepSummary = RecommendationStepSummary(
                todayStepCount = stepSummary.todayStepCount,
                recent7DayAverage = stepSummary.recent7DayAverage.toDouble(),
                recordedDayCount = stepSummary.recordedDayCount,
                differenceFromAverage = stepSummary.differenceFromAverage.toDouble(),
                changeRatePercent = stepSummary.changeRatePercent?.toDouble()
            ),
            activityLevel = activity,
            durationMinutes = durationMinutes,
            reason = reason,
            track = RecommendedTrack(
                title = track.title,
                artist = track.artist,
                searchQuery = track.searchQuery
            ),
            favorite = favorite,
            generatedAt = generatedAt
        )
    }

    private fun <T> Response<ServerResponse<T>>.requireRecommendationData(
        notFoundMessage: String = "추천 기록을 찾을 수 없습니다."
    ): T {
        requireRecommendationSuccess(notFoundMessage)
        return body()?.data
            ?: throw InvalidRecommendationResponseException("서버 응답에 추천 데이터가 없습니다.")
    }

    private fun <T> Response<ServerResponse<T>>.requireRecommendationSuccess(
        notFoundMessage: String = "추천 기록을 찾을 수 없습니다."
    ) {
        when (code()) {
            HTTP_UNAUTHORIZED -> throw UnauthorizedException()
            HTTP_NOT_FOUND -> throw NotFoundException(notFoundMessage)
            HTTP_TOO_MANY_REQUESTS -> throw TooManyRequestsException()
            HTTP_BAD_GATEWAY -> throw InvalidRecommendationResponseException()
            HTTP_SERVICE_UNAVAILABLE -> throw RecommendationUnavailableException()
        }
        val responseBody = body()
        if (!isSuccessful || responseBody?.code != HTTP_OK) {
            throw ServerException(responseBody?.message ?: "음악 추천 요청에 실패했습니다.")
        }
    }

    private companion object {
        const val HTTP_OK = 200
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_NOT_FOUND = 404
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_BAD_GATEWAY = 502
        const val HTTP_SERVICE_UNAVAILABLE = 503
        const val MAX_PAGE_SIZE = 50
    }
}
