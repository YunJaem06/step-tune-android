package hs.project.steptune.domain.repository

interface LocalUserDataRepository {
    suspend fun prepareForUser(
        userId: String,
        previousUserId: String
    )

    suspend fun clearAll()
}
