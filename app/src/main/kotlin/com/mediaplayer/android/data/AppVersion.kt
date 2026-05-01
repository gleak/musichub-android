package com.mediaplayer.android.data

/**
 * App-wide version + changelog source of truth.
 *
 * INVARIANT: every new user-visible feature MUST bump [VERSION] and prepend a
 * matching [ChangelogEntry] at the top of [Changelog.entries]. Bug-fix-only
 * commits do not require a bump. Keep [VERSION] aligned with `versionName`
 * in `app/build.gradle.kts` — the Gradle value drives Play Store metadata,
 * this constant drives the in-app changelog gate.
 */
object AppVersion {
    const val VERSION = "0.3.2"
}

data class ChangelogEntry(
    val version: String,
    val title: String,
    val highlights: List<String>,
)

object Changelog {
    val entries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            version = "0.3.2",
            title = "Bug fixes — like, video download, offline flag",
            highlights = listOf(
                "Like button responds instantly when tapped instead of looking stuck",
                "Video downloads run in the background and finish reliably — no more duplicate downloads when the request times out",
                "Offline icon stops showing while the network actually works",
            ),
        ),
        ChangelogEntry(
            version = "0.3.1",
            title = "Made for you — Discover Daily + On Repeat",
            highlights = listOf(
                "Server-curated Discover Daily and On Repeat playlists now appear on Home and in Your Library",
                "Distinct gradient cover so auto-playlists stand out from your hand-curated ones",
                "Auto-playlists can't be deleted from the library list (they're managed by the server)",
            ),
        ),
        ChangelogEntry(
            version = "0.3.0",
            title = "Welcome sheet + cover rise + design tokens",
            highlights = listOf(
                "First-launch welcome sheet — separate from the changelog returning users see",
                "Notification permission requested at the right moment (when you start playing)",
                "Now Playing's hero cover springs in from the mini-player when you tap to expand",
                "New Spacing + Shape tokens replacing magic-number padding — consistent rhythm across mini-player, song rows, and future screens",
            ),
        ),
        ChangelogEntry(
            version = "0.2.2",
            title = "Hardening + broken-song dimming",
            highlights = listOf(
                "Songs missing their audio file are now greyed out on Android Auto instead of failing on tap",
                "Media session only accepts known controllers (Android Auto, Assistant, system, Bluetooth) — other apps can no longer attach",
            ),
        ),
        ChangelogEntry(
            version = "0.2.1",
            title = "Cover art everywhere + polish",
            highlights = listOf(
                "Album, artist, and playlist tiles now show cover art on Android Auto",
                "Album track lists carry track numbers (1/12, 2/12…)",
                "Voice search and All Songs no longer truncate at 50 results — paging works in the car",
                "Lyrics removed from the browse tree (they belong on the now-playing card)",
                "Haptic tap on like, in the mini-player, Now Playing, and song rows",
                "Now Playing's action row decluttered — Re-download / Mark broken / Save as alarm moved into a ⋮ overflow menu",
                "Friendlier error messages on Home, Search, Liked, Playlists, Find — no more raw stack traces",
                "Library tab stays highlighted when you're inside any sub-route (albums, artists, find…)",
            ),
        ),
        ChangelogEntry(
            version = "0.2.0",
            title = "Guest mode + Android Auto sleep timer",
            highlights = listOf(
                "Continue as guest — use the app without signing in; data lives under your device",
                "Sign-in upgrade banner so you know when you're a guest",
                "Avatar now shows your initial when signed in (or a guest icon)",
                "Like button added to Now Playing and the mini-player",
                "Sleep timer reachable from Android Auto's now-playing card",
                "Phone and Android Auto now share one sleep timer — set it anywhere, see it everywhere",
                "Loading and error states unified across screens",
            ),
        ),
        ChangelogEntry(
            version = "0.1.1",
            title = "Inline video + slow-stream fix",
            highlights = listOf(
                "Music videos now play inline where the cover art sits, not fullscreen",
                "Tap the fullscreen icon in the controller to expand to fullscreen",
                "Removed the 30s read timeout that aborted first-time video streams while the server fetches them",
            ),
        ),
        ChangelogEntry(
            version = "0.0.1",
            title = "Initial release",
            highlights = listOf(
                "Google Sign-In via Credential Manager with silent re-auth",
                "Media3 ExoPlayer playback with foreground MediaSessionService",
                "Home, Search, and Library tabs with mini-player + Now Playing sheet",
                "Liked Songs, Albums, and Artists pages",
                "Playlist management — create, reorder, swipe-to-delete, add songs",
                "Spotify playlist import via Exportify CSV",
                "YouTube-based search and direct download UI",
                "Find new music tab with daily discovery",
                "Recently played history",
                "Synced lyrics sheet",
                "10-band equalizer with explicit downloads",
                "Disk cache + unmetered prev/next prefetch",
                "Pull-to-refresh on major screens",
                "Android Auto browse tree, voice search, lyrics, and resumption",
                "Firebase Crashlytics + Auth integration",
                "Offline badge when no network",
                "Sleep timer and queue management",
                "Album / artist navigation from search results",
                "Ringtone export from current track",
                "Video player sheet for tracks with video",
            ),
        ),
    )
}
