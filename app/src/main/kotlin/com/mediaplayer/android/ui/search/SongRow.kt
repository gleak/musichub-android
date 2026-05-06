package com.mediaplayer.android.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.LocalNowPlaying
import com.mediaplayer.android.ui.common.MHPlayingBars
import com.mediaplayer.android.ui.common.SongCover
import com.mediaplayer.android.ui.theme.CoverShapes
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: SongDto,
    onClick: () -> Unit = {},
    isLiked: Boolean = false,
    onToggleLike: (() -> Unit)? = null,
    isDownloaded: Boolean = false,
    onArtistClick: ((String) -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    rowGestureModifier: Modifier = Modifier,
    /**
     * Optional contributor tag rendered as a lime mono uppercase pill in
     * the subtitle (mockup `mh-library.jsx:319`). Used on collaborative
     * playlists to mark per-track contributors. Null on every other surface.
     */
    contributorTag: String? = null,
) {
    // Auto-detect the active row from the ambient `LocalNowPlaying` state so
    // every call site picks up the lime title + animated `MHPlayingBars`
    // indicator (mockup `mh-screens.jsx:91-95`, `mh-shared.jsx:282-298`)
    // without having to plumb the playback VM through.
    val nowPlaying = LocalNowPlaying.current
    val isCurrentTrack = nowPlaying.currentSongId == song.id
    val isPlaying = isCurrentTrack && nowPlaying.isPlaying
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(rowGestureModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongCover(song = song, size = 44.dp, shape = CoverShapes.SongRow)

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCurrentTrack) MHColors.Lime
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee(),
                )
                if (isCurrentTrack && isPlaying) {
                    Spacer(Modifier.width(6.dp))
                    MHPlayingBars(height = 12.dp)
                }
            }
            SubtitleRow(
                song = song,
                onArtistClick = onArtistClick,
                isDownloaded = isDownloaded,
                contributorTag = contributorTag,
            )
        }

        Spacer(Modifier.width(6.dp))

        if (onToggleLike != null) {
            val haptics = LocalHapticFeedback.current
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleLike()
                    }
                    .padding(4.dp),
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isLiked) "Rimuovi mi piace" else "Mi piace",
                    tint = if (isLiked) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (onMore != null) {
            IconButton(onClick = onMore, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Altre opzioni",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SubtitleRow(
    song: SongDto,
    onArtistClick: ((String) -> Unit)?,
    isDownloaded: Boolean,
    contributorTag: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isDownloaded) {
            Icon(
                imageVector = Icons.Filled.FileDownloadDone,
                contentDescription = "Scaricato",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = (if (onArtistClick != null)
                Modifier.clickable { onArtistClick(song.artist) }
            else Modifier).weight(1f, fill = false),
        )
        val duration = formatDuration(song.durationMs)
        if (duration.isNotEmpty()) {
            Text(
                text = " · ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = duration,
                style = LocalMHMono.current.duration,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (!contributorTag.isNullOrBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = "· ${contributorTag.uppercase()}",
                style = LocalMHMono.current.badge.copy(
                    color = com.mediaplayer.android.ui.theme.MHColors.Lime,
                ),
                maxLines = 1,
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return ""
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
