package com.mediaplayer.android.ui.local

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.local.LocalTrack
import com.mediaplayer.android.ui.common.LocalNowPlaying
import com.mediaplayer.android.ui.common.MHPlayingBars
import com.mediaplayer.android.ui.theme.CoverShapes
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors

/**
 * Row component for an on-device [LocalTrack]. Mirrors the layout of
 * [com.mediaplayer.android.ui.search.SongRow] (cover + title/artist + kebab)
 * but skips the backend-tied LikeButton — local likes have their own
 * DataStore and ride a dedicated heart, passed in as [liked] / [onToggleLike].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalTrackRow(
    track: LocalTrack,
    onClick: () -> Unit,
    onMore: (() -> Unit)? = null,
    liked: Boolean = false,
    onToggleLike: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val nowPlaying = LocalNowPlaying.current
    val syntheticId = -track.id
    val isCurrent = nowPlaying.currentSongId == syntheticId
    val isPlaying = isCurrent && nowPlaying.isPlaying
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LocalCover(
            track = track,
            size = 44.dp,
            shape = CoverShapes.SongRow,
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.title.ifBlank { "(senza titolo)" },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCurrent) MHColors.Lime
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee(),
                )
                if (isPlaying) {
                    Spacer(Modifier.width(6.dp))
                    MHPlayingBars(height = 12.dp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.artist.ifBlank { "Sconosciuto" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                val duration = formatDuration(track.durationMs)
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
            }
        }

        Spacer(Modifier.width(6.dp))

        if (onToggleLike != null) {
            IconButton(onClick = onToggleLike, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (liked) "Tolgi mi piace" else "Mi piace",
                    tint = if (liked) MHColors.Lime else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
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

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return ""
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
