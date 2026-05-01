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
    const val VERSION = "0.9.2"
}

data class ChangelogEntry(
    val version: String,
    val title: String,
    val highlights: List<String>,
)

object Changelog {
    val entries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            version = "0.9.2",
            title = "UI polish — empty states + clearer dialogs",
            highlights = listOf(
                "Empty Liked Songs, Playlists, Up next and song-picker screens now show a Spotify-style icon + message instead of a bare line of text",
                "Song-list loading now animates a shimmer skeleton instead of a centered spinner",
                "Now Playing's overflow menu disambiguates the two redownload paths — \"Re-download from source\" rebuilds the file on the server, \"Refresh local copy\" only redownloads to your phone",
                "Tightened typography in Find, Spotify import and song rows — fewer one-off font weights",
            ),
        ),
        ChangelogEntry(
            version = "0.9.1",
            title = "Update channel + manual check",
            highlights = listOf(
                "The server now hosts a folder of APKs and surfaces the highest version automatically — no operator restart needed when shipping a new build",
                "Settings menu has a new \"Check for updates\" entry for an on-demand check",
            ),
        ),
        ChangelogEntry(
            version = "0.9.0",
            title = "In-app updates",
            highlights = listOf(
                "MediaPlayer now updates itself — when a new build is published to the server, the app prompts you to download and install on next launch",
                "No Play Store needed; integrity check via SHA-256 keeps the update tamper-proof",
            ),
        ),
        ChangelogEntry(
            version = "0.8.2",
            title = "Tap artist in Now Playing",
            highlights = listOf(
                "Tap the artist name on the Now Playing screen to jump straight to the artist's page",
            ),
        ),
        ChangelogEntry(
            version = "0.8.1",
            title = "Artists + Albums no longer cap at 100",
            highlights = listOf(
                "Artists and Albums lists now scroll to load more — large catalogs are no longer truncated",
            ),
        ),
        ChangelogEntry(
            version = "0.8.0",
            title = "Daily Mix — six mood-themed playlists",
            highlights = listOf(
                "Six Daily Mix playlists, each themed by a genre/mood drawn from your listening history",
                "Catalog songs are now classified by genre tag in the background, with backfill for older tracks",
                "Each Daily Mix is named after its theme (Daily Mix 2 — Hip-hop, etc.) and refreshes once a day",
            ),
        ),
        ChangelogEntry(
            version = "0.7.0",
            title = "Follow artists + Release Radar",
            highlights = listOf(
                "Tap the bell on any artist page to follow them",
                "Release Radar auto-playlist surfaces new tracks from the artists you follow — refreshes daily and immediately when you follow someone new",
            ),
        ),
        ChangelogEntry(
            version = "0.6.0",
            title = "Share playlists",
            highlights = listOf(
                "Tap the share icon on any playlist to mint a link and send it through any app on your phone",
                "Recipients tap the link to land directly in MediaPlayer with a one-tap import preview",
                "Imported playlists are independent copies — the sender's edits don't change yours",
            ),
        ),
        ChangelogEntry(
            version = "0.5.3",
            title = "True fullscreen video",
            highlights = listOf(
                "Fullscreen video now hides the status + navigation bars — swipe from the edge to reveal them",
            ),
        ),
        ChangelogEntry(
            version = "0.5.2",
            title = "Equalizer actually works",
            highlights = listOf(
                "Equalizer presets and band sliders now affect playback — fixed an audio-session binding issue that left the effect silently disabled",
            ),
        ),
        ChangelogEntry(
            version = "0.5.1",
            title = "Queue holds order under shuffle",
            highlights = listOf(
                "Songs you Add to queue / Play next now keep their insertion order even when shuffle is on",
                "Toggling shuffle reorders only the upcoming source — your queue and listening history stay put",
            ),
        ),
        ChangelogEntry(
            version = "0.5.0",
            title = "Spotify-style queue",
            highlights = listOf(
                "Up next sheet now splits into Now playing / Next in queue / Next up — manually-queued songs are tracked separately from the source",
                "Add to queue and Play next add to the user queue and play before the source resumes",
                "User-queued songs are consumed once played — they don't linger in the source list",
            ),
        ),
        ChangelogEntry(
            version = "0.4.9",
            title = "Row menu everywhere",
            highlights = listOf(
                "⋮ menu now appears on every song row — Search, Liked, Album, Artist, Playlist, and the Up next queue all open Add to playlist / Play next / queue / Download from one place",
            ),
        ),
        ChangelogEntry(
            version = "0.4.8",
            title = "Visible row menu in Liked Songs",
            highlights = listOf(
                "Each row in Liked Songs now has a visible ⋮ button that opens Add to playlist / Play next / queue (long-press still works too)",
            ),
        ),
        ChangelogEntry(
            version = "0.4.7",
            title = "Liked Songs — full library + real count",
            highlights = listOf(
                "Liked Songs no longer caps at 20 — scroll to load more, all the way down",
                "Header now shows the real total count instead of just what's loaded",
            ),
        ),
        ChangelogEntry(
            version = "0.4.6",
            title = "Remove from queue",
            highlights = listOf(
                "Up next sheet now has a remove (×) button on every queued track",
            ),
        ),
        ChangelogEntry(
            version = "0.4.5",
            title = "Swipe-to-remove fix + remove confirmation",
            highlights = listOf(
                "Swipe-to-remove inside a playlist now reliably commits — no more silent no-ops on partial swipes",
                "A snackbar now confirms when a song is removed from a playlist",
            ),
        ),
        ChangelogEntry(
            version = "0.4.4",
            title = "Add to playlist from Liked + safe-area onboarding",
            highlights = listOf(
                "Long-press a song in Liked Songs to add it to a playlist (or queue / play next via the same sheet)",
                "Onboarding tag picker no longer hides its Skip / Continue buttons under the gesture nav bar",
            ),
        ),
        ChangelogEntry(
            version = "0.4.3",
            title = "Library grid + cover transition + reorder polish",
            highlights = listOf(
                "Your Library now shows playlists as a 2-column grid of tiles, Spotify-style",
                "Tapping the mini-player expands its cover into the Now Playing screen with a smooth shared-element transition",
                "Reordering songs inside a playlist now animates instead of snapping",
            ),
        ),
        ChangelogEntry(
            version = "0.4.2",
            title = "Polish pass — covers, motion, contrast",
            highlights = listOf(
                "Song titles in lists are easier to read at a glance — lighter, larger weight",
                "Now Playing slides up with a soft spring instead of a hard pop",
                "Play button on detail screens responds to your tap with a quick scale",
                "Now Playing controls stay readable on light album covers",
                "Mini-player progress bar finally visible against the dark background",
                "Retry button added on Liked, Album, Artist and Playlist load failures",
            ),
        ),
        ChangelogEntry(
            version = "0.4.1",
            title = "Cold-start CTAs + Search/Find retry",
            highlights = listOf(
                "Empty Home now offers Find new music and Import Spotify shortcuts instead of leaving you at a wall",
                "Search and Find errors finally show a Retry button",
                "Android Auto stops flashing broken cover slots when the head unit can't reach the home network",
                "Shuffle and Repeat advertised explicitly to Android Auto so the car overlay buttons appear reliably",
            ),
        ),
        ChangelogEntry(
            version = "0.4.0",
            title = "Tag picker + Made for you in the car",
            highlights = listOf(
                "First-run tag picker — pick the genres you listen to so we can suggest tracks you'll like",
                "Made for you tile in Android Auto's browse — Discover Daily and On Repeat reachable from the car",
            ),
        ),
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
