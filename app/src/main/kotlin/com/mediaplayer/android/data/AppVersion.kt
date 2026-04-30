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
    const val VERSION = "0.0.1"
}

data class ChangelogEntry(
    val version: String,
    val title: String,
    val highlights: List<String>,
)

object Changelog {
    val entries: List<ChangelogEntry> = listOf(
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
