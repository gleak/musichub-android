package com.mediaplayer.android.ui.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.data.CatalogRepository
import com.mediaplayer.android.data.dto.AlbumDto
import com.mediaplayer.android.ui.common.CenteredMessage
import com.mediaplayer.android.ui.common.CenteredSpinner
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.theme.CoverShapes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AlbumListUiState {
    data object Loading : AlbumListUiState
    data class Success(
        val albums: List<AlbumDto>,
        val totalItems: Long,
        val loadingMore: Boolean = false,
        val endReached: Boolean = false,
    ) : AlbumListUiState
    data class Error(val message: String) : AlbumListUiState
}

class AlbumListViewModel(
    private val repository: CatalogRepository = CatalogRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow<AlbumListUiState>(AlbumListUiState.Loading)
    val state: StateFlow<AlbumListUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var nextPage: Int = 0

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = AlbumListUiState.Loading
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
            val page = repository.listAlbums(page = 0, size = PAGE_SIZE)
            nextPage = 1
            AlbumListUiState.Success(
                albums = page.items,
                totalItems = page.totalItems,
                endReached = page.items.size >= page.totalItems || page.items.isEmpty(),
            )
        } catch (t: Throwable) {
            AlbumListUiState.Error(t.message ?: "Unknown error")
        }
    }

    fun loadMore() {
        val current = _state.value as? AlbumListUiState.Success ?: return
        if (current.loadingMore || current.endReached) return
        _state.value = current.copy(loadingMore = true)
        val pageToLoad = nextPage
        viewModelScope.launch {
            try {
                val page = repository.listAlbums(page = pageToLoad, size = PAGE_SIZE)
                val merged = current.albums + page.items
                nextPage = pageToLoad + 1
                _state.value = AlbumListUiState.Success(
                    albums = merged,
                    totalItems = page.totalItems,
                    loadingMore = false,
                    endReached = merged.size.toLong() >= page.totalItems || page.items.isEmpty(),
                )
            } catch (_: Throwable) {
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
fun AlbumListScreen(
    onBack: () -> Unit,
    onAlbumClick: (name: String, artist: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Albums") },
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
                AlbumListUiState.Loading -> CenteredSpinner()
                is AlbumListUiState.Error -> ErrorWithRetry(s.message, viewModel::refresh)
                is AlbumListUiState.Success -> {
                    if (s.albums.isEmpty()) {
                        CenteredMessage("No albums in catalog.")
                    } else {
                        val gridState = rememberLazyGridState()
                        LaunchedEffect(gridState, s.albums.size, s.endReached) {
                            if (s.endReached) return@LaunchedEffect
                            snapshotFlow {
                                val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                                    ?: return@snapshotFlow false
                                val total = gridState.layoutInfo.totalItemsCount
                                total > 0 && last >= total - 6
                            }
                                .distinctUntilChanged()
                                .filter { it }
                                .collect { viewModel.loadMore() }
                        }
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(items = s.albums, key = { "${it.artist}|${it.name}" }) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onAlbumClick(album.name, album.artist) },
                                )
                            }
                            if (s.loadingMore) {
                                item(
                                    key = "loading-more",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) {
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
private fun AlbumCard(album: AlbumDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CoverShapes.SongRow)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp),
            )
        }
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

