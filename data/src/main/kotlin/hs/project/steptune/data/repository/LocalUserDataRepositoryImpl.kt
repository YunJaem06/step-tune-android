package hs.project.steptune.data.repository

import hs.project.steptune.data.local.database.DayRecordDao
import hs.project.steptune.data.local.preferences.LocalDataOwnerDataSource
import hs.project.steptune.data.local.preferences.PedometerPreferencesDataSource
import hs.project.steptune.domain.repository.LocalUserDataRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalUserDataRepositoryImpl @Inject constructor(
    private val dayRecordDao: DayRecordDao,
    private val preferencesDataSource: PedometerPreferencesDataSource,
    private val ownerDataSource: LocalDataOwnerDataSource
) : LocalUserDataRepository {
    override suspend fun prepareForUser(
        userId: String,
        previousUserId: String
    ) {
        require(userId.isNotBlank()) { "userId must not be blank" }

        val ownerUserId = ownerDataSource.currentOwnerUserId()
        if (ownerUserId == userId) return

        if (ownerUserId.isBlank() && previousUserId == userId) {
            ownerDataSource.updateOwnerUserId(userId)
            return
        }

        clearLocalRecordsAndPreferences()
        ownerDataSource.updateOwnerUserId(userId)
    }

    override suspend fun clearAll() {
        clearLocalRecordsAndPreferences()
        ownerDataSource.clearOwnerUserId()
    }

    private suspend fun clearLocalRecordsAndPreferences() {
        dayRecordDao.deleteAll()
        preferencesDataSource.clearAll()
    }
}
