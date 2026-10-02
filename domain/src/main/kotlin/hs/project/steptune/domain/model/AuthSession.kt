package hs.project.steptune.domain.model

data class AuthSession(
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: String = "",
    val nickName: String = ""
) {
    val hasRefreshToken: Boolean
        get() = refreshToken.isNotBlank()
}
