# MediaPlayer Android

Kotlin + Jetpack Compose client for the MediaPlayer backend. Streams audio
from a self-hosted Spring Boot catalog via Media3 ExoPlayer.

## Stack

- Kotlin 2.1 with the Compose Compiler Gradle plugin
- Android Gradle Plugin 8.7.3, Gradle 8.11
- `compileSdk` 35, `minSdk` 24, `targetSdk` 35
- Compose BOM 2024.12.01 (Material 3, Material icons extended, activity-compose)
- Retrofit 2.11 with the first-party `converter-kotlinx-serialization`
  (Jake Wharton's port is no longer needed since 2.10)
- OkHttp 4.12 with a logging interceptor on debug builds
- Coil 3 (`coil-compose` + `coil-network-okhttp`) sharing the OkHttpClient via
  `SingletonImageLoader.Factory`
- Media3 1.5.1 (`exoplayer`, `session`, `datasource-okhttp`) — single shared
  OkHttp stack all the way down
- Navigation Compose 2.8.5 — four-destination graph with bottom-nav
  (Search, Find, Playlists, Playlist detail)
- Version catalog in `gradle/libs.versions.toml`

## Module layout

Single `:app` module. Package `com.mediaplayer.android`:

```
app/src/main/kotlin/com/mediaplayer/android/
├── MediaPlayerApp.kt          // Application; wires Coil + binds PlayerConnection
├── MainActivity.kt            // AppScaffold: NavHost + bottom-nav + mini-player
├── data/
│   ├── Network.kt             // Retrofit + OkHttp + JSON singleton
│   ├── MediaPlayerApi.kt      // Retrofit interface (songs + playlists + requests)
│   ├── SongRepository.kt      // thin façade over the songs API
│   ├── PlaylistRepository.kt  // thin façade over the playlists API
│   ├── FindRepository.kt      // thin façade over /api/requests (M9c)
│   └── dto/
│       ├── SongDto.kt             // @Serializable song mirror
│       ├── PageResponse.kt        // generic page envelope
│       ├── PlaylistDto.kt         // list summary
│       ├── PlaylistDetailDto.kt   // full payload (ordered songs)
│       ├── PlaylistRequests.kt    // Create/Rename/Add/Reorder request bodies
│       └── RequestDto.kt          // RequestStatus, CandidateDto, RequestDto (M9c)
├── playback/
│   ├── MediaPlaybackService.kt  // MediaSessionService owning ExoPlayer
│   ├── PlayerConnection.kt      // async MediaController binder (singleton)
│   ├── PlayerCache.kt           // process-singleton SimpleCache (1 GiB LRU) (M10)
│   ├── PrefetchOrchestrator.kt  // warms prev/next neighbours on Wi-Fi (M10)
│   └── PlaybackViewModel.kt     // Compose StateFlows + controls (queue-aware)
└── ui/
    ├── theme/Theme.kt
    ├── search/
    │   ├── SearchScreen.kt         // also hosts add-to-playlist sheet + snackbar
    │   ├── SearchViewModel.kt
    │   └── SongRow.kt              // combinedClickable (tap + long-press)
    ├── playlists/
    │   ├── PlaylistsScreen.kt      // LazyColumn + FAB + create dialog
    │   ├── PlaylistsViewModel.kt
    │   ├── PlaylistDetailScreen.kt // header + Play + track rows
    │   ├── PlaylistDetailViewModel.kt
    │   └── AddToPlaylistSheet.kt   // bottom sheet: pick existing or create
    ├── find/
    │   ├── FindScreen.kt           // query → Albums/Singles picker → status header
    │   └── FindViewModel.kt        // polls /api/requests/{id} until terminal
    └── player/
        ├── MiniPlayer.kt        // persistent bar (with shared Cover)
        └── NowPlayingSheet.kt   // full-screen modal bottom sheet
```

Split into `:app + :core` later if M6/M7 start pulling pure code (DTOs, API
definitions) out for reuse.

## Running it

### 1. Start the backend

See [../backend/README.md](../backend/README.md). TL;DR:

```bash
cd ../backend
docker compose up -d        # Postgres
./mvnw spring-boot:run      # app on :8080
curl -X POST http://localhost:8080/api/admin/import
```

### 2. Open the app in Android Studio

- Open the `android/` folder as a project (not the repo root).
- First sync will download AGP, Kotlin, Compose, Media3, etc.
- Gradle wrapper JAR isn't committed (see `.gitignore`) — Android Studio
  writes it during sync. Or regenerate manually:
  ```
  gradle wrapper --gradle-version 8.11.1
  ```

### 3. Run on the emulator

No config needed. The debug `BASE_URL` defaults to `http://10.0.2.2:8080`,
which is the Android emulator's special alias for the host machine's
loopback. Cleartext to that host is permitted via
`res/xml/network_security_config.xml`.

### 4. Run on a physical device

Debug builds fall back to `http://10.0.2.2:8080` (emulator loopback), which
won't work on a real device. Point it at your machine's LAN IP instead — two
ways, whichever is more convenient:

**Per-build override** — pass on the command line:

```
./gradlew :app:installDebug -PBASE_URL_DEBUG=http://192.168.1.42:8080
```

(`-PBASE_URL=...` without the `_DEBUG` / `_RELEASE` suffix works too and
applies to both variants.)

**Per-machine default** — add to `android/local.properties` (gitignored):

```properties
base.url.debug=http://192.168.1.42:8080
```

Make sure the device and machine share a network and nothing firewalls :8080.
Cleartext to `10.0.2.2` and `localhost` is whitelisted in
`res/xml/network_security_config.xml`; other hosts require HTTPS.

### 5. Release builds

Release builds have **no implicit fallback** — `BASE_URL` must be provided
explicitly, or `assembleRelease` / `bundleRelease` fails before packaging.
The guard is wired up as a `releaseUrlCheck` Gradle task that only runs on
release assembly, so debug builds are unaffected.

Pick whichever source is appropriate:

```
./gradlew :app:assembleRelease -PBASE_URL_RELEASE=https://media.example.com
```

…or in `android/local.properties`:

```properties
base.url.release=https://media.example.com
```

Resolution order (first match wins):

1. `-PBASE_URL_DEBUG=...` / `-PBASE_URL_RELEASE=...` on the Gradle CLI
2. `-PBASE_URL=...` on the Gradle CLI (applies to both variants)
3. `base.url.debug=...` / `base.url.release=...` in `local.properties`
4. `base.url=...` in `local.properties` (applies to both variants)
5. Debug: `http://10.0.2.2:8080`. Release: no fallback — build fails.

## Search UX

- Query is debounced 300ms before hitting the backend.
- Empty query returns the full catalog (default pagination, 20 rows).
- `flatMapLatest` cancels any in-flight request when the query changes.
- Rows show title, "artist · album", a duration stamp, and an AsyncImage
  cover pulled from `/api/songs/{id}/cover` (falls back to a music-note icon
  when the song has no cover).
- Tapping a row starts playback.
- Long-pressing a row opens an "Add to playlist" bottom sheet: pick an
  existing playlist or create a new one in a single step. Success shows
  a transient snackbar ("Added to &lt;playlist&gt;").

## Playback UX

- A **mini-player** pins below the search list whenever a track is loaded:
  cover, title, artist, play/pause, and a 2dp linear progress bar.
- Tapping the mini-player expands a **Now Playing** modal bottom sheet: big
  cover, title, artist · album, a scrubbable seek bar (commits on release),
  and a large play/pause button.
- Playback is backed by a **`MediaSessionService` foreground service**, so
  audio keeps running when the Activity dies and the OS mounts lock-screen
  and notification media controls automatically.
- Media bytes flow through the same OkHttp client as catalog + cover-art
  traffic (via `OkHttpDataSource`), so `Range:` requests and connection
  pooling Just Work.
- Playing a song from the search list uses a single-track queue; playing
  from a playlist loads the full ordered list as the queue. Skip
  forward / back only appear in the Now Playing sheet when the queue
  has neighbours — single-track playback stays minimal.

## Permissions

Declared in `AndroidManifest.xml`:

- `INTERNET`, `ACCESS_NETWORK_STATE` — HTTP calls
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` — background
  audio
- `POST_NOTIFICATIONS` — API 33+ media notification
- `WAKE_LOCK` — keep audio pipeline alive

Notification permission is a runtime prompt; denying it doesn't stop
playback, just hides the media notification.

## Playlists UX

- Bottom-nav tabs: **Search**, **Find**, and **Playlists**. The mini-player
  sits directly above the nav bar when a track is loaded.
- Playlists tab lists every playlist with song count + trailing delete.
  `+ New playlist` FAB opens an inline create dialog.
- Tap a playlist → detail screen: header shows the name, song count, and
  a big **Play** button that queues the whole playlist. Tap any row in
  the list to start from that index instead.
- The Playlists tab stays lit even when drilled into detail.
- Duplicates are allowed inside a playlist (backend-enforced ordering),
  so the UI composes row keys on `(index, songId)`.

## Find new music (M9c)

The **Find** tab lets the user fill catalog gaps through the backend's
torrent-indexer → AllDebrid pipeline:

1. Type a query (artist / album / track) and hit Search. The backend
   synchronously queries Prowlarr and returns a `RequestDto` in the
   `AWAITING_SELECTION` state with a list of candidates.
2. Candidates are grouped into **Albums** and **Singles** tabs. The
   backend's heuristic `UNKNOWN` bucket falls into Albums — music
   torrents are overwhelmingly releases, and biasing toward Albums
   matches user expectation.
3. Each row shows title, seeder count, human-readable size, indexer,
   and (when known) track count.
4. Tapping a row calls `POST /api/requests/{id}/select`; the request
   flips to `UNLOCKING` and the backend's scheduled orchestrator takes
   over. The view model polls `GET /api/requests/{id}` every 2 seconds
   and re-renders the status header (`UNLOCKING` → `DOWNLOADING` →
   `IMPORTED` / `IMPORTED_PARTIAL`) until a terminal state is reached.
5. A back arrow clears the request from the UI and returns to the query
   field — the backend row is kept (for audit / troubleshooting) and
   can be discarded separately via `DELETE /api/requests/{id}`.

The whole feature is gated on backend configuration: when
`PROWLARR_API_KEY` or `ALLDEBRID_API_KEY` are unset, `POST /api/requests`
fails with a clear error that surfaces as the `Error` state.

## Offline cache + prefetch (M10)

Audio bytes flow through a disk-backed `CacheDataSource` so seeks, re-plays,
and queue-backward skips avoid re-hitting the backend.

- **`PlayerCache`** — process-singleton `SimpleCache` under
  `Context.cacheDir/audio-cache`, capped at **1 GiB** with an LRU evictor
  (~25 FLAC albums or ~250 lossy tracks). SimpleCache takes a file lock on
  its database at open time, so we instantiate it exactly once per process
  and never call `release()` — Android reclaims the file descriptors on
  process kill. `Context.cacheDir` means Android will reclaim the space
  itself when the device is tight on storage.
- **`CacheDataSource`** wraps `OkHttpDataSource` inside
  `MediaPlaybackService.onCreate`. `FLAG_IGNORE_CACHE_ON_ERROR` falls
  through to upstream on a corrupt cache entry instead of hard-failing
  playback.
- **`PrefetchOrchestrator`** — a `Player.Listener` that watches
  `onTimelineChanged` / `onMediaItemTransition` and keeps the prev + next
  queue neighbours warm via a background `CacheWriter` on
  `Dispatchers.IO`. Out-of-window jobs are cancelled as the user skips
  around; already-cached ranges are a no-op.
- **Unmetered-only gate** — prefetch is opportunistic bandwidth, so a
  `ConnectivityManager.NetworkCallback` watches
  `NET_CAPABILITY_NOT_METERED`. Wi-Fi / Ethernet → prefetch runs;
  mobile data / metered hotspot → in-flight jobs are cancelled.
  Actual playback (the track the user explicitly started) is unaffected.

The cache survives service teardown and process death; it's keyed off
the stream URL so the same track re-played across sessions is served
from disk.

## Android Auto

The app is an Android Auto media client. `MediaPlaybackService` extends
`MediaLibraryService` and its `MediaLibrarySession.Callback` exposes a
browse tree the car head unit can walk.

Tree shape:

```
root
├── all-songs                     first page of the catalog (50 items)
└── playlists
    └── playlist:{id}             ordered songs (duplicates allowed)
```

Tapping a song under `/all-songs` plays that one track; tapping inside a
playlist expands into the full playlist queue at the right start index —
same semantics as `PlaybackViewModel.playPlaylist` on phone. Voice
(`"Hey Google, play Queen on MediaPlayer"`) hits `onSearch` and proxies
straight through to `/api/songs?q=`. Cover art URIs resolve against
`/api/songs/{id}/cover`.

Cold car connect shows a "resume where you left off" chip via
`onPlaybackResumption`. The last queue + index + position are
checkpointed by `PlaybackResumption` (SharedPreferences-backed
`Player.Listener`) and rebuilt into playable `MediaItem`s on demand.

### Testing with the Desktop Head Unit (DHU)

DHU is Google's AA simulator — much faster than plugging into a real
car for every iteration.

1. In Android Studio's SDK Manager → SDK Tools, install **Android Auto
   Desktop Head Unit Emulator**. The DHU binaries land at
   `%ANDROID_HOME%\extras\google\auto\desktop-head-unit.exe`.
2. On the phone: enable developer mode in the *Android Auto* app
   (tap the version number in About 10 times), then toggle **Head Unit
   Server**. Plug the phone in via USB.
3. On the host PC, forward the AA port:

   ```bat
   adb forward tcp:5277 tcp:5277
   ```

4. Launch DHU:

   ```bat
   %ANDROID_HOME%\extras\google\auto\desktop-head-unit.exe
   ```

5. MediaPlayer should appear in the car launcher. If not, verify the
   `com.google.android.gms.car.application` meta-data is present in the
   installed APK (`aapt dump xmltree`) and that the APK you installed is
   actually a debug build that matches the phone.

The app doesn't need HTTPS to talk to the backend from DHU, but a real
head unit won't reach `10.0.2.2` — it'll hit the production `BASE_URL`
baked into a release build. The existing `releaseUrlCheck` Gradle guard
already forces that URL to be set.

## Status

All planned milestones shipped. See [../README.md](../README.md) for the
full cross-repo roadmap.
