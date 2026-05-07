package com.mediaplayer.android.ui.local

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.data.local.LocalTrack
import com.mediaplayer.android.ui.common.CenteredMessage
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MediaPlayerSpacing

/**
 * Drill-down screen showing every track that lives in a folder ([path]) or
 * a single album ([albumName]). Reuses [LocalTrackRow] so playback wiring
 * matches the main library tab.
 */
@Composable
fun LocalFolderOrAlbumScreen(
    titlePrefix: String,
    title: String,
    matcher: (LocalTrack) -> Boolean,
    onBack: () -> Unit,
    onPlay: (LocalTrack, List<LocalTrack>) -> Unit,
    onShuffle: (List<LocalTrack>) -> Unit,
    onPlayNext: (LocalTrack) -> Unit,
    onAddToQueue: (LocalTrack) -> Unit,
    viewModel: LocalLibraryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tracks: List<LocalTrack> = remember(state, matcher) {
        (state as? LocalLibraryViewModel.State.Ready)
            ?.tracks
            ?.filter(matcher)
            .orEmpty()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MediaPlayerSpacing.S, vertical = MediaPlayerSpacing.S),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Indietro",
                )
            }
            Spacer(Modifier.width(MediaPlayerSpacing.Xs))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titlePrefix,
                    style = MaterialTheme.typography.bodySmall,
                    color = MHColors.TextLo2,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MHColors.TextHi,
                    maxLines = 1,
                )
            }
        }

        if (tracks.isEmpty()) {
            CenteredMessage(text = "Nessun brano qui.")
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MediaPlayerSpacing.M),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MediaPlayerSpacing.S),
        ) {
            FilledTonalButton(onClick = { onShuffle(tracks) }) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(MediaPlayerSpacing.Xs))
                Text("Riproduzione casuale")
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${tracks.size} brani",
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.TextLo,
            )
        }
        Spacer(Modifier.height(MediaPlayerSpacing.S))

        var menuFor by remember { mutableStateOf<LocalTrack?>(null) }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            items(tracks, key = { it.id }) { t ->
                Box {
                    LocalTrackRow(
                        track = t,
                        onClick = { onPlay(t, tracks) },
                        onMore = { menuFor = t },
                    )
                    if (menuFor?.id == t.id) {
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = { menuFor = null },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Riproduci") },
                                onClick = {
                                    menuFor = null
                                    onPlay(t, tracks)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Riproduci dopo") },
                                onClick = {
                                    menuFor = null
                                    onPlayNext(t)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Aggiungi alla coda") },
                                onClick = {
                                    menuFor = null
                                    onAddToQueue(t)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
