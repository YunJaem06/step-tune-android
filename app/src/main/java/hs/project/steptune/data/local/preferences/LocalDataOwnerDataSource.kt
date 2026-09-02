package hs.project.steptune.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import hs.project.steptune.core.di.AuthDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class LocalDataOwnerDataSource @Inject constructor(
    @param:AuthDataStore
    private val dataStore: DataStore<Preferences>
) {
    suspend fun currentOwnerUserId(): String =
        dataStore.data.first()[OWNER_USER_ID].orEmpty()

    suspend fun updateOwnerUserId(userId: String) {
        dataStore.edit { preferences ->
            preferences[OWNER_USER_ID] = userId
        }
    }

    suspend fun clearOwnerUserId() {
        dataStore.edit { preferences ->
            preferences.remove(OWNER_USER_ID)
        }
    }

    private companion object {
        val OWNER_USER_ID = stringPreferencesKey("local_data_owner_user_id")
    }
}
