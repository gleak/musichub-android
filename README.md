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
│   ├── MediaPlayerApi.kt      // Retrofit interface (songs + playlists + liked + requests + catalog)
│   ├── SongRepository.kt      // thin façade over the songs API
│   ├── PlaylistRepository.kt  // thin façade over the playlists API
│   ├── LikedRepository.kt     // thin façade over /api/liked (M11a)
│   ├── CatalogRepository.kt   // thin façade over /api/albums + /api/artists (M11b)
│   ├── FindRepository.kt      // thin façade over /api/requests (M9c)
│   ├── HistoryRepository.kt   // thin façade over /api/history (M11c)
│   ├── LyricsRepository.kt    // thin façade over /api/songs/{id}/lyrics (M12)
│   └── dto/
│       ├── SongDto.kt             // @Serializable song mirror
│       ├── PageResponse.kt        // generic page envelope
│       ├── PlaylistDto.kt         // list summary
│       ├── PlaylistDetailDto.kt   // full payload (ordered songs)
│       ├── PlaylistRequests.kt    // Create/Rename/Add/Reorder request bodies
│       ├── RequestDto.kt          // RequestStatus, CandidateDto, RequestDto (M9c)
│       ├── AlbumDto.kt            // AlbumDto + AlbumDetailDto (M11b)
│       ├── ArtistDto.kt           // ArtistDto + ArtistDetailDto (M11b)
│       └── LyricLineDto.kt        // positionMs + text (M12)
├── playback/
│   ├── MediaPlaybackService.kt  // MediaSessionService owning ExoPlayer
│   ├── PlayerConnection.kt      // async MediaController binder (singleton)
│   ├── PlayerCache.kt           // process-singleton SimpleCache (1 GiB LRU) (M10)
│   ├── PrefetchOrchestrator.kt  // warms prev/next neighbours on Wi-Fi (M10)
│   ├── PlaybackViewModel.kt     // Compose StateFlows + controls (queue, shuffle, repeat)
│   └── SleepTimer.kt            // coroutine-based pause timer (M12)
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
    ├── liked/
    │   ├── LikedScreen.kt          // header + Play + heart-toggled song rows
    │   └── LikedViewModel.kt       // optimistic unlike, newest-first list
    ├── albums/
    │   ├── AlbumListScreen.kt      // paginated album list + VM (M11b)
    │   └── AlbumScreen.kt          // album detail + VM: header + track rows (M11b)
    ├── artists/
    │   ├── ArtistListScreen.kt     // paginated artist list + VM (M11b)
    │   └── ArtistScreen.kt         // artist detail + VM: albums + songs (M11b)
    ├── find/
    │   ├── FindScreen.kt           // query → Albums/Singles picker → status header
    │   └── FindViewModel.kt        // polls /api/requests/{id} until terminal
    └── player/
        ├── MiniPlayer.kt        // persistent bar (with shared Cover)
        ├── NowPlayingSheet.kt   // full-screen sheet: transport + shuffle/repeat + controls
        ├── QueueSheet.kt        // current queue list, tap to skip (M12)
        └── LyricsSheet.kt       // synced lyrics, auto-scroll to active line (M12)
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
- Each row shows a heart icon. Tapping it likes/unlikes the track with
  an optimistic UI update (instant visual feedback, rolls back on error).
  Liked state is loaded in bulk via `GET /api/liked/status` after each
  search fetch — one extra round-trip per search, not one per row.
- Artist and album text in each row are individually tappable (tinted
  primary colour): artist → artist detail page, album → album detail page.
- When the search field is empty, the screen shows **Albums** and **Artists**
  browse tiles with "See all" buttons instead of a plain placeholder message.

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

## Liked Songs (M11a)

- The **Playlists** tab pins a "Liked Songs" tile at the top (heart icon
  with primary colour background). Tapping it opens the **Liked Songs
  screen**: a header with a Play button and a `LazyColumn` of songs
  ordered newest-liked-first.
- Each song row in the Liked Songs screen shows a filled heart; tapping
  it unlikes the track and removes it from the list optimistically.
- Heart icons also appear on every `SongRow` in the Search tab. The liked
  state is loaded in bulk after each search result arrives, so it's always
  in sync with no per-row round-trips.

