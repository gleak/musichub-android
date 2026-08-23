package com.mediaplayer.android.ui.common

import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * `Text` a riga singola che fa scorrere il contenuto quando non ci sta,
 * invece di troncarlo con l'ellissi e basta. Nato per i titoli generati
 * dal DJ ("Nu-Metal & Alternative Rock Essentials", "Rock Alternativo:
 * Malinconia e Atmosfera"...), che nelle tessere di Home/Libreria/Per te
 * finivano illeggibili senza aprire la playlist.
 *
 * Due passaggi deliberati, non uno:
 * 1) primo layout, per scoprire via `onTextLayout` se il testo eccede
 *    davvero lo spazio disponibile;
 * 2) solo se eccede, si aggiunge `basicMarquee()`.
 * Il costo di `basicMarquee` (un modifier node + un'animazione che gira
 * finche' il testo e' composto) lo paghiamo solo dove serve — nelle liste
 * lunghe la stragrande maggioranza dei titoli ci sta gia' nella tessera,
 * e per quelli il componente resta un `Text` normale.
 *
 * **Il difetto che questa versione corregge**, segnalato dall'utente perche'
 * lo scorrimento semplicemente non partiva. La prima versione misurava
 * `result.didOverflowWidth` tenendo `softWrap = true`: ma con `softWrap`
 * attivo e `maxLines = 1` un titolo di PIU' PAROLE non straborda in
 * larghezza — va a capo al primo spazio e viene troncato in **altezza**.
 * `didOverflowWidth` restava quindi `false` e il marquee non si attivava
 * mai, tranne che per una singola parola piu' larga della tessera. Proprio
 * i nomi che hanno motivato il componente ("Nu-Metal & Alternative Rock
 * Essentials") hanno spazi, quindi erano esattamente il caso che non
 * funzionava.
 *
 * La correzione e' tenere `softWrap = false` fin dal primo layout: senza
 * andata a capo l'eccedenza torna a essere di larghezza, ed e' quella che
 * `hasVisualOverflow` riporta.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
) {
    // remember(text) e non solo remember{}: se il testo cambia (rename di
    // una playlist, riciclo dello slot in una lazy list) vogliamo rifare la
    // misurazione invece di trascinarci dietro lo stato del testo precedente.
    var overflowing by remember(text) { mutableStateOf(false) }

    // L'unico modo pubblico per sapere se l'utente ha chiesto "meno
    // animazioni" e' la scala di durata degli animator di sistema
    // (Impostazioni sviluppatore > Scala animazioni, e su alcuni OEM anche
    // Accessibilita' > Rimuovi animazioni). Jetpack Compose, in questa
    // versione della BOM (2026.04.01 / foundation 1.11.0), non espone una
    // CompositionLocal tipo "LocalReduceMotion": ne androidx.compose.ui ne
    // androidx.compose.foundation hanno una API dedicata al motion-reduce
    // (verificato decompilando i due .aar). Leggere l'animator scale e'
    // l'unico proxy disponibile ad libitum; se e' zero rispettiamo la scelta
    // e restiamo su un testo troncato normale, statico.
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    Text(
        text = text,
        modifier = modifier.then(
            if (overflowing && !reduceMotion) {
                // Ritardo iniziale sfalsato per testo: in un carosello con
                // piu' tessere troncate, farle partire tutte a t=0 e' rumore
                // visivo puro (tutto lampeggia insieme). Deriviamo lo sfasamento
                // dall'hashCode del titolo — deterministico, niente stato o
                // indice da passare dal chiamante — e lo spalmiamo su ~1.2s
                // sopra il ritardo base di MarqueeDefaults, cosi' le tessere
                // partono in tempi diversi anche se entrano in vista insieme.
                // Firma verificata decompilando foundation-android 1.11.0
                // (quella risolta da questo progetto): i nomi Kotlin dei
                // parametri non sono leggibili dal .class compilato (niente
                // sources.jar in cache), ma l'ORDINE si', da bytecode —
                // (iterations, animationMode, delayMillis, initialDelayMillis,
                // spacing, velocity). Per questo qui sotto e' posizionale
                // fino a initialDelayMillis e lascia spacing/velocity ai
                // default di MarqueeDefaults.
                val stagger = Math.floorMod(text.hashCode(), 1200)
                Modifier.basicMarquee(
                    // Continuo, non tre giri. Un'animazione che si esaurisce
                    // risolve il problema solo per chi guarda nei primi
                    // secondi: chi apre la schermata dopo si ritrova di nuovo
                    // un nome troncato, cioe' esattamente cio' da cui siamo
                    // partiti. Il costo resta contenuto perche' animano solo i
                    // testi che eccedono davvero, e solo finche' la tessera e'
                    // composta — le lazy list compongono i soli elementi visibili.
                    /* iterations = */ Int.MAX_VALUE,
                    /* animationMode = */ androidx.compose.foundation.MarqueeAnimationMode.Immediately,
                    /* delayMillis = */ 1200,
                    /* initialDelayMillis = */ 1200 + stagger,
                )
            } else {
                Modifier
            }
        ),
        style = style,
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = 1,
        // softWrap SEMPRE false, anche al primo layout: e' cio' che trasforma
        // l'eccedenza da "altezza" (va a capo, poi maxLines taglia) a
        // "larghezza", l'unica che il marquee sa scorrere e l'unica che
        // hasVisualOverflow puo' segnalarci qui sotto.
        softWrap = false,
        overflow = if (overflowing) TextOverflow.Clip else TextOverflow.Ellipsis,
        onTextLayout = { result ->
            if (!overflowing && result.hasVisualOverflow) {
                overflowing = true
            }
        },
    )
}
