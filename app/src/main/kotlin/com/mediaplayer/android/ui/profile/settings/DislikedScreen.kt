package com.mediaplayer.android.ui.profile.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.DislikedRepository
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.EmptyState
import com.mediaplayer.android.ui.common.SongCover
import com.mediaplayer.android.ui.theme.CoverShapes
import com.mediaplayer.android.ui.theme.MHColors
import kotlinx.coroutines.launch

/**
 * "Don't recommend" management screen — two tabs (Songs / Artists)
 * listing the user's current dislikes with a one-tap restore. Reads
 * from [DislikedRepository] which falls back to ReadCache offline.
 */
@Composable
fun DislikedScreen(onBack: () -> Unit) {
    val repo = remember { DislikedRepository() }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }

    var songs by remember { mutableStateOf<List<SongDto>>(emptyList()) }
    var artists by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        loading = true
        errorMessage = null
        try {
            songs = repo.dislikedSongs(page = 0, size = 50).items
            artists = repo.dislikedArtists()
        } catch (t: Throwable) {
            errorMessage = t.message ?: "Errore di rete"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    SettingsSubScreen(title = "Non consigliarmi", onBack = onBack) {
        SettingsCard {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("Brani (${songs.size})") },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("Artisti (${artists.size})") },
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        when (tab) {
            0 -> DislikedSongsList(
                loading = loading,
                songs = songs,
                onRestore = { song ->
                    songs = songs.filterNot { it.id == song.id }
                    scope.launch { repo.undislikeSong(song.id) }
                },
            )
            else -> DislikedArtistsList(
                loading = loading,
                artists = artists,
                onRestore = { name ->
                    artists = artists.filterNot { it.equals(name, ignoreCase = true) }
                    scope.launch { repo.undislikeArtist(name) }
                },
            )
        }
    }
}

@Composable
private fun DislikedSongsList(
    loading: Boolean,
    songs: List<SongDto>,
    onRestore: (SongDto) -> Unit,
) {
    if (!loading && songs.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.ThumbDown,
            title = "Nessun brano escluso",
            subtitle = "Quando segni un brano con \"Non consigliarmi\", appare qui.",
        )
        return
    }
    SettingsCard {
        LazyColumn {
            items(items = songs, key = { it.id }) { song ->
                DislikedSongRow(song = song, onRestore = { onRestore(song) })
                HorizontalDivider(color = MHColors.Divider, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun DislikedArtistsList(
    loading: Boolean,
    artists: List<String>,
    onRestore: (String) -> Unit,
) {
    if (!loading && artists.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.PersonOff,
            title = "Nessun artista escluso",
            subtitle = "Quando segni un artista con \"Non consigliarmi\", appare qui.",
        )
        return
    }
    SettingsCard {
        LazyColumn {
            items(items = artists, key = { it }) { name ->
                DislikedArtistRow(name = name, onRestore = { onRestore(name) })
                HorizontalDivider(color = MHColors.Divider, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun DislikedSongRow(song: SongDto, onRestore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongCover(song = song, size = 40.dp, shape = CoverShapes.SongRow)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MHColors.TextHi,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.TextLo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRestore) {
            Icon(
                imageVector = Icons.Filled.Restore,
                contentDescription = "Ripristina nei consigli",
                tint = MHColors.TextHi,
            )
        }
    }
}

@Composable
private fun DislikedArtistRow(name: String, onRestore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MHColors.Card),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PersonOff,
                contentDescription = null,
                tint = MHColors.TextLo,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MHColors.TextHi,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onRestore) {
            Icon(
                imageVector = Icons.Filled.Restore,
                contentDescription = "Ripristina nei consigli",
                tint = MHColors.TextHi,
            )
        }
    }
}
