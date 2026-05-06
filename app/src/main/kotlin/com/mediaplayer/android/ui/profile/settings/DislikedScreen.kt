package com.mediaplayer.android.ui.profile.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.DislikedRepository
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.EmptyState
import com.mediaplayer.android.ui.common.SongCover
import com.mediaplayer.android.ui.theme.CoverShapes
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import kotlinx.coroutines.launch

/**
 * "Non consigliati" — mockup `mh-settings.jsx:156-207`. Pill tabs with
 * mono `· N` suffix counts, dimmed (0.7 opacity) row content because the
 * list represents removed items, and a text `Ripristina` button per row
 * instead of a bare icon. Eyebrow `// CONSIGLI` provided via
 * `SettingsSubScreen`.
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

    SettingsSubScreen(title = "Non consigliati", onBack = onBack, eyebrow = "Consigli") {
        PillTabs(
            tabs = listOf(
                "Brani" to songs.size,
                "Artisti" to artists.size,
            ),
            selectedIndex = tab,
            onSelect = { tab = it },
        )

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
private fun PillTabs(
    tabs: List<Pair<String, Int>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val mono = LocalMHMono.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEachIndexed { i, (label, count) ->
            val active = i == selectedIndex
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (active) MHColors.Lime
                        else Color.White.copy(alpha = 0.06f),
                    )
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (active) Color(0xFF0A0A0A) else MHColors.TextHi,
                )
                Text(
                    text = " · $count",
                    style = mono.duration,
                    color = if (active) Color(0xFF0A0A0A).copy(alpha = 0.7f)
                    else MHColors.TextLo,
                )
            }
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
    Column(modifier = Modifier.fillMaxWidth()) {
        songs.forEach { song ->
            DislikedSongRow(song = song, onRestore = { onRestore(song) })
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.05f),
                thickness = 0.5.dp,
            )
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
    Column(modifier = Modifier.fillMaxWidth()) {
        artists.forEach { name ->
            DislikedArtistRow(name = name, onRestore = { onRestore(name) })
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.05f),
                thickness = 0.5.dp,
            )
        }
    }
}

@Composable
private fun DislikedSongRow(song: SongDto, onRestore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.alpha(0.5f),
        ) {
            SongCover(song = song, size = 44.dp, shape = CoverShapes.SongRow)
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(0.7f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
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
        RestorePillButton(onClick = onRestore)
    }
}

@Composable
private fun DislikedArtistRow(name: String, onRestore: () -> Unit) {
    val mono = LocalMHMono.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .alpha(0.5f)
                .size(44.dp)
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
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(0.7f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MHColors.TextHi,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Artista",
                style = mono.duration,
                color = MHColors.TextLo,
            )
        }
        RestorePillButton(onClick = onRestore)
    }
}

@Composable
private fun RestorePillButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, MHColors.Lime.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = "Ripristina",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MHColors.Lime,
        )
    }
}
