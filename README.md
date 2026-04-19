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
- Version catalog in `gradle/libs.versions.toml`

## Module layout

Single `:app` module. Package `com.mediaplayer.android`:

```
app/src/main/kotlin/com/mediaplayer/android/
├── MediaPlayerApp.kt          // Application; wires Coil + binds PlayerConnection
├── MainActivity.kt            // AppScaffold: search + mini-player + sheet
├── data/
│   ├── Network.kt             // Retrofit + OkHttp + JSON singleton
│   ├── MediaPlayerApi.kt      // Retrofit interface (listSongs)
│   ├── SongRepository.kt      // thin façade over the API
│   └── dto/
│       ├── SongDto.kt         // @Serializable mirror of backend
│       └── PageResponse.kt    // generic page envelope
├── playback/
│   ├── MediaPlaybackService.kt  // MediaSessionService owning ExoPlayer
│   ├── PlayerConnection.kt      // async MediaController binder (singleton)
│   └── PlaybackViewModel.kt     // Compose StateFlows + controls
└── ui/
    ├── theme/Theme.kt
    ├── search/
    │   ├── SearchScreen.kt
    │   ├── SearchViewModel.kt
    │   └── SongRow.kt
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

Override `BASE_URL` at build time with your machine's LAN IP:

```
./gradlew :app:installDebug -PBASE_URL=http://192.168.1.42:8080
```

Make sure the device and machine share a network and nothing firewalls :8080.

## Search UX

- Query is debounced 300ms before hitting the backend.
- Empty query returns the full catalog (default pagination, 20 rows).
- `flatMapLatest` cancels any in-flight request when the query changes.
- Rows show title, "artist · album", a duration stamp, and an AsyncImage
  cover pulled from `/api/songs/{id}/cover` (falls back to a music-note icon
  when the song has no cover).
- Tapping a row starts playback.

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
- Queue is single-track — playing a new row replaces the current item.
  Real queues land with playlists in M6.

## Permissions

Declared in `AndroidManifest.xml`:

- `INTERNET`, `ACCESS_NETWORK_STATE` — HTTP calls
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` — background
  audio
- `POST_NOTIFICATIONS` — API 33+ media notification
- `WAKE_LOCK` — keep audio pipeline alive

Notification permission is a runtime prompt; denying it doesn't stop
playback, just hides the media notification.

## What's next

- **M6** — Playlists (server CRUD + Android UI + real queue / next-prev)
- **M7** — Polish + release-config URL + Docker packaging for the backend
