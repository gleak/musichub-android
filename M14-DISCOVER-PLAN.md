# M14 — Discover / Made For You

Spotify-style auto-curated playlists. Server picks tracks matched to user
taste, ingests them from YouTube, and refreshes a per-user playlist daily.
Phone + Android Auto surface them like any other playlist.

> **Scope locked** by user 2026-04-30:
> - Auto YouTube match (top result + title-distance threshold), no human confirm
> - Keep MP3 catalog format — transcode YT m4a/opus → mp3
> - Cover art: MusicBrainz CAA primary, yt-dlp thumbnail fallback
> - Cadence: **daily** (not weekly)
> - Cold start: my call → onboarding tag picker (3 seed genres)
> - License posture: skip, instance is private
> - Storage: not a concern
> - **Rate-limit / IP-block protection mandatory** for yt-dlp

---

## Existing pieces to reuse

**Backend** (`../backend/src/main/java/com/mediaplayer/backend/`):
- `youtube/YoutubeDownloadService.java` — YT download wrapper
- `request/YtDlpClient.java` — yt-dlp invocation, args, timeouts
- `request/YtDlpCookieRefresher.java` — cookie/session rotation infra
- `request/YtDlpUpdater.java` — binary updater
- `request/RequestOrchestrator.java` — state machine for the existing Find tab
- `ingest/` — MP3 → catalog importer; yt-dlp output funnels through this
- `history/`, `liked/`, `playlist/`, `song/` — repos for taste signals
- DB changelog dir: `src/main/resources/db/changelog/changes/` (Liquibase)

**Android** (current repo):
- `LibraryRepository`-style data layer with Retrofit → wire new endpoints
- `LibraryTree` (M13) — already exposes Liked/Recents/Playlists; add
  `made-for-you` root section, filtered from `listPlaylists()` by `kind`
- `ui/playlists/PlaylistDetailScreen` reusable for auto-playlists
- `ui/home/HomeScreen` — add carousel section above Recents

**Constraint from existing infra**: a `YtDlpClient` already exists. Do NOT
fork it; the M14a search endpoint reuses it through a new method
`searchAndDownload(query, expected)`.

---

## Phase breakdown

### M14a — Backend: YouTube search-and-import endpoint

**Deliverable.** `POST /api/youtube/import` body
`{query: String, expectedTitle: String, expectedArtist: String}`. Returns
`{songId | null, status: ENUM, reason?}`.

**Implementation.**
1. New `YoutubeSearchService` in `youtube/` package.
2. Reuse `YtDlpClient`. Run `yt-dlp ytsearch1:"{query}" --print-json`.
3. Title-distance gate: Levenshtein-normalized ≤ 0.35 against
   `"{expectedArtist} {expectedTitle}"`. Reject otherwise.
4. Download bestaudio → `ffmpeg` re-encode to MP3 320kbps (matches catalog).
5. Tag with ID3: title/artist/album from input + MBID if resolved.
6. Cover lookup chain:
   - MusicBrainz `recording/?query=` → `release/{id}` → CAA
     `https://coverartarchive.org/release/{mbid}/front`
   - Fallback: yt-dlp `--write-thumbnail`, embed via APIC frame
7. Funnel into existing `ingest/` pipeline → song row inserted, dedup by
   hash + (title,artist) fuzzy.
8. Audit row in new `youtube_imports` table.

**Rate-limit protection (mandatory, per user)**:
- **Token bucket per IP**: max 30 yt-dlp invocations/hour. Spring
  `Bucket4j` or hand-rolled semaphore.
- **Concurrency cap**: max 2 simultaneous yt-dlp processes globally.
  Reject extras with HTTP 429 (cron will retry next tick).
- **Backoff on failure**: detect "HTTP Error 429", "Sign in to confirm",
  "Too many requests" in stderr → exponential backoff 5min → 15min →
  60min, persisted in `youtube_imports.next_retry_at`.
- **Cookie rotation**: leverage existing `YtDlpCookieRefresher`. Trigger
  refresh on consecutive 429s.
- **User-Agent rotation** + `--sleep-requests 1 --sleep-interval 2 --max-sleep-interval 8` flags.
- **Circuit breaker**: 5 consecutive failures within 10min → trip
  breaker, halt YT-imports for 30min, log + emit `discover_blocked`
  metric. Cron must check breaker state before submitting work.
- **Per-instance IP only** — never proxy/multiplex from cloud IPs.
  Document as deployment constraint.

