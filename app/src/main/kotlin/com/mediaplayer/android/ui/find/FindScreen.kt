package com.mediaplayer.android.ui.find

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.mediaplayer.android.R
import com.mediaplayer.android.data.dto.CandidateDto
import com.mediaplayer.android.data.dto.RequestDto
import com.mediaplayer.android.data.dto.RequestStatus
import com.mediaplayer.android.data.dto.RequestSummaryDto
import com.mediaplayer.android.ui.common.EyebrowText
import com.mediaplayer.android.ui.common.MHCover
import com.mediaplayer.android.ui.common.mhCoverFor
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MHGradient

private val Amber = Color(0xFFFFC857)
private val ErrorRed = Color(0xFFE14848)

@Composable
fun FindScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    viewModel: FindViewModel = viewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activeRequests by viewModel.activeRequests.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    LifecycleStartEffect(viewModel) {
        viewModel.resume()
        onStopOrDispose { viewModel.pause() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MHGradient.screenBg()),
    ) {
        when (val s = state) {
            is FindUiState.Tracking ->
                if (s.request.status.isTerminal) {
                    TerminalScreen(
                        request = s.request,
                        onAction = viewModel::reset,
                        onBack = { viewModel.reset(); onBack?.invoke() },
                    )
                } else {
                    TrackingBody(
                        request = s.request,
                        onCandidateClick = viewModel::select,
                        onBack = viewModel::reset,
                    )
                }

            else -> Column(modifier = Modifier.fillMaxSize()) {
                FindTopBar(onBack = onBack)
                QueryBar(
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    onSubmit = viewModel::submit,
                    onClear = { viewModel.onQueryChange("") },
                    enabled = state !is FindUiState.Searching,
                )

                if (state is FindUiState.Error) {
                    InlineErrorBanner(
                        message = (state as FindUiState.Error).message,
                        onRetry = viewModel::submit,
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (state) {
                        FindUiState.Searching -> SearchingSkeleton()

                        FindUiState.Idle, is FindUiState.Error ->
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = viewModel::refreshActiveRequests,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                IdleBody(activeRequests = activeRequests)
                            }

                        else -> Unit
                    }
                }
            }
        }
    }
}

// ── Top bar with eyebrow + title ────────────────────────────────

@Composable
private fun FindTopBar(onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.find_back),
                    tint = MHColors.TextHi,
                )
            }
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            EyebrowText(stringResource(R.string.find_eyebrow))
            Text(
                text = stringResource(R.string.find_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MHColors.TextHi,
            )
        }
    }
}

// ── Card-style search field, IME submit, X clear ────────────────

@Composable
private fun QueryBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    enabled: Boolean,
) {
    val focus = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MHColors.TextLo,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MHColors.TextHi),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MHColors.Lime),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (query.isNotBlank()) {
                    focus.clearFocus()
                    onSubmit()
                }
            }),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.find_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MHColors.TextLo2,
                        )
                    }
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.find_clear),
                    tint = MHColors.TextLo,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ── Inline error banner (non-blocking) ───────────────────────────

@Composable
private fun InlineErrorBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ErrorRed.copy(alpha = 0.1f))
            .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.find_error_banner_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFFFB3B3),
            )
            Text(
                text = message.ifBlank { stringResource(R.string.find_error_banner_body) },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFB3B3).copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(onClick = onRetry)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = stringResource(R.string.find_error_retry),
                style = MaterialTheme.typography.labelMedium,
                color = MHColors.TextHi,
            )
        }
    }
}

// ── Idle: empty hero or active-requests list ─────────────────────

@Composable
private fun IdleBody(activeRequests: List<RequestSummaryDto>) {
    if (activeRequests.isEmpty()) {
        EmptyHero()
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                EyebrowText(
                    text = stringResource(
                        R.string.find_section_active,
                        activeRequests.size,
                    ),
                    color = MHColors.TextLo,
                )
                Text(
                    text = stringResource(R.string.find_poll_caption),
                    style = LocalMHMono.current.eyebrow,
                    color = MHColors.TextLo2,
                )
            }
        }
        items(items = activeRequests, key = { it.id }) { req ->
            ActiveRequestRow(req)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun EmptyHero() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MHColors.TextLo2,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.find_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MHColors.TextHi,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.find_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MHColors.TextLo,
            modifier = Modifier.padding(horizontal = 8.dp),
            maxLines = 3,
        )
    }
}

