package com.mediaplayer.android.playback

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import com.mediaplayer.android.data.DownloadRepository
import com.mediaplayer.android.data.HistoryRepository
import com.mediaplayer.android.data.LikedSongsCache
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.RecentsCache
import com.mediaplayer.android.data.PlayerSettings
import com.mediaplayer.android.data.SongRepository
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.data.local.LocalLikedStore
import com.mediaplayer.android.data.local.LocalMediaResolver
import com.mediaplayer.android.data.local.LocalTrack
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Playback session prefs (shuffle, repeat) now live in [PlaybackPrefs] so the
// service-owned shuffle/endless engine and this ViewModel read the same store.

/**
 * One playback failure surfaced to the UI as a dialog: human-readable [reason]
 * for the user, raw [errorCodeName] for the curious, optional [recoveryHint]
 * when the VM is already kicking off (or recommending) a retry.
 */
data class PlaybackErrorInfo(
    val songTitle: String,
    val reason: String,
    val errorCodeName: String,
    val recoveryHint: String? = null,
)

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

    /** Remaining ms while a minute-mode sleep timer is armed; 0 otherwise. */
    private val _sleepTimerRemainingMs = MutableStateFlow(0L)
    val sleepTimerRemainingMs: StateFlow<Long> = _sleepTimerRemainingMs.asStateFlow()

    /**
     * Identity of the collection the current queue was started from
     * (e.g. "playlist:42"), or null for ad-hoc / single-track playback.
     * Lets a detail screen tell "this collection is playing" apart from
     * "a track that merely also appears here is playing elsewhere".
     */
    private val _activeSourceKey = MutableStateFlow<String?>(null)
    val activeSourceKey: StateFlow<String?> = _activeSourceKey.asStateFlow()

    /** True only when the sleep timer is armed in `Fine traccia` end-of-track mode. */
    private val _sleepTimerEndOfTrack = MutableStateFlow(false)
    val sleepTimerEndOfTrack: StateFlow<Boolean> = _sleepTimerEndOfTrack.asStateFlow()

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

    // Playback error surfaced to the UI as an AlertDialog. Replaces the older
    // toast surface so the user sees *why* a song failed (codec / network /
    // corruption) instead of just a generic "couldn't play" line that
    // disappears on its own. Cleared when the user dismisses the dialog.
    private val _playbackError = MutableStateFlow<PlaybackErrorInfo?>(null)
    val playbackError: StateFlow<PlaybackErrorInfo?> = _playbackError.asStateFlow()

    fun dismissPlaybackError() { _playbackError.value = null; erroredSongId = null }

    /**
     * Raise a synthetic error when a play tap can't proceed because the
     * MediaController hasn't bound yet (or the bind failed). Without this
     * the play call silently returns and the user gets no signal — exactly
     * what the "I can't start any song" car incident showed.
     */
    private fun raiseControllerNotReady(reason: String) {
        _playbackError.value = PlaybackErrorInfo(
            songTitle = _currentSong.value?.title?.takeIf { it.isNotBlank() }
                ?: "questo brano",
            reason = reason,
            errorCodeName = "PLAYER_NOT_READY",
            recoveryHint = "Riprova fra qualche secondo. Se persiste, riavvia l'app.",
        )
    }

    /**
     * Re-prepare and resume the current MediaItem after a transient playback
     * failure (`Riprova` button on the error dialog). No item swap, no
     * cache invalidation — that's [redownloadCurrent]'s job.
     */
    fun retryCurrent() {
        val c = controller ?: return
        _playbackError.value = null
        erroredSongId = null
        c.prepare()
        c.playWhenReady = true
    }

    // Per-session set of song IDs we've already auto-recovered after a
    // playback error. Prevents an infinite refresh→error→refresh loop when
    // the corruption is server-side (refreshLocalDownload only re-fetches
    // from backend, not from YouTube). One auto-attempt per song; after
    // that we tell the user to use "Re-download from source" manually.
    private val autoFixedSongs = mutableSetOf<Long>()

    /** Song id the on-screen playback-error dialog refers to, so a STATE_READY
     *  from an unrelated auto-advanced track doesn't dismiss it. */
    private var erroredSongId: Long? = null

    sealed class AlarmExportState {
        data object Idle : AlarmExportState()
        data object Exporting : AlarmExportState()
        data class Success(val title: String) : AlarmExportState()
        data class Failure(val message: String) : AlarmExportState()
    }

    // Play-time tracking for history reporting
    private var trackedSongId: Long? = null
    private var trackedSongTitle: String? = null
    private var trackedSongArtist: String? = null
    private var trackedDurationMs: Long = 0L
    private val listenClock = ListenClock()

    private var controller: MediaController? = null

    private val playbackPrefs: DataStore<Preferences> = PlaybackPrefs.dataStore(application)

    private var positionPollJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            if (playing) listenClock.resume() else listenClock.bank()
            if (playing) startPositionPoll() else {
                stopPositionPoll()
                pushPositionOnce()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            listenClock.bankAndContinue(stillPlaying = controller?.isPlaying == true)
            maybeRecordPlay()
            val nextDto = mediaItem?.toSongDto()
            // Resolved id, not the raw mediaId: car / library / resumption
            // items carry the "song:{id}" form, which parsed to null here and
            // silently suppressed play history for car-started sessions.
            trackedSongId = nextDto?.id
            trackedSongTitle = nextDto?.title
            trackedSongArtist = nextDto?.artist
            trackedDurationMs = 0L
            listenClock.reset()
            _currentSong.value = nextDto
            refreshCurrentLiked(nextDto?.id)
            pushDuration()
            // The queue snapshot bakes in which row is playing, so it must be
            // rebuilt on every transition. Leaving it to onTimelineChanged
            // meant the sheet kept pointing at a song that finished several
            // tracks ago: the service only reshapes the timeline once history
            // exceeds its window, and never at all under repeat-all.
            pushQueue()
            // Endless-queue refill + history prune now live on the service's own
            // player ([EndlessQueueController]) so they keep working when no
            // Activity/ViewModel is alive (Android Auto cold start, screen-off
            // drive). Doing them here too would double-append, so the VM only
            // observes the resulting timeline.
            pushQueueAvailability()
        }

        override fun onPlaybackStateChanged(state: Int) {
            pushDuration()
            // The service self-heals transient stream errors (retry with
            // backoff); once THE SAME song is healthy again drop its error
            // dialog. Only clear when the ready item is the one the error was
            // raised for — otherwise auto-advancing to a different, healthy
            // track would silently dismiss a dialog about a still-broken song.
            if (state == Player.STATE_READY) {
                val err = erroredSongId
                if (err == null || _currentSong.value?.id == err) {
                    _playbackError.value = null
                    erroredSongId = null
                }
            }
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
            // Phantom-skip detection + truncated-download heal moved to the
            // service's own player ([PhantomSkipHealer]) so it fires headless
            // (Android Auto / screen off) too, and stays single-owner (no double
            // cache-evict when an Activity is alive).
            //
            // Consuming a user-queued item once we leave it moved to the
            // service's own player ([EndlessQueueController]): under repeat-OFF
            // that engine prunes/refills concurrently, and a front-prune shifts
            // indices, so doing the removal here against the (async, possibly
            // stale) controller index could delete the wrong item.
            //
            // Shuffle (including reshuffle-on-wrap under repeat-ALL) is now
            // owned by the service's [EndlessQueueController] so it works
            // headless (Android Auto / screen off) and always covers the whole
            // source pool. Nothing to do here.
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            pushQueueAvailability()
            pushQueue()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            // App owns shuffle at the app level (see [EndlessQueueController]);
            // the native flag is kept off by the service. Never let a stray
            // native toggle from a head unit take effect on this controller.
            controller?.let { c ->
                if (c.shuffleModeEnabled) c.shuffleModeEnabled = false
            }
            // The UI's shuffle state is driven by the shared shuffle pref
            // (collected in init), not by this native callback.
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
            viewModelScope.launch {
                runCatching {
                    playbackPrefs.edit { it[PlaybackPrefs.REPEAT_KEY] = repeatMode }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            handlePlaybackError(error)
        }
    }

    /**
     * Some songs silently fail to start — most often because the local cached
     * bytes got truncated or the container is malformed (background download
     * killed by the OS, partial write to disk, decoder couldn't handle the
     * frame). Without this handler the player just sits idle and the user has
     * no idea why nothing happened.
     *
     * For codes that point at corruption / missing-bytes (parsing, decoding,
     * file-not-found, truncated reads) we automatically drop the local copy
     * and refetch from the backend via [refreshLocalDownload], which also
     * re-prepares the player from position 0 so playback retries on the
     * fresh bytes. We toast the user so they know what happened.
     *
     * For network / transport codes (no internet, bad HTTP status, timeout)
     * a re-download wouldn't help — we just report it and let the user retry.
     *
     * Each song id is auto-fixed at most once per VM lifetime so a server-side
     * corruption (where the backend file is also bad) doesn't spin into a
     * refresh→error→refresh loop. After that single attempt we point the user
     * at "Re-download from source" which goes back to YouTube.
     */
    private fun handlePlaybackError(error: PlaybackException) {
        val current = _currentSong.value
        val title = current?.title?.takeIf { it.isNotBlank() } ?: "questo brano"
        val songId = current?.id
        erroredSongId = songId
        val reason = humanReason(error)

        // Local files (negative id) have no backend copy to re-fetch, and the
        // "Riscarica dalla sorgente" (YouTube) path is disabled for them.
        // Surface a local-appropriate message instead of promising a server
        // re-download that will never happen.
        if (songId != null && LocalMediaResolver.isLocal(songId)) {
            _playbackError.value = PlaybackErrorInfo(
                songTitle = title,
                reason = reason,
                errorCodeName = error.errorCodeName,
                recoveryHint = "Il file locale non è più disponibile (spostato o eliminato). Ricontrolla la libreria del telefono.",
            )
            return
        }

        val recoverable = isCorruptionLikeError(error)

        if (recoverable && songId != null && autoFixedSongs.add(songId)) {
            _playbackError.value = PlaybackErrorInfo(
                songTitle = title,
                reason = reason,
                errorCodeName = error.errorCodeName,
                recoveryHint = "Sto riscaricando il file dal server. Prova a riavviare il brano tra qualche secondo.",
            )
            refreshLocalDownload()
            return
        }

        if (recoverable) {
            _playbackError.value = PlaybackErrorInfo(
                songTitle = title,
                reason = reason,
                errorCodeName = error.errorCodeName,
                recoveryHint = "Il riscarico dal server non ha risolto il problema. Prova \"Riscarica dalla sorgente\" dal menu del brano per recuperarlo da YouTube.",
            )
            return
        }

        // Transient transport errors are auto-retried by the service with
        // backoff — tell the user that instead of demanding a manual retry.
        val selfHealing = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_TIMEOUT -> true
            else -> false
        }
        _playbackError.value = PlaybackErrorInfo(
            songTitle = title,
            reason = reason,
            errorCodeName = error.errorCodeName,
            recoveryHint = if (selfHealing) {
                "Riprovo automaticamente appena la connessione torna disponibile."
            } else null,
        )
    }

    private fun humanReason(e: PlaybackException): String = when (e.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
            "Nessuna connessione di rete o server irraggiungibile."
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
            "Il server ha rifiutato la richiesta dello stream (HTTP error)."
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ->
            "Risposta del server non valida per uno stream audio."
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
            "Permesso negato per leggere il file audio."
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            "File audio non trovato sul dispositivo."
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ->
            "Lettura del file audio fallita: dimensioni inattese (file probabilmente troncato)."
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
            "Connessione in chiaro bloccata dalle policy di rete."
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
            "File audio danneggiato (container malformato)."
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
            "Formato del file non riconosciuto."
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ->
            "Decodifica fallita: il flusso audio è corrotto o il codec ha rinunciato."
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
            "Inizializzazione del decoder fallita: codec non supportato dal dispositivo."
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ->
            "Errore dell'output audio del dispositivo."
        PlaybackException.ERROR_CODE_TIMEOUT ->
            "Timeout durante la riproduzione."
        PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ->
            "Posizione fuori dalla finestra di riproduzione live."
        else -> e.localizedMessage?.takeIf { it.isNotBlank() }
            ?: "Errore di riproduzione sconosciuto."
    }

    private fun isCorruptionLikeError(e: PlaybackException): Boolean = when (e.errorCode) {
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> true
        else -> false
    }

    init {
        viewModelScope.launch {
            PlayerConnection.controller.collectLatest { c ->
                controller?.removeListener(listener)
                controller = c
                if (c != null) {
                    // Auto-dismiss any "not ready" / "bind failed" dialog the
                    // user might be staring at — the controller is here now,
                    // so the message no longer reflects reality.
                    val err = _playbackError.value
                    if (err != null &&
                        (err.errorCodeName == "PLAYER_NOT_READY" ||
                            err.errorCodeName == "PLAYER_BIND_FAILED")
                    ) {
                        _playbackError.value = null
                    }
                    c.addListener(listener)
                    val snapshot = runCatching { playbackPrefs.data.first() }.getOrNull()
                    val savedShuffle = snapshot?.get(PlaybackPrefs.SHUFFLE_KEY) ?: false
                    val savedRepeat = snapshot?.get(PlaybackPrefs.REPEAT_KEY) ?: Player.REPEAT_MODE_OFF
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

        // Surface MediaController bind failures so the user sees a dialog
        // instead of taps silently no-op'ing forever. Cleared on retry.
        viewModelScope.launch {
            PlayerConnection.bindError.collectLatest { err ->
                if (err != null) {
                    _playbackError.value = PlaybackErrorInfo(
                        songTitle = "Player",
                        reason = "Impossibile collegarsi al servizio di riproduzione.",
                        errorCodeName = "PLAYER_BIND_FAILED",
                        recoveryHint = "Chiudi e riapri l'app, oppure riavvia il telefono.",
                    )
                }
            }
        }

        // Shuffle is service-owned now: keep the UI toggle in sync with the
        // shared shuffle pref so a toggle from Android Auto (or a headless
        // service reorder) flips the phone's shuffle icon too.
        viewModelScope.launch {
            PlaybackPrefs.shuffleFlow(application).collectLatest {
                _shuffleEnabled.value = it
            }
        }

        // Mirror service-owned UX state (sleep timer + like) to UI.
        viewModelScope.launch {
            PlayerConnection.sessionExtras.collectLatest { extras ->
                _sleepTimerActive.value =
                    extras.getBoolean(MediaPlaybackService.EXTRA_SLEEP_ACTIVE, false)
                _sleepTimerRemainingMs.value =
                    extras.getLong(MediaPlaybackService.EXTRA_SLEEP_REMAINING_MS, 0L)
                _sleepTimerEndOfTrack.value =
                    extras.getBoolean(MediaPlaybackService.EXTRA_SLEEP_END_OF_TRACK, false)
                val current = _currentSong.value
                val liked = if (current != null && LocalMediaResolver.isLocal(current.id)) {
                    // Service has no record of local likes — read from the
                    // dedicated DataStore instead. Mirror into _currentLiked
                    // so the heart icon flips when the track changes.
                    LocalLikedStore.instance(getApplication())
                        .isLiked(-current.id)
                } else {
                    extras.getBoolean(MediaPlaybackService.EXTRA_LIKED, false)
                }
                _currentLiked.value = liked
                // Keep the shared cache in sync with the service-resolved
                // truth so heart icons elsewhere (rows, kebab sheet) match
                // the player's heart instantly.
                if (current != null && !LocalMediaResolver.isLocal(current.id)) {
                    LikedSongsCache.markLiked(current.id, liked)
                }
            }
        }
    }

    /**
     * Suspend until the [MediaController] has bound (up to [timeoutMs]).
     * Widget quick-launch taps can fire before the async controller resolves
     * on a cold start; awaiting first avoids a silent "player non pronto" no-op.
     * Returns true if a controller is available.
     */
    suspend fun awaitControllerReady(timeoutMs: Long = 5_000L): Boolean {
        if (controller != null) return true
        return kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            PlayerConnection.controller.filterNotNull().first()
            true
        } ?: false
    }

    fun play(song: SongDto) {
        // Segnaposto del DJ (playable=false, download ancora in corso) o file
        // sparito: in entrambi i casi non c'e' audio da avviare. La riga che
        // genera questa chiamata dovrebbe gia' essere non toccabile, ma un
        // no-op qui costa nulla ed evita un player che parte a vuoto.
        if (!song.playable) return
        val c = controller
        if (c == null) {
            raiseControllerNotReady("Player non ancora pronto.")
            return
        }
        _activeSourceKey.value = null
        c.shuffleModeEnabled = false
        c.setMediaItem(song.toMediaItem())
        c.prepare()
        c.playWhenReady = true
    }

    /** Start one local track. Mirrors [play] but takes a [LocalTrack]. */
    fun playLocal(track: LocalTrack) {
        val c = controller
        if (c == null) {
            raiseControllerNotReady("Player non ancora pronto.")
            return
        }
        LocalMediaResolver.register(track)
        _activeSourceKey.value = null
        c.shuffleModeEnabled = false
        c.setMediaItem(track.toMediaItem())
        c.prepare()
        c.playWhenReady = true
    }

    /** Play a list of local tracks starting at [startIndex]. */
    fun playLocalAll(tracks: List<LocalTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val c = controller
        if (c == null) {
            raiseControllerNotReady("Player non ancora pronto.")
            return
        }
        LocalMediaResolver.registerAll(tracks)
        // Hand the player the ORIGINAL order and let the service-owned
        // EndlessQueueController do the shuffle. Pre-shuffling here froze the
        // scrambled order as the engine's "original", so shuffle-OFF could
        // never restore it (and caused a visible double reorder).
        _activeSourceKey.value = null
        val items = tracks.map { it.toMediaItem() }
        val playIndex = startIndex.coerceIn(0, tracks.lastIndex)
        c.shuffleModeEnabled = false
        c.setMediaItems(items, playIndex, 0L)
        c.prepare()
        c.playWhenReady = true
    }

    /** Shuffle and play a list of local tracks. */
    fun playLocalShuffled(tracks: List<LocalTrack>) {
        if (tracks.isEmpty()) return
        val c = controller
        if (c == null) {
            raiseControllerNotReady("Player non ancora pronto.")
            return
        }
        LocalMediaResolver.registerAll(tracks)
        // Turn shuffle on (pref = single source of truth; the service reshuffles
        // the whole pool). Pass ORIGINAL order so the pool stays authoritative;
        // start on a random track for an immediate shuffled feel.
        _activeSourceKey.value = null
        _shuffleEnabled.value = true
        persistShuffle(true)
        val items = tracks.map { it.toMediaItem() }
        c.shuffleModeEnabled = false
        c.setMediaItems(items, tracks.indices.random(), 0L)
        c.prepare()
        c.playWhenReady = true
    }

    /** Insert a local track right after the currently playing item. */
    fun playNextLocal(track: LocalTrack) {
        val c = controller
        if (c == null) {
            raiseControllerNotReady("Player non ancora pronto.")
            return
        }
        LocalMediaResolver.register(track)
        val insertIndex = (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount)
        c.addMediaItem(insertIndex, track.toMediaItem(userQueued = true))
    }

    /** Append a local track to the tail of the user queue. */
    fun addLocalToQueue(track: LocalTrack) {
        val c = controller
        if (c == null) {
            raiseControllerNotReady("Player non ancora pronto.")
            return
        }
        LocalMediaResolver.register(track)
        var i = c.currentMediaItemIndex + 1
        while (i < c.mediaItemCount && c.getMediaItemAt(i).isUserQueued()) i++
        c.addMediaItem(i.coerceAtMost(c.mediaItemCount), track.toMediaItem(userQueued = true))
    }

    fun playPlaylist(songs: List<SongDto>, startIndex: Int = 0, sourceKey: String? = null) {
        if (songs.isEmpty()) return
        val c = controller
        if (c == null) {
            raiseControllerNotReady("Player non ancora pronto.")
            return
        }
        // Punto unico attraversato da ogni schermata di dettaglio (playlist,
        // album, artista, liked, generi) per costruire la coda: i segnaposto
        // del DJ (playable=false, download non ancora finito) o i brani col
        // file sparito non devono mai finire nel player. Su Android Auto un
        // elemento che non parte si comporta esattamente come uno skip
        // fantasma, e questa app ha gia' pagato due incidenti di quella
        // famiglia — non li riapriamo qui.
        //
        // L'indice si ricalcola per POSIZIONE, non per id: una playlist puo'
        // avere lo stesso brano due volte (duplicati stile Spotify), quindi
        // un confronto per id rischierebbe di puntare all'occorrenza
        // sbagliata. Si cerca il primo brano riproducibile alla posizione
        // toccata o subito dopo, cosi' un segnaposto proprio nella posizione
        // toccata scivola al brano riproducibile successivo invece di
        // bloccare tutto.
        val indexed = songs.withIndex().filter { it.value.playable }
        if (indexed.isEmpty()) return
        _activeSourceKey.value = sourceKey
        // Always hand the player the ORIGINAL order + the tapped start index.
        // Shuffle is owned by the service (EndlessQueueController): if shuffle
        // is on it keeps the tapped song current and reshuffles the tail, and
        // it keeps the true order for un-shuffle. Pre-shuffling here poisoned
        // the engine's "original order" pool.
        val items = indexed.map { it.value.toMediaItem() }
        val playIndex = indexed.indexOfFirst { it.index >= startIndex }
            .let { if (it >= 0) it else indexed.lastIndex }
        c.shuffleModeEnabled = false
        c.setMediaItems(items, playIndex, 0L)
        c.prepare()
        c.playWhenReady = true
    }

    fun playPlaylistShuffled(songs: List<SongDto>, sourceKey: String? = null) {
        // Stesso filtro di [playPlaylist] — vedi commento li' per il perche'.
        // Qui l'indice di partenza e' comunque casuale, quindi non serve
        // ricalcolare nessuna posizione: basta togliere i non riproducibili
        // prima di scegliere il punto di partenza.
        val playable = songs.filter { it.playable }
        if (playable.isEmpty()) return
        val c = controller
        if (c == null) {
            raiseControllerNotReady("Player non ancora pronto.")
            return
        }
        _activeSourceKey.value = sourceKey
        // Explicit shuffle-play: turn shuffle on via the pref (the service
        // reshuffles the full pool) and pass ORIGINAL order so the pool stays
        // authoritative; start on a random track for an immediate shuffled feel.
        _shuffleEnabled.value = true
        persistShuffle(true)
        val items = playable.map { it.toMediaItem() }
        c.shuffleModeEnabled = false
        c.setMediaItems(items, playable.indices.random(), 0L)
        c.prepare()
        c.playWhenReady = true
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun pause() { controller?.pause() }
    fun play() { controller?.play() }

    /**
     * Stop playback and clear the timeline so the MiniPlayer hides itself
     * (currentSong → null). Triggered by swipe-to-dismiss on the bar.
     */
    fun dismissPlayback() {
        val c = controller ?: return
        c.pause()
        c.clearMediaItems()
        _currentSong.value = null
        _queue.value = emptyList()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun skipNext() {
        // seekToNextMediaItem is routed through the session to the service's
        // [EndlessForwardingPlayer], which refills the tail from the full source
        // before advancing — so this stays endless without the VM touching the
        // queue, exactly like a car/BT NEXT does.
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        controller?.seekToPrevious()
    }

    fun toggleShuffle() {
        val c = controller ?: return
        val newShuffle = !_shuffleEnabled.value
        // Optimistic UI; the shared shuffle pref (collected in init) is the
        // authority and reconciles if this races the service.
        _shuffleEnabled.value = newShuffle
        // Route through the session so the service's [EndlessQueueController]
        // performs the actual reorder over the FULL source pool — same path an
        // Android Auto press takes, so behaviour is identical on phone and car
        // and keeps working headless.
        c.sendCustomCommand(
            SessionCommand(MediaPlaybackService.ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY),
            Bundle.EMPTY,
        )
    }

    private fun persistShuffle(value: Boolean) {
        viewModelScope.launch {
            runCatching {
                playbackPrefs.edit { it[PlaybackPrefs.SHUFFLE_KEY] = value }
            }
        }
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
        // Vale lo stesso ragionamento di [play]: un segnaposto del DJ non ha
        // ancora un file da mettere in coda.
        if (!song.playable) return
        val c = controller
        if (c == null) {
            raiseControllerNotReady("Player non ancora pronto.")
            return
        }
        val insertIndex = (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount)
        c.addMediaItem(insertIndex, song.toMediaItem(userQueued = true))
    }

    /**
     * Append [song] to the tail of the user queue (after any existing
     * user-queued items, before the auto-queue from the current source).
     * Spotify-style "Add to queue".
     */
    fun addToQueue(song: SongDto) {
        // Vale lo stesso ragionamento di [play]: un segnaposto del DJ non ha
        // ancora un file da mettere in coda.
        if (!song.playable) return
        val c = controller
        if (c == null) {
            raiseControllerNotReady("Player non ancora pronto.")
            return
        }
        var i = c.currentMediaItemIndex + 1
        while (i < c.mediaItemCount && c.getMediaItemAt(i).isUserQueued()) i++
        c.addMediaItem(i.coerceAtMost(c.mediaItemCount), song.toMediaItem(userQueued = true))
    }

    /**
     * Resolve the live timeline index for a queue row. The [QueueEntry.index]
     * captured in [pushQueue] can go stale: the service's front-prune
     * (`removeMediaItems(0, …)` on each transition) shifts every index down
     * between the last snapshot and the user's tap. Verify the hint still
     * points at the expected song; if not, fall back to the occurrence nearest
     * the hint so we act on the row the user actually meant, not a shifted one.
     */
    private fun resolveQueueIndex(expectedSongId: Long, hintIndex: Int): Int? {
        val c = controller ?: return null
        val timelineIds = (0 until c.mediaItemCount).map {
            c.getMediaItemAt(it).mediaId.removePrefix("song:").toLongOrNull()
        }
        return QueueIndexResolver.resolve(expectedSongId, hintIndex, timelineIds)
    }

    fun skipToQueueItem(songId: Long, hintIndex: Int) {
        val c = controller ?: return
        val idx = resolveQueueIndex(songId, hintIndex) ?: return
        c.seekTo(idx, 0L)
    }

    /**
     * Remove the queue item for [songId] (identified by its snapshot
     * [hintIndex], re-resolved against the live timeline). No-op for the
     * currently playing item — Media3 would silently advance to the next
     * track, which is never what a user means when they tap "remove" on a
     * row that's actively playing.
     */
    fun removeFromQueue(songId: Long, hintIndex: Int) {
        val c = controller ?: return
        val index = resolveQueueIndex(songId, hintIndex) ?: return
        if (index == c.currentMediaItemIndex) return
        c.removeMediaItem(index)
    }

    /**
     * Drop every track ahead of the currently-playing one — both user-queued
     * and source items. Wired to the QueueSheet "Cancella coda" sticky CTA.
     * Current track keeps playing; tapping play-next or queueing a new track
     * after this rebuilds the queue from scratch.
     */
    fun clearQueue() {
        val c = controller ?: return
        val current = c.currentMediaItemIndex
        val last = c.mediaItemCount - 1
        if (last <= current) return
        c.removeMediaItems(current + 1, last + 1)
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
        if (LocalMediaResolver.isLocal(current.id)) {
            _redownloadError.value = "Non disponibile per i brani locali"
            return
        }
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
                    if (wasDownloaded) DownloadRepository.download(fresh.id, fresh.title)
                }
                runCatching {
                    val loader = SingletonImageLoader.get(ctx)
                    loader.diskCache?.remove(coverUrl)
                    loader.memoryCache?.remove(MemoryCache.Key(coverUrl))
                    CoverContentProvider.invalidate(ctx, fresh.id)
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
                                    if (fresh.hasCoverArt) CoverContentProvider.uriFor(fresh.id) else null
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
     * Report a song as "wrong" globally — the backend wipes the audio,
     * cover, and video files from disk, hard-removes references from
     * playlists/likes/history, and tombstones the row so the importer
     * refuses to re-download the same content. Locally we drop every
     * matching item from the current Media3 timeline; if the flagged
     * song is the one playing, playback advances to the next item (or
     * stops if the queue is empty).
     */
    @Suppress("TooGenericExceptionCaught")
    fun flagWrong(songId: Long) {
        // Backend-only operation; positive ids only. Local items (negative)
        // and the sentinel zero are no-ops.
        if (songId <= 0L) return
        viewModelScope.launch {
            runCatching { songRepository.flagWrong(songId) }
            val c = controller ?: return@launch
            // Tell the service to drop it from the endless-queue source pool so
            // a refill/wrap never re-appends the flagged (now tombstoned) song.
            c.sendCustomCommand(
                SessionCommand(MediaPlaybackService.ACTION_DROP_FROM_SOURCE, Bundle.EMPTY),
                Bundle().apply { putLong(MediaPlaybackService.EXTRA_DROP_SONG_ID, songId) },
            )
            // Match by resolved song id, not raw mediaId — AA/library items use
            // the "song:{id}" form. If the flagged song is playing, leave it
            // first; then remove EVERY matching item by re-scanning the live
            // timeline back-to-front (indices captured up front go stale as the
            // endless engine prunes/refills, which used to delete the wrong row).
            val matches = { item: MediaItem? ->
                item != null && item.mediaId.removePrefix("song:").toLongOrNull() == songId
            }
            if (matches(c.currentMediaItem)) {
                if (c.hasNextMediaItem()) {
                    c.seekToNextMediaItem()
                } else {
                    c.pause()
                    c.clearMediaItems()
                    _currentSong.value = null
                    _queue.value = emptyList()
                    return@launch
                }
            }
            for (i in (c.mediaItemCount - 1) downTo 0) {
                if (matches(c.getMediaItemAt(i))) c.removeMediaItem(i)
            }
            // Cover cache will refetch and 404 once the row is flagged.
            runCatching {
                val ctx = getApplication<Application>()
                val coverUrl = Network.coverUrl(songId)
                val loader = SingletonImageLoader.get(ctx)
                loader.diskCache?.remove(coverUrl)
                loader.memoryCache?.remove(MemoryCache.Key(coverUrl))
                CoverContentProvider.invalidate(ctx, songId)
            }
        }
    }

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
        if (LocalMediaResolver.isLocal(current.id)) {
            _videoDownloadError.value = "Non disponibile per i brani locali"
            return
        }
        if (_videoDownloading.value) return
        _videoDownloading.value = true
        _videoDownloadError.value = null
        viewModelScope.launch {
            try {
                songRepository.downloadVideo(current.id)
                var wait = VideoJobPolling.INITIAL_DELAY_MS
                var attempts = 0
                var outcome: JobPollOutcome = JobPollOutcome.StillRunning
                while (attempts < VideoJobPolling.MAX_ATTEMPTS) {
                    delay(wait)
                    attempts++
                    val s = songRepository.getDownloadVideoStatus(current.id)
                    outcome = VideoJobPolling.outcomeOf(
                        status = s.status,
                        error = s.error,
                        fallbackMessage = "Download del video non riuscito",
                    )
                    if (outcome != JobPollOutcome.StillRunning) break
                    wait = VideoJobPolling.nextDelayMs(wait)
                }
                when (val o = outcome) {
                    JobPollOutcome.Done -> _currentSong.value = current.copy(hasVideo = true)
                    is JobPollOutcome.Failed -> _videoDownloadError.value = o.message
                    JobPollOutcome.StillRunning ->
                        _videoDownloadError.value = "Download del video scaduto"
                }
            } catch (t: Throwable) {
                _videoDownloadError.value = t.message ?: "Download del video non riuscito"
            } finally {
                _videoDownloading.value = false
            }
        }
    }

    fun consumeVideoDownloadError() { _videoDownloadError.value = null }

    @Suppress("TooGenericExceptionCaught")
    fun reinitializeVideoForCurrent() {
        val current = _currentSong.value ?: return
        if (LocalMediaResolver.isLocal(current.id)) {
            _videoReinitializeError.value = "Non disponibile per i brani locali"
            return
        }
        if (_videoReinitializing.value) return
        _videoReinitializing.value = true
        _videoReinitializeError.value = null
        viewModelScope.launch {
            try {
                songRepository.reinitializeVideo(current.id)
                var wait = VideoJobPolling.INITIAL_DELAY_MS
                var attempts = 0
                var outcome: JobPollOutcome = JobPollOutcome.StillRunning
                while (attempts < VideoJobPolling.MAX_ATTEMPTS) {
                    delay(wait)
                    attempts++
                    val s = songRepository.getReinitializeStatus(current.id)
                    outcome = VideoJobPolling.outcomeOf(
                        status = s.status,
                        error = s.error,
                        fallbackMessage = "Reinizializzazione non riuscita",
                    )
                    if (outcome != JobPollOutcome.StillRunning) break
                    wait = VideoJobPolling.nextDelayMs(wait)
                }
                when (val o = outcome) {
                    // Nothing to update on success: the video is re-fetched
                    // under the same id, so the current song is unchanged.
                    JobPollOutcome.Done -> Unit
                    is JobPollOutcome.Failed -> _videoReinitializeError.value = o.message
                    JobPollOutcome.StillRunning ->
                        _videoReinitializeError.value = "Reinizializzazione scaduta"
                }
            } catch (t: Throwable) {
                _videoReinitializeError.value = t.message ?: "Reinizializzazione non riuscita"
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
        return t.message ?: "Riscaricamento non riuscito"
    }

    /**
     * Save the current song's audio into MediaStore so the system Clock app
     * can pick it as an alarm sound. Result is surfaced via [alarmExportState].
     */
    @Suppress("TooGenericExceptionCaught")
    fun saveCurrentAsAlarmSound() {
        val current = _currentSong.value ?: return
        if (LocalMediaResolver.isLocal(current.id)) {
            _alarmExportState.value =
                AlarmExportState.Failure("Non disponibile per i brani locali")
            return
        }
        if (_alarmExportState.value is AlarmExportState.Exporting) return
        _alarmExportState.value = AlarmExportState.Exporting
        viewModelScope.launch {
            try {
                RingtoneExporter.exportAsAlarm(getApplication(), current)
                _alarmExportState.value = AlarmExportState.Success(current.title)
            } catch (t: Throwable) {
                _alarmExportState.value =
                    AlarmExportState.Failure(t.message ?: "Impossibile salvare la suoneria")
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
        if (LocalMediaResolver.isLocal(current.id)) return
        val ctx = getApplication<Application>()
        val streamUrl = Network.streamUrl(current.id)
        val coverUrl = Network.coverUrl(current.id)

        runCatching { PlayerCache.get(ctx).removeResource(streamUrl) }
        runCatching {
            val wasDownloaded = DownloadRepository.isDownloaded(current.id)
            DownloadRepository.remove(current.id)
            if (wasDownloaded) DownloadRepository.download(current.id, current.title)
        }
        runCatching {
            val loader = SingletonImageLoader.get(ctx)
            loader.diskCache?.remove(coverUrl)
            loader.memoryCache?.remove(MemoryCache.Key(coverUrl))
            CoverContentProvider.invalidate(ctx, current.id)
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
                            if (current.hasCoverArt) CoverContentProvider.uriFor(current.id) else null
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

    /**
     * Arms the service-side sleep timer. Triggers a pause when it expires.
     *
     * Local state flips optimistically so the UI reflects the new timer
     * immediately — in-process `MediaController` instances do not reliably
     * receive `onExtrasChanged` callbacks, so we cannot wait for the
     * service to publish fresh extras (same trick used by [toggleCurrentLike]).
     */
    fun setSleepTimer(minutes: Int) {
        val c = controller ?: return
        if (minutes > 0) {
            _sleepTimerActive.value = true
            _sleepTimerRemainingMs.value = minutes * 60_000L
            _sleepTimerEndOfTrack.value = false
        } else {
            _sleepTimerActive.value = false
            _sleepTimerRemainingMs.value = 0L
            _sleepTimerEndOfTrack.value = false
        }
        val args = android.os.Bundle().apply { putInt("minutes", minutes) }
        c.sendCustomCommand(
            SessionCommand(MediaPlaybackService.ACTION_SLEEP_TIMER, android.os.Bundle.EMPTY),
            args,
        )
    }

    /** Cancels the service-side sleep timer if armed. */
    fun cancelSleepTimer() {
        val c = controller ?: return
        _sleepTimerActive.value = false
        _sleepTimerRemainingMs.value = 0L
        _sleepTimerEndOfTrack.value = false
        c.sendCustomCommand(
            SessionCommand(MediaPlaybackService.ACTION_SLEEP_TIMER, android.os.Bundle.EMPTY),
            android.os.Bundle.EMPTY,
        )
    }

    /**
     * Arms the service-side sleep timer in end-of-track mode. Pause fires
     * on the next AUTO/REPEAT track transition; user-driven skips do not
     * count. Replaces any minute-mode timer already armed.
     */
    fun setEndOfTrackSleepTimer() {
        val c = controller ?: return
        _sleepTimerActive.value = true
        _sleepTimerRemainingMs.value = 0L
        _sleepTimerEndOfTrack.value = true
        val args = android.os.Bundle().apply { putBoolean("end_of_track", true) }
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
        val current = _currentSong.value
        // Flip from what the heart is actually showing. _currentLiked is fed by
        // session extras, which in-process controllers receive unreliably and
        // which carried the previous track's value across a transition, so
        // deriving the direction from it inverted the first tap after a track
        // change and wrote the wrong value into the cache the UI reads.
        val shownLiked = current?.id?.let { LikedSongsCache.isLiked(it) } ?: _currentLiked.value
        val newLiked = !shownLiked
        _currentLiked.value = newLiked
        if (current != null && LocalMediaResolver.isLocal(current.id)) {
            // Local heart writes to a separate DataStore — backend likes are
            // keyed by positive song ids, this side has no equivalent row.
            val ctx = getApplication<Application>()
            viewModelScope.launch {
                LocalLikedStore.instance(ctx).setLiked(-current.id, newLiked)
            }
            return
        }
        current?.id?.let { LikedSongsCache.markLiked(it, newLiked) }
        c.sendCustomCommand(
            SessionCommand(MediaPlaybackService.ACTION_TOGGLE_LIKE, android.os.Bundle.EMPTY),
            android.os.Bundle.EMPTY,
        )
    }

    /**
     * Re-seed the heart for the track that just started.
     *
     * Session extras are the service's channel for liked state, but in-process
     * controllers receive `onExtrasChanged` unreliably, so without this the
     * flag simply carried the previous track's value into the new one.
     * Positive ids read the shared cache the heart itself renders from; local
     * tracks (negative ids) live in their own store and are read off-thread.
     */
    private fun refreshCurrentLiked(songId: Long?) {
        if (songId == null) {
            _currentLiked.value = false
            return
        }
        if (!LocalMediaResolver.isLocal(songId)) {
            _currentLiked.value = LikedSongsCache.isLiked(songId)
            return
        }
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            _currentLiked.value = LocalLikedStore.instance(ctx).isLiked(-songId)
        }
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
        val listened = listenClock.bankedMs
        val duration = trackedDurationMs
        // Every gate lives in the policy: the local-file exclusion, the
        // micro-skip floor, the full-play rule and the completion ratio.
        // Both this path and flushPlayHistoryAwait go through it, so the
        // two can't drift — they used to, and the flush path let sub-second
        // listens count as full plays whenever the duration was bogus.
        val record = PlayRecordingPolicy.evaluate(id, listened, duration) ?: return
        val countsAsFullPlay = record.countsAsFullPlay
        val ratio = record.completionRatio
        val playLabel = PlayRecordingPolicy.displayLabel(trackedSongTitle, trackedSongArtist)
        viewModelScope.launch {
            historyRepository.record(
                songId = id,
                durationListenedMs = listened,
                completionRatio = ratio,
                wasSkipped = !countsAsFullPlay,
                displayLabel = playLabel,
            )
        }
        // Optimistically push to the shared recents cache so Home + Search
        // carousels reflect the play immediately. Skip micro-skips: only
        // count plays that pass the full-play threshold (matches what the
        // backend's /recent eventually emits).
        if (countsAsFullPlay) {
            // _currentSong still points to the song that just played —
            // onMediaItemTransition assigns the new SongDto only after
            // maybeRecordPlay returns.
            _currentSong.value?.takeIf { it.id == id }?.let(RecentsCache::markPlayed)
        }
        // Auto-download only songs the user actually listened through —
        // same full-play bar used for history (>=30s OR >=50% of duration).
        // A manual skip after a few seconds must NOT cache the track: the
        // user rejected it, downloading it wastes storage + bandwidth and
        // pollutes the offline library with stuff they skipped past.
        val title = trackedSongTitle
        if (countsAsFullPlay) {
            viewModelScope.launch {
                if (PlayerSettings.instance.downloadAutoNow() &&
                    !DownloadRepository.isDownloaded(id)
                ) {
                    DownloadRepository.download(id, title)
                }
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
        listenClock.bankAndContinue(stillPlaying = controller?.isPlaying == true)
        val id = trackedSongId ?: return
        val listened = listenClock.bankedMs
        val duration = trackedDurationMs
        // Same policy as maybeRecordPlay — local-file exclusion and
        // micro-skip floor included. This path only emits full plays;
        // partial ones wait for the next transition to be reported as skips.
        val record = PlayRecordingPolicy.evaluate(id, listened, duration) ?: return
        if (!record.countsAsFullPlay) return
        val ratio = record.completionRatio
        val flushLabel = PlayRecordingPolicy.displayLabel(trackedSongTitle, trackedSongArtist)
        // Direct POST so /recent reflects this play on the next refresh.
        // Falls back to the queue inside recordImmediate if offline.
        historyRepository.recordImmediate(
            songId = id,
            durationListenedMs = listened,
            completionRatio = ratio,
            wasSkipped = false,
            displayLabel = flushLabel,
        )
        // Optimistically prepend in the shared cache (same gate as the
        // historyRepository call above — only full plays, no skips).
        _currentSong.value?.takeIf { it.id == id }?.let(RecentsCache::markPlayed)
        if (PlayerSettings.instance.downloadAutoNow() &&
            !DownloadRepository.isDownloaded(id)
        ) {
            DownloadRepository.download(id, trackedSongTitle)
        }
        listenClock.reset()
    }

    override fun onCleared() {
        listenClock.bank()
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
        // The listen threshold and the micro-skip floor live in
        // PlayRecordingPolicy; the video job backoff and its cap live in
        // VideoJobPolling.
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

internal const val KEY_USER_QUEUED = "user_queued"

@UnstableApi
private fun SongDto.toMediaItem(userQueued: Boolean = false): MediaItem {
    // Local tracks: id is the negation of a MediaStore _ID. Resolve the
    // content:// URI from the in-memory bridge so the player streams from
    // disk instead of hitting the backend.
    if (LocalMediaResolver.isLocal(id)) {
        val track = LocalMediaResolver.get(id)
            ?: error("Local track $id is not registered with LocalMediaResolver")
        return track.toMediaItem(userQueued = userQueued)
    }
    val extras = if (userQueued) {
        android.os.Bundle().apply { putBoolean(KEY_USER_QUEUED, true) }
    } else null
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(
            if (hasCoverArt) CoverContentProvider.uriFor(id) else null
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
internal fun LocalTrack.toSongDto(): SongDto = SongDto(
    id = -id,
    title = title.ifBlank { "(senza titolo)" },
    artist = artist.ifBlank { "Sconosciuto" },
    album = album,
    durationMs = durationMs,
    hasCoverArt = albumArtUri != null,
    hasVideo = false,
    playable = true,
)

@UnstableApi
private fun LocalTrack.toMediaItem(userQueued: Boolean = false): MediaItem {
    val extras = if (userQueued) {
        android.os.Bundle().apply { putBoolean(KEY_USER_QUEUED, true) }
    } else null
    val metadata = MediaMetadata.Builder()
        .setTitle(title.ifBlank { "(senza titolo)" })
        .setArtist(artist.ifBlank { "Sconosciuto" })
        .setAlbumTitle(album)
        .setArtworkUri(albumArtUri)
        .apply { if (extras != null) setExtras(extras) }
        .build()
    return MediaItem.Builder()
        .setMediaId((-id).toString())
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}

@UnstableApi
internal fun MediaItem.isUserQueued(): Boolean =
    mediaMetadata.extras?.getBoolean(KEY_USER_QUEUED, false) == true

@UnstableApi
internal fun MediaItem.toSongDto(): SongDto? {
    // Android Auto, the media library and playback resumption all emit the
    // "song:{id}" mediaId form; only the phone UI uses a bare number. Without
    // stripping the prefix this returned null for every car-, resumption- or
    // voice-started session, which blanked the mini player, auto-closed the
    // Now Playing sheet, emptied the queue sheet and suppressed play history.
    val songId = mediaId.removePrefix("song:").toLongOrNull() ?: return null
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
