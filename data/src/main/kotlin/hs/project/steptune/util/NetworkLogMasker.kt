package hs.project.steptune.util

object NetworkLogMasker {
    private val sensitiveJsonValue = Regex(
        pattern = """("(?:token|accessToken|refreshToken)"\s*:\s*")[^"]*(")""",
        option = RegexOption.IGNORE_CASE
    )

    fun mask(message: String): String = sensitiveJsonValue.replace(message) { match ->
        "${match.groupValues[1]}██${match.groupValues[2]}"
    }
}
