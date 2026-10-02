package hs.project.steptune.data.config

class NetworkConfig(
    val baseUrl: String,
    val enableHttpLogging: Boolean,
    val logMessage: (String) -> Unit
)
