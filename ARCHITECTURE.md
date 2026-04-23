# Android architecture

Living doc for the MediaPlayer Android app. Update alongside each milestone.

## Current state (Milestone 10 complete)

### Build

- Kotlin 2.1 with `org.jetbrains.kotlin.plugin.compose` (single-plugin Compose
  compiler — the old `composeOptions { kotlinCompilerExtensionVersion }`
  block is gone).
- AGP 8.7.3 on Gradle 8.11.1.
- Version catalog at `gradle/libs.versions.toml`; everything else in
  `settings.gradle.kts` / `app/build.gradle.kts` refers to it via
  `alias(libs.plugins.*)` and `libs.*`.

### Module layout

Single `:app` module. Package `com.mediaplayer.android`.

- `data/` — Retrofit + OkHttp + kotlinx.serialization. `Network` is the one
  place base URL, JSON config, HTTP client, and Retrofit live. `SongRepository`
  wraps the generated API so the VM depends on code we own.
- `ui/theme/` — Material 3 with Material You dynamic color on Android 12+,
  fallback dark/light otherwise.
- `ui/search/` — debounced query → paginated catalog fetch → Compose
  `LazyColumn` of `SongRow`s. State is a sealed `SearchUiState`. Rows also
  support a long-press that opens the shared "add to playlist" sheet.
- `ui/playlists/` — playlists list, playlist detail, and the
  `AddToPlaylistSheet` bottom sheet. VMs own their state
  (`PlaylistsViewModel`, `PlaylistDetailViewModel`) and depend on a
  `PlaylistRepository`.
- `ui/player/` — `MiniPlayer` bar and `NowPlayingSheet` (Material 3 modal
  bottom sheet). Both bind to the same `PlaybackViewModel`.
- `ui/find/` — `FindScreen` + `FindViewModel` for the "Find new music"
  tab (M9c). Polls the backend request state machine until terminal.
- `playback/` — `MediaPlaybackService`, `PlayerConnection` (singleton that
  binds a `MediaController`), `PlaybackViewModel` (Compose-facing facade),
  plus the M10 cache pair: `PlayerCache` (process-singleton `SimpleCache`)
  and `PrefetchOrchestrator` (warms prev/next on Wi-Fi).

No DI framework yet — `Network` is an object, `PlayerConnection` is an
object, and ViewModels construct their own collaborators. Introduce Hilt
if M6/M7 make the graph non-trivial.

### Networking

- `BuildConfig.BASE_URL` is injected per build type. Debug defaults to
  `http://10.0.2.2:8080` (emulator loopback); override with
  `-PBASE_URL=...` for a physical device on the LAN. Release picks a
  placeholder until M7 (no deploy target yet).
- Cleartext is allowed only for `10.0.2.2` and `localhost` via
  `network_security_config.xml`. Everything else stays HTTPS-only.
- `OkHttpLoggingInterceptor` at `BASIC` on debug, off on release.
- One `OkHttpClient` is shared across Retrofit, Coil
  (`MediaPlayerApp : SingletonImageLoader.Factory`), and the ExoPlayer
  `OkHttpDataSource` — one connection pool, one cache, one cert chain.

### JSON

- `Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }`
  so the client survives backward-compatible backend changes (added fields,
  nullable-to-explicit transitions, etc.).
- DTOs (`SongDto`, `PageResponse<T>`) mirror the backend exactly. The
  backend deliberately never emits `filePath`, so the client never has a
  way to even reason about it.

### Search screen

- `SearchViewModel` holds a `MutableStateFlow<String>` for the query.
- The exposed `state: StateFlow<SearchUiState>` is built from the query flow
  via `debounce(300ms) → distinctUntilChanged() → flatMapLatest { emit Loading; emit fetch() }`.
- `flatMapLatest` guarantees that if a user is still typing when an older
  request is in flight, the older one gets cancelled and we never render
  a stale page.
- `SongRow` uses Coil's `AsyncImage`; when `hasCoverArt = false` it falls
  back to a `MusicNote` icon so the row layout is stable. Rows are
  clickable and forward the `SongDto` up to the Activity for playback.

### Playback (M5)

**Architecture: MediaSessionService + MediaController.** The player lives
in a bound foreground service (`MediaPlaybackService`). Everything in the
UI talks to it via a `MediaController` instead of holding an `ExoPlayer`
directly. The upside:

- The OS keeps audio running when the Activity is destroyed (background
  playback, screen-off, task switch).
- Lock-screen and notification media controls wire up automatically — the
  `<intent-filter>` for `androidx.media3.session.MediaSessionService` is
  all we need.
- We get Android Auto / Wear / Bluetooth media buttons for free when we
  decide to care about them.

**`MediaPlaybackService`** owns the `ExoPlayer` and the `MediaSession`.
The player is built with:

