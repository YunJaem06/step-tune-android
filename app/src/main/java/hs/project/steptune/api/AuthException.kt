package hs.project.steptune.api

import java.io.IOException

open class ServerException(
    message: String
) : IOException(message)

class UnauthorizedException(
    message: String = "인증 정보가 만료되었습니다."
) : ServerException(message)
