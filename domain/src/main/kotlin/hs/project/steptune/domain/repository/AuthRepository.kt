package hs.project.steptune.domain.repository

import hs.project.steptune.domain.model.AuthSession
import hs.project.steptune.domain.model.NicknameAvailability
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeSession(): Flow<AuthSession>

    suspend fun getCurrentSession(): AuthSession

    suspend fun loginWithGoogle(idToken: String): AuthSession

    suspend fun refreshSession(): AuthSession

    suspend fun syncCurrentUser(): AuthSession

    suspend fun checkNicknameAvailability(nickName: String): NicknameAvailability

    suspend fun updateNickname(nickName: String): AuthSession

    suspend fun deleteAccount()

    suspend fun logout()

    suspend fun clearSession()
}