- `OkHttpDataSource` wrapping `Network.okHttp` so the audio stream shares
  the same HTTP stack as the rest of the app (range requests, pooling,
  logging). Wrapped in `DefaultDataSource.Factory` so local/asset URIs
  still work if we add them later.
- `AudioAttributes(CONTENT_TYPE_MUSIC, USAGE_MEDIA)` + `handleAudioFocus`
  so the system ducks us for navigation prompts and pauses us on focus
  loss.
- `setHandleAudioBecomingNoisy(true)` — pauses when headphones are
  yanked.

`onTaskRemoved` stops the service only if paused; a playing stream
survives a swipe-away.

**`PlayerConnection`** is a singleton that binds/unbinds the
`MediaController`. The controller handshake is async (`buildAsync()` →
`ListenableFuture`), so we expose `StateFlow<MediaController?>` and null-gate
the UI on it. `connect()` is called once from `MediaPlayerApp.onCreate()`
and is idempotent.

**`PlaybackViewModel`** translates the imperative `Player.Listener` API
into Compose-friendly StateFlows: `currentSong`, `isPlaying`,
`positionMs`, `durationMs`. It doesn't own a player — it subscribes to
whichever `MediaController` `PlayerConnection` currently holds, across
reconnects. Position is poll-driven at 500ms because Media3 doesn't emit
position events.

One `PlaybackViewModel` is activity-scoped via `viewModel()` in
`AppScaffold`, so `SearchScreen`'s row taps, `MiniPlayer`, and
`NowPlayingSheet` all see identical state.

**UX.** Mini-player + full-screen sheet:

- `MiniPlayer` pins below the content whenever `currentSong != null` —
  cover, title, artist, play/pause, and a 2dp progress bar. Click the bar
  to expand.
- `NowPlayingSheet` is a `ModalBottomSheet` (`skipPartiallyExpanded = true`)
  with a 260dp cover, title, artist · album, a seek `Slider`, and a
  72dp play/pause button. Scrub is buffered locally and only pushed to
  the controller on release so dragging is fluid.

**Queue model (M6).** The VM exposes both single-track (`play(song)`)
and multi-track (`playPlaylist(songs, startIndex)`) paths. Behind the
scenes the multi-track path goes to `MediaController.setMediaItems(items,
startIndex, 0L)`, which swaps the whole timeline atomically.
`PlaybackViewModel` watches `Player.Listener.onTimelineChanged` alongside
`onMediaItemTransition` to keep `hasNext` / `hasPrevious` StateFlows fresh
— those drive the visibility of skip buttons in the Now Playing sheet.
Skip-previous delegates to `Player.seekToPrevious`, which ships Media3's
Spotify-style behaviour: within 3s of track start, jump back; otherwise,
restart the current track.

**Stream URL.** Plain `GET /api/songs/{id}/stream` — no signing, no
tokens. The backend is LAN-only in development and auth is a non-goal
for now.

**Permissions.** `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`,
`POST_NOTIFICATIONS` (API 33+), and `WAKE_LOCK`. All declared up-front;
runtime permission for notifications is left to the user — if they deny
it the media notification just doesn't render, playback still works.

### Navigation + playlists (M6)

**Navigation.** `androidx-navigation-compose` 2.8.5 with a tiny
hand-rolled route table (`Routes.SEARCH`, `Routes.FIND`,
`Routes.PLAYLISTS`, `Routes.PLAYLIST_DETAIL`). Plain string constants —
a sealed hierarchy would be ceremony for a four-destination graph.

`MainActivity.AppScaffold` is now a Material 3 `Scaffold` with a
`bottomBar` slot containing an optional `MiniPlayer` stacked above a
`NavigationBar`. The bar has three tabs (Search, Find, Playlists).
Drilling into a playlist (`Routes.PLAYLIST_DETAIL`) keeps the Playlists
tab lit — the selector treats the detail route as "still inside
Playlists".
Tab clicks use the standard `popUpTo(startDestinationId) { saveState }`
+ `launchSingleTop` + `restoreState` combo so switching tabs preserves
list scroll state.

**Playlists list (`PlaylistsScreen`).** `LazyColumn` of
`PlaylistRow`s, each row showing a generic queue icon (playlists don't
have cover art in the data model yet), the name, and a song count.
Trailing delete icon opens a confirm dialog; delete is optimistic —
the row vanishes immediately and we refetch on failure. An
`ExtendedFloatingActionButton` (`+ New playlist`) opens an
`AlertDialog` with a name field, enabled only when the trimmed input
is non-empty.

