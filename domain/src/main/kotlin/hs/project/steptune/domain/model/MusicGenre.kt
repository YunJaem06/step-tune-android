package hs.project.steptune.domain.model

enum class MusicGenre(val storageKey: String) {
    BALLAD("ballad"),
    HIP_HOP("hip_hop"),
    RNB("rnb"),
    POP("pop"),
    ROCK("rock"),
    INDIE("indie"),
    JAZZ("jazz"),
    CLASSICAL("classical");

    companion object {
        fun fromStorageKey(storageKey: String): MusicGenre? {
            return entries.firstOrNull { genre -> genre.storageKey == storageKey }
        }
    }
}