@Composable
private fun ActiveRequestRow(request: RequestSummaryDto) {
    val color = when (request.status) {
        RequestStatus.UNLOCKING -> Amber
        else -> MHColors.Lime
    }
    val statusCaps = when (request.status) {
        RequestStatus.SEARCHING -> stringResource(R.string.find_status_searching_caps)
        RequestStatus.AWAITING_SELECTION -> stringResource(R.string.find_status_awaiting)
        RequestStatus.UNLOCKING -> stringResource(R.string.find_status_unlocking_caps)
        RequestStatus.DOWNLOADING -> stringResource(R.string.find_status_downloading_caps, 50)
        else -> ""
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\"${request.query}\"",
                    style = MaterialTheme.typography.titleSmall,
                    color = MHColors.TextHi,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusCaps,
                    style = LocalMHMono.current.eyebrow.copy(color = color),
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = color,
            trackColor = Color.White.copy(alpha = 0.06f),
        )
    }
}

// ── Searching: skeleton rows + footer note ──────────────────────

@Composable
private fun SearchingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        EyebrowText(
            text = stringResource(R.string.find_status_searching_caps),
            color = MHColors.Lime,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        repeat(5) { i ->
            SkeletonRow(widthFrac = 0.75f - i * 0.08f)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MHColors.Lime.copy(alpha = 0.06f))
                .border(1.dp, MHColors.Lime.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                color = MHColors.Lime,
                strokeWidth = 2.dp,
                trackColor = MHColors.Lime.copy(alpha = 0.25f),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.find_status_searching),
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.Lime,
            )
        }
    }
}

@Composable
private fun SkeletonRow(widthFrac: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .alpha(0.5f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.06f)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFrac.coerceAtLeast(0.2f))
                    .height(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth((widthFrac * 0.65f).coerceAtLeast(0.15f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
            )
        }
    }
}

// ── Tracking: status header + candidates ────────────────────────

@Composable
private fun TrackingBody(
    request: RequestDto,
    onCandidateClick: (CandidateDto) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        StatusHeader(request = request, onBack = onBack)
        when (request.status) {
            RequestStatus.SEARCHING -> SearchingSkeleton()

            RequestStatus.AWAITING_SELECTION,
            RequestStatus.UNLOCKING,
            RequestStatus.DOWNLOADING -> CandidateList(
                candidates = request.candidates,
                selectedId = request.selectedCandidateId,
                phase = request.status,
                onCandidateClick = if (request.status == RequestStatus.AWAITING_SELECTION) onCandidateClick else { _ -> },
            )

            else -> Unit
        }
    }
}

@Composable
private fun StatusHeader(request: RequestDto, onBack: () -> Unit) {
    val color = when (request.status) {
        RequestStatus.UNLOCKING -> Amber
        else -> MHColors.Lime
    }
    val statusCaps = when (request.status) {
        RequestStatus.SEARCHING -> stringResource(R.string.find_status_searching_caps)
        RequestStatus.AWAITING_SELECTION -> stringResource(R.string.find_status_awaiting)
        RequestStatus.UNLOCKING -> stringResource(R.string.find_status_unlocking_caps)
        RequestStatus.DOWNLOADING -> stringResource(R.string.find_status_downloading_caps, 50)
        else -> ""
    }
    val showStrip = request.status == RequestStatus.SEARCHING ||
        request.status == RequestStatus.UNLOCKING ||
        request.status == RequestStatus.DOWNLOADING
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.find_back),
                    tint = MHColors.TextHi,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\"${request.query}\"",
                    style = MaterialTheme.typography.titleSmall,
                    color = MHColors.TextHi,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusCaps,
                    style = LocalMHMono.current.eyebrow.copy(color = color),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (showStrip) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = color,
                trackColor = Color.White.copy(alpha = 0.06f),
            )
        }
    }
}

