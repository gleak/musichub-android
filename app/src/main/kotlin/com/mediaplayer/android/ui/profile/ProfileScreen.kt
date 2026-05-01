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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.android.data.AppVersion
import com.mediaplayer.android.ui.common.EyebrowText
import com.mediaplayer.android.ui.common.LocalCurrentUser
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MHGradient
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
    onShowChangelog: () -> Unit = {},
    onCheckUpdates: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onOpenSetting: (route: String) -> Unit = {},
    statsViewModel: ProfileStatsViewModel = viewModel(),
) {
    val user = LocalCurrentUser.current?.user
    val mono = LocalMHMono.current
    val stats by statsViewModel.state.collectAsStateWithLifecycle()
    val crossfadeSec by com.mediaplayer.android.data.PlayerSettings.instance
        .crossfadeSeconds.collectAsStateWithLifecycle(initialValue = 0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MHGradient.screenBg()),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 24.dp),
    ) {
        item {
            Text(
                text = "Profilo",
                style = MaterialTheme.typography.headlineMedium,
                color = MHColors.TextHi,
                modifier = Modifier.padding(horizontal = MediaPlayerSpacing.M),
            )
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
                        text = user?.name ?: user?.email ?: "Ospite",
                        style = MaterialTheme.typography.titleLarge,
                        color = MHColors.TextHi,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (user?.anonymous == true) "Sessione ospite"
                        else user?.email ?: "—",
                        style = mono.caption,
                        color = MHColors.TextLo,
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
                StatCard("Brani", stats.songs, modifier = Modifier.weight(1f))
                StatCard("Playlist", stats.playlists, modifier = Modifier.weight(1f))
                StatCard("Artisti", stats.artists, modifier = Modifier.weight(1f))
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        item {
            SettingsSection(title = "ACCOUNT") {
                SettingsRow(label = "Profilo", detail = user?.email ?: "Ospite")
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
            SettingsSection(title = "APP") {
                SettingsRow(label = "Tema", detail = themeLabel,
                    onClick = { onOpenSetting("profile/theme") })
                SettingsRow(label = "Cosa c'è di nuovo", onClick = onShowChangelog)
                SettingsRow(label = "Controlla aggiornamenti", onClick = onCheckUpdates)
                SettingsRow(label = "Versione", detail = "v${AppVersion.VERSION}")
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MediaPlayerSpacing.M),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Disconnetti",
                    color = Color(0xFFFF4D2E),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clickable(onClick = onSignOut),
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
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    val mono = LocalMHMono.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MHColors.Card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = value,
            style = mono.statValue,
            color = MHColors.Lime,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MHColors.TextLo,
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
                .background(MHColors.Card),
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
            .border(0.5.dp, MHColors.Divider, RoundedCornerShape(0.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MHColors.TextHi,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (detail != null) {
            Text(
                text = detail,
                color = MHColors.TextLo,
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
                tint = MHColors.TextLo,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

private fun displayInitial(name: String?, email: String?): String {
    val s = name?.takeIf { it.isNotBlank() } ?: email
    return s?.trim()?.firstOrNull()?.uppercase() ?: "?"
}
