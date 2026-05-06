package com.mediaplayer.android.ui.playlists

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.PlaylistRepository
import com.mediaplayer.android.data.dto.ShareLinkDto
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import kotlinx.coroutines.launch

/**
 * Owner-side share sheet (`mh-library.jsx:331-367`). Mints a token on open
 * and surfaces the URL with a copy CTA + system-share fallback. Replaces the
 * earlier flow that fired `Intent.createChooser` directly from the
 * `TopAppBar` icon — the user never saw the URL itself, couldn't copy it
 * without picking a clipboard target, and had no surface to revoke from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistShareSheet(
    playlistId: Long,
    playlistName: String,
    memberCount: Int,
    onDismiss: () -> Unit,
    repository: PlaylistRepository = remember { PlaylistRepository() },
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mono = LocalMHMono.current

    var link by remember { mutableStateOf<ShareLinkDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    var revokeConfirm by remember { mutableStateOf(false) }
    var revoking by remember { mutableStateOf(false) }
    var revokedNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(playlistId) {
        try {
            link = repository.createShare(playlistId)
        } catch (t: Throwable) {
            error = t.message ?: "Impossibile creare il link di condivisione"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MHColors.Card,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "// CONDIVIDI · COLLABORATIVA",
                style = mono.eyebrow.copy(color = MHColors.Lime),
            )
            Text(
                text = playlistName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Chi apre il link entra come collaboratore della stessa playlist — " +
                    "non viene creata una copia. Solo tu, come creatore, puoi generare nuovi link.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when {
                error != null -> Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                link == null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        "Genero il link…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LinkBox(
                        url = link!!.url,
                        copied = copied,
                        onCopy = {
                            copyToClipboard(context, link!!.url)
                            copied = true
                        },
                    )
                    SystemShareButton(url = link!!.url, name = playlistName, context = context)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (memberCount == 0) "Nessun membro attivo"
                           else "${pluralizeMembers(memberCount)} · revoca per chiudere il link",
                    style = mono.duration.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(end = 8.dp),
                )
                Spacer(Modifier.width(0.dp))
                TextButton(
                    onClick = { revokeConfirm = true },
                    enabled = link != null && !revoking,
                    modifier = Modifier.padding(start = 0.dp),
                ) {
                    if (revoking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFF7A7A),
                        )
                    } else Text(
                        "Revoca link",
                        color = Color(0xFFFF7A7A),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (revokedNotice != null) {
                Text(
                    text = revokedNotice!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (revokeConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { revokeConfirm = false },
            title = { Text("Revocare il link?") },
            text = {
                Text(
                    "Il link smetterà di funzionare per chi non l'ha ancora aperto. " +
                        "I membri già accettati restano nella playlist."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    revokeConfirm = false
                    revoking = true
                    scope.launch {
                        try {
                            repository.revokeShares(playlistId)
                            link = null
                            error = null
                            revokedNotice = "Link revocato. Genera un nuovo link riaprendo questa schermata."
                        } catch (t: Throwable) {
                            revokedNotice = t.message ?: "Impossibile revocare il link"
                        } finally {
                            revoking = false
                        }
                    }
                }) {
                    Text("Revoca", color = Color(0xFFFF7A7A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { revokeConfirm = false }) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun LinkBox(url: String, copied: Boolean, onCopy: () -> Unit) {
    val mono = LocalMHMono.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A0A0A))
            .border(1.dp, MHColors.Lime.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = url,
            style = mono.duration.copy(color = MaterialTheme.colorScheme.onSurface),
            maxLines = 1,
            modifier = Modifier.padding(end = 10.dp).fillMaxWidth(0.7f),
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onCopy,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (copied) MHColors.LimePressed else MHColors.Lime,
                contentColor = Color(0xFF0A0A0A),
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(if (copied) "Copiato" else "Copia", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SystemShareButton(url: String, name: String, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Ascolta $name")
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                context.startActivity(Intent.createChooser(intent, "Condividi playlist"))
            }
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "Condividi via sistema",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("Link playlist", text))
}

private fun pluralizeMembers(count: Int): String =
    if (count == 1) "1 membro attivo" else "$count membri attivi"