**Playlist detail (`PlaylistDetailScreen`).** Loaded via a
`viewModel(key = "playlist-$id", factory = ...)` so each playlist id
gets its own VM scoped to the back-stack entry. A top bar carries the
name + back button. The body is a `LazyColumn` with one header item
(96dp icon tile, name, song count, a filled `Play` button) followed by
the track rows. Tapping any track calls `onPlayFromIndex(songs, idx)`
which the Activity wires to `PlaybackViewModel.playPlaylist`. Duplicate
songs are allowed server-side, so row keys are composed as
`"$index-$songId"` rather than the song id alone.

**Add-to-playlist (`AddToPlaylistSheet`).** Long-pressing a `SongRow`
opens a `ModalBottomSheet`. The sheet pins a "+ New playlist" row at
the top for discoverability, then lists existing playlists fetched
once when the sheet opens. Tapping an existing playlist adds the song
and closes; the inline "New playlist" path creates and then adds in
one shot. Search screen shows a `SnackbarHost` with a brief
confirmation toast ("Added to Chill Vibes") on success. Errors surface
as in-sheet text rather than kicking the user out.

`SongRow` switched from `clickable` to `combinedClickable` and now
takes an optional `onLongPress`. Long-press stays a no-op in contexts
that don't pass the callback (e.g. future uses).

### Find new music (M9c)

A third bottom-nav tab — "Find" — lets the user request missing tracks
from the backend's torrent-indexer → AllDebrid pipeline (see
`../backend/README.md` for the server side).

**Data layer.** `FindRepository` wraps five `MediaPlayerApi` endpoints
(`POST /api/requests`, `GET /api/requests`, `GET /{id}`,
`POST /{id}/select`, `DELETE /{id}`). DTOs live in
`data/dto/RequestDto.kt`: a `RequestStatus` enum with an `isTerminal`
extension, plus `CandidateKind`, `CandidateDto`, `RequestDto`,
`RequestSummaryDto`, and the two request-body records. Magnet URIs are
never exposed to the client — the server picks by candidate id and
hands the magnet off to AllDebrid itself.

**ViewModel.** `FindViewModel` exposes a `MutableStateFlow<String>` for
the query and a `StateFlow<FindUiState>` for the screen state
(`Idle`, `Searching`, `Error`, `Tracking(request)`). After `submit()`,
the VM creates the request synchronously and transitions to
`Tracking`. After `select(candidate)`, the VM polls
`GET /api/requests/{id}` every 2 seconds until
`RequestStatus.isTerminal` — the poll job is cancelled on `reset()` and
in `onCleared()` to avoid leaking coroutines.

**Screen.** `FindScreen` is a single composable hosting both phases:

- `QueryBar`: outlined text field + Search button, disabled during
  `Searching`.
- `StatusHeader`: back arrow, quoted query, and a human-readable label
  per `RequestStatus`. The `FAILED` label appends the backend's
  `errorMessage` when provided.
- `CandidateTabs`: Material 3 `TabRow` splitting the candidates into
  Albums vs Singles. The backend's `UNKNOWN` kind falls into the
  Albums bucket — music torrents are overwhelmingly releases.
- `CandidateRow`: title (semibold) + meta row with seeders,
  IEC-formatted size, track count, and indexer. Rows become inert
  once the backend is past `AWAITING_SELECTION`; the selected row
  keeps a tinted background.

**Why a single screen.** The flow is short (query → pick → wait) and
users expect Back to drop them on an empty query field, not into a
stale picker. A sub-nav graph would also create a second destination
that `popUpTo(startDestination)` would need to special-case.

### Offline cache + prefetch (M10)

M10 adds a disk-backed cache to the ExoPlayer pipeline and an optional
prefetch that warms the next and previous tracks in the queue on
unmetered networks.

**Why not just a RAM buffer.** ExoPlayer already buffers a few
seconds ahead in memory, but that evaporates on skip-back, app
restart, or even a big seek within the current track. Persisting
bytes under `Context.cacheDir` turns re-plays and seeks into local
disk reads and keeps a "last N tracks" working set warm across service
restarts.

**`PlayerCache`** — process-singleton `SimpleCache` backed by a
`StandaloneDatabaseProvider` (from the `media3-database` artifact) and
a `LeastRecentlyUsedCacheEvictor`. Capped at **1 GiB**, which is ~25
FLAC albums or ~250 lossy tracks. We singleton it because
`SimpleCache` takes a file lock on its database directory at
construction time — a second instance blows up with
`Cache folder already locked`. We deliberately never call `release()`:
the cache is process-scoped and Android's process kill reclaims the
fds cleanly, but mid-process release would break any subsequent
service instantiation (pause → swipe-away → replay cycle).

**`CacheDataSource` wiring.** `MediaPlaybackService.onCreate`
wraps `OkHttpDataSource` in a
`CacheDataSource.Factory(cache, okHttpFactory, FLAG_IGNORE_CACHE_ON_ERROR)`
before handing it to `DefaultDataSource.Factory`. The ignore-on-error
flag means a corrupted cache entry falls through to upstream and
playback continues; it never hard-fails on a bad cache entry.

