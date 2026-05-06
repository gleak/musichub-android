package com.mediaplayer.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediaplayer.android.BuildConfig
import com.mediaplayer.android.data.dto.AppUpdateDto
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MonoFamily
import com.mediaplayer.android.update.AppUpdateChecker
import com.mediaplayer.android.update.AppUpdateInstaller

/**
 * Self-managing host: renders nothing when no update pending, otherwise
 * picks one of three banner states (`available`, `progress`, `failed`)
 * based on `AppUpdateChecker.state` + `AppUpdateInstaller.progress`.
 *
 * Mockup contract: `mockup/mh-update.jsx:5-89`. Lime gradient for the
 * available state, neutral card with spinner + percent + progress bar
 * for the in-flight state, rose-tinted card with `Riprova` for failure.
 */
@Composable
fun AppUpdateBannerHost(modifier: Modifier = Modifier) {
    val manifest by AppUpdateChecker.state.collectAsStateWithLifecycle()
    val download by AppUpdateInstaller.progress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val m = manifest ?: return
    // Required updates render as a full-screen overlay (see
    // [AppUpdateRequiredOverlay]) — skip the inline banner so we don't
    // double up.
    if (m.required) return
    val fromVersion = remember { BuildConfig.VERSION_NAME }

    val startDownload: () -> Unit = {
        AppUpdateInstaller.startDownload(
            context = context,
            url = m.url,
            expectedSha256 = m.sha256.takeIf { it.isNotBlank() },
            onError = { /* surfaced via DownloadProgress.Failed */ },
            onReady = { apk -> AppUpdateInstaller.launchInstall(context, apk) },
        )
    }

    when (val p = download) {
        AppUpdateInstaller.DownloadProgress.Idle -> AvailableBanner(
            fromVersion = fromVersion,
            toVersion = m.version,
            modifier = modifier,
            required = m.required,
            onInstall = startDownload,
            onDismiss = {
                AppUpdateInstaller.resetProgress()
                AppUpdateChecker.dismiss(context, m)
            },
        )
        is AppUpdateInstaller.DownloadProgress.Active -> ProgressBanner(
            toVersion = m.version,
            percent = p.percent,
            bytesDownloaded = p.bytesDownloaded,
            totalBytes = p.totalBytes,
            modifier = modifier,
        )
        is AppUpdateInstaller.DownloadProgress.Failed -> FailedBanner(
            modifier = modifier,
            onRetry = startDownload,
        )
    }
}

/**
 * Full-screen blocking overlay for `manifest.required = true` updates.
 * Renders a scrim that swallows all input below + a center card with the
 * same Available / Progress / Failed states as the inline banner. The
 * user can't dismiss — only paths out are: install the new APK, or kill
 * the app.
 */
@Composable
fun AppUpdateRequiredOverlay() {
    val manifest by AppUpdateChecker.state.collectAsStateWithLifecycle()
    val download by AppUpdateInstaller.progress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val m = manifest ?: return
    if (!m.required) return
    val fromVersion = remember { BuildConfig.VERSION_NAME }

    val startDownload: () -> Unit = {
        AppUpdateInstaller.startDownload(
            context = context,
            url = m.url,
            expectedSha256 = m.sha256.takeIf { it.isNotBlank() },
            onError = { /* state observable via AppUpdateInstaller.progress */ },
            onReady = { apk -> AppUpdateInstaller.launchInstall(context, apk) },
        )
    }

    val mono = LocalMHMono.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            // Swallow taps so the screen below cannot react.
            .clickable(
                interactionSource = remember {
                    androidx.compose.foundation.interaction.MutableInteractionSource()
                },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "// AGGIORNAMENTO RICHIESTO",
                style = mono.eyebrow.copy(color = MHColors.Lime),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Text(
                text = "Devi aggiornare per continuare.",
                color = MHColors.TextHi,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Spacer(Modifier.size(8.dp))
            when (val p = download) {
                AppUpdateInstaller.DownloadProgress.Idle -> AvailableBanner(
                    fromVersion = fromVersion,
                    toVersion = m.version,
                    modifier = Modifier,
                    required = true, // hides the X dismiss
                    onInstall = startDownload,
                    onDismiss = {},
                )
                is AppUpdateInstaller.DownloadProgress.Active -> ProgressBanner(
                    toVersion = m.version,
                    percent = p.percent,
                    bytesDownloaded = p.bytesDownloaded,
                    totalBytes = p.totalBytes,
                    modifier = Modifier,
                )
                is AppUpdateInstaller.DownloadProgress.Failed -> FailedBanner(
                    modifier = Modifier,
                    onRetry = startDownload,
                )
            }
        }
    }
}

