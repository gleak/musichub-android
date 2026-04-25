package com.mediaplayer.android.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.mediaplayer.android.data.HistoryRepository
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
 * Turns the imperative Media3 callback API into cold-ish state flows
 * that Compose can collect:
 *
 *  - [currentSong]  — DTO we pushed in, recovered via MediaItem metadata
 *  - [isPlaying]    — live `Player.isPlaying`
 *  - [positionMs]   — poll-driven current position (updates every 500ms)
 *  - [durationMs]   — known track duration or 0 while unknown
 *  - [hasNext]/[hasPrevious] — queue navigation affordances; hidden in
 *    the single-track M5 world, lit up once M6 pushes a playlist in
 *
 * One instance is expected per Activity scope via the default
 * `viewModel()` owner. The VM doesn't own a player — it only
 * subscribes to whichever controller [PlayerConnection] hands it.
 */
@UnstableApi
class PlaybackViewModel(
    private val historyRepository: HistoryRepository = HistoryRepository(),
) : ViewModel() {

    private val _currentSong = MutableStateFlow<SongDto?>(null)
    val currentSong: StateFlow<SongDto?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _hasNext = MutableStateFlow(false)
    val hasNext: StateFlow<Boolean> = _hasNext.asStateFlow()

    private val _hasPrevious = MutableStateFlow(false)
    val hasPrevious: StateFlow<Boolean> = _hasPrevious.asStateFlow()

    // Play-time tracking for history reporting
    private var trackedSongId: Long? = null
    private var trackedDurationMs: Long = 0L
    private var listenedMs: Long = 0L
    private var playingStartWall: Long = -1L

    private var controller: MediaController? = null
    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            if (playing && playingStartWall == -1L) {
                playingStartWall = System.currentTimeMillis()
            } else if (!playing && playingStartWall != -1L) {
                listenedMs += System.currentTimeMillis() - playingStartWall
                playingStartWall = -1L
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Flush wall-clock accumulator for the item we're leaving
            if (playingStartWall != -1L) {
                listenedMs += System.currentTimeMillis() - playingStartWall
                playingStartWall = if (controller?.isPlaying == true) System.currentTimeMillis() else -1L
            }
            maybeRecordPlay()
            trackedSongId = mediaItem?.mediaId?.toLongOrNull()
            trackedDurationMs = 0L
            listenedMs = 0L
            _currentSong.value = mediaItem?.toSongDto()
            pushDuration()
            pushQueueAvailability()
        }

        override fun onPlaybackStateChanged(state: Int) {
            pushDuration()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            // Queue shape changed (setMediaItems / clear / etc.) — re-check
            // whether skip controls should light up.
            pushQueueAvailability()
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
                    pushQueueAvailability()
                }
            }
        }

        // Position ticker — Media3 doesn't push position events; we poll at
        // 2 Hz, which is plenty for a seek bar and cheap.
        viewModelScope.launch {
            while (true) {
                controller?.let { c ->
                    _positionMs.value = c.currentPosition.coerceAtLeast(0)
                    if (trackedDurationMs == 0L && c.duration > 0) trackedDurationMs = c.duration
                }
                delay(POSITION_POLL_MS)
            }
        }
    }

    /** Play a single song, replacing whatever's queued. */
    fun play(song: SongDto) {
        val c = controller ?: return
        c.setMediaItem(song.toMediaItem())
        c.prepare()
        c.playWhenReady = true
    }

    /**
     * Queue a full playlist and start at [startIndex]. Next / previous
     * buttons light up once the queue has more than one entry.
     */
    fun playPlaylist(songs: List<SongDto>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val c = controller ?: return
        val items = songs.map { it.toMediaItem() }
        val clamped = startIndex.coerceIn(0, items.lastIndex)
        c.setMediaItems(items, clamped, /* startPositionMs = */ 0L)
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

    /** Skip forward in the queue (if anything's there). */
    fun skipNext() {
        val c = controller ?: return
        if (c.hasNextMediaItem()) c.seekToNextMediaItem()
    }

    /**
     * Skip back. Matches Spotify-style behaviour: within the first 3s
     * of a track, jump to the previous item; otherwise restart the
     * current track. Media3 already ships this as `seekToPrevious`.
     */
    fun skipPrevious() {
        controller?.seekToPrevious()
    }

    private fun pushDuration() {
        val c = controller ?: return
        _durationMs.value = if (c.duration > 0) c.duration else 0L
    }

    private fun pushQueueAvailability() {
        val c = controller ?: run {
            _hasNext.value = false
            _hasPrevious.value = false
            return
        }
        _hasNext.value = c.hasNextMediaItem()
        _hasPrevious.value = c.hasPreviousMediaItem()
    }

    private fun maybeRecordPlay() {
        val id = trackedSongId ?: return
        val listened = listenedMs
        val duration = trackedDurationMs
        if (listened >= LISTEN_THRESHOLD_MS || (duration > 0 && listened * 2 >= duration)) {
            viewModelScope.launch {
                try { historyRepository.record(id, listened) } catch (_: Throwable) {}
            }
        }
    }

    override fun onCleared() {
        if (playingStartWall != -1L) {
            listenedMs += System.currentTimeMillis() - playingStartWall
            playingStartWall = -1L
        }
        maybeRecordPlay()
        controller?.removeListener(listener)
        super.onCleared()
    }

    private companion object {
        const val POSITION_POLL_MS = 500L
        const val LISTEN_THRESHOLD_MS = 30_000L
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
