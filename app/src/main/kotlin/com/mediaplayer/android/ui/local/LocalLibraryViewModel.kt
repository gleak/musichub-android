package com.mediaplayer.android.ui.local

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.local.LocalLibraryRepository
import com.mediaplayer.android.data.local.LocalMediaResolver
import com.mediaplayer.android.data.local.LocalPlaylist
import com.mediaplayer.android.data.local.LocalPlaylistStore
import com.mediaplayer.android.data.local.LocalTrack
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocalLibraryViewModel(application: Application) : AndroidViewModel(application) {

    enum class Tab { Tracks, Folders, Albums, Playlists }

    enum class SortBy { Title, Artist, Album, DateAdded, Duration }

    sealed interface State {
        data object PermissionRequired : State
        data object Loading : State
        data class Ready(val tracks: List<LocalTrack>) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _tab = MutableStateFlow(Tab.Tracks)
    val tab: StateFlow<Tab> = _tab.asStateFlow()

    private val _sort = MutableStateFlow(SortBy.Title)
    val sort: StateFlow<SortBy> = _sort.asStateFlow()

    private val playlistStore = LocalPlaylistStore.instance(application)
    val playlists: StateFlow<List<LocalPlaylist>> = playlistStore.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var observerJob: Job? = null

    init {
        bootstrap()
    }

    /** Re-evaluate permission and start (or stop) observing. */
    fun bootstrap() {
        val ctx = getApplication<Application>()
        if (!LocalLibraryRepository.hasPermission(ctx)) {
            observerJob?.cancel()
            observerJob = null
            _state.value = State.PermissionRequired
            return
        }
        if (observerJob?.isActive == true) return
        _state.value = State.Loading
        observerJob = viewModelScope.launch {
            LocalLibraryRepository.observe(ctx).collect { fresh ->
                LocalMediaResolver.replaceAll(fresh)
                _state.value = State.Ready(fresh)
            }
        }
    }

    fun selectTab(tab: Tab) { _tab.value = tab }
    fun selectSort(sort: SortBy) { _sort.value = sort }

    /** One-shot rescan, e.g. after pinning a SAF folder. */
    fun refresh() {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val fresh = LocalLibraryRepository.scan(ctx)
            LocalMediaResolver.replaceAll(fresh)
            _state.value = State.Ready(fresh)
        }
    }

    /** Apply the current sort to the loaded tracks. Inert when not Ready. */
    fun sortedTracks(): List<LocalTrack> {
        val ready = _state.value as? State.Ready ?: return emptyList()
        return when (_sort.value) {
            SortBy.Title -> ready.tracks.sortedBy { it.title.lowercase() }
            SortBy.Artist -> ready.tracks.sortedWith(
                compareBy({ it.artist.lowercase() }, { it.title.lowercase() })
            )
            SortBy.Album -> ready.tracks.sortedWith(
                compareBy({ (it.album ?: "").lowercase() }, { it.title.lowercase() })
            )
            SortBy.DateAdded -> ready.tracks.sortedByDescending { it.dateAddedMs }
            SortBy.Duration -> ready.tracks.sortedByDescending { it.durationMs }
        }
    }

    /** Group by folder. Map iteration is alphabetical by folder name. */
    fun foldersGrouped(): List<Pair<String, List<LocalTrack>>> {
        val ready = _state.value as? State.Ready ?: return emptyList()
        return ready.tracks
            .groupBy { it.folderPath.ifBlank { it.folderName } }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .map { (path, items) -> path to items }
    }

    /** Group by album. Albums-without-name are filtered out. */
    fun albumsGrouped(): List<Pair<String, List<LocalTrack>>> {
        val ready = _state.value as? State.Ready ?: return emptyList()
        return ready.tracks
            .filter { !it.album.isNullOrBlank() }
            .groupBy { it.album!! }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .map { (album, items) -> album to items }
    }

    /** Resolve a list of MediaStore ids to the in-memory [LocalTrack] objects. */
    fun resolveTracks(trackIds: List<Long>): List<LocalTrack> {
        if (trackIds.isEmpty()) return emptyList()
        val ready = _state.value as? State.Ready ?: return emptyList()
        val byId = ready.tracks.associateBy { it.id }
        return trackIds.mapNotNull { byId[it] }
    }

    fun createPlaylist(name: String, trackIds: List<Long> = emptyList()) {
        viewModelScope.launch { playlistStore.create(name, trackIds) }
    }

    fun renamePlaylist(id: String, name: String) {
        viewModelScope.launch { playlistStore.rename(id, name) }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch { playlistStore.delete(id) }
    }

    fun addTracksToPlaylist(id: String, trackIds: List<Long>) {
        viewModelScope.launch { playlistStore.addTracks(id, trackIds) }
    }

    fun removeTrackFromPlaylist(id: String, trackId: Long) {
        viewModelScope.launch { playlistStore.removeTrack(id, trackId) }
    }

    fun reorderPlaylist(id: String, trackIds: List<Long>) {
        viewModelScope.launch { playlistStore.reorder(id, trackIds) }
    }
}
