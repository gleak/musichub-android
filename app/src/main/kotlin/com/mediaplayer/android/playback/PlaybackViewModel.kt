package com.mediaplayer.android.playback

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import com.mediaplayer.android.data.PlayerSettings
import com.mediaplayer.android.data.SongRepository
import com.mediaplayer.android.data.dto.SongDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val PLAYBACK_DATASTORE = "playback"

/**
 * Playback session prefs (shuffle, repeat). [SharedPreferencesMigration]
 * pulls in the legacy `playback` SharedPreferences file on first access so
 * existing user state migrates seamlessly.
 */
private val Context.playbackDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PLAYBACK_DATASTORE,
    produceMigrations = { ctx ->
        listOf(SharedPreferencesMigration(ctx, PLAYBACK_DATASTORE))
    },
)

private val PREF_SHUFFLE_KEY = booleanPreferencesKey("shuffle")
private val PREF_REPEAT_KEY = intPreferencesKey("repeat")

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

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue: StateFlow<List<QueueEntry>> = _queue.asStateFlow()

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

    // Original (un-shuffled) order of the active source's song IDs. We own
    // shuffle at the app level (controller.shuffleModeEnabled is forced
    // off) so that the user queue can sit at currentIndex+1 and play
    // before any source item under any shuffle state. Spotify-equivalent.
    private var originalSourceOrder: List<Long> = emptyList()

    private val playbackPrefs: DataStore<Preferences> = application.playbackDataStore

    private var positionPollJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            if (playing && playingStartWall == -1L) {
                playingStartWall = System.currentTimeMillis()
            } else if (!playing && playingStartWall != -1L) {
                listenedMs += System.currentTimeMillis() - playingStartWall
                playingStartWall = -1L
            }
            if (playing) startPositionPoll() else {
                stopPositionPoll()
                pushPositionOnce()
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

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            // Push position once on every discontinuity (seek, auto-advance,
            // skip) so the UI tracks jumps even while the periodic poll is
            // not running (paused state).
            pushPositionOnce()
            // Spotify-queue contract: a user-queued item is consumed the
            // moment we leave it (auto-advance or manual skip). The new
            // item is already prepared and playing, so removing the old
            // index is a safe timeline mutation that just shifts indices.
            val oldIdx = oldPosition.mediaItemIndex
            val newIdx = newPosition.mediaItemIndex
            if (oldIdx == newIdx) return
            val c = controller ?: return
            if (oldIdx < 0 || oldIdx >= c.mediaItemCount) return
            if (c.getMediaItemAt(oldIdx).isUserQueued()) {
                c.removeMediaItem(oldIdx)
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            pushQueueAvailability()
            pushQueue()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            // App owns shuffle so the user queue can occupy a fixed slot.
            // If anything (Android Auto, system controllers, etc.) toggles
            // the controller's shuffle flag, adopt the change at the app
            // level and immediately force the controller flag back off.
            controller?.let { c ->
                if (c.shuffleModeEnabled) c.shuffleModeEnabled = false
            }
            if (shuffleModeEnabled != _shuffleEnabled.value) {
                _shuffleEnabled.value = shuffleModeEnabled
                viewModelScope.launch {
                    runCatching {
                        playbackPrefs.edit { it[PREF_SHUFFLE_KEY] = shuffleModeEnabled }
                    }
                }
                rearrangeSourceItems(toShuffled = shuffleModeEnabled)
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
            viewModelScope.launch {
                runCatching {
                    playbackPrefs.edit { it[PREF_REPEAT_KEY] = repeatMode }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            PlayerConnection.controller.collectLatest { c ->
                controller?.removeListener(listener)
                controller = c
                if (c != null) {
                    c.addListener(listener)
                    val snapshot = runCatching { playbackPrefs.data.first() }.getOrNull()
                    val savedShuffle = snapshot?.get(PREF_SHUFFLE_KEY) ?: false
                    val savedRepeat = snapshot?.get(PREF_REPEAT_KEY) ?: Player.REPEAT_MODE_OFF
                    // Force the controller's shuffle off — app owns shuffle
                    // semantics now, layered on top of the timeline so the
                    // user queue stays at currentIndex+1 under any state.
                    c.shuffleModeEnabled = false
                    c.repeatMode = savedRepeat
                    _shuffleEnabled.value = savedShuffle
                    _repeatMode.value = savedRepeat
                    _isPlaying.value = c.isPlaying
                    _currentSong.value = c.currentMediaItem?.toSongDto()
                    pushDuration()
                    pushQueueAvailability()
                    pushQueue()
                    pushPositionOnce()
                    if (c.isPlaying) startPositionPoll() else stopPositionPoll()
                } else {
                    stopPositionPoll()
                }
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
        originalSourceOrder = listOf(song.id)
        c.shuffleModeEnabled = false
        c.setMediaItem(song.toMediaItem())
        c.prepare()
        c.playWhenReady = true
    }

    fun playPlaylist(songs: List<SongDto>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val c = controller ?: return
        originalSourceOrder = songs.map { it.id }
        // Honour the existing app-level shuffle flag: if shuffle is on,
        // hand the player a pre-shuffled timeline. The player itself stays
        // un-shuffled so the user queue's slot stays predictable.
        val ordered = if (_shuffleEnabled.value) songs.shuffled() else songs
        val items = ordered.map { it.toMediaItem() }
        val clampedSongIndex = startIndex.coerceIn(0, songs.lastIndex)
        // After a shuffle, find where the originally-requested startIndex
        // landed so playback still begins on the song the user tapped.
        val startSongId = songs[clampedSongIndex].id
        val playIndex = ordered.indexOfFirst { it.id == startSongId }.coerceAtLeast(0)
        c.shuffleModeEnabled = false
        c.setMediaItems(items, playIndex, 0L)
        c.prepare()
        c.playWhenReady = true
    }

    fun playPlaylistShuffled(songs: List<SongDto>) {
        if (songs.isEmpty()) return
        val c = controller ?: return
        originalSourceOrder = songs.map { it.id }
        if (!_shuffleEnabled.value) {
            _shuffleEnabled.value = true
            persistShuffle(true)
        }
        val items = songs.shuffled().map { it.toMediaItem() }
        c.shuffleModeEnabled = false
        c.setMediaItems(items, 0, 0L)
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
        val newShuffle = !_shuffleEnabled.value
        _shuffleEnabled.value = newShuffle
        persistShuffle(newShuffle)
        rearrangeSourceItems(toShuffled = newShuffle)
    }

    private fun persistShuffle(value: Boolean) {
        viewModelScope.launch {
            runCatching {
                playbackPrefs.edit { it[PREF_SHUFFLE_KEY] = value }
            }
        }
    }

    /**
     * Reorder the source-only portion of the timeline that's still ahead
     * of playback. Items already played stay in place (history); the user
     * queue between current and the first source item is preserved so its
     * insertion order survives a shuffle toggle.
     */
    private fun rearrangeSourceItems(toShuffled: Boolean) {
        val c = controller ?: return
        if (originalSourceOrder.isEmpty()) return
        val total = c.mediaItemCount
        val currentIdx = c.currentMediaItemIndex
        if (currentIdx < 0 || total <= currentIdx + 1) return

        // Skip past the user-queue block immediately after the current item.
        var keepEnd = currentIdx + 1
        while (keepEnd < total && c.getMediaItemAt(keepEnd).isUserQueued()) keepEnd++
        if (keepEnd >= total) return

        val futureItems = (keepEnd until total).map { c.getMediaItemAt(it) }
        val byId = futureItems.associateBy { it.mediaId.toLongOrNull() }
        val futureIds = byId.keys.filterNotNull().toSet()

        val targetIds = if (toShuffled) {
            futureIds.shuffled()
        } else {
            // Restore by original order, filtering to whatever's still ahead
            // (items already played stay where they are).
            originalSourceOrder.filter { it in futureIds }
        }
        val targetItems = targetIds.mapNotNull { byId[it] }
        if (targetItems.isEmpty()) return

        // remove + add (rather than replaceMediaItems) keeps this working
        // on older MediaController surfaces and avoids an in-place mutation
        // glitch we saw under quick shuffle-toggles.
        c.removeMediaItems(keepEnd, total)
        c.addMediaItems(keepEnd, targetItems)
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /**
     * Insert [song] at the head of the user queue (right after the
     * currently-playing item), marked with the user-queue flag so it
     * gets consumed once played. Spotify-style "Play next".
     */
    fun playNext(song: SongDto) {
        val c = controller ?: return
        val insertIndex = (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount)
        c.addMediaItem(insertIndex, song.toMediaItem(userQueued = true))
    }

    /**
     * Append [song] to the tail of the user queue (after any existing
     * user-queued items, before the auto-queue from the current source).
     * Spotify-style "Add to queue".
     */
    fun addToQueue(song: SongDto) {
        val c = controller ?: return
        var i = c.currentMediaItemIndex + 1
        while (i < c.mediaItemCount && c.getMediaItemAt(i).isUserQueued()) i++
        c.addMediaItem(i.coerceAtMost(c.mediaItemCount), song.toMediaItem(userQueued = true))
    }

    fun skipToQueueItem(index: Int) {
        controller?.seekTo(index, 0L)
    }

    /**
     * Remove the queue item at [index]. No-op for the currently playing
     * item — Media3 would silently advance to the next track, which is
     * never what a user means when they tap "remove" on a row that's
     * actively playing.
     */
    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index < 0 || index >= c.mediaItemCount) return
        if (index == c.currentMediaItemIndex) return
        c.removeMediaItem(index)
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

    /**
     * Kick off the backend's async video download and poll for completion.
     *
     * The backend hands back 202 immediately and runs yt-dlp on a virtual
     * thread, so we never sit on a single HTTP request long enough to hit
     * OkHttp's 30s read timeout. Polling avoids the prior failure mode where
     * a timed-out POST left the user re-tapping and spawning duplicate
     * yt-dlp processes — the backend now de-dupes by song id.
     */
    @Suppress("TooGenericExceptionCaught")
    fun downloadVideoForCurrent() {
        val current = _currentSong.value ?: return
        if (_videoDownloading.value) return
        _videoDownloading.value = true
        _videoDownloadError.value = null
        viewModelScope.launch {
            try {
                songRepository.downloadVideo(current.id)
                var wait = VIDEO_POLL_INITIAL_MS
                var attempts = 0
                while (attempts < VIDEO_POLL_MAX_ATTEMPTS) {
                    delay(wait)
                    attempts++
                    val s = songRepository.getDownloadVideoStatus(current.id)
                    when (s.status) {
                        "DONE" -> {
                            _currentSong.value = current.copy(hasVideo = true)
                            break
                        }
                        "ERROR" -> {
                            _videoDownloadError.value = s.error.ifBlank { "Video download failed" }
                            break
                        }
                    }
                    wait = (wait * 2).coerceAtMost(VIDEO_POLL_MAX_MS)
                }
                if (attempts >= VIDEO_POLL_MAX_ATTEMPTS && _videoDownloadError.value == null) {
                    _videoDownloadError.value = "Video download timed out"
                }
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
                var wait = VIDEO_POLL_INITIAL_MS
                var attempts = 0
                while (attempts < VIDEO_POLL_MAX_ATTEMPTS) {
                    delay(wait)
                    attempts++
                    val s = songRepository.getReinitializeStatus(current.id)
                    when (s.status) {
                        "DONE" -> break
                        "ERROR" -> {
                            _videoReinitializeError.value = s.error.ifBlank { "Reinitialize failed" }
                            break
                        }
                    }
                    wait = (wait * 2).coerceAtMost(VIDEO_POLL_MAX_MS)
                }
                if (attempts >= VIDEO_POLL_MAX_ATTEMPTS && _videoReinitializeError.value == null) {
                    _videoReinitializeError.value = "Reinitialize timed out"
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

    /**
     * Toggles the like state of the currently playing track via the service.
     *
     * Optimistically flips the local flag so the heart icon updates immediately —
     * in-process MediaController instances do not reliably receive
     * {@code onExtrasChanged} callbacks, so we cannot wait for the service to
     * publish the new extras. The service still owns the persisted truth: the
     * next {@code onMediaItemTransition} re-reads liked status from the server
     * and corrects any divergence.
     */
    fun toggleCurrentLike() {
        val c = controller ?: return
        _currentLiked.value = !_currentLiked.value
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
        val current = c.currentMediaItemIndex
        _queue.value = (0 until c.mediaItemCount).mapNotNull { i ->
            val item = c.getMediaItemAt(i)
            val song = item.toSongDto() ?: return@mapNotNull null
            QueueEntry(
                song = song,
                index = i,
                isCurrent = i == current,
                userQueued = item.isUserQueued(),
            )
        }
    }

    private fun maybeRecordPlay() {
        val id = trackedSongId ?: return
        val listened = listenedMs
        val duration = trackedDurationMs
        // Drop pure no-ops AND micro-skips. Sub-second listens are noise:
        // user mashing skip burns a request per item without giving the
        // recommender useful signal. Real "skipped after a moment" plays
        // still get through above the floor.
        if (listened < MIN_RECORD_MS) return

        val countsAsFullPlay = listened >= LISTEN_THRESHOLD_MS ||
            (duration > 0 && listened * 2 >= duration)
        // `completion_ratio` is the per-play listened/total ratio, capped
        // at 1.0 (replay past the end via seek-back is possible). Null
        // when duration is unknown (live streams / pre-prepare) so the
        // backend can distinguish "no signal" from "0% played".
        val ratio = if (duration > 0)
            (listened.toDouble() / duration).coerceAtMost(1.0)
        else null
        viewModelScope.launch {
            historyRepository.record(
                songId = id,
                durationListenedMs = listened,
                completionRatio = ratio,
                wasSkipped = !countsAsFullPlay,
            )
        }
        // Auto-download every song the user actually starts (listened > 0,
        // already gated above). Setting toggles whether we cache at all.
        viewModelScope.launch {
            if (PlayerSettings.instance.downloadAutoNow() &&
                !DownloadRepository.isDownloaded(id)
            ) {
                DownloadRepository.download(id)
            }
        }
    }

    /**
     * Force-flush the in-flight play so /recent reflects the current track
     * even when there's been no transition. Suspends until the backend
     * record call completes so callers can refresh /recent immediately
     * afterwards. Only sends if the listen threshold is met — partial
     * plays still wait for the next transition (or onCleared) to be
     * reported as skips. Resets the running counter so the same
     * listened-ms isn't double-counted at transition time.
     */
    suspend fun flushPlayHistoryAwait() {
        if (playingStartWall != -1L) {
            val now = System.currentTimeMillis()
            listenedMs += now - playingStartWall
            playingStartWall = if (controller?.isPlaying == true) now else -1L
        }
        val id = trackedSongId ?: return
        val listened = listenedMs
        val duration = trackedDurationMs
        val countsAsFullPlay = listened >= LISTEN_THRESHOLD_MS ||
            (duration > 0 && listened * 2 >= duration)
        if (!countsAsFullPlay) return
        val ratio = if (duration > 0)
            (listened.toDouble() / duration).coerceAtMost(1.0)
        else null
        // Direct POST so /recent reflects this play on the next refresh.
        // Falls back to the queue inside recordImmediate if offline.
        historyRepository.recordImmediate(
            songId = id,
            durationListenedMs = listened,
            completionRatio = ratio,
            wasSkipped = false,
        )
        if (PlayerSettings.instance.downloadAutoNow() &&
            !DownloadRepository.isDownloaded(id)
        ) {
            DownloadRepository.download(id)
        }
        listenedMs = 0L
    }

    override fun onCleared() {
        if (playingStartWall != -1L) {
            listenedMs += System.currentTimeMillis() - playingStartWall
            playingStartWall = -1L
        }
        maybeRecordPlay()
        stopPositionPoll()
        controller?.removeListener(listener)
        super.onCleared()
    }

    /**
     * Push a single fresh position snapshot to the StateFlow. Cheap — no
     * allocation, no suspension. Used on transitions/seeks/pause to keep
     * the UI accurate without leaving the periodic poll running.
     */
    private fun pushPositionOnce() {
        controller?.let { c ->
            _positionMs.value = c.currentPosition.coerceAtLeast(0)
            if (trackedDurationMs == 0L && c.duration > 0) trackedDurationMs = c.duration
        }
    }

    /**
     * Tick `positionMs` once per second while the player is playing.
     * Driven by `onIsPlayingChanged` so the loop never runs while paused
     * or while no controller is attached — earlier versions polled on
     * a forever-running `while(true)` that woke the CPU even with the
     * screen off.
     */
    private fun startPositionPoll() {
        if (positionPollJob?.isActive == true) return
        positionPollJob = viewModelScope.launch {
            while (true) {
                pushPositionOnce()
                delay(POSITION_POLL_MS)
            }
        }
    }

    private fun stopPositionPoll() {
        positionPollJob?.cancel()
        positionPollJob = null
    }

    private companion object {
        const val POSITION_POLL_MS = 1_000L
        const val LISTEN_THRESHOLD_MS = 30_000L
        // Floor below which a transition isn't reported at all — drops
        // sub-second micro-skips that would otherwise spam the backend
        // during rapid skipping with no useful recommender signal.
        const val MIN_RECORD_MS = 1_500L
        // Video download/reinit polling: exponential backoff, hard cap.
        // Stops the loop wedging the network if the backend never returns
        // a terminal status (network flap, 502, navigated-away dialog).
        const val VIDEO_POLL_INITIAL_MS = 2_000L
        const val VIDEO_POLL_MAX_MS = 30_000L
        const val VIDEO_POLL_MAX_ATTEMPTS = 30
    }
}

/**
 * One row in the playback timeline as exposed to the UI. Splits the queue
 * into two semantic groups via [userQueued]: items the user explicitly
 * dropped via "Add to queue" / "Play next" (true) versus auto-queued
 * tracks coming from the playing source — album, playlist, etc. (false).
 * Mirrors Spotify's "Next in queue" / "Next from <source>" split.
 */
@UnstableApi
data class QueueEntry(
    val song: SongDto,
    val index: Int,
    val isCurrent: Boolean,
    val userQueued: Boolean,
)

private const val KEY_USER_QUEUED = "user_queued"

@UnstableApi
private fun SongDto.toMediaItem(userQueued: Boolean = false): MediaItem {
    val extras = if (userQueued) {
        android.os.Bundle().apply { putBoolean(KEY_USER_QUEUED, true) }
    } else null
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(
            if (hasCoverArt) android.net.Uri.parse(Network.coverUrl(id)) else null
        )
        .apply { if (extras != null) setExtras(extras) }
        .build()
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(Network.streamUrl(id))
        .setMediaMetadata(metadata)
        .build()
}

@UnstableApi
private fun MediaItem.isUserQueued(): Boolean =
    mediaMetadata.extras?.getBoolean(KEY_USER_QUEUED, false) == true

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
