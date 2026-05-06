package com.mediaplayer.android.ui.playlists

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.R
import com.mediaplayer.android.ui.common.EyebrowText
import com.mediaplayer.android.ui.common.MHCover
import com.mediaplayer.android.ui.common.mhCoverFor
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MHGradient

private val Amber = Color(0xFFFFC857)
private val ErrorRed = Color(0xFFE14848)

@Composable
fun SpotifyImportScreen(
    modifier: Modifier = Modifier,
    viewModel: SpotifyImportViewModel = viewModel(),
    onBack: () -> Unit,
    onPlaylistCreated: (playlistId: Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MHGradient.screenBg()),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ImportTopBar(onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                Stepper(stepIndex = stepIndexFor(state))
                when (val s = state) {
                    SpotifyImportUiState.Idle -> IdleContent(
                        onFilePicked = viewModel::importFromUri,
                    )
                    SpotifyImportUiState.FetchingPlaylist -> FetchingContent()
                    is SpotifyImportUiState.Confirming -> ConfirmingContent(
                        state = s,
                        onStartImport = viewModel::startImport,
                        onCancel = viewModel::reset,
                    )
                    is SpotifyImportUiState.Importing -> ImportingContent(state = s)
                    is SpotifyImportUiState.Done -> DoneContent(
                        state = s,
                        onViewPlaylist = { onPlaylistCreated(s.playlistId) },
                        onBack = onBack,
                    )
                    is SpotifyImportUiState.Error -> ErrorContent(
                        message = s.message,
                        onRetry = viewModel::reset,
                        onPickAnother = viewModel::reset,
                    )
                }
            }
        }
    }
}

private fun stepIndexFor(state: SpotifyImportUiState): Int = when (state) {
    SpotifyImportUiState.Idle -> 1
    SpotifyImportUiState.FetchingPlaylist -> 2
    is SpotifyImportUiState.Confirming -> 2
    is SpotifyImportUiState.Importing -> 3
    is SpotifyImportUiState.Done -> 4
    is SpotifyImportUiState.Error -> 2
}

// ── Top bar ─────────────────────────────────────────────────────

@Composable
private fun ImportTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.spotify_import_back),
                tint = MHColors.TextHi,
            )
        }
        Spacer(Modifier.size(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            EyebrowText(stringResource(R.string.spotify_import_eyebrow))
            Text(
                text = stringResource(R.string.spotify_import_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MHColors.TextHi,
            )
        }
    }
}

// ── 5-segment stepper ──────────────────────────────────────────

@Composable
private fun Stepper(stepIndex: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(5) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        if (i <= stepIndex) MHColors.Lime
                        else Color.White.copy(alpha = 0.1f)
                    ),
            )
        }
    }
}

// ── Idle: instructions + drop-zone + pick CTA ──────────────────

@Composable
private fun IdleContent(onFilePicked: (Uri) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(onFilePicked) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp),
    ) {
        EyebrowText(stringResource(R.string.spotify_import_step_export))
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.spotify_import_idle_title),
            style = MaterialTheme.typography.titleLarge,
            color = MHColors.TextHi,
        )
        Spacer(Modifier.height(14.dp))
        InstructionRow(1, stringResource(R.string.spotify_import_idle_step1))
        InstructionRow(2, stringResource(R.string.spotify_import_idle_step2))
        InstructionRow(3, stringResource(R.string.spotify_import_idle_step3))

        Spacer(Modifier.height(18.dp))
        OutlinedButton(
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://watsonbox.github.io/exportify"),
                )
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                text = stringResource(R.string.spotify_import_idle_open_exportify),
                color = MHColors.TextHi,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        Spacer(Modifier.height(22.dp))
        DropZone()
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { launcher.launch("*/*") },
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MHColors.Lime,
                contentColor = Color(0xFF0A0A0A),
            ),
        ) {
            Icon(
                Icons.Filled.FileUpload,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = stringResource(R.string.spotify_import_idle_pick_file),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun InstructionRow(step: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MHColors.Lime.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = step.toString(),
                style = LocalMHMono.current.badge.copy(color = MHColors.Lime),
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MHColors.TextLo,
            modifier = Modifier
                .weight(1f)
                .padding(top = 1.dp),
        )
    }
}

