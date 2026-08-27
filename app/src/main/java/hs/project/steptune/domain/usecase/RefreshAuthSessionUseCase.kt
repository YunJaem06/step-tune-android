package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.repository.AuthRepository
import javax.inject.Inject

class RefreshAuthSessionUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.refreshSession()
}
