package hs.project.steptune.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import hs.project.steptune.data.local.database.DayRecordDao
import hs.project.steptune.data.local.database.DayRecordEntity
import hs.project.steptune.data.local.preferences.LocalDataOwnerDataSource
import hs.project.steptune.data.local.preferences.PedometerPreferences
import hs.project.steptune.data.local.preferences.PedometerPreferencesDataSource
import hs.project.steptune.data.local.preferences.StepTrackingState
import hs.project.steptune.data.local.preferences.StepTrackingStateDataSource
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalUserDataRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `same owner keeps existing local data`() = runBlocking {
        val fixture = createFixture()
        fixture.ownerDataSource.updateOwnerUserId("1")
        fixture.preferencesDataSource.updateProfileSettings(12_000, 75, 180, 80)
        fixture.dao.upsert(testRecord())

        fixture.repository.prepareForUser(userId = "1", previousUserId = "1")

        assertEquals(0, fixture.dao.deleteAllCallCount)
        assertEquals(12_000, fixture.preferencesDataSource.preferences.first().dailyGoal)
        assertEquals(testRecord(), fixture.dao.getDayRecord(TEST_DATE))
    }

    @Test
    fun `different owner clears local data and stores the new owner`() = runBlocking {
        val fixture = createFixture()
        fixture.ownerDataSource.updateOwnerUserId("1")
        fixture.preferencesDataSource.updateProfileSettings(12_000, 75, 180, 80)
        fixture.trackingStateDataSource.updateState(TEST_DATE, 1_000, 500)
        fixture.dao.upsert(testRecord())

        fixture.repository.prepareForUser(userId = "2", previousUserId = "")

        assertEquals(1, fixture.dao.deleteAllCallCount)
        assertEquals(null, fixture.dao.getDayRecord(TEST_DATE))
        assertEquals(PedometerPreferences(), fixture.preferencesDataSource.preferences.first())
        assertEquals(StepTrackingState(), fixture.trackingStateDataSource.state.first())
        assertEquals("2", fixture.ownerDataSource.currentOwnerUserId())
    }

    @Test
    fun `account deletion clears records preferences tracking state and owner`() = runBlocking {
        val fixture = createFixture()
        fixture.ownerDataSource.updateOwnerUserId("1")
        fixture.preferencesDataSource.updateProfileSettings(12_000, 75, 180, 80)
        fixture.trackingStateDataSource.updateState(TEST_DATE, 1_000, 500)
        fixture.dao.upsert(testRecord())

        fixture.repository.clearAll()

        assertEquals(null, fixture.dao.getDayRecord(TEST_DATE))
        assertEquals(PedometerPreferences(), fixture.preferencesDataSource.preferences.first())
        assertEquals(StepTrackingState(), fixture.trackingStateDataSource.state.first())
        assertEquals("", fixture.ownerDataSource.currentOwnerUserId())
    }

    private fun createFixture(): Fixture {
        val settingsDataStore = createDataStore("settings.preferences_pb")
        val authDataStore = createDataStore("auth.preferences_pb")
        val dao = FakeDayRecordDao()
        val preferencesDataSource = PedometerPreferencesDataSource(settingsDataStore)
        val trackingStateDataSource = StepTrackingStateDataSource(settingsDataStore)
        val ownerDataSource = LocalDataOwnerDataSource(authDataStore)
        return Fixture(
            dao = dao,
            preferencesDataSource = preferencesDataSource,
            trackingStateDataSource = trackingStateDataSource,
            ownerDataSource = ownerDataSource,
            repository = LocalUserDataRepositoryImpl(
                dayRecordDao = dao,
                preferencesDataSource = preferencesDataSource,
                ownerDataSource = ownerDataSource
            )
        )
    }

    private fun createDataStore(fileName: String): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { File(temporaryFolder.root, fileName) }
        )

    private fun testRecord() = DayRecordEntity(
        date = TEST_DATE,
        steps = 4_000,
        goal = 10_000,
        distanceMeters = 2_800f,
        calories = 120f
    )

    private data class Fixture(
        val dao: FakeDayRecordDao,
        val preferencesDataSource: PedometerPreferencesDataSource,
        val trackingStateDataSource: StepTrackingStateDataSource,
        val ownerDataSource: LocalDataOwnerDataSource,
        val repository: LocalUserDataRepositoryImpl
    )

    private companion object {
        const val TEST_DATE = "2026-09-02"
    }
}

private class FakeDayRecordDao : DayRecordDao {
    private val records = linkedMapOf<String, DayRecordEntity>()
    var deleteAllCallCount: Int = 0

    override fun observeDayRecord(date: String): Flow<DayRecordEntity?> =
        flowOf(records[date])

    override suspend fun getDayRecord(date: String): DayRecordEntity? = records[date]

    override fun observeDayRecordsBetween(
        startDate: String,
        endDate: String
    ): Flow<List<DayRecordEntity>> = flowOf(
        records.values.filter { record -> record.date in startDate..endDate }
    )

    override suspend fun upsert(record: DayRecordEntity) {
        records[record.date] = record
    }

    override suspend fun deleteAll() {
        deleteAllCallCount++
        records.clear()
    }
}
