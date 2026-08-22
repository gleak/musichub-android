package com.mediaplayer.android.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.DjRefusal
import com.mediaplayer.android.data.PlaylistsCache
import com.mediaplayer.android.ui.common.friendlyMessage
import com.mediaplayer.android.ui.theme.MHColors
import kotlinx.coroutines.launch

/**
 * Il patto col DJ, scritto sulla playlist che sta per essere riscritta.
 *
 * Uno slot DJ si rinnova da solo a ogni ciclo — nome compreso — e senza
 * questa card la prima sovrascrittura si legge come una perdita di dati:
 * l'utente torna sulla proposta di ieri e ci trova altro. Promuovere e' il
 * gesto con cui dice "questa tienila": da quel momento la playlist e' sua a
 * ogni effetto e il DJ non la considera piu'.
 *
 * Tiene il proprio stato invece di riceverlo da
 * [PlaylistDetailScreen]: riguarda una riga sola, e passare tre callback in
 * piu' attraverso il grafo di navigazione e il ViewModel costerebbe piu' di
 * quanto valga. L'effetto arriva comunque ovunque, perche' passa da
 * [PlaylistsCache], che e' la sorgente di verita' di ogni schermata.
 */
@Composable
fun DjProposalCard(playlistId: Long, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var promoting by remember { mutableStateOf(false) }
    var promoted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MHColors.Lime.copy(alpha = 0.10f))
            .border(1.dp, MHColors.Lime.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(
            text = "Questa e’ una proposta del DJ: si rinnova da sola, nome compreso. " +
                "Se ti piace, promuovila e diventa tua — il DJ non la tocca piu’.",
            style = MaterialTheme.typography.bodySmall,
            color = MHColors.TextHi,
        )
        if (promoted) {
            Text(
                text = "Adesso e’ tua.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MHColors.Lime,
                modifier = Modifier.padding(top = 10.dp),
            )
        } else {
            Button(
                onClick = {
                    if (promoting) return@Button
                    promoting = true
                    error = null
                    scope.launch {
                        val result = runCatching { PlaylistsCache.promote(playlistId) }
                        promoting = false
                        result.onSuccess { promoted = true }
                        result.onFailure { t ->
                            error = DjRefusal.of(t)?.message ?: friendlyMessage(t)
                        }
                    }
                },
                enabled = !promoting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MHColors.Lime,
                    contentColor = Color(0xFF0A0A0A),
                ),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                Text("Promuovi", fontWeight = FontWeight.Bold)
            }
        }
        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.TextLo,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
