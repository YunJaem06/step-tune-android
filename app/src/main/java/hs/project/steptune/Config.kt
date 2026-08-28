package hs.project.steptune

object Config {
    val BASE_URL: String = BuildConfig.BASE_URL
    val GOOGLE_WEB_CLIENT_ID: String = BuildConfig.GOOGLE_WEB_CLIENT_ID

    object API {
        const val AUTH_SOCIAL = "api/v1/auth/social"
        const val AUTH_REFRESH = "api/v1/auth/refresh"
        const val AUTH_LOGOUT = "api/v1/auth/logout"
        const val USER_PROFILE = "api/v1/me/profile"
        const val USER_NICKNAME = "api/v1/me/nickname"
        const val USER_NICKNAME_AVAILABILITY = "api/v1/me/nickname/availability"
        const val USER_ACCOUNT = "api/v1/me/account"
    }
}
