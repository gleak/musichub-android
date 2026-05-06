package com.mediaplayer.android.ui.profile

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.mediaplayer.android.data.PlaylistRepository
import com.mediaplayer.android.data.dto.UserDto
import com.mediaplayer.android.data.sync.EventQueue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mediaplayer.android.data.AppVersion
import com.mediaplayer.android.ui.common.EyebrowText
import com.mediaplayer.android.ui.common.LocalCurrentUser
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MHGradient
import com.mediaplayer.android.ui.theme.MHTheme
import com.mediaplayer.android.ui.theme.MediaPlayerSpacing

/**
 * Mockup screen 08 — Profile / Settings. Replaces the dropdown menu
 * that lived on the Home greeting in v0.9.x. Hosts: avatar + stats,
 * Account / Riproduzione / App sections, Disconnetti CTA.
 *
 * Premium / Family / Audio quality intentionally absent — the app is
 * fully free per the design contract.
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onShowChangelog: () -> Unit = {},
    onCheckUpdates: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onOpenSetting: (route: String) -> Unit = {},
    onSongsClick: () -> Unit = {},
    onPlaylistsClick: () -> Unit = {},
    onArtistsClick: () -> Unit = {},
    statsViewModel: ProfileStatsViewModel = viewModel(),
) {
    val user = LocalCurrentUser.current?.user
    val mono = LocalMHMono.current
    val stats by statsViewModel.state.collectAsStateWithLifecycle()
    val crossfadeSec by com.mediaplayer.android.data.PlayerSettings.instance
        .crossfadeSeconds.collectAsStateWithLifecycle(initialValue = 0)

    // Manual Daily Mix refresh — fire-and-forget POST. Detail label
    // doubles as transient status: idle / In corso… / count or error.
    val playlistRepo = remember { PlaylistRepository() }
    val scope = rememberCoroutineScope()
    var dailyMixDetail by remember { mutableStateOf("Aggiorna ora") }
    var dailyMixBusy by remember { mutableStateOf(false) }

    // Confirmation gate for sign-out — both Disconnetti and Cambia
    // account funnel through here so an accidental tap doesn't drop
    // the session silently.
    var showSignOutConfirm by remember { mutableStateOf(false) }
    if (showSignOutConfirm) {
        AccountSwitchDialog(
            user = user,
            onDismiss = { showSignOutConfirm = false },
            onConfirm = {
                showSignOutConfirm = false
                onSignOut()
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MHGradient.screenBg()),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 24.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Indietro",
                        tint = MHTheme.textHi,
                    )
                }
                Text(
                    text = "Profilo",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MHTheme.textHi,
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MediaPlayerSpacing.M, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarBlock(
                    initial = user?.let { displayInitial(it.name, it.email) } ?: "M",
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user?.name ?: user?.email ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        color = MHTheme.textHi,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = user?.email ?: "—",
                        style = mono.caption,
                        color = MHTheme.textLo,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            // Stats — placeholder counts, wire to ViewModel later
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MediaPlayerSpacing.M),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard("Brani", stats.songs, modifier = Modifier.weight(1f), onClick = onSongsClick)
                StatCard("Playlist", stats.playlists, modifier = Modifier.weight(1f), onClick = onPlaylistsClick)
                StatCard("Artisti", stats.artists, modifier = Modifier.weight(1f), onClick = onArtistsClick)
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        item {
            SettingsSection(title = "ACCOUNT") {
                SettingsRow(label = "Profilo", detail = user?.email ?: "—")
                // Same callback as Disconnetti — signOut clears the
                // credential + flips AuthState so LoginScreen comes back,
                // and the user picks a different Google account (or
                // continues as guest) from there.
                SettingsRow(
                    label = "Cambia account",
                    detail = "Esci e accedi con un altro account",
                    onClick = { showSignOutConfirm = true },
                )
            }
        }
        item {
            SettingsSection(title = "RIPRODUZIONE") {
                SettingsRow(
                    label = "Crossfade",
                    detail = if (crossfadeSec > 0) "$crossfadeSec sec" else "Off",
                    onClick = { onOpenSetting("profile/crossfade") },
                )
                SettingsRow(
                    label = "Download offline",
                    detail = "Gestisci",
                    onClick = { onOpenSetting("profile/download") },
                )
                val onDailyMixClick: (() -> Unit)? = if (dailyMixBusy) null else {
                    {
                        dailyMixBusy = true
                        dailyMixDetail = "In corso…"
                        scope.launch {
                            val result = runCatching { playlistRepo.refreshDailyMix() }
                            dailyMixDetail = result.fold(
                                onSuccess = { n -> "Aggiornato ($n)" },
                                onFailure = { "Errore — riprova" },
                            )
                            dailyMixBusy = false
                        }
                        Unit
                    }
                }
                SettingsRow(
                    label = "Daily Mix",
                    detail = dailyMixDetail,
                    onClick = onDailyMixClick,
                )
                SettingsRow(
                    label = "Non consigliarmi",
                    detail = "Brani e artisti esclusi",
                    onClick = { onOpenSetting("profile/disliked") },
                )
            }
        }
        item {
            val themeMode by com.mediaplayer.android.data.PlayerSettings.instance
                .theme.collectAsStateWithLifecycle(initialValue = "dark")
            val themeLabel = when (themeMode) {
                "light" -> "Chiaro"
                "system" -> "Sistema"
                else -> "Scuro"
            }
            val pending by EventQueue.pending.collectAsStateWithLifecycle()
            SettingsSection(title = "APP") {
                SettingsRow(label = "Tema", detail = themeLabel,
                    onClick = { onOpenSetting("profile/theme") })
                SettingsRow(label = "Cosa c'è di nuovo", onClick = onShowChangelog)
                SettingsRow(label = "Controlla aggiornamenti", onClick = onCheckUpdates)
                SettingsRow(
                    label = "Eventi in coda",
                    detail = if (pending == 0) "Tutto sincronizzato" else "$pending in attesa",
                    onClick = { onOpenSetting("profile/queued-events") },
                )
                SettingsRow(label = "Versione", detail = "v${AppVersion.VERSION}")
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            // Full-width clickable card. The previous version put
            // .clickable on the Text node only, so the hit target was
            // the text glyphs alone — easy to miss and felt broken.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MediaPlayerSpacing.M)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MHTheme.card)
                    .clickable(onClick = { showSignOutConfirm = true })
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Disconnetti",
                    color = Color(0xFFFF4D2E),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun AvatarBlock(initial: String) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    0f to MHColors.Lime,
                    1f to Color(0xFF3A0CA3),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial.uppercase(),
            color = Color(0xFF0A0A0A),
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val mono = LocalMHMono.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MHTheme.card)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = value,
            style = mono.statValue,
            color = MHTheme.accent,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MHTheme.textLo,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediaPlayerSpacing.M, vertical = 8.dp),
    ) {
        EyebrowText(text = title)
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MHTheme.card),
            content = content,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.SettingsRow(
    label: String,
    detail: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .border(0.5.dp, MHTheme.divider, RoundedCornerShape(0.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MHTheme.textHi,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (detail != null) {
            Text(
                text = detail,
                color = MHTheme.textLo,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        // Chevron only when the row is actionable. Static rows (e.g. version)
        // would lie about being tappable if they showed one.
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MHTheme.textLo,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

private fun displayInitial(name: String?, email: String?): String {
    val s = name?.takeIf { it.isNotBlank() } ?: email
    return s?.trim()?.firstOrNull()?.uppercase() ?: "?"
}

/**
 * Mockup `AccountSwitchDialog` (mh-auth.jsx:186-219). Custom 16-radius
 * card surface with `// CAMBIA ACCOUNT` eyebrow, two-sentence cloud-sync
 * body, account preview row, and side-by-side pill buttons (Annulla
 * subtle / Disconnetti red filled). Replaces the stock Material3
 * AlertDialog that lived inline in `ProfileScreen` before v0.13.4.
 */
