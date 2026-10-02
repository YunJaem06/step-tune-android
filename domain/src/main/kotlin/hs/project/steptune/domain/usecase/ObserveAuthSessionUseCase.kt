package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.repository.AuthRepository
import javax.inject.Inject

class ObserveAuthSessionUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke() = repository.observeSession()
}
