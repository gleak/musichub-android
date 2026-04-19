package com.mediaplayer.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.playback.PlaybackViewModel
import com.mediaplayer.android.ui.player.MiniPlayer
import com.mediaplayer.android.ui.player.NowPlayingSheet
import com.mediaplayer.android.ui.search.SearchScreen
import com.mediaplayer.android.ui.theme.MediaPlayerTheme

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppScaffold()
                }
            }
        }
    }
}

@UnstableApi
@androidx.compose.runtime.Composable
private fun AppScaffold() {
    // Single activity-scoped PlaybackViewModel so the search screen, the
    // mini-player, and the Now Playing sheet all observe the same state.
    val playbackVm: PlaybackViewModel = viewModel()

    val currentSong by playbackVm.currentSong.collectAsStateWithLifecycle()
    var sheetOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Main content fills the remaining space; mini-player pins to the
        // bottom of the column when a track is loaded.
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
        ) {
            SearchScreen(onSongClick = playbackVm::play)
        }

        if (currentSong != null) {
            MiniPlayer(
                viewModel = playbackVm,
                onExpand = { sheetOpen = true },
            )
        }
    }

    if (sheetOpen) {
        NowPlayingSheet(
            viewModel = playbackVm,
            onDismiss = { sheetOpen = false },
        )
    }
}