@Composable
private fun CandidateList(
    candidates: List<CandidateDto>,
    selectedId: Long?,
    phase: RequestStatus,
    onCandidateClick: (CandidateDto) -> Unit,
) {
    if (candidates.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.find_no_candidates),
                style = MaterialTheme.typography.bodyMedium,
                color = MHColors.TextLo,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        item {
            EyebrowText(
                text = stringResource(R.string.find_results_header, candidates.size),
                color = MHColors.TextLo,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
        }
        items(items = candidates, key = { it.id }) { c ->
            CandidateRow(
                candidate = c,
                selected = selectedId == c.id,
                phase = phase,
                onClick = { onCandidateClick(c) },
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun CandidateRow(
    candidate: CandidateDto,
    selected: Boolean,
    phase: RequestStatus,
    onClick: () -> Unit,
) {
    val color = if (phase == RequestStatus.UNLOCKING) Amber else MHColors.Lime
    val bg = if (selected) MHColors.Lime.copy(alpha = 0.1f) else Color.Transparent
    val borderColor = if (selected) MHColors.Lime.copy(alpha = 0.3f) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = phase == RequestStatus.AWAITING_SELECTION, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            Thumbnail(candidate = candidate)
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = color,
                        strokeWidth = 2.dp,
                        trackColor = Color.White.copy(alpha = 0.25f),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.title,
                style = MaterialTheme.typography.titleSmall,
                color = MHColors.TextHi,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                candidate.channelName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MHColors.TextLo,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("·", color = MHColors.TextLo, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(6.dp))
                }
                candidate.durationSeconds?.let {
                    Text(
                        text = formatDuration(it),
                        style = LocalMHMono.current.duration,
                        color = MHColors.TextLo,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                YtPill()
            }
        }
        Spacer(Modifier.width(8.dp))
        TrailingPill(phase = phase, selected = selected, onClick = onClick)
    }
}

@Composable
private fun Thumbnail(candidate: CandidateDto) {
    val (kind, palette) = remember(candidate.id) { mhCoverFor(candidate.id) }
    val shape = RoundedCornerShape(4.dp)
    if (candidate.thumbnailUrl != null) {
        AsyncImage(
            model = candidate.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(shape),
        )
    } else {
        MHCover(
            kind = kind,
            palette = palette,
            modifier = Modifier.size(48.dp),
            cornerRadius = 4.dp,
        )
    }
}

@Composable
private fun YtPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFF4D2E).copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text = stringResource(R.string.find_source_yt),
            color = Color(0xFFFF7A5C),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun TrailingPill(phase: RequestStatus, selected: Boolean, onClick: () -> Unit) {
    when {
        selected && phase == RequestStatus.UNLOCKING -> StatusPill(
            label = stringResource(R.string.find_pill_unlocking),
            color = Amber,
        )
        selected && phase == RequestStatus.DOWNLOADING -> StatusPill(
            label = "···",
            color = MHColors.Lime,
        )
        phase == RequestStatus.AWAITING_SELECTION && !selected -> AddPill(onClick = onClick)
        else -> AddPill(onClick = {}, dim = true)
    }
}

@Composable
private fun AddPill(onClick: () -> Unit, dim: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MHColors.Lime.copy(alpha = if (dim) 0.4f else 1f))
            .clickable(enabled = !dim, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = stringResource(R.string.find_pill_add),
            color = Color(0xFF0A0A0A),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            style = LocalMHMono.current.badge.copy(color = color, fontSize = 10.5.sp),
        )
    }
}

// ── Terminal screen (IMPORTED / PARTIAL / FAILED / CANCELED) ────

