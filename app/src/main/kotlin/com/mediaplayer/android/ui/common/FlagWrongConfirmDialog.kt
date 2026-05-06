package com.mediaplayer.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors

/**
 * Custom "Segnala brano sbagliato?" confirm dialog matching mockup
 * `mh-player-sheets.jsx:238-266`. Replaces the stock Material `AlertDialog`
 * to add the red `// SEGNALA · DEFINITIVO` eyebrow, the embedded track
 * preview row, and a destructive red `Segnala` CTA. Reused by every
 * surface that surfaces the flag-wrong action so the visual contract
 * stays in sync.
 *
 * `hasCoverArt = false` falls back to a generic note icon — pass `true`
 * when the caller knows the song has artwork on file, otherwise the
 * Coil request still tries to fetch and the placeholder fills the gap.
 */
@Composable
fun FlagWrongConfirmDialog(
    songId: Long,
    songTitle: String,
    songArtist: String,
    hasCoverArt: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val mono = LocalMHMono.current
    val danger = Color(0xFFE14848)
    val cardShape = RoundedCornerShape(16.dp)
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .background(color = Color(0xFF181818), shape = cardShape)
                .border(width = 1.dp, color = MHColors.Divider, shape = cardShape)
                .padding(22.dp),
        ) {
            Column {
                Text(
                    text = "// SEGNALA · DEFINITIVO",
                    style = mono.eyebrow,
                    color = danger,
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = "Brano sbagliato?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MHColors.TextHi,
                )
                Spacer(Modifier.size(12.dp))

                // Embedded track preview — cover + title + artist as a single row.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF0E0E0E),
                            shape = RoundedCornerShape(10.dp),
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SongCover(songId = songId, hasCoverArt = hasCoverArt, size = 40.dp)
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = songTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MHColors.TextHi,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = songArtist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MHColors.TextLo,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.size(14.dp))
                Text(
                    text = "Verrà rimosso da ricerca, playlist, brani che ti piacciono " +
                        "e cronologia su tutti i tuoi dispositivi. Il file sarà " +
                        "eliminato dal server e il match non verrà ri-scaricato in futuro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MHColors.TextLo,
                )
                Spacer(Modifier.size(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(
                        label = "Annulla",
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                    )
                    PillButton(
                        label = "Segnala",
                        modifier = Modifier.weight(1f),
                        background = danger,
                        contentColor = Color.White,
                        onClick = onConfirm,
                    )
                }
            }
        }
    }
}

@Composable
private fun PillButton(
    label: String,
    modifier: Modifier = Modifier,
    background: Color = Color.White.copy(alpha = 0.06f),
    contentColor: Color = MHColors.TextHi,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(color = background, shape = RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}
