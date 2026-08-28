package hs.project.steptune.data.repository

import hs.project.steptune.api.AuthAPI
import hs.project.steptune.api.ConflictException
import hs.project.steptune.api.ServerException
import hs.project.steptune.api.UnauthorizedException
import hs.project.steptune.data.ServerResponse
import hs.project.steptune.data.auth.response.ResponseAuthLogin
import hs.project.steptune.data.local.preferences.AuthPreferencesDataSource
import hs.project.steptune.data.user.request.RequestUpdateNickname
import hs.project.steptune.domain.model.AuthSession
import hs.project.steptune.domain.model.NicknameAvailability
import hs.project.steptune.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import retrofit2.Response

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authAPI: AuthAPI,
    private val authPreferencesDataSource: AuthPreferencesDataSource
) : AuthRepository {
    override fun observeSession(): Flow<AuthSession> = authPreferencesDataSource.session

    override suspend fun getCurrentSession(): AuthSession =
        authPreferencesDataSource.currentSession()

    override suspend fun loginWithGoogle(idToken: String): AuthSession {
        val authData = authAPI.requestSocialLogin(
            AuthAPI.RequestSocialLogin(
                provider = GOOGLE_PROVIDER,
                token = idToken
            )
        ).requireData()
        return saveAuthData(authData)
    }

    override suspend fun refreshSession(): AuthSession {
        val refreshToken = getCurrentSession().refreshToken
        if (refreshToken.isBlank()) {
            throw UnauthorizedException()
        }

        return try {
            val authData = authAPI.requestRefreshToken(
                AuthAPI.RequestRefreshToken(refreshToken)
            ).requireData()
            saveAuthData(authData)
        } catch (exception: UnauthorizedException) {
            authPreferencesDataSource.clearSession()
            throw exception
        }
    }

    override suspend fun syncCurrentUser(): AuthSession {
        val sessionBeforeRequest = getCurrentSession()
        if (sessionBeforeRequest.accessToken.isBlank()) {
            throw UnauthorizedException()
        }
        val user = authAPI.requestMyInfo().requireData()
        val latestSession = getCurrentSession()
        if (latestSession.accessToken != sessionBeforeRequest.accessToken) {
            return latestSession
        }
        authPreferencesDataSource.updateUser(
            userId = user.userId.toString(),
            nickName = user.nickName
        )
        return getCurrentSession()
    }

    override suspend fun checkNicknameAvailability(nickName: String): NicknameAvailability {
        val response = authAPI.requestNicknameAvailability(nickName).requireData()
        return NicknameAvailability(
            nickName = response.nickName,
            isAvailable = response.available
        )
    }

    override suspend fun updateNickname(nickName: String): AuthSession {
        val user = authAPI.requestUpdateNickname(
            RequestUpdateNickname(nickName = nickName)
        ).requireData()
        authPreferencesDataSource.updateUser(
            userId = user.userId.toString(),
            nickName = user.nickName
        )
        return getCurrentSession()
    }

    override suspend fun deleteAccount() {
        authAPI.requestDeleteAccount().requireSuccess()
        authPreferencesDataSource.clearSession()
    }

    override suspend fun logout() {
        val refreshToken = getCurrentSession().refreshToken
        try {
            if (refreshToken.isNotBlank()) {
                authAPI.requestLogout(AuthAPI.RequestLogout(refreshToken))
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // 서버 연결 실패 여부와 관계없이 기기에서는 로그아웃한다.
        } finally {
            authPreferencesDataSource.clearSession()
        }
    }

    override suspend fun clearSession() {
        authPreferencesDataSource.clearSession()
    }

    private suspend fun saveAuthData(data: ResponseAuthLogin): AuthSession {
        val session = AuthSession(
            accessToken = data.accessToken,
            refreshToken = data.refreshToken,
            userId = data.userData.userId.toString(),
            nickName = data.userData.nickName
        )
        authPreferencesDataSource.saveSession(session)
        return session
    }

    private fun <T> Response<ServerResponse<T>>.requireData(): T {
        if (code() == HTTP_UNAUTHORIZED) {
            throw UnauthorizedException()
        }
        if (code() == HTTP_CONFLICT) {
            throw ConflictException(body()?.message ?: "이미 사용 중인 닉네임입니다.")
        }

        val responseBody = body()
        if (!isSuccessful || responseBody?.code != HTTP_OK) {
            throw ServerException(responseBody?.message ?: "서버 요청에 실패했습니다.")
        }
        return responseBody.data
            ?: throw ServerException("서버 응답에 필요한 데이터가 없습니다.")
    }

    private fun Response<ServerResponse<Any>>.requireSuccess() {
        if (code() == HTTP_UNAUTHORIZED) {
            throw UnauthorizedException()
        }

        val responseBody = body()
        if (!isSuccessful || responseBody?.code != HTTP_OK) {
            throw ServerException(responseBody?.message ?: "서버 요청에 실패했습니다.")
        }
    }

    private companion object {
        const val GOOGLE_PROVIDER = "google"
        const val HTTP_OK = 200
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_CONFLICT = 409
    }
}
