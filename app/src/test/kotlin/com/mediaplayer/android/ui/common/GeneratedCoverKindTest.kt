package com.mediaplayer.android.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Una proposta del DJ deve essere riconoscibile a colpo d'occhio dentro
 * l'elenco delle playlist. Finche' `familyOf("DJ_SET")` cade nel ramo `else`
 * si presenta come un mix giornaliero, cioe' come una cosa che non e'.
 */
class GeneratedCoverKindTest {

    @Test
    fun `a DJ proposal has a family of its own`() {
        assertEquals(AutoPlaylistFamily.Dj, familyOf("DJ_SET"))
        assertNotEquals(AutoPlaylistFamily.Daily, familyOf("DJ_SET"))
    }

    @Test
    fun `the kind comparison is case-insensitive like every other one here`() {
        assertEquals(AutoPlaylistFamily.Dj, familyOf("dj_set"))
    }

    @Test
    fun `the DJ badge is its own mark`() {
        assertEquals("DJ", badgeFor("DJ_SET"))
    }

    @Test
    fun `an unknown kind still falls back to the daily mix`() {
        // Guardia di regressione: il ramo `else` deve restare, altrimenti un
        // kind nuovo lato server sparirebbe dalle liste invece di comparire
        // con un aspetto generico.
        assertEquals(AutoPlaylistFamily.Daily, familyOf("SOMETHING_NEW"))
    }

    @Test
    fun `the DJ palette is not the daily-mix palette`() {
        assertNotEquals(paletteFor(AutoPlaylistFamily.Daily), paletteFor(AutoPlaylistFamily.Dj))
    }
}
