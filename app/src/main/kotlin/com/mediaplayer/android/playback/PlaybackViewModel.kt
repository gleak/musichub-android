package com.mediaplayer.android.playback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

@UnstableApi
class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private val historyRepository = HistoryRepository()

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

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<SongDto>>(emptyList())
    val queue: StateFlow<List<SongDto>> = _queue.asStateFlow()

    private val sleepTimer = SleepTimer(viewModelScope)
    val sleepTimerActive: StateFlow<Boolean> = sleepTimer.isActive

    // Play-time tracking for history reporting
    private var trackedSongId: Long? = null
    private var trackedDurationMs: Long = 0L
    private var listenedMs: Long = 0L
    private var playingStartWall: Long = -1L

    private var controller: MediaController? = null

    private val prefs = application.getSharedPreferences("playback", android.content.Context.MODE_PRIVATE)

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
            pushQueueAvailability()
            pushQueue()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleEnabled.value = shuffleModeEnabled
            prefs.edit().putBoolean(PREF_SHUFFLE, shuffleModeEnabled).apply()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
            prefs.edit().putInt(PREF_REPEAT, repeatMode).apply()
        }
    }

    init {
        viewModelScope.launch {
            PlayerConnection.controller.collectLatest { c ->
                controller?.removeListener(listener)
                controller = c
                if (c != null) {
                    c.addListener(listener)
                    val savedShuffle = prefs.getBoolean(PREF_SHUFFLE, false)
                    val savedRepeat = prefs.getInt(PREF_REPEAT, Player.REPEAT_MODE_OFF)
                    c.shuffleModeEnabled = savedShuffle
                    c.repeatMode = savedRepeat
                    _shuffleEnabled.value = savedShuffle
                    _repeatMode.value = savedRepeat
                    _isPlaying.value = c.isPlaying
                    _currentSong.value = c.currentMediaItem?.toSongDto()
                    pushDuration()
                    pushQueueAvailability()
                    pushQueue()
                }
            }
        }

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

    fun play(song: SongDto) {
        val c = controller ?: return
        c.setMediaItem(song.toMediaItem())
        c.prepare()
        c.playWhenReady = true
    }

    fun playPlaylist(songs: List<SongDto>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val c = controller ?: return
        val items = songs.map { it.toMediaItem() }
        val clamped = startIndex.coerceIn(0, items.lastIndex)
        c.setMediaItems(items, clamped, 0L)
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

    fun skipNext() {
        val c = controller ?: return
        if (c.hasNextMediaItem()) c.seekToNextMediaItem()
    }

    fun skipPrevious() {
        controller?.seekToPrevious()
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun playNext(song: SongDto) {
        val c = controller ?: return
        val insertIndex = (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount)
        c.addMediaItem(insertIndex, song.toMediaItem())
    }

    fun addToQueue(song: SongDto) {
        val c = controller ?: return
        c.addMediaItem(song.toMediaItem())
    }

    fun skipToQueueItem(index: Int) {
        controller?.seekTo(index, 0L)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimer.set(minutes) { controller?.pause() }
    }

    fun cancelSleepTimer() {
        sleepTimer.cancel()
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

    private fun pushQueue() {
        val c = controller ?: run { _queue.value = emptyList(); return }
        _queue.value = (0 until c.mediaItemCount).mapNotNull { i -> c.getMediaItemAt(i).toSongDto() }
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
        const val PREF_SHUFFLE = "shuffle"
        const val PREF_REPEAT = "repeat"
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
    return SongDto(
        id = songId,
        title = md.title?.toString().orEmpty(),
        artist = md.artist?.toString().orEmpty(),
        album = md.albumTitle?.toString(),
        durationMs = 0,
        hasCoverArt = md.artworkUri != null,
    )
}
