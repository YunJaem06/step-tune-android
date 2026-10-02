package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.repository.AuthRepository
import javax.inject.Inject

class LoginWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(idToken: String) = repository.loginWithGoogle(idToken)
}
