package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.repository.AuthRepository
import javax.inject.Inject

class SyncCurrentUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.syncCurrentUser()
}
