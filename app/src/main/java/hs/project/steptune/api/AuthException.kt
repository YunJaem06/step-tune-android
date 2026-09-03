package hs.project.steptune.api

import java.io.IOException

open class ServerException(
    message: String
) : IOException(message)

class UnauthorizedException(
    message: String = "인증 정보가 만료되었습니다."
) : ServerException(message)

class ConflictException(
    message: String = "요청한 값이 현재 서버 상태와 충돌합니다."
) : ServerException(message)

class NotFoundException(
    message: String = "요청한 데이터를 찾을 수 없습니다."
) : ServerException(message)
