package hs.project.steptune.data

data class ServerResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)