@Composable
private fun TerminalScreen(
    request: RequestDto,
    onAction: () -> Unit,
    onBack: () -> Unit,
) {
    val res = terminalResources(request.status)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MHGradient.heroBg(res.tint))
            .padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 0.dp, end = 0.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.find_back),
                    tint = MHColors.TextHi,
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                EyebrowText(text = res.eyebrow, color = res.color)
                Text(
                    text = stringResource(R.string.find_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MHColors.TextHi,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(88.dp)
                .clip(CircleShape)
                .background(res.color.copy(alpha = 0.15f))
                .border(1.dp, res.color.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = res.icon,
                contentDescription = null,
                tint = res.color,
                modifier = Modifier.size(42.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = res.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MHColors.TextHi,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "\"${request.query}\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MHColors.TextLo,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (res.sub != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = res.sub,
                style = MaterialTheme.typography.bodyMedium,
                color = MHColors.TextLo,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        Spacer(Modifier.height(28.dp))

        // Meta card
        val selected = request.candidates.firstOrNull { it.id == request.selectedCandidateId }
        if (selected != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MHColors.Card)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetaRow(
                    key = stringResource(R.string.find_terminal_meta_source),
                    value = "YouTube",
                )
                selected.durationSeconds?.let {
                    MetaRow(
                        key = stringResource(R.string.find_terminal_meta_duration),
                        value = formatDuration(it),
                    )
                }
                MetaRow(
                    key = stringResource(R.string.find_terminal_meta_status),
                    value = request.status.name,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MHColors.Lime,
                contentColor = Color(0xFF0A0A0A),
            ),
        ) {
            Text(
                text = stringResource(res.primaryLabel),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 32.dp),
            shape = CircleShape,
        ) {
            Text(
                text = stringResource(res.secondaryLabel),
                style = MaterialTheme.typography.labelLarge,
                color = MHColors.TextHi,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun MetaRow(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = key.uppercase(),
            style = LocalMHMono.current.eyebrow.copy(color = MHColors.TextLo),
        )
        Text(
            text = value,
            style = LocalMHMono.current.duration.copy(
                color = MHColors.TextHi,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

private data class TerminalRes(
    val color: Color,
    val tint: Color,
    val eyebrow: String,
    val title: String,
    val sub: String?,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val primaryLabel: Int,
    val secondaryLabel: Int,
)

@Composable
private fun terminalResources(status: RequestStatus): TerminalRes = when (status) {
    RequestStatus.IMPORTED -> TerminalRes(
        color = MHColors.Lime,
        tint = Color(0xFF2A4615),
        eyebrow = stringResource(R.string.find_terminal_imported_eyebrow),
        title = stringResource(R.string.find_terminal_imported_title),
        sub = null,
        icon = Icons.Filled.Check,
        primaryLabel = R.string.find_terminal_open,
        secondaryLabel = R.string.find_terminal_find_another,
    )
    RequestStatus.IMPORTED_PARTIAL -> TerminalRes(
        color = Amber,
        tint = Color(0xFF4A3A14),
        eyebrow = stringResource(R.string.find_terminal_partial_eyebrow),
        title = stringResource(R.string.find_terminal_partial_title),
        sub = stringResource(R.string.find_terminal_partial_sub),
        icon = Icons.Filled.PriorityHigh,
        primaryLabel = R.string.find_terminal_open,
        secondaryLabel = R.string.find_terminal_find_another,
    )
    RequestStatus.FAILED -> TerminalRes(
        color = ErrorRed,
        tint = Color(0xFF3A1818),
        eyebrow = stringResource(R.string.find_terminal_failed_eyebrow),
        title = stringResource(R.string.find_terminal_failed_title),
        sub = stringResource(R.string.find_terminal_failed_sub),
        icon = Icons.Filled.Close,
        primaryLabel = R.string.find_terminal_retry,
        secondaryLabel = R.string.find_terminal_back,
    )
    RequestStatus.CANCELED -> TerminalRes(
        color = MHColors.TextLo,
        tint = Color(0xFF2A2A2A),
        eyebrow = stringResource(R.string.find_terminal_canceled_eyebrow),
        title = stringResource(R.string.find_terminal_canceled_title),
        sub = stringResource(R.string.find_terminal_canceled_sub),
        icon = Icons.Filled.Close,
        primaryLabel = R.string.find_terminal_retry,
        secondaryLabel = R.string.find_terminal_back,
    )
    else -> error("Not terminal: $status")
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
