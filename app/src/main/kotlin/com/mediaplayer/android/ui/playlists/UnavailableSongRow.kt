package com.mediaplayer.android.ui.playlists

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.SongCover
import com.mediaplayer.android.ui.theme.CoverShapes
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors

/**
 * Riga per un brano segnaposto dentro una playlist: il DJ l'ha scelto ma il
 * download non e' ancora finito (`SongDto.playable == false` — lo stesso
 * campo che oggi segnala anche un file sparito, quindi questa riga serve
 * anche per quel caso).
 *
 * Deliberatamente un componente a parte e non un ramo dentro
 * `com.mediaplayer.android.ui.search.SongRow`: quel componente e' condiviso
 * da schermate che questo lavoro non deve toccare, ed e' meglio isolare la
 * variante "spenta" qui piuttosto che infilare un altro parametro opzionale
 * in un componente gia' usato ovunque.
 *
 * Niente `onClick`: il brano non ha un file da riprodurre, quindi la riga
 * resta inerte (nessuna ripple, nessun tap che finisce nel player). Resta
 * pero' visibile con copertina, titolo e artista — il DJ l'ha scelto e
 * l'utente deve poterlo vedere, non far sparire la riga come se non
 * esistesse.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnavailableSongRow(
    song: SongDto,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Alpha ridotta su tutta la riga (non solo sul testo) cosi' anche la
        // copertina si legge come "non ancora pronta" invece che come un
        // brano normale con solo l'etichetta diversa.
        Row(
            modifier = Modifier.weight(1f).alpha(UNAVAILABLE_ROW_ALPHA),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SongCover(song = song, size = 44.dp, shape = CoverShapes.SongRow)

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Etichetta "In arrivo" al posto della durata/like/kebab: non c'e'
        // ancora un brano vero su cui agire.
        val mono = LocalMHMono.current
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MHColors.TextLo.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = "IN ARRIVO",
                style = mono.badge.copy(color = MHColors.TextLo),
            )
        }
    }
}

/** Applicata a copertina + testo, non all'etichetta "IN ARRIVO": quella deve restare leggibile. */
private const val UNAVAILABLE_ROW_ALPHA = 0.45f
