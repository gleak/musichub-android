# Android architecture

Living doc for the MediaPlayer Android app. Update alongside each milestone.

## Current state (Milestone 5 complete)

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
  `LazyColumn` of `SongRow`s. State is a sealed `SearchUiState`.
- `ui/player/` — `MiniPlayer` bar and `NowPlayingSheet` (Material 3 modal
  bottom sheet). Both bind to the same `PlaybackViewModel`.
- `playback/` — `MediaPlaybackService`, `PlayerConnection` (singleton that
  binds a `MediaController`), and `PlaybackViewModel` (Compose-facing facade).

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

**Queue model.** Single-track. Playing a new row replaces the media item.
Next/previous are deferred until M6, when playlists give us a real queue.

**Stream URL.** Plain `GET /api/songs/{id}/stream` — no signing, no
tokens. The backend is LAN-only in development and auth is a non-goal
for now.

**Permissions.** `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`,
`POST_NOTIFICATIONS` (API 33+), and `WAKE_LOCK`. All declared up-front;
runtime permission for notifications is left to the user — if they deny
it the media notification just doesn't render, playback still works.

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
| M6 |        | Playlists (server CRUD + Android UI)                         |
| M7 |        | Polish, Docker packaging                                     |

## Non-goals (for now)

- DI framework. Plain singletons (`Network`, `PlayerConnection`) and
  default ViewModel construction are enough through M5.
- Paging 3. Backend already returns an offset/limit page; the full list
  fits easily on one screen for now. Revisit if the library grows past a
  few hundred tracks.
- Offline cache of the catalog. We have the network; loading failures
  surface via the `Error` state.
- Download / offline playback. Streaming only.
- Audio effects (EQ, replay-gain). ExoPlayer defaults are fine for a v1.
- Multi-track queueing + gapless. Deferred to M6 alongside playlists.
