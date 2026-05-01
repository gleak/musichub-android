package com.mediaplayer.android.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.dto.AppUpdateDto

@Composable
fun AppUpdateDialog(
    manifest: AppUpdateDto,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var downloading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!manifest.required && !downloading) onDismiss() },
        title = {
            Text(
                if (manifest.required) "Update required" else "Update available"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Version ${manifest.version} is available.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (manifest.releaseNotes.isNotBlank()) {
                    Text(
                        text = manifest.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !downloading,
                onClick = {
                    downloading = true
                    error = null
                    AppUpdateInstaller.startDownload(
                        context = context,
                        url = manifest.url,
                        expectedSha256 = manifest.sha256.takeIf { it.isNotBlank() },
                        onError = { msg ->
                            downloading = false
                            error = msg
                        },
                        onReady = { apk ->
                            downloading = false
                            AppUpdateInstaller.launchInstall(context, apk)
                            // Don't auto-dismiss — the user will leave the app
                            // for the package installer; if they back out we
                            // still want the dialog as the entry point.
                        },
                    )
                },
            ) {
                if (downloading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Update now")
                }
            }
        },
        dismissButton = {
            if (!manifest.required) {
                TextButton(
                    enabled = !downloading,
                    onClick = onDismiss,
                ) { Text("Later") }
            }
        },
    )
}
