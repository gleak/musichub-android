package com.mediaplayer.android.ui.dj

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Il tetto di frequenza rifiuta con un numero di secondi. Mostrarlo cosi'
 * com'e' scaricherebbe sull'utente una conversione che il codice puo' fare.
 */
class DjWaitFormatTest {

    @Test
    fun `seconds become something a person can act on`() {
        assertEquals("45 s", formatWait(45))
        assertEquals("4 min", formatWait(240))
        assertEquals("5 min 5 s", formatWait(305))
        assertEquals("0 s", formatWait(0))
    }

    @Test
    fun `a negative wait is not a negative countdown`() {
        // Il server manda un intero non negativo, ma un client che ricalcolasse
        // da un timestamp potrebbe arrivare qui sotto zero.
        assertEquals("0 s", formatWait(-3))
    }
}
