package com.mediaplayer.android.ui.dj

import org.junit.Assert.assertEquals
import org.junit.Test

class DjTurnPhaseTextTest {

    @Test
    fun `traduce le fasi note`() {
        assertEquals("sta cercando nella tua libreria", phaseText("CATALOG"))
        assertEquals("sta scrivendo la risposta", phaseText("WRITING"))
        assertEquals("sta guardando i tuoi gusti", phaseText("PROFILE"))
    }

    /**
     * Una fase aggiunta dal backend e sconosciuta a questa versione dell'app
     * non deve produrre una riga vuota sotto "sta pensando": sembrerebbe un
     * difetto dell'app, non una versione da aggiornare.
     */
    @Test
    fun `una fase sconosciuta ricade sul generico`() {
        assertEquals("sta leggendo il tuo messaggio", phaseText("FASE_NUOVA"))
        assertEquals("sta leggendo il tuo messaggio", phaseText(null))
    }

    @Test
    fun `il tempo trascorso e minuti e secondi, mai secondi grezzi`() {
        assertEquals("0:00", formatElapsed(0))
        assertEquals("0:47", formatElapsed(47))
        assertEquals("2:05", formatElapsed(125))
        assertEquals("0:00", formatElapsed(-3))
    }
}
