package com.mediaplayer.android.ui.common

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediaplayer.android.data.PlayerSettings
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import kotlinx.coroutines.launch

/**
 * Inline banner shown on Home (below AppUpdateBanner) when running on a
 * Xiaomi/Redmi/Poco/MIUI device. MIUI's aggressive autostart + battery
 * policies kill the foreground media service mid-playback and block
 * MediaSession reconnects — Android Auto steering-wheel commands then
 * fail silently. The banner walks the user to the two settings screens
 * that resolve the issue (Avvio automatico + Risparmio batteria), with a
 * one-shot "ignora" that hides the banner forever.
 *
 * Renders nothing on non-Xiaomi devices, or after the user dismisses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiuiRestrictionsBannerHost(modifier: Modifier = Modifier) {
    if (!isXiaomiDevice()) return
    val context = LocalContext.current
    val settings = remember { PlayerSettings.instance }
    val dismissed by settings.miuiWarningDismissed
        .collectAsStateWithLifecycle(initialValue = true)
    if (dismissed) return

    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }

    val mono = LocalMHMono.current
    val amber = Color(0xFFFFB547)
    val amberSoft = Color(0xFFFFD58A)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(amber.copy(alpha = 0.10f))
            .border(1.dp, amber.copy(alpha = 0.32f), RoundedCornerShape(14.dp))
            .clickable { showSheet = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(amber.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = amberSoft,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "// XIAOMI · ANDROID AUTO",
                style = mono.eyebrow.copy(color = amberSoft),
            )
            Text(
                text = "Comandi al volante non rispondono",
                color = MHColors.TextHi,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = "Tocca per sistemare i permessi MIUI",
                color = MHColors.TextLo,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Spacer(Modifier.size(8.dp))
        IconButton(
            onClick = { scope.launch { settings.setMiuiWarningDismissed(true) } },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Non mostrare più",
                tint = MHColors.TextLo,
                modifier = Modifier.size(18.dp),
            )
        }
    }

    if (showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MHColors.Card,
        ) {
            MiuiSheetContent(
                onOpenAutostart = { openMiuiAutostart(context) },
                onOpenBattery = { openMiuiBattery(context) },
                onDismissForever = {
                    scope.launch { settings.setMiuiWarningDismissed(true) }
                    showSheet = false
                },
            )
        }
    }
}

@Composable
private fun MiuiSheetContent(
    onOpenAutostart: () -> Unit,
    onOpenBattery: () -> Unit,
    onDismissForever: () -> Unit,
) {
    val mono = LocalMHMono.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            text = "// XIAOMI · MIUI",
            style = mono.eyebrow.copy(color = Color(0xFFFFD58A)),
        )
        Text(
            text = "Sistema i permessi per Android Auto",
            color = MHColors.TextHi,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "MIUI mette in pausa l'app in background, così i comandi del " +
                "volante e il Bluetooth dell'auto smettono di rispondere. Servono " +
                "due interruttori — toccali in ordine.",
            color = MHColors.TextLo,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.size(20.dp))

        SheetStep(
            number = "01",
            icon = Icons.Filled.PlayArrow,
            title = "Avvio automatico",
            subtitle = "Attiva «MediaPlayer» nella lista. Permette al servizio di " +
                "ripartire quando colleghi l'auto.",
            cta = "Apri Avvio automatico",
            onClick = onOpenAutostart,
        )
        Spacer(Modifier.size(12.dp))
        SheetStep(
            number = "02",
            icon = Icons.Filled.Battery6Bar,
            title = "Risparmio batteria",
            subtitle = "Imposta «Nessuna restrizione». Evita che MIUI uccida " +
                "l'audio dopo qualche minuto in background.",
            cta = "Apri Batteria app",
            onClick = onOpenBattery,
        )

        Spacer(Modifier.size(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(onClick = onDismissForever)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Non mostrare più",
                color = MHColors.TextLo,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.size(20.dp))
    }
}

@Composable
private fun SheetStep(
    number: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    cta: String,
    onClick: () -> Unit,
) {
    val mono = LocalMHMono.current
    val amber = Color(0xFFFFB547)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = number,
                style = mono.caption.copy(color = amber, fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(amber.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = amber,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.size(10.dp))
            Text(
                text = title,
                color = MHColors.TextHi,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = subtitle,
            color = MHColors.TextLo,
            fontSize = 12.5.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.size(10.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(amber)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = cta,
                color = Color(0xFF0A0A0A),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * MIUI / HyperOS detection. Covers Xiaomi-branded handsets plus the
 * Redmi and Poco sub-brands which all ship with MIUI's autostart +
 * battery restrictions enabled. We deliberately don't try to read the
 * MIUI version property (`ro.miui.ui.version.name`) — Build.MANUFACTURER
 * is reliable and doesn't require reflection or a hidden API call.
 */
private fun isXiaomiDevice(): Boolean {
    val mfg = Build.MANUFACTURER?.lowercase().orEmpty()
    val brand = Build.BRAND?.lowercase().orEmpty()
    return mfg == "xiaomi" || brand == "xiaomi" ||
        brand == "redmi" || brand == "poco"
}

/**
 * MIUI Security Center hosts the autostart whitelist on every recent
 * MIUI/HyperOS build. Component name has been stable across MIUI 10–14.
 * Falls back to the app-info screen if the Security Center package is
 * disabled or absent (some MIUI Global builds strip it).
 */
private fun openMiuiAutostart(context: Context) {
    val intent = Intent().apply {
        component = ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
        )
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "Apri Sicurezza → Autorizzazioni → Avvio automatico e attiva MediaPlayer.",
            Toast.LENGTH_LONG,
        ).show()
        openAppDetailsFallback(context)
    } catch (_: SecurityException) {
        openAppDetailsFallback(context)
    }
}

/**
 * MIUI's per-app battery saver lives inside PowerKeeper. The dedicated
 * activity isn't reliably exposed across MIUI versions, so we route
 * straight to the system app-info page where the "Risparmio batteria"
 * row sits — works on stock Android, MIUI, and HyperOS alike.
 */
private fun openMiuiBattery(context: Context) {
    openAppDetailsFallback(context)
    Toast.makeText(
        context,
        "Tocca «Risparmio batteria» e scegli «Nessuna restrizione».",
        Toast.LENGTH_LONG,
    ).show()
}

private fun openAppDetailsFallback(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Last-resort silent failure — should never happen on real devices.
    }
}
