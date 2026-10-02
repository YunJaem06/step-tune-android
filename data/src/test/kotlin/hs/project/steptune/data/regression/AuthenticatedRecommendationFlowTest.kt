package hs.project.steptune.data.regression

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.gson.Gson
import com.google.gson.JsonObject
import hs.project.steptune.api.AuthAPI
import hs.project.steptune.api.MusicRecommendationAPI
import hs.project.steptune.api.StepAPI
import hs.project.steptune.core.auth.AuthSessionEventBus
import hs.project.steptune.core.auth.AuthSessionEvent
import hs.project.steptune.data.ServerResponse
import hs.project.steptune.data.auth.response.ResponseAuthLogin
import hs.project.steptune.data.auth.response.ResponseUserData
import hs.project.steptune.data.config.NetworkConfig
import hs.project.steptune.data.di.NetworkModule
import hs.project.steptune.data.local.preferences.AuthPreferencesDataSource
import hs.project.steptune.data.local.preferences.PedometerPreferencesDataSource
import hs.project.steptune.data.repository.AuthRepositoryImpl
import hs.project.steptune.data.repository.MusicRecommendationRepositoryImpl
import hs.project.steptune.data.repository.SettingsRepositoryImpl
import hs.project.steptune.data.repository.StepRecordRepositoryImpl
import hs.project.steptune.api.client.AccessTokenAuthenticator
import hs.project.steptune.api.client.BearerAuthInterceptor
import hs.project.steptune.domain.error.UnauthorizedException
import hs.project.steptune.domain.model.AuthSession
import hs.project.steptune.domain.model.DailyProgress
import hs.project.steptune.domain.model.MusicGenre
import hs.project.steptune.domain.model.MusicMood
import hs.project.steptune.domain.repository.LocalUserDataRepository
import hs.project.steptune.domain.repository.PedometerRepository
import hs.project.steptune.domain.usecase.GenerateMusicRecommendationUseCase
import hs.project.steptune.domain.usecase.SyncDailyStepRecordsUseCase
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Uses the production HTTP wiring, JSON mapping and DataStore without a live account. */
class AuthenticatedRecommendationFlowTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    @get:Rule val server = MockWebServer()
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    @After
    fun tearDown() {
        storeScope.cancel()
    }

    @Test
    fun `login auto login 401 recovery sync recommendation library and logout work together`() = runBlocking {
        val fixture = createFixture()
        server.enqueue(authResponse("login-access", "login-refresh"))
        fixture.authRepository.loginWithGoogle("test-google-id-token")
        val loginRequest = takeRequest()
        assertEquals("/api/v1/auth/social", loginRequest.path)
        assertNull(loginRequest.getHeader("Authorization"))
        assertEquals(setOf("provider", "token"), body(loginRequest).keySet())
        assertEquals("google", bodyValue(loginRequest, "provider"))

        // A new repository reads the persisted refresh token, as a new app launch does.
        server.enqueue(authResponse("auto-access", "auto-refresh"))
        AuthRepositoryImpl(fixture.authAPI, fixture.authDataSource, fixture.localData).refreshSession()
        val autoLoginRequest = takeRequest()
        assertEquals("/api/v1/auth/refresh", autoLoginRequest.path)
        assertEquals("login-refresh", bodyValue(autoLoginRequest, "refreshToken"))
        assertEquals("auto-refresh", fixture.authDataSource.currentSession().refreshToken)

        server.enqueue(jsonResponse(mapOf("nickName" to "new-name", "available" to true)))
        server.enqueue(jsonResponse(ResponseUserData(1L, "new-name")))
        assertTrue(fixture.authRepository.checkNicknameAvailability("new-name").isAvailable)
        fixture.authRepository.updateNickname("new-name")
        assertEquals("GET", takeRequest().method)
        assertEquals("PATCH", takeRequest().method)
        assertEquals("new-name", fixture.authDataSource.currentSession().nickName)

        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        server.enqueue(authResponse("renewed-access", "renewed-refresh", "new-name"))
        server.enqueue(jsonResponse(mapOf("records" to emptyList<Any>(), "syncTime" to GENERATED_AT)))
        server.enqueue(jsonResponse(trackData()))
        val result = fixture.generateUseCase(
            preferredMoods = setOf(MusicMood.CALM),
            preferredGenres = setOf(MusicGenre.INDIE),
            durationMinutes = 30,
            recordDate = RECORD_DATE
        )

        val expiredSync = takeRequest()
        assertEquals("PUT", expiredSync.method)
        assertEquals("/api/v1/steps/daily-records/sync", expiredSync.path)
        assertEquals("Bearer auto-access", expiredSync.getHeader("Authorization"))
        val refresh = takeRequest()
        assertNull(refresh.getHeader("Authorization"))
        assertEquals("auto-refresh", bodyValue(refresh, "refreshToken"))
        val retriedSync = takeRequest()
        assertEquals("Bearer renewed-access", retriedSync.getHeader("Authorization"))
        assertEquals(3_200, body(retriedSync).getAsJsonArray("records")[0].asJsonObject["stepCount"].asInt)
        assertEquals(
            Instant.parse(GENERATED_AT),
            OffsetDateTime.parse(body(retriedSync).getAsJsonArray("records")[0].asJsonObject["measuredAt"].asString).toInstant()
        )
        val generate = takeRequest()
        assertEquals("/api/v1/music-recommendations/generate", generate.path)
        assertEquals("Bearer renewed-access", generate.getHeader("Authorization"))
        assertEquals(RECORD_DATE, bodyValue(generate, "recordDate"))
        assertEquals("CALM", body(generate).getAsJsonArray("preferredMoods")[0].asString)
        assertEquals("INDIE", body(generate).getAsJsonArray("preferredGenres")[0].asString)
        assertEquals("BTS Dynamite official audio", result.track.searchQuery)

        server.enqueue(jsonResponse(mapOf(
            "userId" to 1L, "recommendations" to listOf(trackData()), "favoriteOnly" to false,
            "page" to 0, "size" to 20, "totalElements" to 1L, "totalPages" to 1, "hasNext" to false
        )))
        server.enqueue(jsonResponse(trackData(favorite = true)))
        server.enqueue(jsonResponse(null))
        assertEquals(result, fixture.musicRepository.getHistory(0, 20, false).recommendations.single())
        assertTrue(fixture.musicRepository.updateFavorite("recommendation-1", true).favorite)
        fixture.musicRepository.delete("recommendation-1")
        assertEquals("/api/v1/music-recommendations/history?page=0&size=20&favoriteOnly=false", takeRequest().path)
        assertEquals("PATCH", takeRequest().method)
        assertEquals("DELETE", takeRequest().method)

        server.enqueue(jsonResponse(null))
        fixture.authRepository.logout()
        val logout = takeRequest()
        assertEquals("renewed-refresh", bodyValue(logout, "refreshToken"))
        assertNull(logout.getHeader("Authorization"))
        assertEquals(AuthSession(), fixture.authDataSource.currentSession())
        assertEquals(0, fixture.localData.clearCount)
    }

    @Test
    fun `refresh rejection stops protected request and clears the session`() = runBlocking {
        val fixture = createFixture()
        fixture.authDataSource.saveSession(AuthSession("old-access", "invalid-refresh", "1", "name"))
        val expiredEvent = async(start = CoroutineStart.UNDISPATCHED) { fixture.eventBus.events.first() }
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        assertThrows(UnauthorizedException::class.java) {
            runBlocking { fixture.authRepository.syncCurrentUser() }
        }

        assertEquals("/api/v1/me/profile", takeRequest().path)
        assertEquals("/api/v1/auth/refresh", takeRequest().path)
        assertEquals(2, server.requestCount)
        assertEquals(AuthSession(), fixture.authDataSource.currentSession())
        assertEquals(AuthSessionEvent.SessionExpired, expiredEvent.await())
        assertEquals(0, fixture.localData.clearCount)
    }

    @Test
    fun `failed step sync does not send a recommendation request`() = runBlocking {
        val fixture = createFixture()
        fixture.authDataSource.saveSession(AuthSession("access", "refresh", "1", "name"))
        server.enqueue(MockResponse().setResponseCode(503).setBody("{}"))

        assertThrows(java.io.IOException::class.java) {
            runBlocking { fixture.generateUseCase(emptySet(), emptySet(), 30, RECORD_DATE) }
        }

        assertEquals("/api/v1/steps/daily-records/sync", takeRequest().path)
        assertEquals(1, server.requestCount)
        assertFalse(fixture.authDataSource.currentSession().accessToken.isBlank())
    }

    private fun createFixture(): Fixture {
        val authStore = PreferenceDataStoreFactory.create(scope = storeScope) {
            File(temporaryFolder.root, "auth.preferences_pb")
        }
        val settingsStore = PreferenceDataStoreFactory.create(scope = storeScope) {
            File(temporaryFolder.root, "settings.preferences_pb")
        }
        val authDataSource = AuthPreferencesDataSource(authStore)
        val config = NetworkConfig(server.url("/").toString(), false) {}
        val logging = NetworkModule.provideHttpLoggingInterceptor(config)
        val refreshClient = NetworkModule.provideRefreshAuthOkHttpClient(logging, config)
        val refreshAPI = NetworkModule.provideRefreshAuthRetrofit(refreshClient, config).create(AuthAPI::class.java)
        val eventBus = AuthSessionEventBus()
        val client = NetworkModule.provideOkHttpClient(
            BearerAuthInterceptor(authDataSource),
            AccessTokenAuthenticator(refreshAPI, authDataSource, eventBus),
            logging,
            config
        )
        val retrofit = NetworkModule.provideRetrofit(client, config)
        val authAPI = retrofit.create(AuthAPI::class.java)
        val localData = TestLocalData()
        val musicRepository = MusicRecommendationRepositoryImpl(retrofit.create(MusicRecommendationAPI::class.java))
        return Fixture(
            authAPI, authDataSource, eventBus, localData,
            AuthRepositoryImpl(authAPI, authDataSource, localData),
            musicRepository,
            GenerateMusicRecommendationUseCase(
                TestPedometer(), SettingsRepositoryImpl(PedometerPreferencesDataSource(settingsStore)),
                musicRepository, SyncDailyStepRecordsUseCase(StepRecordRepositoryImpl(retrofit.create(StepAPI::class.java)))
            )
        )
    }

    private fun authResponse(access: String, refresh: String, nickname: String = "name") = jsonResponse(
        ResponseAuthLogin(access, 900, refresh, ResponseUserData(1L, nickname))
    )

    private fun jsonResponse(data: Any?) = MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody(gson.toJson(ServerResponse(200, "success", data)))

    private fun trackData(favorite: Boolean = false): Map<String, Any> = mapOf(
        "recommendationId" to "recommendation-1", "recordDate" to RECORD_DATE,
        "stepSummary" to mapOf("todayStepCount" to 3200, "recent7DayAverage" to 3000.0,
            "recordedDayCount" to 7, "differenceFromAverage" to 200.0, "changeRatePercent" to 6.67),
        "activityLevel" to "MODERATE", "durationMinutes" to 30, "reason" to "가벼운 산책에 어울려요.",
        "track" to mapOf("title" to "Dynamite", "artist" to "BTS", "searchQuery" to "BTS Dynamite official audio"),
        "favorite" to favorite, "generatedAt" to GENERATED_AT
    )

    private fun takeRequest(): RecordedRequest = requireNotNull(server.takeRequest(3, TimeUnit.SECONDS))
    private fun body(request: RecordedRequest): JsonObject = gson.fromJson(request.body.clone().readUtf8(), JsonObject::class.java)
    private fun bodyValue(request: RecordedRequest, key: String): String = body(request)[key].asString

    private data class Fixture(
        val authAPI: AuthAPI,
        val authDataSource: AuthPreferencesDataSource,
        val eventBus: AuthSessionEventBus,
        val localData: TestLocalData,
        val authRepository: AuthRepositoryImpl,
        val musicRepository: MusicRecommendationRepositoryImpl,
        val generateUseCase: GenerateMusicRecommendationUseCase
    )

    private class TestLocalData : LocalUserDataRepository {
        var clearCount = 0
        override suspend fun prepareForUser(userId: String, previousUserId: String) = Unit
        override suspend fun clearAll() { clearCount++ }
    }

    private class TestPedometer : PedometerRepository {
        private val progress = DailyProgress(RECORD_DATE, 3200, 10_000, 2304f, 80f, Instant.parse(GENERATED_AT).toEpochMilli())
        override fun observeDailyProgress(date: String): Flow<DailyProgress> = flowOf(progress)
        override fun observeDailyProgressRange(startDate: String, endDate: String): Flow<List<DailyProgress>> = flowOf(listOf(progress))
        override suspend fun getDailyProgress(date: String): DailyProgress = progress
        override suspend fun upsertDailyProgress(progress: DailyProgress) = Unit
    }

    private companion object {
        const val RECORD_DATE = "2026-10-02"
        const val GENERATED_AT = "2026-10-02T00:00:00Z"
    }
}