@Composable
private fun AccountSwitchDialog(
    user: UserDto?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val mono = LocalMHMono.current
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A1A))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EyebrowText(text = "Cambia account")
                Text(
                    text = "Disconnettersi da MusicHub?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = MHColors.TextHi,
                )
                Text(
                    text = "La libreria scaricata e i preferiti restano sul dispositivo. " +
                        "La sincronizzazione con il cloud si interrompe finché non accedi di nuovo.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    ),
                    color = MHColors.TextLo,
                )
                if (user != null) {
                    AccountPreviewRow(user = user)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PillButton(
                        modifier = Modifier.weight(1f),
                        text = "Annulla",
                        bg = Color.White.copy(alpha = 0.06f),
                        contentColor = MHColors.TextHi,
                        onClick = onDismiss,
                    )
                    PillButton(
                        modifier = Modifier.weight(1f),
                        text = "Disconnetti",
                        bg = Color(0xFFE14848),
                        contentColor = Color.White,
                        onClick = onConfirm,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountPreviewRow(user: UserDto) {
    val mono = LocalMHMono.current
    val initial = displayInitial(user.name, user.email)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        0f to MHColors.Lime,
                        1f to Color(0xFF3A0CA3),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                color = Color(0xFF0A0A0A),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.email ?: user.name ?: "—",
                color = MHColors.TextHi,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Account corrente",
                style = mono.duration.copy(
                    fontSize = 10.5.sp,
                    color = MHColors.TextLo2,
                ),
            )
        }
    }
}

@Composable
private fun PillButton(
    modifier: Modifier = Modifier,
    text: String,
    bg: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}
