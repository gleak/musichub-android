package com.mediaplayer.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors

/**
 * Bottom sheet shown when the user taps the kebab on any `SongRow`.
 * Lists all per-track actions in MusicHub style. Each option calls
 * its own callback so the host screen can wire only what it supports.
 *
 * Pass `null` callbacks to hide rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionSheet(
    song: SongDto,
    onDismiss: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onLikeToggle: (() -> Unit)? = null,
    isLiked: Boolean = false,
    onShare: (() -> Unit)? = null,
    onGoToArtist: (() -> Unit)? = null,
    onGoToAlbum: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onRemoveDownload: (() -> Unit)? = null,
    isDownloaded: Boolean = false,
    onShowLyrics: (() -> Unit)? = null,
    onShowVideo: (() -> Unit)? = null,
    onSleepTimer: (() -> Unit)? = null,
    onDislikeSong: (() -> Unit)? = null,
    onDislikeArtist: (() -> Unit)? = null,
    onFlagWrong: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mono = LocalMHMono.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161616),
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MHColors.TextLo2),
            )
        },
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            // Track header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MHCover(
                    kind = mhCoverFor(song.id).first,
                    palette = mhCoverFor(song.id).second,
                    modifier = Modifier.size(48.dp),
                    cornerRadius = 6.dp,
                )
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = MHColors.TextHi,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.artist,
                        color = MHColors.TextLo,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            androidx.compose.material3.HorizontalDivider(color = MHColors.Divider)

            // `// AZIONI` eyebrow per mockup `mh-player-sheets.jsx:407-465`.
            Text(
                text = "// AZIONI",
                style = mono.eyebrow,
                color = MHColors.TextLo,
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
            )

            onPlayNext?.let { ActionRow(Icons.AutoMirrored.Filled.QueueMusic, "Riproduci dopo", onClick = { onDismiss(); it() }) }
            onAddToQueue?.let { ActionRow(Icons.AutoMirrored.Filled.QueueMusic, "Aggiungi alla coda", onClick = { onDismiss(); it() }) }
            onAddToPlaylist?.let { ActionRow(Icons.AutoMirrored.Filled.PlaylistAdd, "Aggiungi a playlist", onClick = { onDismiss(); it() }) }
            onLikeToggle?.let {
                ActionRow(
                    Icons.Filled.Favorite,
                    if (isLiked) "Rimuovi dai preferiti" else "Aggiungi ai preferiti",
                    iconTint = if (isLiked) MHColors.Lime else MHColors.TextLo,
                    onClick = { onDismiss(); it() },
                )
            }
            onDownload?.let { ActionRow(Icons.Filled.CloudDownload, "Scarica", onClick = { onDismiss(); it() }) }
            onRemoveDownload?.let { ActionRow(Icons.Filled.Delete, "Rimuovi download", onClick = { onDismiss(); it() }) }
            onShowLyrics?.let { ActionRow(Icons.Filled.Subtitles, "Mostra testo", onClick = { onDismiss(); it() }) }
            onShowVideo?.let { ActionRow(Icons.Filled.Movie, "Apri video", onClick = { onDismiss(); it() }) }
            onGoToArtist?.let { ActionRow(Icons.Filled.Person, "Vai all'artista", onClick = { onDismiss(); it() }) }
            onGoToAlbum?.let { ActionRow(Icons.Filled.Album, "Vai all'album", onClick = { onDismiss(); it() }) }
            onShare?.let { ActionRow(Icons.Filled.Share, "Condividi", onClick = { onDismiss(); it() }) }
            onSleepTimer?.let { ActionRow(Icons.Filled.Bedtime, "Timer di spegnimento", onClick = { onDismiss(); it() }) }

            // Destructive group — divider per mockup, then dislike/report rows
            // grouped together. Rendered only when at least one callback is set
            // so non-destructive call sites stay clean.
            val anyDestructive = onDislikeSong != null || onDislikeArtist != null ||
                onFlagWrong != null || onRemoveFromPlaylist != null
            if (anyDestructive) {
                Spacer(Modifier.size(4.dp))
                androidx.compose.material3.HorizontalDivider(color = MHColors.Divider)
            }
            onDislikeSong?.let {
                ActionRow(
                    Icons.Filled.ThumbDown,
                    "Non consigliarmi questo brano",
                    iconTint = MHColors.TextLo2,
                    onClick = { onDismiss(); it() },
                )
            }
            onDislikeArtist?.let {
                ActionRow(
                    Icons.Filled.PersonOff,
                    "Non consigliarmi questo artista",
                    iconTint = MHColors.TextLo2,
                    onClick = { onDismiss(); it() },
                )
            }
            onFlagWrong?.let {
                ActionRow(
                    Icons.Filled.ReportProblem,
                    "Segnala brano sbagliato",
                    iconTint = Color(0xFFFF4D2E),
                    onClick = { onDismiss(); it() },
                )
            }
            onRemoveFromPlaylist?.let {
                ActionRow(
                    Icons.Filled.Delete,
                    "Rimuovi dalla playlist",
                    iconTint = Color(0xFFFF4D2E),
                    onClick = { onDismiss(); it() },
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = MHColors.TextLo,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = label,
            color = MHColors.TextHi,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