@Composable
private fun DropZone() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MHColors.Lime.copy(alpha = 0.04f))
            .border(1.5.dp, MHColors.Lime.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MHColors.Lime.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Description,
                contentDescription = null,
                tint = MHColors.Lime,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = stringResource(R.string.spotify_import_idle_dropzone_title),
            style = MaterialTheme.typography.titleSmall,
            color = MHColors.TextHi,
        )
        Text(
            text = stringResource(R.string.spotify_import_idle_dropzone_hint),
            style = LocalMHMono.current.duration.copy(color = MHColors.TextLo2),
        )
    }
}

// ── Fetching: parse-CSV intermediate ───────────────────────────

@Composable
private fun FetchingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp),
    ) {
        EyebrowText(stringResource(R.string.spotify_import_step_parse))
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.spotify_import_fetching_title),
            style = MaterialTheme.typography.titleLarge,
            color = MHColors.TextHi,
        )
        Spacer(Modifier.height(22.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MHColors.Card)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MHColors.Lime.copy(alpha = 0.10f))
                    .border(1.dp, MHColors.Lime.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Description,
                    contentDescription = null,
                    tint = MHColors.Lime,
                    modifier = Modifier.size(28.dp),
                )
            }
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MHColors.Lime,
                trackColor = Color.White.copy(alpha = 0.06f),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.spotify_import_fetching_caption),
            style = LocalMHMono.current.duration.copy(color = MHColors.TextLo),
            modifier = Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

// ── Confirming: rename + summary + count CTA ────────────────────

@Composable
private fun ConfirmingContent(
    state: SpotifyImportUiState.Confirming,
    onStartImport: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var playlistName by remember(state.playlistName) { mutableStateOf(state.playlistName) }
    val original = remember(state.playlistName) { state.playlistName }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp),
    ) {
        EyebrowText(stringResource(R.string.spotify_import_step_confirm))
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.spotify_import_confirming_title),
            style = MaterialTheme.typography.titleLarge,
            color = MHColors.TextHi,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.spotify_import_confirming_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MHColors.TextLo,
        )
        Spacer(Modifier.height(22.dp))

        OutlinedTextField(
            value = playlistName,
            onValueChange = { playlistName = it.take(60) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.spotify_import_confirming_label)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MHColors.Lime,
                focusedLabelColor = MHColors.Lime,
                cursorColor = MHColors.Lime,
                focusedTextColor = MHColors.TextHi,
                unfocusedTextColor = MHColors.TextHi,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(
                    R.string.spotify_import_confirming_original,
                    original,
                ),
                style = LocalMHMono.current.duration.copy(color = MHColors.TextLo2),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "${playlistName.length} / 60",
                style = LocalMHMono.current.duration.copy(color = MHColors.TextLo2),
            )
        }

        Spacer(Modifier.height(14.dp))
        SummaryCard(
            playlistName = playlistName.ifBlank { original },
            trackCount = state.tracks.size,
            sourceFile = "${original}.csv",
        )

        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = CircleShape,
            ) {
                Text(
                    text = stringResource(R.string.spotify_import_confirming_cancel),
                    color = MHColors.TextHi,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            Button(
                onClick = { onStartImport(playlistName) },
                modifier = Modifier.weight(2f),
                enabled = playlistName.trim().isNotEmpty(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MHColors.Lime,
                    contentColor = Color(0xFF0A0A0A),
                ),
            ) {
                Text(
                    text = stringResource(
                        R.string.spotify_import_confirming_start,
                        state.tracks.size,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(playlistName: String, trackCount: Int, sourceFile: String) {
    val (kind, palette) = remember(playlistName) {
        mhCoverFor(playlistName.hashCode().toLong())
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MHColors.Card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MHCover(
            kind = kind,
            palette = palette,
            modifier = Modifier.size(44.dp),
            cornerRadius = 6.dp,
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlistName,
                style = MaterialTheme.typography.titleSmall,
                color = MHColors.TextHi,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.spotify_import_confirming_summary,
                    trackCount,
                    sourceFile,
                ),
                style = LocalMHMono.current.duration.copy(color = MHColors.TextLo),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// ── Importing: matching backend ────────────────────────────────

@Composable
private fun ImportingContent(state: SpotifyImportUiState.Importing) {
    val pct = if (state.total > 0) {
        (state.current.toFloat() / state.total).coerceIn(0f, 1f)
    } else 0f
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp),
    ) {
        EyebrowText(stringResource(R.string.spotify_import_step_match))
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.spotify_import_importing_title),
            style = MaterialTheme.typography.titleLarge,
            color = MHColors.TextHi,
        )
        Spacer(Modifier.height(22.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MHColors.Card)
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = state.currentTrack ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MHColors.TextLo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(
                        R.string.spotify_import_importing_counter,
                        state.current,
                        state.total,
                    ),
                    style = LocalMHMono.current.duration.copy(color = MHColors.Lime),
                )
            }
            Spacer(Modifier.height(10.dp))
            if (state.total > 0 && state.current > 0) {
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MHColors.Lime,
                    trackColor = Color.White.copy(alpha = 0.08f),
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MHColors.Lime,
                    trackColor = Color.White.copy(alpha = 0.08f),
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile(
                    label = stringResource(R.string.spotify_import_importing_stat_found),
                    value = state.matched.toString(),
                    color = MHColors.Lime,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.spotify_import_importing_stat_approx),
                    value = state.approx.toString(),
                    color = Amber,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.spotify_import_importing_stat_skipped),
                    value = state.failed.toString(),
                    color = MHColors.TextLo,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ── Done: check circle + stat grid + dual CTA ──────────────────

@Composable
private fun DoneContent(
    state: SpotifyImportUiState.Done,
    onViewPlaylist: () -> Unit,
    onBack: () -> Unit,
) {
    val totalAdded = state.matched + state.approx
    val singular = totalAdded == 1
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp),
    ) {
        EyebrowText(stringResource(R.string.spotify_import_step_done))
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MHColors.Lime.copy(alpha = 0.15f))
                    .border(1.dp, MHColors.Lime.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MHColors.Lime,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.spotify_import_done_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MHColors.TextHi,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(
                if (singular) R.string.spotify_import_done_added_one
                else R.string.spotify_import_done_added_many,
                totalAdded,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MHColors.TextLo,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        if (state.queued > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.spotify_import_done_downloading,
                    state.queued,
                ),
                style = LocalMHMono.current.duration.copy(color = MHColors.Lime),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MHColors.Card)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile(
                label = stringResource(R.string.spotify_import_done_stat_imported),
                value = state.matched.toString(),
                color = MHColors.Lime,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.spotify_import_done_stat_approx),
                value = state.approx.toString(),
                color = Amber,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.spotify_import_done_stat_queued),
                value = state.queued.toString(),
                color = MHColors.TextLo,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(22.dp))
        SummaryCard(
            playlistName = state.playlistName,
            trackCount = totalAdded,
            sourceFile = stringResource(R.string.spotify_import_done_created_now),
        )

        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onViewPlaylist,
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MHColors.Lime,
                contentColor = Color(0xFF0A0A0A),
            ),
        ) {
            Text(
                text = stringResource(R.string.spotify_import_done_open, state.playlistName),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.spotify_import_done_back),
                style = MaterialTheme.typography.bodyMedium,
                color = MHColors.TextLo,
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = LocalMHMono.current.statValue.copy(color = color),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            style = LocalMHMono.current.eyebrow.copy(color = MHColors.TextLo),
        )
    }
}

