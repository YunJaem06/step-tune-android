package hs.project.steptune.domain.model

object MusicPreferenceRules {
    const val MAX_GENRES = 3
    const val MAX_MOODS = 2

    fun toggleGenre(
        selectedGenres: Set<MusicGenre>,
        genre: MusicGenre
    ): Set<MusicGenre> {
        return when {
            genre in selectedGenres -> selectedGenres - genre
            selectedGenres.size < MAX_GENRES -> selectedGenres + genre
            else -> selectedGenres
        }
    }

    fun toggleMood(
        selectedMoods: Set<MusicMood>,
        mood: MusicMood
    ): Set<MusicMood> {
        return when {
            mood in selectedMoods -> selectedMoods - mood
            selectedMoods.size < MAX_MOODS -> selectedMoods + mood
            else -> selectedMoods
        }
    }
}
