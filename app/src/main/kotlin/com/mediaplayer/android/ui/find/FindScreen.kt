package com.mediaplayer.android.ui.find

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.R
import com.mediaplayer.android.data.dto.CandidateDto
import com.mediaplayer.android.data.dto.CandidateKind
import com.mediaplayer.android.data.dto.RequestDto
import com.mediaplayer.android.data.dto.RequestStatus

/**
 * "Find new music" tab.
 *
 * Two phases in one composable:
 *   - Query form → candidate picker (Albums / Singles tabs).
 *   - After a candidate is selected: status screen polling the backend
 *     request until IMPORTED / IMPORTED_PARTIAL / FAILED.
 *
 * Kept as a single screen (rather than a sub-nav graph) because the
 * flow is short and users expect the back-arrow to return them to an
 * empty query field, not re-enter a stale picker.
 */
@Composable
fun FindScreen(
    modifier: Modifier = Modifier,
    viewModel: FindViewModel = viewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        QueryBar(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onSubmit = viewModel::submit,
            enabled = state !is FindUiState.Searching,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(modifier = Modifier.fillMaxSize()) {
            when (val s = state) {
                FindUiState.Idle ->
                    CenteredMessage(stringResource(R.string.find_empty))

                FindUiState.Searching -> CenteredSpinner()

                is FindUiState.Error ->
                    CenteredMessage(
                        stringResource(R.string.find_error_prefix) + "\n" + s.message,
                    )

                is FindUiState.Tracking -> TrackingBody(
                    request = s.request,
                    onCandidateClick = viewModel::select,
                    onBack = viewModel::reset,
                )
            }
        }
    }
}

@Composable
private fun QueryBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.find_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            enabled = enabled,
        )
        Spacer(Modifier.size(12.dp))
        Button(
            onClick = onSubmit,
            enabled = enabled && query.isNotBlank(),
        ) {
            Text(stringResource(R.string.find_button_search))
        }
    }
}

@Composable
private fun TrackingBody(
    request: RequestDto,
    onCandidateClick: (CandidateDto) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        StatusHeader(request = request, onBack = onBack)

        // While the backend is still searching there's nothing to show
        // below; once AWAITING_SELECTION (or any later state) we render
        // the candidate tabs so the user can still see what they picked.
        when (request.status) {
            RequestStatus.SEARCHING -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            RequestStatus.AWAITING_SELECTION -> CandidateTabs(
                candidates = request.candidates,
                selectedId = null,
                onCandidateClick = onCandidateClick,
            )

            RequestStatus.UNLOCKING, RequestStatus.DOWNLOADING -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                CandidateTabs(
                    candidates = request.candidates,
                    selectedId = request.selectedCandidateId,
                    onCandidateClick = { /* inert — backend is already working */ },
                )
            }

            // Terminal states just keep the header; picker is frozen.
            RequestStatus.IMPORTED,
            RequestStatus.IMPORTED_PARTIAL,
            RequestStatus.FAILED,
            RequestStatus.CANCELED -> CandidateTabs(
                candidates = request.candidates,
                selectedId = request.selectedCandidateId,
                onCandidateClick = { /* inert */ },
            )
        }
    }
}

@Composable
private fun StatusHeader(request: RequestDto, onBack: () -> Unit) {
    val label = when (request.status) {
        RequestStatus.SEARCHING -> stringResource(R.string.find_status_searching)
        RequestStatus.AWAITING_SELECTION -> stringResource(R.string.find_status_awaiting)
        RequestStatus.UNLOCKING -> stringResource(R.string.find_status_unlocking)
        RequestStatus.DOWNLOADING -> stringResource(R.string.find_status_downloading)
        RequestStatus.IMPORTED -> stringResource(R.string.find_status_imported)
        RequestStatus.IMPORTED_PARTIAL -> stringResource(R.string.find_status_imported_partial)
        RequestStatus.FAILED ->
            stringResource(R.string.find_status_failed) +
                (request.errorMessage?.let { " $it" } ?: "")
        RequestStatus.CANCELED -> stringResource(R.string.find_status_canceled)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.find_back),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = "\"${request.query}\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CandidateTabs(
    candidates: List<CandidateDto>,
    selectedId: Long?,
    onCandidateClick: (CandidateDto) -> Unit,
) {
    // UNKNOWN lives in the Albums bucket — music torrents are mostly
    // releases, and biasing there matches user expectation for the
    // "UNKNOWN bucket-falls into Albums" rule from backend classifier.
    val albums = candidates.filter {
        it.kind == CandidateKind.ALBUM || it.kind == CandidateKind.UNKNOWN
    }
    val singles = candidates.filter { it.kind == CandidateKind.SINGLE }

    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.find_tab_albums) to albums,
        stringResource(R.string.find_tab_singles) to singles,
    )

    TabRow(selectedTabIndex = tab) {
        tabs.forEachIndexed { i, (label, rows) ->
            Tab(
                selected = tab == i,
                onClick = { tab = i },
                text = { Text("$label (${rows.size})") },
            )
        }
    }

    val visible = tabs[tab].second
    if (visible.isEmpty()) {
        CenteredMessage(stringResource(R.string.find_no_candidates))
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = visible, key = { it.id }) { c ->
                CandidateRow(
                    candidate = c,
                    selected = selectedId == c.id,
                    onClick = { onCandidateClick(c) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: CandidateDto,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = candidate.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            candidate.seeders?.let {
                MetaText(stringResource(R.string.find_candidate_seeders, it))
            }
            candidate.sizeBytes?.let {
                MetaText(humanBytes(it))
            }
            candidate.trackCount?.let {
                MetaText(stringResource(R.string.find_candidate_tracks, it))
            }
            candidate.indexer?.let {
                MetaText(it)
            }
        }
        // Keep the selected-bg visible for at least one pixel, so the row
        // reads as "picked" once UNLOCKING is polling.
        if (selected) {
            Spacer(Modifier.height(2.dp))
            HorizontalDivider(color = bg)
        }
    }
}

@Composable
private fun MetaText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CenteredSpinner() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Byte-count formatter: keeps a single decimal and IEC suffixes so rows
 * look tidy even for multi-GiB albums. Works with `size` null-handled
 * at the call site.
 */
private fun humanBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KiB", "MiB", "GiB", "TiB")
    var value = bytes.toDouble() / 1024.0
    var i = 0
    while (value >= 1024.0 && i < units.lastIndex) {
        value /= 1024.0
        i++
    }
    return String.format("%.1f %s", value, units[i])
}
