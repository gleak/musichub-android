package com.mediaplayer.android.update

import java.net.URI

/**
 * Decides whether an update manifest may be acted on at all.
 *
 * Both rules here are fail-closed on purpose, because the two controls guarding
 * the install path used to fail open:
 *
 * - the checksum is optional in the wire format (`AppUpdateDto.sha256` defaults
 *   to an empty string), and the installer only compared it when non-blank — so
 *   a manifest without a hash was installed with no integrity check at all,
 *   reporting success identically;
 * - the download URL was taken from the manifest and passed straight to
 *   `DownloadManager` with the session Bearer and the API key attached, with no
 *   check that it even pointed at our own backend.
 *
 * Kept free of Android so the decision can be tested directly.
 */
internal object UpdateSourcePolicy {

    /**
     * Returns a human-readable reason to refuse [url], or `null` when the
     * source is acceptable. [baseUrl] is the backend this build talks to.
     */
    fun rejectionFor(url: String, sha256: String?, baseUrl: String): String? {
        if (sha256.isNullOrBlank()) {
            return "Aggiornamento rifiutato: manca il codice di controllo del file"
        }
        if (!isValidSha256(sha256)) {
            return "Aggiornamento rifiutato: codice di controllo non valido"
        }
        val target = url.toUriOrNull()
            ?: return "Aggiornamento rifiutato: indirizzo di download non valido"
        val base = baseUrl.toUriOrNull()
            ?: return "Aggiornamento rifiutato: indirizzo del server non valido"

        val targetHost = target.host
        val baseHost = base.host
        if (targetHost.isNullOrBlank() || baseHost.isNullOrBlank()) {
            return "Aggiornamento rifiutato: indirizzo di download non valido"
        }
        if (!targetHost.equals(baseHost, ignoreCase = true)) {
            return "Aggiornamento rifiutato: il file non arriva dal server dell'app"
        }
        // DownloadManager runs in the system's download process and does not
        // apply this app's network-security config, so an http:// manifest
        // would send the Bearer token in the clear. Only allow cleartext when
        // the backend itself is cleartext (local development builds).
        if (!target.scheme.equals("https", ignoreCase = true) &&
            base.scheme.equals("https", ignoreCase = true)
        ) {
            return "Aggiornamento rifiutato: download non cifrato"
        }
        return null
    }

    private fun isValidSha256(value: String): Boolean =
        value.length == SHA256_HEX_LENGTH && value.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }

    private fun String.toUriOrNull(): URI? = runCatching { URI(this) }.getOrNull()

    private const val SHA256_HEX_LENGTH = 64
}
