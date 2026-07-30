package com.mediaplayer.android.update

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The update channel installs an APK, so these are the two controls that decide
 * what gets executed on the device. Both used to fail open; these tests exist to
 * keep them failing closed.
 */
class UpdateSourcePolicyTest {

    /**
     * The regression that matters most: `sha256` defaults to an empty string in
     * the wire format, and the installer only compared it when it was non-blank.
     * A manifest that simply omits the field therefore installed an unverified
     * binary and reported success exactly like a verified one.
     */
    @Test
    fun `a manifest without a checksum is refused`() {
        assertNotNull(rejection(sha256 = null))
        assertNotNull(rejection(sha256 = ""))
        assertNotNull(rejection(sha256 = "   "))
    }

    @Test
    fun `a malformed checksum is refused`() {
        assertNotNull("too short", rejection(sha256 = "abc123"))
        assertNotNull("not hex", rejection(sha256 = "z".repeat(64)))
        assertNotNull("too long", rejection(sha256 = "a".repeat(65)))
    }

    /**
     * The download request carries the session Bearer and the API key, so the
     * host it goes to is a credential-disclosure decision, not a convenience.
     */
    @Test
    fun `a download from another host is refused`() {
        assertNotNull(rejection(url = "https://evil.example/app.apk"))
        assertNotNull(rejection(url = "https://q-musichub.duckdns.org.evil.example/app.apk"))
    }

    /**
     * DownloadManager runs in the system's download process and does not apply
     * this app's network-security config, so cleartext here really is cleartext.
     */
    @Test
    fun `a cleartext download from an https backend is refused`() {
        assertNull(rejection(url = "$HTTPS_BASE/updates/app.apk"))
        assertNotNull(rejection(url = "http://q-musichub.duckdns.org/updates/app.apk"))
    }

    /** Local development backends are themselves cleartext; don't block them. */
    @Test
    fun `a cleartext download from a cleartext backend is allowed`() {
        assertNull(
            rejection(
                url = "http://10.0.2.2:8080/updates/app.apk",
                baseUrl = "http://10.0.2.2:8080/",
            )
        )
    }

    @Test
    fun `a well-formed manifest from the app's own backend is accepted`() {
        assertNull(rejection())
    }

    @Test
    fun `an unparseable address is refused`() {
        assertNotNull(rejection(url = "not a url at all"))
        assertNotNull(rejection(url = ""))
    }

    /** Hex case is not meaningful; only shape is. */
    @Test
    fun `checksum case is accepted either way`() {
        assertNull(rejection(sha256 = "A".repeat(64)))
        assertNull(rejection(sha256 = "a".repeat(64)))
    }

    private fun rejection(
        url: String = "$HTTPS_BASE/updates/app.apk",
        sha256: String? = "a".repeat(64),
        baseUrl: String = "$HTTPS_BASE/",
    ): String? = UpdateSourcePolicy.rejectionFor(url = url, sha256 = sha256, baseUrl = baseUrl)

    private companion object {
        const val HTTPS_BASE = "https://q-musichub.duckdns.org"
    }
}
