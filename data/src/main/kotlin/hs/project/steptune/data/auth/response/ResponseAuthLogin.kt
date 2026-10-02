package hs.project.steptune.data.auth.response

data class ResponseAuthLogin(
    val accessToken: String,
    val accessTokenExpiresIn: Long,
    val refreshToken: String,
    val userData: ResponseUserData
)