## Album + Artist pages (M11b)

- When the search field is **empty**, the Search tab shows two browse rows —
  **Albums** and **Artists** — each with a "See all" button.
- **Albums list** (`GET /api/albums`) — paginated, filterable. Each row shows
  album name, artist, and song count. Tapping opens the album detail screen.
- **Album detail** — header tile (name, artist as a tappable link to the artist
  page, song count, Play button), followed by the ordered track list.
- **Artists list** (`GET /api/artists`) — same pattern with a person icon and
  album + song count subtitle.
- **Artist detail** — circular avatar header (Play all button), Albums
  sub-section (tappable tiles), Songs sub-section.
- Artist/album text in every `SongRow` is individually clickable (primary
  tint) and navigates directly to the respective detail page.

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

All planned milestones shipped through M11b. See [../README.md](../README.md)
for the full cross-repo roadmap.

## Mockup audit

`.planning/mockup-audit/REPORT.md` lists every place the shipped UI drifts
from the MusicHub design contract (`mockup/*.jsx`). Each area has a per-file
detail doc — mark items resolved there as the rebrand lands.

| # | Area                                              | Detail                                          | Status                |
|---|---------------------------------------------------|-------------------------------------------------|-----------------------|
| 1 | Auth & first-run                                  | [01-auth.md](.planning/mockup-audit/01-auth.md) | **Done** (post-v0.13.3) |
| 2 | Discover & Spotify import                         | [02-discover.md](.planning/mockup-audit/02-discover.md) | Pending               |
| 3 | Library drilldowns + sharing                      | [03-library.md](.planning/mockup-audit/03-library.md) | Pending               |
| 4 | Player sheets / dialogs                           | [04-player-sheets.md](.planning/mockup-audit/04-player-sheets.md) | Pending               |
| 5 | Profile / settings sub-pages                      | [05-settings.md](.planning/mockup-audit/05-settings.md) | Pending               |
| 6 | App-update + changelog + event-queue              | [06-update.md](.planning/mockup-audit/06-update.md) | Pending               |
| 7 | Ringtone trim editor                              | [07-trim.md](.planning/mockup-audit/07-trim.md) | **Done** (v0.16.1)    |
| 8 | Android Auto extras                               | [08-auto-extra.md](.planning/mockup-audit/08-auto-extra.md) | Pending               |
| 9 | Core screens parity                               | [09-core-screens.md](.planning/mockup-audit/09-core-screens.md) | Pending               |

### Area 1 — Auth & first-run

Brand contract from `mockup/mh-auth.jsx` + state contract from
`mockup/mh-auth-states.jsx` are both implemented:

- `LoginScreen.kt` — equalizer-bars monogram, white pill with multi-color
  Google G, lime-tinted radial gradient, italian copy + T&C footer, boxed
  red error panel with categorized mono code (`auth/network-error` /
  `auth/server-rejected` / `auth/google-rejected` / `auth/unknown`, mapped
  by `AuthViewModel.classifyAuthError`), signing-in button-label swap with
  `auth/google · credential-exchange` diagnostic, soft `auth/picker-cancel`
  toast on Google picker dismiss.
- `OnboardingScreen.kt` — 12 italian-cased pill cloud (FlowRow), `// PASSO 1 / 1`
  eyebrow, footer row with thin top divider + mono counter, dashed-look
  ghost lime CTA "Scegli ancora N" below threshold, dimmed grid + spinner
  CTA + "SALVATAGGIO…" counter while saving, red error band with retry pill
  + `onboarding/seed-genres` code on save failure.
- `OnboardingSheet.kt` — lime-tinted gradient sheet, custom drag handle,
  `// BENVENUTO IN MUSICHUB` eyebrow, "La tua libreria, il tuo ritmo."
  tagline, three lime-tile feature rows, single "Inizia" pill CTA.
- `ProfileScreen.kt#AccountSwitchDialog` — custom `Dialog` (not stock
  `AlertDialog`) with `// CAMBIA ACCOUNT` eyebrow, account-preview row
  (gradient avatar + email + "Account corrente" mono caption), side-by-side
  pill buttons (Annulla subtle + Disconnetti red filled), cloud-sync warning
  copy.