**`PrefetchOrchestrator`** — a `Player.Listener` + lightweight network
supervisor that keeps the prev + next queue neighbours warm.

- On `onTimelineChanged` / `onMediaItemTransition` it recomputes the
  desired window (`listOfNotNull(prev?.uri, next?.uri)`) and diffs
  it against a `ConcurrentHashMap<String, Job>` of in-flight
  prefetches — out-of-window jobs are cancelled, new ones are
  launched via `CacheWriter(cacheSource, dataSpec, null, null).cache()`
  on `Dispatchers.IO`. Already-cached ranges inside
  `CacheWriter` are a no-op, so the system self-heals: restart →
  queue loads → prev/next are already on disk → prefetch completes
  instantly.
- Network gating via `ConnectivityManager.isActiveNetworkMetered()`
  plus a long-lived `NetworkCallback` watching
  `NET_CAPABILITY_NOT_METERED`. When the user drops off Wi-Fi the
  callback fires on a system thread, we bounce to
  `player.applicationLooper` via a `Handler`, flip the `allowed` gate,
  and cancel every in-flight job.
- Prefetch is strictly **best-effort**: 404s, IO errors, and stale ids
  are swallowed (cancellation is still propagated). The user's
  explicit playback is unaffected — that request path doesn't go
  through the orchestrator.

**Why `CacheWriter` over `PreloadMediaSource`.** `PreloadMediaSource`
is still marked experimental and pulls in the full renderer pipeline
to pre-extract a few seconds of audio. For a "warm the whole file so a
seek is instant" goal, `CacheWriter` is a straight byte-copy: cheaper,
simpler, and fully cancellable.

**Storage shape.** The cache lives under
`Context.cacheDir/audio-cache`. `cacheDir` is the right choice —
Android will reclaim space there when storage gets tight, no app-side
code needed. No upgrade migration either: if we ever change the
cache format, bumping the directory name is enough.

### Testing (deferred)

No JVM / instrumentation tests in M4 or M5. The backend has the catalog
and streaming APIs covered by integration tests already. We'll add VM
tests once M6 introduces playlist state worth asserting, at which point
an in-memory fake `MediaController` can cover the `PlaybackViewModel`
too.

## Roadmap

| M  | Status | Headline                                                     |
|----|--------|--------------------------------------------------------------|
| M1 | ✅     | Backend catalog + search                                     |
| M2 | ✅     | MP3 ingestion                                                |
| M3 | ✅     | HTTP range streaming                                         |
| M4 | ✅     | Android app scaffold (Compose) with search                   |
| M5 | ✅     | Media3 ExoPlayer playback + media notification               |
| M6 | ✅     | Playlists (server CRUD + Android UI + real queue)            |
| M7a| ✅     | Backend Docker packaging                                     |
| M7b| ✅     | Android release URL hardening + UX polish                    |
| M7c| ✅     | Top-level README + cross-repo polish                         |
| M8a| ✅     | Android Auto discovery (MediaLibraryService + car metadata)  |
| M8b| ✅     | Android Auto browse tree + voice search                      |
| M8c| ✅     | AA polish: onPlaybackResumption + DHU testing docs           |
| M9a| ✅     | Backend: Prowlarr + song_request state machine               |
| M9b| ✅     | Backend: AllDebrid unlock + archive extraction + auto-import |
| M9c| ✅     | Android "Find new music" tab                                 |
| M10| ✅     | Disk cache (1 GiB) + unmetered-only prev/next prefetch       |

## Non-goals (for now)

- DI framework. Plain singletons (`Network`, `PlayerConnection`) and
  default ViewModel construction are enough through M5.
- Paging 3. Backend already returns an offset/limit page; the full list
  fits easily on one screen for now. Revisit if the library grows past a
  few hundred tracks.
- Offline cache of the catalog. We have the network; loading failures
  surface via the `Error` state.
- Explicit download / "keep this song offline" UX. M10 caches
  opportunistically (on-play + prev/next prefetch on Wi-Fi) but
  doesn't pin tracks — the LRU evictor can reclaim anything.
- Audio effects (EQ, replay-gain). ExoPlayer defaults are fine for a v1.
- Drag-to-reorder inside playlist detail. Backend supports it via
  `PUT /api/playlists/{id}/songs`, but the Compose UI is read-only for
  now — ordering is set at creation time.
- Playlist cover art. The `PlaylistDto` has no cover field; list rows
  and the detail header use a generic queue icon tile instead.
- Removing songs from within the detail screen. The VM exposes
  `removeSong`, but no affordance is wired in yet.
