package com.mediaplayer.android.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.mediaplayer.android.data.AuthTokenHolder
import com.mediaplayer.android.data.Network
import java.io.File
import java.security.MessageDigest

/**
 * Drives the download → integrity-check → install flow for the
 * self-hosted update channel. Uses the system DownloadManager so the
 * download survives the activity being torn down + shows in the
 * notification shade. Auth headers (X-Api-Key + Authorization Bearer)
 * are attached to the request so the protected backend endpoint accepts
 * the fetch without a separate public route.
 */
object AppUpdateInstaller {

    private const val APK_DIR = "updates"
    private const val APK_FILE = "mediaplayer-update.apk"

    /**
     * Kick off the system-level download of [url]. Re-uses an in-flight
     * download if one already exists. The completion-callback fires the
     * install intent (or a SHA mismatch error toast).
     */
    fun startDownload(
        context: Context,
        url: String,
        expectedSha256: String?,
        onError: (String) -> Unit,
        onReady: (File) -> Unit,
    ) {
        val target = apkFile(context)
        if (target.exists()) target.delete()

        val token = AuthTokenHolder.idToken
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Aggiornamento MusicHub")
            .setDescription("Scaricamento nuova versione…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(target))
            .addRequestHeader("X-Api-Key", Network.API_KEY)
        if (token != null) {
            request.addRequestHeader("Authorization", "Bearer $token")
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val finishedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (finishedId != downloadId) return
                ctx.unregisterReceiver(this)

                val query = DownloadManager.Query().setFilterById(finishedId)
                val (status, reason) = dm.query(query)?.use { c ->
                    if (c.moveToFirst()) {
                        c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) to
                            c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    } else -1 to -1
                } ?: (-1 to -1)

                if (status != DownloadManager.STATUS_SUCCESSFUL) {
                    val detail = when (reason) {
                        DownloadManager.ERROR_CANNOT_RESUME -> "Impossibile riprendere il download"
                        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Spazio di archiviazione non trovato"
                        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "Conflitto sul file"
                        DownloadManager.ERROR_FILE_ERROR -> "Errore di archiviazione"
                        DownloadManager.ERROR_HTTP_DATA_ERROR -> "Errore dati HTTP"
                        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Spazio insufficiente"
                        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Troppi redirect"
                        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Risposta del server inattesa"
                        DownloadManager.ERROR_UNKNOWN -> "Errore sconosciuto"
                        401 -> "Non autorizzato — prova a uscire e accedere di nuovo"
                        403 -> "Accesso negato"
                        404 -> "File di aggiornamento non trovato sul server"
                        else -> if (reason in 400..599) "Errore del server $reason" else "Errore $reason"
                    }
                    onError("Download non riuscito: $detail")
                    return
                }

                if (!target.exists() || target.length() == 0L) {
                    onError("Download non riuscito: file mancante al termine")
                    return
                }
                if (!expectedSha256.isNullOrBlank()) {
                    val actual = sha256(target)
                    if (!actual.equals(expectedSha256, ignoreCase = true)) {
                        target.delete()
                        onError("Controllo di integrità non riuscito — file rifiutato")
                        return
                    }
                }
                onReady(target)
            }
        }
        // Android 13+ requires explicit export flag for runtime-registered receivers.
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Context.RECEIVER_EXPORTED else 0
        context.registerReceiver(
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            flags,
        )
    }

    /**
     * Launch the system package installer for [apk]. On Android 8+ the
     * user must have granted "Install unknown apps" for our package; if
     * not, we send them to the relevant settings screen first.
     */
    fun launchInstall(context: Context, apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && !context.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun apkFile(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), APK_DIR).apply { mkdirs() }
        return File(dir, APK_FILE)
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
