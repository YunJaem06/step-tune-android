package hs.project.steptune.domain.error

class ResourceNotFoundException(
    message: String = "요청한 데이터를 찾을 수 없습니다."
) : Exception(message)
