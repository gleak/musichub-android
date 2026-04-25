package com.mediaplayer.android.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mediaplayer.android.R
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.SongDto
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: SongDto,
    onClick: () -> Unit = {},
    onLongPress: (() -> Unit)? = null,
    isLiked: Boolean = false,
    onToggleLike: (() -> Unit)? = null,
    isDownloaded: Boolean = false,
    onArtistClick: ((String) -> Unit)? = null,
    onAlbumClick: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(song = song)

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SubtitleRow(song = song, onArtistClick = onArtistClick, onAlbumClick = onAlbumClick)
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = formatDuration(song.durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (onToggleLike != null) {
            IconButton(onClick = onToggleLike, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isLiked) "Unlike" else "Like",
                    tint = if (isLiked) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (isDownloaded) {
            Icon(
                imageVector = Icons.Filled.FileDownloadDone,
                contentDescription = "Downloaded",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun CoverArt(song: SongDto) {
    val size = 52.dp
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (song.hasCoverArt) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Network.coverUrl(song.id))
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.content_desc_cover_art),
                modifier = Modifier.size(size),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SubtitleRow(
    song: SongDto,
    onArtistClick: ((String) -> Unit)?,
    onAlbumClick: ((String, String) -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = if (onArtistClick != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (onArtistClick != null)
                Modifier.clickable { onArtistClick(song.artist) }
            else Modifier,
        )
        if (!song.album.isNullOrBlank()) {
            Text(
                text = " · ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = song.album,
                style = MaterialTheme.typography.bodyMedium,
                color = if (onAlbumClick != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (onAlbumClick != null)
                    Modifier.clickable { onAlbumClick(song.album, song.artist) }
                else Modifier,
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
