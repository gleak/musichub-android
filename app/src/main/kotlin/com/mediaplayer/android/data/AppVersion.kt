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
    const val VERSION = "0.2.1"
}

data class ChangelogEntry(
    val version: String,
    val title: String,
    val highlights: List<String>,
)

object Changelog {
    val entries: List<ChangelogEntry> = listOf(
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
