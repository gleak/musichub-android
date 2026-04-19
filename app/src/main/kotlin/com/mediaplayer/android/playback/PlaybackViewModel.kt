package com.mediaplayer.android.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.SongDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * UI-facing facade over the [MediaController].
 *
 * Turns the imperative Media3 callback API into cold-ish state flows that
 * Compose can collect:
 *
 *  - [currentSong]  — the full DTO we pushed in, recovered via MediaItem extras
 *  - [isPlaying]    — live `Player.isPlaying`
 *  - [positionMs]   — poll-driven current position (updates every 500ms while playing)
 *  - [durationMs]   — known track duration or C.TIME_UNSET while unknown
 *
 * One instance is expected per Activity scope (the default `viewModel()`
 * owner). The VM doesn't own a player — it only subscribes to whichever
 * controller [PlayerConnection] hands it.
 */
@UnstableApi
class PlaybackViewModel : ViewModel() {

    private val _currentSong = MutableStateFlow<SongDto?>(null)
    val currentSong: StateFlow<SongDto?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private var controller: MediaController? = null
    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentSong.value = mediaItem?.toSongDto()
            pushDuration()
        }

        override fun onPlaybackStateChanged(state: Int) {
            pushDuration()
        }
    }

    init {
        // Bind to PlayerConnection's flow; swap listeners across reconnects.
        viewModelScope.launch {
            PlayerConnection.controller.collectLatest { c ->
                controller?.removeListener(listener)
                controller = c
                if (c != null) {
                    c.addListener(listener)
                    _isPlaying.value = c.isPlaying
                    _currentSong.value = c.currentMediaItem?.toSongDto()
                    pushDuration()
                }
            }
        }

        // Position ticker — Media3 doesn't push position events; we poll at
        // 2 Hz, which is plenty for a seek bar and cheap.
        viewModelScope.launch {
            while (true) {
                controller?.let { _positionMs.value = it.currentPosition.coerceAtLeast(0) }
                delay(POSITION_POLL_MS)
            }
        }
    }

    fun play(song: SongDto) {
        val c = controller ?: return
        val item = song.toMediaItem()
        c.setMediaItem(item)
        c.prepare()
        c.playWhenReady = true
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    private fun pushDuration() {
        val c = controller ?: return
        _durationMs.value = if (c.duration > 0) c.duration else 0L
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        super.onCleared()
    }

    private companion object {
        const val POSITION_POLL_MS = 500L
    }
}

@UnstableApi
private fun SongDto.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(Network.streamUrl(id))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(
                    if (hasCoverArt) android.net.Uri.parse(Network.coverUrl(id)) else null
                )
                .build()
        )
        .build()

@UnstableApi
private fun MediaItem.toSongDto(): SongDto? {
    val songId = mediaId.toLongOrNull() ?: return null
    val md = mediaMetadata
    // We stash enough to render the mini-player without another API round
    // trip. Duration comes from the Player once playback starts; we don't
    // guess it here.
    return SongDto(
        id = songId,
        title = md.title?.toString().orEmpty(),
        artist = md.artist?.toString().orEmpty(),
        album = md.albumTitle?.toString(),
        durationMs = 0,
        hasCoverArt = md.artworkUri != null,
    )
}
