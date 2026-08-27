package hs.project.steptune.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import hs.project.steptune.domain.model.AuthSession
import hs.project.steptune.core.di.AuthDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class AuthPreferencesDataSource @Inject constructor(
    @param:AuthDataStore
    private val dataStore: DataStore<Preferences>
) {
    val session: Flow<AuthSession> = dataStore.data.map { preferences ->
        AuthSession(
            accessToken = preferences[ACCESS_TOKEN].orEmpty(),
            refreshToken = preferences[REFRESH_TOKEN].orEmpty(),
            userId = preferences[USER_ID].orEmpty(),
            nickName = preferences[NICK_NAME].orEmpty()
        )
    }

    suspend fun currentSession(): AuthSession = session.first()

    suspend fun saveSession(session: AuthSession) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = session.accessToken
            preferences[REFRESH_TOKEN] = session.refreshToken
            preferences[USER_ID] = session.userId
            preferences[NICK_NAME] = session.nickName
        }
    }

    suspend fun updateUser(userId: String, nickName: String) {
        dataStore.edit { preferences ->
            preferences[USER_ID] = userId
            preferences[NICK_NAME] = nickName
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(REFRESH_TOKEN)
            preferences.remove(USER_ID)
            preferences.remove(NICK_NAME)
        }
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("auth_access_token")
        val REFRESH_TOKEN = stringPreferencesKey("auth_refresh_token")
        val USER_ID = stringPreferencesKey("auth_user_id")
        val NICK_NAME = stringPreferencesKey("auth_nick_name")
    }
}
