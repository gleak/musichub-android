package com.mediaplayer.android.ui.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.data.CatalogRepository
import com.mediaplayer.android.data.dto.ArtistDto
import com.mediaplayer.android.ui.common.CenteredMessage
import com.mediaplayer.android.ui.common.CenteredSpinner
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.MHCaptionHeader
import com.mediaplayer.android.ui.common.friendlyMessage
import com.mediaplayer.android.ui.theme.LocalMHMono
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ArtistListUiState {
    data object Loading : ArtistListUiState
    data class Success(
        val artists: List<ArtistDto>,
        val totalItems: Long,
        val loadingMore: Boolean = false,
        val endReached: Boolean = false,
    ) : ArtistListUiState
    data class Error(val message: String) : ArtistListUiState
}

class ArtistListViewModel(
    private val repository: CatalogRepository = CatalogRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow<ArtistListUiState>(ArtistListUiState.Loading)
    val state: StateFlow<ArtistListUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var nextPage: Int = 0

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = ArtistListUiState.Loading
            loadFirstPage()
        }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadFirstPage()
            _isRefreshing.value = false
        }
    }

    private suspend fun loadFirstPage() {
        nextPage = 0
        _state.value = try {
            val page = repository.listArtists(page = 0, size = PAGE_SIZE)
            nextPage = 1
            ArtistListUiState.Success(
                artists = page.items,
                totalItems = page.totalItems,
                endReached = page.items.size >= page.totalItems || page.items.isEmpty(),
            )
        } catch (t: Throwable) {
            ArtistListUiState.Error(friendlyMessage(t))
        }
    }

    fun loadMore() {
        val current = _state.value as? ArtistListUiState.Success ?: return
        if (current.loadingMore || current.endReached) return
        _state.value = current.copy(loadingMore = true)
        val pageToLoad = nextPage
        viewModelScope.launch {
            try {
                val page = repository.listArtists(page = pageToLoad, size = PAGE_SIZE)
                val merged = current.artists + page.items
                nextPage = pageToLoad + 1
                _state.value = ArtistListUiState.Success(
                    artists = merged,
                    totalItems = page.totalItems,
                    loadingMore = false,
                    endReached = merged.size.toLong() >= page.totalItems || page.items.isEmpty(),
                )
            } catch (_: Throwable) {
                // Silent: keep what we have, user can pull-to-refresh.
                _state.value = current.copy(loadingMore = false)
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistListScreen(
    onBack: () -> Unit,
    onArtistClick: (name: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val totalCount = (state as? ArtistListUiState.Success)?.totalItems?.toInt()
    val coroutineScope = rememberCoroutineScope()
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MHCaptionHeader(
                eyebrow = "LIBRERIA",
                title = "Artisti",
                count = totalCount,
                onBack = onBack,
            )
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::pullRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val s = state) {
                    ArtistListUiState.Loading -> CenteredSpinner()
                    is ArtistListUiState.Error -> ErrorWithRetry(s.message, viewModel::refresh)
                    is ArtistListUiState.Success -> {
                        if (s.artists.isEmpty()) {
                            CenteredMessage("Nessun artista nel catalogo.")
                        } else {
                        val listState = rememberLazyListState()
                        com.mediaplayer.android.ui.common.PrefetchNearEnd(
                            listState = listState,
                            enabled = !s.endReached,
                            onLoadMore = { viewModel.loadMore() },
                        )
                        Row(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                            ) {
                                items(items = s.artists, key = { it.name }) { artist ->
                                    ArtistRow(artist = artist, onClick = { onArtistClick(artist.name) })
                                }
                                if (s.loadingMore) {
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
                            AlphabetScrubber(
                                artists = s.artists,
                                onLetterTap = { idx ->
                                    if (idx >= 0) coroutineScope.launch {
                                        listState.scrollToItem(idx)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun ArtistRow(artist: ArtistDto, onClick: () -> Unit) {
    val mono = LocalMHMono.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildSubtitle(artist),
                style = mono.duration.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun buildSubtitle(artist: ArtistDto): String {
    val albumPart = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} album"
    val songPart = if (artist.songCount == 1) "1 brano" else "${artist.songCount} brani"
    return "$albumPart · $songPart"
}

// Italian alphabet from `mh-library.jsx:87` — drops J/K/U/W/X (rare in IT names).
private val SCRUBBER_LETTERS = listOf(
    'A','B','C','D','E','F','G','H','I','L',
    'M','N','O','P','Q','R','S','T','V','Y','Z',
)

@Composable
private fun AlphabetScrubber(
    artists: List<ArtistDto>,
    onLetterTap: (index: Int) -> Unit,
) {
    val mono = LocalMHMono.current
    // Map letter → index of first artist whose name starts with it (case-fold).
    val firstIndexByLetter = remember(artists) {
        val out = mutableMapOf<Char, Int>()
        artists.forEachIndexed { idx, artist ->
            val first = artist.name.firstOrNull()?.uppercaseChar() ?: return@forEachIndexed
            out.putIfAbsent(first, idx)
        }
        out
    }
    Column(
        modifier = Modifier
            .width(20.dp)
            .padding(end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SCRUBBER_LETTERS.forEach { letter ->
            val idx = firstIndexByLetter[letter] ?: -1
            val active = idx >= 0
            Box(
                modifier = Modifier
                    .size(width = 18.dp, height = 12.dp)
                    .clickable(enabled = active) { onLetterTap(idx) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter.toString(),
                    style = mono.duration.copy(
                        color = if (active) com.mediaplayer.android.ui.theme.MHColors.Lime
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

