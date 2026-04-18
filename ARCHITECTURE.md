# Android architecture

Living doc for the MediaPlayer Android app. Update alongside each milestone.

## Current state (Milestone 4 complete)

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

No DI framework yet — `Network` is an object and `SearchViewModel` constructs
its own `SongRepository`. Introduce Hilt if M5/M6 make the graph non-trivial.

### Networking

- `BuildConfig.BASE_URL` is injected per build type. Debug defaults to
  `http://10.0.2.2:8080` (emulator loopback); override with
  `-PBASE_URL=...` for a physical device on the LAN. Release picks a
  placeholder until M7 (no deploy target yet).
- Cleartext is allowed only for `10.0.2.2` and `localhost` via
  `network_security_config.xml`. Everything else stays HTTPS-only.
- `OkHttpLoggingInterceptor` at `BASIC` on debug, off on release.
- One `OkHttpClient` is shared across Retrofit and Coil
  (`MediaPlayerApp : SingletonImageLoader.Factory`) so we have one connection
  pool + one cache.

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
  back to a `MusicNote` icon so the row layout is stable.

### Testing (deferred)

No JVM / instrumentation tests in M4. The backend has the catalog API
covered by integration tests already. We'll add VM tests once M5 introduces
playback state worth asserting.

## Roadmap

| M  | Status | Headline                                                     |
|----|--------|--------------------------------------------------------------|
| M1 | ✅     | Backend catalog + search                                     |
| M2 | ✅     | MP3 ingestion                                                |
| M3 | ✅     | HTTP range streaming                                         |
| M4 | ✅     | Android app scaffold (Compose) with search                   |
| M5 |        | Media3 ExoPlayer playback + media notification               |
| M6 |        | Playlists (server CRUD + Android UI)                         |
| M7 |        | Polish, Docker packaging                                     |

## Non-goals (for now)

- DI framework. Plain constructors and singletons are enough for M4.
- Paging 3. Backend already returns an offset/limit page; the full list
  fits easily on one screen for now. Revisit if the library grows past a
  few hundred tracks.
- Offline cache of the catalog. We have the network; loading failures
  surface via the `Error` state.