**Schema** (new Liquibase changelog `15-youtube-imports.sql`):
```
youtube_imports
  id BIGSERIAL PK
  user_id BIGINT FK → users.id NULLABLE  -- null = system/cron job
  query TEXT
  expected_title TEXT
  expected_artist TEXT
  yt_video_id TEXT NULLABLE
  song_id BIGINT FK → songs.id NULLABLE
  status ENUM('PENDING','SEARCHING','DOWNLOADING','IMPORTED','REJECTED','RATE_LIMITED','FAILED')
  reason TEXT NULLABLE
  attempted_at TIMESTAMP
  next_retry_at TIMESTAMP NULLABLE
  attempts INT DEFAULT 0
```

**Tests.** Integration test extending `YoutubeDownloadServiceIntegrationTest`
pattern — uses real yt-dlp against a stable public video id.

---

### M14b — Backend: taste profile materialization

**Deliverable.** Nightly job builds `user_taste` table from signals.

**Schema:**
```
user_taste
  user_id BIGINT FK
  dimension ENUM('ARTIST','ALBUM','DECADE','GENRE')
  key TEXT
  weight DOUBLE
  updated_at TIMESTAMP
  PK (user_id, dimension, key)
```

**Job.** `@Scheduled(cron = "0 0 3 * * *")` — 03:00 daily.

**Algorithm.**
- Pull `play_history` (extend existing if needed: `completion_ratio`,
  `was_skipped`).
- Pull `liked`, `playlist_songs` (excluding auto-playlists).
- Apply weights:
  - liked: +5
  - completed (≥80% of duration): +2
  - skipped (<30s and explicit skip event): -3
  - in user playlist: +3
- Apply recency decay: `weight × exp(-days_since_event / 30)`.
- Aggregate per dimension. Truncate to top 100 per dimension.
- Atomic swap: write to `user_taste_staging`, `BEGIN; TRUNCATE user_taste; INSERT FROM staging; COMMIT;`.

**Schema extension to `play_history`** (new changelog `16-history-completion.sql`):
```
ALTER TABLE play_history
  ADD COLUMN completion_ratio DOUBLE,
  ADD COLUMN was_skipped BOOLEAN DEFAULT FALSE;
```
Android side: `RecordPlayRequest` DTO gains those fields.
`PlaybackViewModel` records on `onMediaItemTransition` with computed ratio
from prev item's playback position.

---

### M14c — Backend: recommender

**Deliverable.** `GET /api/recommendations?userId={uid}&limit=50` →
`List<{title, artist, mbid?, score, reason}>`.

**Two stages.**

**Stage A — candidate generation.**
1. Top-20 artists from `user_taste`.
2. For each, fetch via Last.fm API:
   - `artist.getTopTracks` → ~10 tracks each
   - `artist.getSimilar` → ~5 similar artists, then their `getTopTracks`
3. Filter: drop tracks already in catalog (`songs.title + artist` match).
4. Drop tracks marked bad in `discover_candidates.status='BAD_MATCH'`.

**Stage B — ranking.**
- Score = `artist_weight × similarity_decay × recency_boost × novelty`
- `recency_boost` peaks for tracks released within preferred decade
- `novelty` = 1 / (1 + already_skipped_count)

**External clients.**
- `LastFmClient` (new) — wraps `ws.audioscrobbler.com/2.0/`. Key via
  `application.yml` env var `LASTFM_API_KEY`. Cache 7d in new
  `lastfm_cache` table (key = endpoint+params hash → JSON blob).
- `MusicBrainzClient` (new) — `musicbrainz.org/ws/2/`. 1 req/s rate
  limit hard-enforced via semaphore. Cache 30d.

**Cold start**: if `user_taste` row count < 10 → use onboarding tags
(see M14e) to seed Last.fm `tag.getTopTracks` for each tag.

---

### M14d — Backend: auto-playlists schema + daily refresh

**Deliverable.** Per-user "Discover Daily" playlist auto-refreshed.

**Schema** (`17-auto-playlists.sql`):
```
ALTER TABLE playlists
  ADD COLUMN kind VARCHAR(32) DEFAULT 'USER',  -- USER|DISCOVER_DAILY|ON_REPEAT|RELEASE_RADAR
  ADD COLUMN auto_owner_user_id BIGINT FK → users.id NULLABLE,
  ADD COLUMN last_refreshed_at TIMESTAMP NULLABLE;

CREATE INDEX idx_playlists_auto_owner
  ON playlists(auto_owner_user_id, kind)
  WHERE kind != 'USER';

CREATE TABLE auto_playlist_history (
  id BIGSERIAL PK,
  playlist_id BIGINT FK,
  refreshed_at TIMESTAMP,
  added_song_ids BIGINT[],
  removed_song_ids BIGINT[]
);
```