@Composable
private fun AvailableBanner(
    fromVersion: String,
    toVersion: String,
    modifier: Modifier,
    required: Boolean,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val mono = LocalMHMono.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    0f to MHColors.Lime.copy(alpha = 0.16f),
                    1f to MHColors.Lime.copy(alpha = 0.04f),
                ),
            )
            .border(1.dp, MHColors.Lime.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquircleIcon(bg = MHColors.Lime) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = Color(0xFF0A0A0A),
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "// AGGIORNAMENTO",
                style = mono.eyebrow.copy(color = MHColors.Lime),
            )
            Text(
                text = buildAnnotatedString {
                    append("v$fromVersion → ")
                    withStyle(
                        SpanStyle(
                            color = MHColors.Lime,
                            fontFamily = MonoFamily,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) { append("v$toVersion") }
                },
                color = MHColors.TextHi,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.size(8.dp))
        InstallaPill(onClick = onInstall)
        if (!required) {
            Spacer(Modifier.size(2.dp))
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Ignora",
                    tint = MHColors.TextLo,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ProgressBanner(
    toVersion: String,
    percent: Int,
    bytesDownloaded: Long,
    totalBytes: Long,
    modifier: Modifier,
) {
    val mono = LocalMHMono.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MHColors.Card)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SquircleIcon(bg = MHColors.Lime.copy(alpha = 0.14f)) {
                CircularProgressIndicator(
                    color = MHColors.Lime,
                    trackColor = MHColors.Lime.copy(alpha = 0.25f),
                    strokeWidth = 2.4.dp,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "// SCARICAMENTO APK",
                    style = mono.eyebrow.copy(color = MHColors.Lime),
                )
                Text(
                    text = buildAnnotatedString {
                        append("v$toVersion · ")
                        withStyle(
                            SpanStyle(
                                color = MHColors.TextLo,
                                fontFamily = MonoFamily,
                            ),
                        ) { append(formatBytesPair(bytesDownloaded, totalBytes)) }
                    },
                    color = MHColors.TextHi,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = "$percent%",
                color = MHColors.Lime,
                fontFamily = MonoFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.size(8.dp))
        ProgressBar(percent = percent)
    }
}

@Composable
private fun FailedBanner(
    modifier: Modifier,
    onRetry: () -> Unit,
) {
    val mono = LocalMHMono.current
    val rose = Color(0xFFE14848)
    val roseLight = Color(0xFFFF7A7A)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(rose.copy(alpha = 0.08f))
            .border(1.dp, rose.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquircleIcon(bg = rose.copy(alpha = 0.18f)) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = roseLight,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "// AGGIORNAMENTO FALLITO",
                style = mono.eyebrow.copy(color = roseLight),
            )
            Text(
                text = "Scaricamento interrotto",
                color = MHColors.TextHi,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.size(8.dp))
        RetryPill(onClick = onRetry)
    }
}

@Composable
private fun SquircleIcon(
    bg: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
private fun InstallaPill(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MHColors.Lime)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Installa",
            color = Color(0xFF0A0A0A),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun RetryPill(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Riprova",
            color = MHColors.TextHi,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ProgressBar(percent: Int) {
    val frac = (percent.coerceIn(0, 100)) / 100f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = frac)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MHColors.Lime),
        )
    }
}

private fun formatBytesPair(soFar: Long, total: Long): String {
    if (total <= 0L) return formatMb(soFar)
    return "${formatMb(soFar)} / ${formatMb(total)}"
}

private fun formatMb(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 10.0) "%.0f MB".format(mb) else "%.1f MB".format(mb)
}

/**
 * Backwards-compatible thin wrapper kept for any external callers that
 * still embed the legacy banner directly. New callers should use
 * [AppUpdateBannerHost], which handles state internally.
 */
@Suppress("unused")
@Composable
fun AppUpdateBanner(
    fromVersion: String,
    toVersion: String,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AvailableBanner(
        fromVersion = fromVersion,
        toVersion = toVersion,
        modifier = modifier,
        required = false,
        onInstall = onInstall,
        onDismiss = onDismiss,
    )
}

// Quiet "unused" lint for the manifest type being routed through the host —
// it's used in production, this just keeps the import tree explicit.
@Suppress("unused")
private fun keepImport(@Suppress("UNUSED_PARAMETER") m: AppUpdateDto) = Unit