// ── Error: red eyebrow, panel, suggestions, retry ──────────────

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onPickAnother: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp),
    ) {
        EyebrowText(
            text = stringResource(R.string.spotify_import_step_error),
            color = ErrorRed,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.spotify_import_error_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MHColors.TextHi,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.spotify_import_error_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MHColors.TextLo,
        )
        Spacer(Modifier.height(22.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ErrorRed.copy(alpha = 0.10f))
                .border(1.dp, ErrorRed.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ErrorRed.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PriorityHigh,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    text = message.ifBlank { stringResource(R.string.spotify_import_error_body) },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFB3B3),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MHColors.Card)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            EyebrowText(
                text = stringResource(R.string.spotify_import_error_suggestions_eyebrow),
                color = MHColors.TextLo,
            )
            Spacer(Modifier.height(6.dp))
            SuggestionRow(stringResource(R.string.spotify_import_error_suggestion_1))
            SuggestionRow(stringResource(R.string.spotify_import_error_suggestion_2))
            SuggestionRow(stringResource(R.string.spotify_import_error_suggestion_3))
        }

        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MHColors.Lime,
                contentColor = Color(0xFF0A0A0A),
            ),
        ) {
            Text(
                text = stringResource(R.string.spotify_import_error_retry),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        TextButton(
            onClick = onPickAnother,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.spotify_import_error_pick_other),
                style = MaterialTheme.typography.bodyMedium,
                color = MHColors.TextLo,
            )
        }
    }
}

@Composable
private fun SuggestionRow(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MHColors.TextLo,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MHColors.TextLo,
        )
    }
}
