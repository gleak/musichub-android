package com.mediaplayer.android.ui.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Fires [onLoadMore] when the user scrolls within [threshold] items of the
 * bottom. Replaces the per-screen `LaunchedEffect { snapshotFlow { ... }
 * .distinctUntilChanged().filter { it }.collect { ... } }` recipe that
 * used to live on every paged list (Liked, Genre, Search artists, etc.).
 *
 * Pass [enabled] = false once the page cursor reaches the end of the
 * dataset so the trigger goes silent and the snapshotFlow doesn't keep
 * firing on every scroll past the tail.
 */
@Composable
fun PrefetchNearEnd(
    listState: LazyListState,
    threshold: Int = 5,
    enabled: Boolean = true,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(listState, threshold, enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@snapshotFlow false
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - threshold
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
}

/** Grid sibling of the [LazyListState] [PrefetchNearEnd]. Same semantics. */
@Composable
fun PrefetchNearEnd(
    gridState: LazyGridState,
    threshold: Int = 6,
    enabled: Boolean = true,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(gridState, threshold, enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@snapshotFlow false
            val total = gridState.layoutInfo.totalItemsCount
            total > 0 && last >= total - threshold
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
}
