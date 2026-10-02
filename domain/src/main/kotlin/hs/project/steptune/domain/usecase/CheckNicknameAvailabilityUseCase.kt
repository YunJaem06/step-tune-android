package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.model.NicknameAvailability
import hs.project.steptune.domain.repository.AuthRepository
import javax.inject.Inject

class CheckNicknameAvailabilityUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(nickName: String): NicknameAvailability =
        repository.checkNicknameAvailability(nickName)
}
