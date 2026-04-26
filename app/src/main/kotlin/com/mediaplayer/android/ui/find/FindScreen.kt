package com.mediaplayer.android.ui.find

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.mediaplayer.android.R
import com.mediaplayer.android.data.dto.CandidateDto
import com.mediaplayer.android.data.dto.RequestDto
import com.mediaplayer.android.data.dto.RequestStatus
import com.mediaplayer.android.data.dto.RequestSummaryDto

@Composable
fun FindScreen(
    modifier: Modifier = Modifier,
    viewModel: FindViewModel = viewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activeRequests by viewModel.activeRequests.collectAsStateWithLifecycle()

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
                    IdleBody(
                        activeRequests = activeRequests,
                        emptyMessage = stringResource(R.string.find_empty),
                    )

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
        Spacer(Modifier.width(12.dp))
        Button(
            onClick = onSubmit,
            enabled = enabled && query.isNotBlank(),
        ) {
            Text(stringResource(R.string.find_button_search))
        }
    }
}

@Composable
private fun IdleBody(
    activeRequests: List<RequestSummaryDto>,
    emptyMessage: String,
) {
    if (activeRequests.isEmpty()) {
        CenteredMessage(emptyMessage)
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = stringResource(R.string.find_active_downloads),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        items(items = activeRequests, key = { it.id }) { req ->
            ActiveRequestRow(req)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ActiveRequestRow(request: RequestSummaryDto) {
    val label = when (request.status) {
        RequestStatus.SEARCHING -> stringResource(R.string.find_status_searching)
        RequestStatus.AWAITING_SELECTION -> stringResource(R.string.find_status_awaiting)
        RequestStatus.UNLOCKING,
        RequestStatus.DOWNLOADING -> stringResource(R.string.find_status_downloading)
        else -> ""
    }
    val showProgress = request.status == RequestStatus.SEARCHING ||
        request.status == RequestStatus.UNLOCKING ||
        request.status == RequestStatus.DOWNLOADING

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "\"${request.query}\"",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showProgress) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
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

        when (request.status) {
            RequestStatus.SEARCHING -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            RequestStatus.AWAITING_SELECTION -> CandidateList(
                candidates = request.candidates,
                selectedId = null,
                onCandidateClick = onCandidateClick,
            )

            RequestStatus.UNLOCKING, RequestStatus.DOWNLOADING -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                CandidateList(
                    candidates = request.candidates,
                    selectedId = request.selectedCandidateId,
                    onCandidateClick = {},
                )
            }

            RequestStatus.IMPORTED,
            RequestStatus.IMPORTED_PARTIAL,
            RequestStatus.FAILED,
            RequestStatus.CANCELED -> CandidateList(
                candidates = request.candidates,
                selectedId = request.selectedCandidateId,
                onCandidateClick = {},
            )
        }
    }
}

@Composable
private fun StatusHeader(request: RequestDto, onBack: () -> Unit) {
    val label = when (request.status) {
        RequestStatus.SEARCHING -> stringResource(R.string.find_status_searching)
        RequestStatus.AWAITING_SELECTION -> stringResource(R.string.find_status_awaiting)
        RequestStatus.UNLOCKING,
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
private fun CandidateList(
    candidates: List<CandidateDto>,
    selectedId: Long?,
    onCandidateClick: (CandidateDto) -> Unit,
) {
    if (candidates.isEmpty()) {
        CenteredMessage(stringResource(R.string.find_no_candidates))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = candidates, key = { it.id }) { c ->
            CandidateRow(
                candidate = c,
                selected = selectedId == c.id,
                onClick = { onCandidateClick(c) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: CandidateDto,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (selected)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(url = candidate.thumbnailUrl)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            candidate.channelName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                candidate.durationSeconds?.let {
                    MetaText(formatDuration(it))
                }
                candidate.viewCount?.let {
                    MetaText(formatViewCount(it))
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(url: String?) {
    val shape = RoundedCornerShape(4.dp)
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 100.dp, height = 72.dp)
                .clip(shape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 72.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
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

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatViewCount(count: Long): String = when {
    count >= 1_000_000_000 -> "%.1fB views".format(count / 1_000_000_000.0)
    count >= 1_000_000 -> "%.1fM views".format(count / 1_000_000.0)
    count >= 1_000 -> "%.1fK views".format(count / 1_000.0)
    else -> "$count views"
}
