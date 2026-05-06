package com.mediaplayer.android.ui.profile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.PlayerSettings
import com.mediaplayer.android.data.PlaylistRepository
import com.mediaplayer.android.playback.PlayerCache
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MonoFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CACHE_CAP_BYTES = 1_024L * 1_024L * 1_024L // 1 GiB — matches PlayerCache.MAX_BYTES

/**
 * Settings → Download offline. Mockup `mh-settings.jsx:69-105`: storage
 * gauge (eyebrow `// SPAZIO USATO`, glued mono cap, sub-line of cached
 * counts), per-toggle separate cards, `// GESTIONE` chevron row(s), and
 * a destructive pill button at the foot.
 *
 * The mockup's three GESTIONE rows aren't all backed by real actions —
 * "Riscarica da origine" and "Svuota cache locale" both alias to the
 * same `cache.removeResource` loop the destructive pill triggers, so
 * shipping all three would lie about doing different things. Only the
 * "Forza rigenerazione Daily Mix" row remains, since it has its own
 * backend endpoint.
 */
@OptIn(UnstableApi::class)
@Composable
fun DownloadOfflineScreen(onBack: () -> Unit) {
    val mono = LocalMHMono.current
    val context = LocalContext.current
    val settings = PlayerSettings.instance
    val playlistRepo = remember { PlaylistRepository() }
    val scope = rememberCoroutineScope()

    val wifiOnly by settings.downloadWifiOnly.collectAsState(initial = true)
    val autoDownload by settings.downloadAuto.collectAsState(initial = false)

    var usedBytes by remember { mutableLongStateOf(0L) }
    var trackCount by remember { mutableIntStateOf(0) }
    var refreshTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(refreshTick) {
        val (bytes, count) = withContext(Dispatchers.IO) {
            runCatching {
                val cache = PlayerCache.get(context)
                cache.cacheSpace to cache.keys.size
            }.getOrDefault(0L to 0)
        }
        usedBytes = bytes
        trackCount = count
    }

    var dailyMixDetail by remember { mutableStateOf<String?>(null) }
    var dailyMixBusy by remember { mutableStateOf(false) }

    val usedGb = usedBytes / 1024f / 1024f / 1024f
    val capGb = CACHE_CAP_BYTES / 1024f / 1024f / 1024f
    val fillFraction = (usedBytes.toFloat() / CACHE_CAP_BYTES.toFloat()).coerceIn(0f, 1f)

    SettingsSubScreen(title = "Download offline", onBack = onBack) {
        SettingsCard(eyebrow = "Spazio usato") {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (usedGb < 0.1f) "%.0fMB".format(usedBytes / 1024f / 1024f)
                        else "%.1fGB".format(usedGb),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = MonoFamily,
                            fontSize = 32.sp,
                        ),
                        color = MHColors.Lime,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = " / ${capGb.toInt()}GB",
                        style = mono.caption.copy(fontSize = 14.sp),
                        color = MHColors.TextLo,
                        modifier = Modifier.padding(bottom = 6.dp, start = 6.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MHColors.Divider),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fillFraction)
                            .height(6.dp)
                            .background(MHColors.Lime),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (trackCount == 1) "1 brano scaricato"
                    else "$trackCount brani scaricati",
                    style = mono.duration,
                    color = MHColors.TextLo,
                )
            }
        }
        // Mockup specs each toggle as its own pill card. We use one card
        // with a divider — visually closer to the mockup than the v0.13.1
        // "Opzioni" wrapper while still grouping related controls.
        SettingsCard {
            SettingsToggleRow(
                label = "Solo Wi-Fi",
                detail = "Non scaricare con dati mobili",
                checked = wifiOnly,
                onCheckedChange = { v -> scope.launch { settings.setDownloadWifiOnly(v) } },
                showDivider = true,
            )
            SettingsToggleRow(
                label = "Download automatico",
                detail = "Scarica le novità delle playlist sincronizzate appena online. " +
                    "Disattivato per default da v0.12.6 — ti chiediamo conferma prima di " +
                    "occupare spazio.",
                checked = autoDownload,
                onCheckedChange = { v -> scope.launch { settings.setDownloadAuto(v) } },
            )
        }
        SettingsCard(eyebrow = "Gestione") {
            SettingsActionRow(
                label = "Forza rigenerazione Daily Mix",
                detail = dailyMixDetail
                    ?: "Ricalcola il Daily Mix di domani al prossimo aggiornamento",
                enabled = !dailyMixBusy,
                onClick = {
                    dailyMixBusy = true
                    dailyMixDetail = "In corso…"
                    scope.launch {
                        val result = runCatching { playlistRepo.refreshDailyMix() }
                        dailyMixDetail = result.fold(
                            onSuccess = { n -> "Aggiornato ($n brani)" },
                            onFailure = { "Errore — riprova" },
                        )
                        dailyMixBusy = false
                    }
                },
            )
        }
        DestructivePillButton(
            text = "Cancella tutti i download",
            onClick = {
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
            },
        )
    }
}
