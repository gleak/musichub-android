package com.mediaplayer.android.ui.genre

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mediaplayer.android.data.SongRepository
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.CenteredMessage
import com.mediaplayer.android.ui.common.CenteredSpinner
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.MHCaptionHeader
import com.mediaplayer.android.ui.common.friendlyMessage
import com.mediaplayer.android.ui.search.SongRow
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

sealed interface GenreDetailUiState {
    data object Loading : GenreDetailUiState
    data class Success(
        val songs: List<SongDto>,
        val totalItems: Long,
        val loadingMore: Boolean = false,
        val endReached: Boolean = false,
    ) : GenreDetailUiState
    data class Error(val message: String) : GenreDetailUiState
}

class GenreDetailViewModel(
    private val tag: String,
    private val repository: SongRepository = SongRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow<GenreDetailUiState>(GenreDetailUiState.Loading)
    val state: StateFlow<GenreDetailUiState> = _state.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private var nextPage: Int = 0

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = GenreDetailUiState.Loading
            loadFirst()
        }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadFirst()
            _isRefreshing.value = false
        }
    }

    private suspend fun loadFirst() {
        nextPage = 0
        _state.value = try {
            val page = repository.listSongs(query = null, genre = tag, page = 0, size = PAGE_SIZE)
            nextPage = 1
            GenreDetailUiState.Success(
                songs = page.items,
                totalItems = page.totalItems,
                endReached = page.items.size >= page.totalItems || page.items.isEmpty(),
            )
        } catch (t: Throwable) {
            GenreDetailUiState.Error(friendlyMessage(t))
        }
    }

    fun loadMore() {
        val cur = _state.value as? GenreDetailUiState.Success ?: return
        if (cur.loadingMore || cur.endReached) return
        _state.value = cur.copy(loadingMore = true)
        val pageToLoad = nextPage
        viewModelScope.launch {
            try {
                val page = repository.listSongs(query = null, genre = tag, page = pageToLoad, size = PAGE_SIZE)
                val merged = cur.songs + page.items
                nextPage = pageToLoad + 1
                _state.value = GenreDetailUiState.Success(
                    songs = merged,
                    totalItems = page.totalItems,
                    loadingMore = false,
                    endReached = merged.size.toLong() >= page.totalItems || page.items.isEmpty(),
                )
            } catch (_: Throwable) {
                _state.value = cur.copy(loadingMore = false)
            }
        }
    }

    private companion object { const val PAGE_SIZE = 30 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreDetailScreen(
    tag: String,
    displayName: String,
    onBack: () -> Unit,
    onSongClick: (SongDto) -> Unit,
    onPlayAll: (List<SongDto>) -> Unit,
    onShufflePlay: (List<SongDto>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: GenreDetailViewModel = viewModel(
        key = "genre-$tag",
        factory = viewModelFactory { initializer { GenreDetailViewModel(tag) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val mono = LocalMHMono.current

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MHCaptionHeader(
                eyebrow = "SFOGLIA · GENERE",
                title = displayName,
                onBack = onBack,
            )
            // Removable pill — tapping it pops back to Search.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GenrePill(name = displayName, onClear = onBack)
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::pullRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val s = state) {
                    GenreDetailUiState.Loading -> CenteredSpinner()
                    is GenreDetailUiState.Error -> ErrorWithRetry(s.message, viewModel::refresh)
                    is GenreDetailUiState.Success -> {
                        if (s.songs.isEmpty()) {
                            CenteredMessage("Nessun brano in $displayName.")
                        } else {
                            GenreBody(
                                state = s,
                                displayName = displayName,
                                totalItems = s.totalItems,
                                mono = mono,
                                onSongClick = onSongClick,
                                onPlayAll = { onPlayAll(s.songs) },
                                onShufflePlay = { onShufflePlay(s.songs) },
                                onLoadMore = viewModel::loadMore,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreBody(
    state: GenreDetailUiState.Success,
    displayName: String,
    totalItems: Long,
    mono: com.mediaplayer.android.ui.theme.MHMonoTextStyles,
    onSongClick: (SongDto) -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.songs) {
        com.mediaplayer.android.data.LikedSongsCache.prime(state.songs.map { it.id })
    }
    com.mediaplayer.android.ui.common.PrefetchNearEnd(
        listState = listState,
        enabled = !state.endReached,
        onLoadMore = onLoadMore,
    )
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item(key = "ctas") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onPlayAll,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MHColors.Lime,
                        contentColor = Color(0xFF0A0A0A),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "Riproduci tutti",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(onClick = onShufflePlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Casuale",
                        tint = MHColors.Lime,
                    )
                }
            }
        }
        item(key = "count_caption") {
            Text(
                text = "// ${totalItems.toInt()} BRANI",
                style = mono.eyebrow.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
            )
        }
        itemsIndexed(items = state.songs, key = { _, song -> song.id }) { _, song ->
            SongRow(
                song = song,
                onClick = { onSongClick(song) },
            )
        }
        if (state.loadingMore) {
            item(key = "loading-more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun GenrePill(name: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MHColors.Lime)
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
            .clickable(onClick = onClear),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF0A0A0A),
        )
        Spacer(Modifier.size(6.dp))
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(0x26000000)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Rimuovi filtro",
                tint = Color(0xFF0A0A0A),
                modifier = Modifier.size(11.dp),
            )
        }
    }
}
