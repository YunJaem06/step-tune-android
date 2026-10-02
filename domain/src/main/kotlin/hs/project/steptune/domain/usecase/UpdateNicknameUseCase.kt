package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.model.AuthSession
import hs.project.steptune.domain.repository.AuthRepository
import javax.inject.Inject

class UpdateNicknameUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(nickName: String): AuthSession =
        repository.updateNickname(nickName)
}
