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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.mediaplayer.android.ui.common.friendlyMessage
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Artists") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::pullRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when (val s = state) {
                ArtistListUiState.Loading -> CenteredSpinner()
                is ArtistListUiState.Error -> ErrorWithRetry(s.message, viewModel::refresh)
                is ArtistListUiState.Success -> {
                    if (s.artists.isEmpty()) {
                        CenteredMessage("No artists in catalog.")
                    } else {
                        val listState = rememberLazyListState()
                        LaunchedEffect(listState, s.artists.size, s.endReached) {
                            if (s.endReached) return@LaunchedEffect
                            snapshotFlow {
                                val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                                    ?: return@snapshotFlow false
                                val total = listState.layoutInfo.totalItemsCount
                                total > 0 && last >= total - 5
                            }
                                .distinctUntilChanged()
                                .filter { it }
                                .collect { viewModel.loadMore() }
                        }
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
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
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistRow(artist: ArtistDto, onClick: () -> Unit) {
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun buildSubtitle(artist: ArtistDto): String {
    val albumPart = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"
    val songPart = if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"
    return "$albumPart · $songPart"
}

