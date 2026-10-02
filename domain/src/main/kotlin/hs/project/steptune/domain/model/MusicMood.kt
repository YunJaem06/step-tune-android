package hs.project.steptune.domain.model

enum class MusicMood(val storageKey: String) {
    CALM("calm"),
    ENERGETIC("energetic"),
    EMOTIONAL("emotional"),
    FOCUSED("focused"),
    LIVELY("lively");

    companion object {
        fun fromStorageKey(storageKey: String): MusicMood? {
            return entries.firstOrNull { mood -> mood.storageKey == storageKey }
        }
    }
}
