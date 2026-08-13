package hs.project.steptune.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicPreferenceStorageKeyTest {

    @Test
    fun `every genre can be restored from its storage key`() {
        MusicGenre.entries.forEach { genre ->
            assertEquals(genre, MusicGenre.fromStorageKey(genre.storageKey))
        }
    }

    @Test
    fun `unknown genre storage key returns null`() {
        assertNull(MusicGenre.fromStorageKey("unknown_genre"))
    }

    @Test
    fun `every mood can be restored from its storage key`() {
        MusicMood.entries.forEach { mood ->
            assertEquals(mood, MusicMood.fromStorageKey(mood.storageKey))
        }
    }

    @Test
    fun `unknown mood storage key returns null`() {
        assertNull(MusicMood.fromStorageKey("unknown_mood"))
    }
}
