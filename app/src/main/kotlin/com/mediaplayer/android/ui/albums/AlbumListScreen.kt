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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.mediaplayer.android.ui.common.MHCaptionHeader
import com.mediaplayer.android.ui.common.friendlyMessage
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
            AlbumListUiState.Error(friendlyMessage(t))
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

    val totalCount = (state as? AlbumListUiState.Success)?.totalItems?.toInt()
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(AlbumSort.Recenti) }
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MHCaptionHeader(
                eyebrow = "LIBRERIA",
                title = "Album",
                count = totalCount,
                onBack = onBack,
            )
            AlbumControlsRow(
                query = query,
                onQueryChange = { query = it },
                sort = sort,
                onSortToggle = {
                    sort = if (sort == AlbumSort.Recenti) AlbumSort.Alfabetico else AlbumSort.Recenti
                },
            )
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::pullRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val s = state) {
                    AlbumListUiState.Loading -> CenteredSpinner()
                    is AlbumListUiState.Error -> ErrorWithRetry(s.message, viewModel::refresh)
                    is AlbumListUiState.Success -> {
                        if (s.albums.isEmpty()) {
                            CenteredMessage("Nessun album nel catalogo.")
                        } else {
                        val displayAlbums = remember(s.albums, query, sort) {
                            val q = query.trim().lowercase()
                            val filtered = if (q.isEmpty()) s.albums else s.albums.filter {
                                it.name.lowercase().contains(q) || it.artist.lowercase().contains(q)
                            }
                            when (sort) {
                                AlbumSort.Recenti -> filtered
                                AlbumSort.Alfabetico -> filtered.sortedBy { it.name.lowercase() }
                            }
                        }
                        val gridState = rememberLazyGridState()
                        LaunchedEffect(gridState, s.albums.size, s.endReached) {
                            // Pause server-paged load-more while the user is
                            // searching: the in-memory filter would otherwise
                            // request more pages even when the visible result
                            // set is tiny.
                            if (s.endReached || query.isNotBlank()) return@LaunchedEffect
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
                            if (displayAlbums.isEmpty()) {
                                item(
                                    key = "search-empty",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) {
                                    CenteredMessage("Nessun album corrisponde a \"$query\".")
                                }
                            }
                            items(items = displayAlbums, key = { "${it.artist}|${it.name}" }) { album ->
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

private enum class AlbumSort { Recenti, Alfabetico }

@Composable
private fun AlbumControlsRow(
    query: String,
    onQueryChange: (String) -> Unit,
    sort: AlbumSort,
    onSortToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(8.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = "Cerca album…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(
                        MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                IconBox(
                    icon = Icons.Filled.Close,
                    onClick = { onQueryChange("") },
                )
            }
        }
        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onSortToggle)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Sort,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = when (sort) {
                    AlbumSort.Recenti -> "Recenti"
                    AlbumSort.Alfabetico -> "A → Z"
                },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun IconBox(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

