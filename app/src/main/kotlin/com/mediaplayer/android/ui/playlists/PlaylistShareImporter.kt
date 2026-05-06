package com.mediaplayer.android.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.PlaylistRepository
import com.mediaplayer.android.data.dto.SharePreviewDto
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import kotlinx.coroutines.launch

/**
 * Full-screen modal that previews a shared playlist and lets the user
 * accept it (`mh-library.jsx:370-421`). Replaces the earlier `AlertDialog`
 * variant — the mockup expects a hero-style landing instead of a stock
 * Material confirm box, so the dialog is rendered inside a fullscreen
 * [Dialog] with `usePlatformDefaultWidth = false`.
 *
 * Phases:
 *   1. Loading — fetch the preview.
 *   2a. `alreadyAccessible` — auto-accept and route through (skips the modal).
 *   2b. Confirm — render the hero + state-aware CTA.
 *   3. Error — keep the modal up with a red copy line.
 */
@Composable
fun PlaylistShareImporter(
    token: String,
    onDismiss: () -> Unit,
    onImported: (playlistId: Long, playlistName: String) -> Unit,
    repository: PlaylistRepository = remember { PlaylistRepository() },
) {
    var preview by remember { mutableStateOf<SharePreviewDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(token) {
        try {
            val p = repository.previewShare(token)
            if (p.alreadyAccessible) {
                val detail = repository.acceptShare(token)
                onImported(detail.id, detail.name)
            } else {
                preview = p
            }
        } catch (t: Throwable) {
            error = t.message ?: "Impossibile caricare la playlist condivisa"
        }
    }

    Dialog(
        onDismissRequest = { if (!importing) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !importing,
            dismissOnClickOutside = false,
        ),
    ) {
        ImporterContent(
            preview = preview,
            error = error,
            importing = importing,
            onClose = { if (!importing) onDismiss() },
            onAccept = {
                if (importing) return@ImporterContent
                importing = true
                scope.launch {
                    try {
                        val detail = repository.acceptShare(token)
                        onImported(detail.id, detail.name)
                    } catch (t: Throwable) {
                        error = t.message ?: "Importazione non riuscita"
                        importing = false
                    }
                }
            },
            shareToken = token,
        )
    }
}

@Composable
private fun ImporterContent(
    preview: SharePreviewDto?,
    error: String?,
    importing: Boolean,
    onClose: () -> Unit,
    onAccept: () -> Unit,
    shareToken: String,
) {
    val mono = LocalMHMono.current
    val gradient = Brush.verticalGradient(
        0f to Color(0xFF2A4615),
        0.6f to MHColors.BgBottom,
        1f to MHColors.BgBottom,
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            // Top bar — X dismiss left, eyebrow center, spacer right.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !importing, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Chiudi",
                        tint = MHColors.OnHero,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "// LINK CONDIVISO",
                    style = mono.eyebrow.copy(color = MHColors.Lime),
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(40.dp))
            }

            // Hero section.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when {
                    error != null -> Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    preview == null -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        CircularProgressIndicator(color = MHColors.Lime)
                        Text(
                            "Carico la playlist condivisa…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MHColors.OnHeroMuted,
                        )
                    }
                    else -> ImporterHero(preview = preview)
                }
            }

            // Bottom CTA + URL footer.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onAccept,
                    enabled = preview != null && !importing && error == null,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MHColors.Lime,
                        contentColor = Color(0xFF0A0A0A),
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (importing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF0A0A0A),
                        )
                    } else {
                        Text(
                            text = "Aggiungi alla mia libreria",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "mh.duckdns.org/p/${shareToken.take(7)}",
                    style = mono.duration.copy(color = MHColors.OnHeroDim),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ImporterHero(preview: SharePreviewDto) {
    val mono = LocalMHMono.current
    val coverUrl = preview.coverSongId?.let { Network.coverUrl(it) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MHColors.Card),
            contentAlignment = Alignment.Center,
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = preview.playlistName,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MHColors.OnHero,
            textAlign = TextAlign.Center,
        )
        Text(
            text = buildOwnerCaption(preview.ownerName),
            style = MaterialTheme.typography.bodyMedium,
            color = MHColors.OnHeroMuted,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "${preview.songCount} BRANI",
            style = mono.eyebrow.copy(
                color = MHColors.OnHeroDim,
                letterSpacing = 1.2.sp,
            ),
        )
        Spacer(Modifier.height(8.dp))
        OwnerAvatar(ownerName = preview.ownerName)
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Aggiungendola alla libreria potrai aggiungere brani e tutti i collaboratori vedranno le tue modifiche.",
            style = MaterialTheme.typography.bodyMedium,
            color = MHColors.OnHeroMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OwnerAvatar(ownerName: String) {
    val initial = ownerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF5C2D8C)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color(0xFF0A0A0A),
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun buildOwnerCaption(ownerName: String): String =
    "Playlist collaborativa di $ownerName"
