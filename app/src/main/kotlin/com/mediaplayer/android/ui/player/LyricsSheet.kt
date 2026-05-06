package com.mediaplayer.android.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.LyricsRepository
import com.mediaplayer.android.data.dto.LyricLineDto
import kotlinx.coroutines.launch

@Composable
fun LyricsView(
    songId: Long,
    positionMs: Long,
    modifier: Modifier = Modifier,
    repository: LyricsRepository = remember { LyricsRepository() },
) {
    var lines by remember(songId) { mutableStateOf<List<LyricLineDto>>(emptyList()) }
    var loading by remember(songId) { mutableStateOf(true) }
    var noLyrics by remember(songId) { mutableStateOf(false) }
    var importing by remember(songId) { mutableStateOf(false) }
    var importFailed by remember(songId) { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(songId) {
        loading = true
        noLyrics = false
        importFailed = false
        lines = emptyList()
        try {
            lines = repository.getLyrics(songId)
        } catch (_: Throwable) {
            noLyrics = true
        } finally {
            loading = false
        }
    }

    val activeIndex = remember(positionMs, lines) {
        if (lines.isEmpty()) -1
        else lines.indexOfLast { it.positionMs <= positionMs }.coerceAtLeast(0)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && lines.isNotEmpty()) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Testo",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        when {
            loading -> Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
            noLyrics -> Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (importFailed) "Testo non trovato" else "Testo non disponibile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (importing) return@Button
                        importing = true
                        importFailed = false
                        scope.launch {
                            try {
                                val fetched = repository.importLyrics(songId)
                                lines = fetched
                                noLyrics = false
                            } catch (_: Throwable) {
                                importFailed = true
                            } finally {
                                importing = false
                            }
                        }
                    },
                    enabled = !importing,
                ) {
                    if (importing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Scaricamento…")
                    } else {
                        Text(if (importFailed) "Riprova" else "Scarica testo")
                    }
                }
            }
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 420.dp),
            ) {
                itemsIndexed(items = lines, key = { i, _ -> i }) { index, line ->
                    val isActive = index == activeIndex
                    val isPast = index < activeIndex
                    val baseColor = MaterialTheme.colorScheme.onSurface
                    Text(
                        text = line.text,
                        style = if (isActive)
                            MaterialTheme.typography.headlineSmall
                        else
                            MaterialTheme.typography.bodyLarge,
                        color = when {
                            isActive -> MaterialTheme.colorScheme.primary
                            isPast -> baseColor.copy(alpha = 0.35f)
                            else -> baseColor.copy(alpha = 0.5f)
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(
    songId: Long,
    positionMs: Long,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LyricsView(
            songId = songId,
            positionMs = positionMs,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}