**On user creation** (`AuthController`/sign-in path): also create their
`Discover Daily` and `On Repeat` playlists.

**Cron** `@Scheduled(cron = "0 0 6 * * *")` — 06:00 daily.

For each user with `kind='DISCOVER_DAILY'`:
1. Check rate-limit circuit breaker. Skip user if tripped.
2. Call recommender → top 30 candidates.
3. For each not in catalog: enqueue `youtube_imports` row (status PENDING).
4. Worker drains queue respecting global concurrency cap.
5. After ingest, replace playlist contents:
   - Keep top-scoring 50% from previous run if still scoring
   - Add new top candidates to fill ~30 slots
   - Audit diff into `auto_playlist_history`

**`On Repeat`** (separate cron, 04:00 daily): top 30 by play count last
30 days, no YT ingest needed. Pure catalog query.

**Idempotency**: runs within last 18h skip with log.

---

### M14e — Android: Discover screen + onboarding

**`ui/discover/DiscoverScreen.kt`** — Spotify "Made For You" hub.
- Tile grid: `Discover Daily`, `On Repeat`. Future-ready for `Release Radar`.
- Each tile resolves into existing `PlaylistDetailScreen`
  (auto-playlists are real playlists server-side).

**`ui/home/HomeScreen.kt`** — new carousel "Made For You" above Recents.

**Onboarding** (first-run, `OnboardingScreen.kt`):
- Triggered when `getMe()` returns `onboardingComplete=false`.
- 3-step tag picker: pick 3 from preset list of 20 (rock, pop,
  electronic, jazz, hip-hop, classical, ...). Backend stores as
  `user_taste.dimension='GENRE'` rows with synthetic weight 5.
- Skippable; if skipped, recommender uses Last.fm geo-charts as cold-start.

**`Routes`**: add `discover`, `onboarding`. Bottom-nav stays 3 tabs;
Discover lives inside Home or as 4th tab — TBD during planning.

---

### M14f — Android Auto: Made For You section

`LibraryTree` adds:
- Root child `made-for-you` (grid, MEDIA_TYPE_FOLDER_PLAYLISTS)
- Children = `playlists` filtered server-side by `kind != 'USER'`.

Backend: `GET /api/playlists?kind=auto` query param filter. Already
returns `kind` field in `PlaylistDto` — Android filters client-side too
for safety.

Reuses existing `pl:{pid}:{pos}:{sid}` leaf scheme — no new mediaId.

---

## Cross-cutting

### Configuration (backend `application.yml`)
```yaml
discover:
  enabled: true
  daily-cron: "0 0 6 * * *"
  taste-cron: "0 0 3 * * *"
  on-repeat-cron: "0 0 4 * * *"
  yt-rate-limit:
    bucket-capacity: 30
    bucket-refill-per-hour: 30
    max-concurrent: 2
    breaker-threshold: 5
    breaker-window-min: 10
    breaker-cooldown-min: 30
lastfm:
  api-key: ${LASTFM_API_KEY}
  cache-ttl-days: 7
musicbrainz:
  rate-per-second: 1
  cache-ttl-days: 30
```

### Observability
- Metrics: `discover_runs_total`, `discover_imports_total{status=}`,
  `yt_rate_limited_total`, `yt_breaker_open` gauge.
- Log every breaker trip + cookie refresh at WARN.

### Testing strategy
- M14a: integration vs real yt-dlp + a known stable video id.
- M14b: unit tests with synthetic history, golden-master taste output.
- M14c: mock Last.fm/MB clients with recorded fixtures.
- M14d: in-memory worker test, freeze clock.
- M14e/f: Compose screenshot tests + AA browse tree assertions.

### Rollout order
M14a → M14b → M14c → M14d → M14e (depends on c for recs visible) → M14f.
M14a is gated by working rate-limit; ship that section first.

---

## Open dependencies

- Last.fm API key — needs registration before M14c.
- Verify yt-dlp binary in current backend Docker image supports
  `--print-json` and `ytsearch1:` (likely fine, version check during
  M14a).
- Confirm `play_history` schema and decide if `completion_ratio` /
  `was_skipped` are additive or require backfill (probably skip backfill).

---

## Out of scope (defer to M15+)

- Collaborative filtering (waits for multi-user adoption)
- Release Radar (followed-artists feature first)
- "Daily Mix" multi-mood split à la Spotify
- Non-YouTube sources (SoundCloud, Bandcamp)
- Lyrics-based mood scoring
