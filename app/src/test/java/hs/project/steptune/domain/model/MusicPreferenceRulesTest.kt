package hs.project.steptune.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicPreferenceRulesTest {

    @Test
    fun `genre selection accepts up to three genres`() {
        val selected = listOf(
            MusicGenre.BALLAD,
            MusicGenre.HIP_HOP,
            MusicGenre.RNB
        ).fold(emptySet<MusicGenre>()) { genres, genre ->
            MusicPreferenceRules.toggleGenre(genres, genre)
        }

        assertEquals(
            setOf(MusicGenre.BALLAD, MusicGenre.HIP_HOP, MusicGenre.RNB),
            selected
        )
    }

    @Test
    fun `fourth genre is ignored when three genres are selected`() {
        val selected = setOf(
            MusicGenre.BALLAD,
            MusicGenre.HIP_HOP,
            MusicGenre.RNB
        )

        val result = MusicPreferenceRules.toggleGenre(selected, MusicGenre.POP)

        assertEquals(selected, result)
    }

    @Test
    fun `selected genre can be removed when genre limit is reached`() {
        val selected = setOf(
            MusicGenre.BALLAD,
            MusicGenre.HIP_HOP,
            MusicGenre.RNB
        )

        val result = MusicPreferenceRules.toggleGenre(selected, MusicGenre.HIP_HOP)

        assertEquals(setOf(MusicGenre.BALLAD, MusicGenre.RNB), result)
    }

    @Test
    fun `mood selection accepts up to two moods`() {
        val selected = listOf(
            MusicMood.CALM,
            MusicMood.ENERGETIC
        ).fold(emptySet<MusicMood>()) { moods, mood ->
            MusicPreferenceRules.toggleMood(moods, mood)
        }

        assertEquals(setOf(MusicMood.CALM, MusicMood.ENERGETIC), selected)
    }

    @Test
    fun `third mood is ignored when two moods are selected`() {
        val selected = setOf(MusicMood.CALM, MusicMood.ENERGETIC)

        val result = MusicPreferenceRules.toggleMood(selected, MusicMood.EMOTIONAL)

        assertEquals(selected, result)
    }

    @Test
    fun `selected mood can be removed when mood limit is reached`() {
        val selected = setOf(MusicMood.CALM, MusicMood.ENERGETIC)

        val result = MusicPreferenceRules.toggleMood(selected, MusicMood.CALM)

        assertEquals(setOf(MusicMood.ENERGETIC), result)
    }
}