- `AuthProbeScreen.kt` — brand-locked splash for the initial silent token
  probe with Token / Me / RejectedSilent stages: lime (or red, when muted)
  radial gradient, equalizer monogram, animated progress strip, mono
  diagnostic line (`auth/token-refresh` / `auth/refresh-me` /
  `auth/token-rejected · clear`).
- `AuthViewModel.kt` — `State.Probe(stage)` and `State.SigningIn` split out
  from the old single `Loading`; `pickerCancelled: SharedFlow<Unit>` emits
  on Google picker dismiss; rejected-token path flashes the splash for
  ~900ms before falling back to `NotSignedIn`.

### Area 7 — Modalità Taglio (ringtone trim editor)

Implements `mockup/mh-trim.jsx` end-to-end. Reachable from Now Playing →
overflow → **Taglia traccia…**:

- `ui/trim/TrimScreen.kt` — full-screen editor: top bar (X / `// MODALITÀ ·
  TAGLIO` lime eyebrow / lime `Salva` pill), track header with cover, **01 ·
  ASCOLTO** preview card (96-bar pseudo-random waveform mask, dashed lime
  IN/OUT region overlay, yellow `#FFC857` playhead with flag head, mono
  playhead chip, transport row of 5 buttons), **02 · TAGLIO** trim card
  (mini lime-masked waveform, 8dp lime active bar, two draggable handles —
  picks the closest one on press-down so the cursor doesn't hop —, two
  `NudgeBox`es with mono `±1s` / `±.1` quad), Fade in/out pill, Risultato
  card showing window duration + amount cut, saved/error toast pinned to
  the bottom edge, hint footer.
- `ui/trim/TrimViewModel.kt` — IN/OUT cursors with `MIN_WINDOW_MS = 1000`
  guard, fade-toggle state, `save()` round-trip to the backend cut endpoint
  with `friendlyMessage`-shaped error fallback.
- Preview audio reuses the host `PlaybackViewModel` instead of spawning a
  second ExoPlayer — scrubbing on the waveform, `Vai a IN/OUT`, and `±5s`
  all map onto `MediaController.seekTo`. The lockscreen / Auto / mini-player
  surfaces stay in sync with the editor cursor.
- Backend: `POST /api/songs/{id}/cut` (`SongCutService` + `CutSongRequest`).
  Runs `ffmpeg -i src -ss <inSec> -to <outSec> -c copy …` so the cut snaps
  to the nearest MP3 frame boundary (~26ms) without re-encoding. The new
  `Song` row gets a `(cut)` title suffix, copies the cover bytes (so
  relocating one row doesn't break the other), and ports lyric lines whose
  `position_ms` lands inside the window with positions shifted by `-inMs`.
  Dedup by `content_hash` — saving the same window twice returns the
  existing row instead of failing on the unique constraint.

All four mockup pills are now live (v0.16.1):

- **Long-press ×8 zoom** — long-press a handle and the timeline narrows to
  1/8 of the song centered on that handle. The focused handle paints in
  goldenrod and a `ZOOM ×8 · IN/OUT` badge floats top-right of the trim
  card; tap the badge or anywhere outside the handle to exit zoom.
- **Anteprima A/B** — pill that flips a loop flag in `TrimViewModel`. A
  `LaunchedEffect` watching `playbackVm.positionMs` seeks back to IN
  whenever the playhead crosses OUT, so the user hears the cut on repeat
  without manually scrubbing.
- **Aggancia al silenzio** — `TrimViewModel.snapToSilence(waveform)` walks
  ±4s around each handle and pulls IN/OUT to the lowest waveform bar
  inside that window. The waveform seed is `song.id` so the snap is stable
  per track.
- **Replace-in-playlists Yes/No** — backend ships `POST /api/playlists/replace-song`
  (`PlaylistRepository.replaceSongInAccessiblePlaylists` does the bulk
  `UPDATE` with a duplicate guard). The `Saved` toast renders inline `Sì / No`
  CTAs matching the mockup copy; `Sì` flips the VM into `Replacing` and
  shows a spinner inside the toast badge until the swap completes, then
  pops the editor with the new master.
