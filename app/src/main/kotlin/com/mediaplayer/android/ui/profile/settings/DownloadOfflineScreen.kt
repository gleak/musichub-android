package com.mediaplayer.android.ui.profile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.PlayerSettings
import com.mediaplayer.android.playback.PlayerCache
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MonoFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CACHE_CAP_BYTES = 1_024L * 1_024L * 1_024L // 1 GiB — matches PlayerCache.MAX_BYTES

/**
 * Settings → Download offline. Reads [PlayerCache] for actual usage and
 * [PlayerSettings] for user preferences. The "Cancella tutti i download"
 * row removes every cached span so subsequent playback re-fetches from
 * the backend.
 */
@OptIn(UnstableApi::class)
@Composable
fun DownloadOfflineScreen(onBack: () -> Unit) {
    val mono = LocalMHMono.current
    val context = LocalContext.current
    val settings = PlayerSettings.instance
    val scope = rememberCoroutineScope()

    val wifiOnly by settings.downloadWifiOnly.collectAsState(initial = true)
    val autoDownload by settings.downloadAuto.collectAsState(initial = false)

    var usedBytes by remember { mutableLongStateOf(0L) }
    var refreshTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(refreshTick) {
        usedBytes = withContext(Dispatchers.IO) {
            runCatching { PlayerCache.get(context).cacheSpace }.getOrDefault(0L)
        }
    }

    val usedGb = usedBytes / 1024f / 1024f / 1024f
    val capGb = CACHE_CAP_BYTES / 1024f / 1024f / 1024f
    val fillFraction = (usedBytes.toFloat() / CACHE_CAP_BYTES.toFloat()).coerceIn(0f, 1f)

    SettingsSubScreen(title = "Download offline", onBack = onBack) {
        SettingsCard(eyebrow = "Spazio occupato") {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (usedGb < 0.1f) "%.0f".format(usedBytes / 1024f / 1024f)
                        else "%.1f".format(usedGb),
                        style = MaterialTheme.typography.displayLarge.copy(fontFamily = MonoFamily),
                        color = MHColors.Lime,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = if (usedGb < 0.1f) "MB di ${capGb.toInt()} GB"
                        else "GB di ${capGb.toInt()} GB",
                        style = mono.caption,
                        color = MHColors.TextLo,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MHColors.Divider),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fillFraction)
                            .height(8.dp)
                            .background(MHColors.Lime),
                    )
                }
            }
        }
        SettingsCard(eyebrow = "Opzioni") {
            SettingsToggleRow(
                label = "Solo Wi-Fi",
                detail = "Scarica solo quando sei connesso a una rete Wi-Fi",
                checked = wifiOnly,
                onCheckedChange = { v -> scope.launch { settings.setDownloadWifiOnly(v) } },
                showDivider = true,
            )
            SettingsToggleRow(
                label = "Download automatico",
                detail = "Scarica automaticamente ogni brano che ascolti. Disattivato per impostazione predefinita.",
                checked = autoDownload,
                onCheckedChange = { v -> scope.launch { settings.setDownloadAuto(v) } },
            )
        }
        SettingsCard(eyebrow = "Manutenzione") {
            Text(
                text = "Cancella tutti i download",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFF4D2E),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                runCatching {
                                    val cache = PlayerCache.get(context)
                                    cache.keys.toList().forEach { key ->
                                        cache.removeResource(key)
                                    }
                                }
                            }
                            refreshTick = System.currentTimeMillis()
                        }
                    }
                    .padding(14.dp),
            )
        }
    }
}
