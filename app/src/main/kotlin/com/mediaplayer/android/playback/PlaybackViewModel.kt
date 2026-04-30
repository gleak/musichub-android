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
import androidx.media3.session.SessionCommand
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import com.mediaplayer.android.data.DownloadRepository
import com.mediaplayer.android.data.HistoryRepository
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.SongRepository
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
    private val songRepository = SongRepository()

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

    // Sleep timer state mirrored from MediaPlaybackService — the service owns the
    // authoritative timer so phone + AA stay in sync. We seed from the controller's
    // session extras and refresh whenever the service publishes a change.
    private val _sleepTimerActive = MutableStateFlow(false)
    val sleepTimerActive: StateFlow<Boolean> = _sleepTimerActive.asStateFlow()

    /** Liked state of the currently playing song. Mirrored from the service via session extras. */
    private val _currentLiked = MutableStateFlow(false)
    val currentLiked: StateFlow<Boolean> = _currentLiked.asStateFlow()

    private val _redownloading = MutableStateFlow(false)
    val redownloading: StateFlow<Boolean> = _redownloading.asStateFlow()

    private val _redownloadError = MutableStateFlow<String?>(null)
    val redownloadError: StateFlow<String?> = _redownloadError.asStateFlow()

    private val _alarmExportState = MutableStateFlow<AlarmExportState>(AlarmExportState.Idle)
    val alarmExportState: StateFlow<AlarmExportState> = _alarmExportState.asStateFlow()

    private val _videoDownloading = MutableStateFlow(false)
    val videoDownloading: StateFlow<Boolean> = _videoDownloading.asStateFlow()

    private val _videoDownloadError = MutableStateFlow<String?>(null)
    val videoDownloadError: StateFlow<String?> = _videoDownloadError.asStateFlow()

    private val _videoReinitializing = MutableStateFlow(false)
    val videoReinitializing: StateFlow<Boolean> = _videoReinitializing.asStateFlow()

    private val _videoReinitializeError = MutableStateFlow<String?>(null)
    val videoReinitializeError: StateFlow<String?> = _videoReinitializeError.asStateFlow()

    sealed class AlarmExportState {
        data object Idle : AlarmExportState()
        data object Exporting : AlarmExportState()
        data class Success(val title: String) : AlarmExportState()
        data class Failure(val message: String) : AlarmExportState()
    }

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
                delay(if (controller?.isPlaying == true) POSITION_POLL_MS else POSITION_POLL_IDLE_MS)
            }
        }

        // Mirror service-owned UX state (sleep timer + like) to UI.
        viewModelScope.launch {
            PlayerConnection.sessionExtras.collectLatest { extras ->
                _sleepTimerActive.value =
                    extras.getBoolean(MediaPlaybackService.EXTRA_SLEEP_ACTIVE, false)
                _currentLiked.value =
                    extras.getBoolean(MediaPlaybackService.EXTRA_LIKED, false)
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

    fun playPlaylistShuffled(songs: List<SongDto>) {
        if (songs.isEmpty()) return
        val c = controller ?: return
        val items = songs.shuffled().map { it.toMediaItem() }
        c.setMediaItems(items, 0, 0L)
        c.shuffleModeEnabled = true
        c.prepare()
        c.playWhenReady = true
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun pause() { controller?.pause() }
    fun play() { controller?.play() }

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

    /**
     * Re-download the currently playing song from its YouTube source. The
     * backend deletes the corrupted file/cover and refetches; on success we
     * invalidate the streaming cache + offline download + Coil cover cache,
     * then reload the current MediaItem from position 0 so the user hears
     * the fresh bytes immediately.
     *
     * 422 from the backend (no source URL — non-YouTube imports) is exposed
     * via [redownloadError] for the UI to surface as a snackbar.
     */
    @Suppress("TooGenericExceptionCaught")
    fun redownloadCurrent() {
        val current = _currentSong.value ?: return
        if (_redownloading.value) return
        _redownloading.value = true
        _redownloadError.value = null
        viewModelScope.launch {
            try {
                val fresh = songRepository.redownload(current.id)
                val ctx = getApplication<Application>()
                val streamUrl = Network.streamUrl(fresh.id)
                val coverUrl = Network.coverUrl(fresh.id)

                runCatching { PlayerCache.get(ctx).removeResource(streamUrl) }
                runCatching {
                    val wasDownloaded = DownloadRepository.isDownloaded(fresh.id)
                    DownloadRepository.remove(fresh.id)
                    if (wasDownloaded) DownloadRepository.download(fresh.id)
                }
                runCatching {
                    val loader = SingletonImageLoader.get(ctx)
                    loader.diskCache?.remove(coverUrl)
                    loader.memoryCache?.remove(MemoryCache.Key(coverUrl))
                }

                controller?.let { c ->
                    val idx = c.currentMediaItemIndex
                    val item = MediaItem.Builder()
                        .setMediaId(fresh.id.toString())
                        .setUri(streamUrl)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(fresh.title)
                                .setArtist(fresh.artist)
                                .setAlbumTitle(fresh.album)
                                .setArtworkUri(
                                    if (fresh.hasCoverArt) android.net.Uri.parse(coverUrl) else null
                                )
                                .build()
                        )
                        .build()
                    c.replaceMediaItem(idx, item)
                    c.seekTo(idx, 0L)
                    c.prepare()
                    c.playWhenReady = true
                }
                _currentSong.value = fresh.copy(durationMs = current.durationMs)
            } catch (t: Throwable) {
                _redownloadError.value = redownloadErrorMessage(t)
            } finally {
                _redownloading.value = false
            }
        }
    }

    fun consumeRedownloadError() { _redownloadError.value = null }

    @Suppress("TooGenericExceptionCaught")
    fun downloadVideoForCurrent() {
        val current = _currentSong.value ?: return
        if (_videoDownloading.value) return
        _videoDownloading.value = true
        _videoDownloadError.value = null
        viewModelScope.launch {
            try {
                val fresh = songRepository.downloadVideo(current.id)
                _currentSong.value = current.copy(hasVideo = fresh.hasVideo)
            } catch (t: Throwable) {
                _videoDownloadError.value = t.message ?: "Video download failed"
            } finally {
                _videoDownloading.value = false
            }
        }
    }

    fun consumeVideoDownloadError() { _videoDownloadError.value = null }

    @Suppress("TooGenericExceptionCaught")
    fun reinitializeVideoForCurrent() {
        val current = _currentSong.value ?: return
        if (_videoReinitializing.value) return
        _videoReinitializing.value = true
        _videoReinitializeError.value = null
        viewModelScope.launch {
            try {
                songRepository.reinitializeVideo(current.id)
                while (true) {
                    delay(2_000)
                    val s = songRepository.getReinitializeStatus(current.id)
                    when (s.status) {
                        "DONE" -> break
                        "ERROR" -> {
                            _videoReinitializeError.value = s.error.ifBlank { "Reinitialize failed" }
                            break
                        }
                    }
                }
            } catch (t: Throwable) {
                _videoReinitializeError.value = t.message ?: "Reinitialize failed"
            } finally {
                _videoReinitializing.value = false
            }
        }
    }

    fun consumeVideoReinitializeError() { _videoReinitializeError.value = null }

    private fun redownloadErrorMessage(t: Throwable): String {
        if (t is retrofit2.HttpException) {
            val body = runCatching { t.response()?.errorBody()?.string() }.getOrNull()
            if (!body.isNullOrBlank()) return body.trim()
        }
        return t.message ?: "Re-download failed"
    }

    /**
     * Save the current song's audio into MediaStore so the system Clock app
     * can pick it as an alarm sound. Result is surfaced via [alarmExportState].
     */
    @Suppress("TooGenericExceptionCaught")
    fun saveCurrentAsAlarmSound() {
        val current = _currentSong.value ?: return
        if (_alarmExportState.value is AlarmExportState.Exporting) return
        _alarmExportState.value = AlarmExportState.Exporting
        viewModelScope.launch {
            try {
                RingtoneExporter.exportAsAlarm(getApplication(), current)
                _alarmExportState.value = AlarmExportState.Success(current.title)
            } catch (t: Throwable) {
                _alarmExportState.value =
                    AlarmExportState.Failure(t.message ?: "Failed to save alarm sound")
            }
        }
    }

    fun consumeAlarmExportState() { _alarmExportState.value = AlarmExportState.Idle }

    /**
     * Re-download the current song's bytes locally — no backend work. Drops the
     * streaming cache, the offline copy and the Coil cover for this song, then
     * reloads the current MediaItem so ExoPlayer refetches from the backend.
     *
     * Use when the local copy is corrupted (truncated, scrambled, wrong cover)
     * but the backend's master file is fine. For a backend-side re-acquire from
     * YouTube, use [redownloadCurrent].
     */
    fun refreshLocalDownload() {
        val current = _currentSong.value ?: return
        val ctx = getApplication<Application>()
        val streamUrl = Network.streamUrl(current.id)
        val coverUrl = Network.coverUrl(current.id)

        runCatching { PlayerCache.get(ctx).removeResource(streamUrl) }
        runCatching {
            val wasDownloaded = DownloadRepository.isDownloaded(current.id)
            DownloadRepository.remove(current.id)
            if (wasDownloaded) DownloadRepository.download(current.id)
        }
        runCatching {
            val loader = SingletonImageLoader.get(ctx)
            loader.diskCache?.remove(coverUrl)
            loader.memoryCache?.remove(MemoryCache.Key(coverUrl))
        }

        controller?.let { c ->
            val idx = c.currentMediaItemIndex
            val item = MediaItem.Builder()
                .setMediaId(current.id.toString())
                .setUri(streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(current.title)
                        .setArtist(current.artist)
                        .setAlbumTitle(current.album)
                        .setArtworkUri(
                            if (current.hasCoverArt) android.net.Uri.parse(coverUrl) else null
                        )
                        .build()
                )
                .build()
            c.replaceMediaItem(idx, item)
            c.seekTo(idx, 0L)
            c.prepare()
            c.playWhenReady = true
        }
    }

    /** Arms the service-side sleep timer. Triggers a pause when it expires. */
    fun setSleepTimer(minutes: Int) {
        val c = controller ?: return
        val args = android.os.Bundle().apply { putInt("minutes", minutes) }
        c.sendCustomCommand(
            SessionCommand(MediaPlaybackService.ACTION_SLEEP_TIMER, android.os.Bundle.EMPTY),
            args,
        )
    }

    /** Cancels the service-side sleep timer if armed. */
    fun cancelSleepTimer() {
        val c = controller ?: return
        val args = android.os.Bundle().apply { putInt("minutes", 0) }
        c.sendCustomCommand(
            SessionCommand(MediaPlaybackService.ACTION_SLEEP_TIMER, android.os.Bundle.EMPTY),
            args,
        )
    }

    /** Toggles the like state of the currently playing track via the service. */
    fun toggleCurrentLike() {
        val c = controller ?: return
        c.sendCustomCommand(
            SessionCommand(MediaPlaybackService.ACTION_TOGGLE_LIKE, android.os.Bundle.EMPTY),
            android.os.Bundle.EMPTY,
        )
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
            if (!DownloadRepository.isDownloaded(id)) {
                DownloadRepository.download(id)
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
        const val POSITION_POLL_MS = 1_000L
        const val POSITION_POLL_IDLE_MS = 2_000L
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
