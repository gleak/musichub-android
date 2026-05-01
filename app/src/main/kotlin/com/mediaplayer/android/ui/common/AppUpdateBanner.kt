package com.mediaplayer.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors

/**
 * Non-modal banner shown on Home when `AppUpdateChecker` reports a
 * newer APK is available on the operator's host. Tap → triggers
 * install via `AppUpdateInstaller`. Dismiss is local-only — re-shows
 * on next launch until the user installs or the file disappears.
 */
@Composable
fun AppUpdateBanner(
    fromVersion: String,
    toVersion: String,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mono = LocalMHMono.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MHColors.Lime.copy(alpha = 0.10f))
            .border(1.dp, MHColors.Lime.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onInstall)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MHColors.Lime),
            contentAlignment = Alignment.Center,
        ) {
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
                text = "// AGGIORNAMENTO DISPONIBILE",
                style = mono.eyebrow.copy(color = MHColors.Lime),
            )
            Text(
                text = "v$fromVersion → v$toVersion",
                style = MaterialTheme.typography.titleMedium,
                color = MHColors.TextHi,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Tocca per installare",
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.TextLo,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Ignora",
                tint = MHColors.TextLo,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
