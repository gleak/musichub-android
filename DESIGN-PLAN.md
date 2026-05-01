# MusicHub design implementation plan

**Source bundle:** `mockup/` (Claude Design — MusicHub).
**Chosen launcher mark:** Onda sonora (waveform) — `mockup/android-icons/waveform-lime/`.
**Visual direction:** original (not Spotify clone). Lime accent
`#A8E04E`, Inter + JetBrains Mono, generative covers, eyebrow
`// SECTION` mono, gradient backgrounds.

This plan maps the mockup screens to the existing Compose screens,
calls out the deltas, and sequences the work in phases that each
ship behind a version bump per `CLAUDE.md`.

---

## 1 · Mapping — mockup → existing Android screens

| Mockup screen                       | Existing Compose target                                   | Status |
|-------------------------------------|-----------------------------------------------------------|--------|
| 01 Home                             | `ui/home/HomeScreen.kt`                                   | redesign |
| 02 Search                           | `ui/search/SearchScreen.kt`                               | redesign |
| 03 Library                          | `ui/playlists/PlaylistsScreen.kt` (+ tabs)                | redesign + tabs |
| 04 Album detail                     | `ui/albums/AlbumScreen.kt`                                | redesign |
| 05 Now Playing                      | `ui/player/NowPlayingSheet.kt` (`NowPlayingContent`)      | redesign |
| 06 Artist                           | `ui/artists/ArtistScreen.kt`                              | redesign |
| 07 Playlist detail                  | `ui/playlists/PlaylistDetailScreen.kt`                    | redesign |
| 08 Profile / Settings               | **NEW** `ui/profile/ProfileScreen.kt`                     | new screen |
| 09 Lyrics                           | `ui/player/LyricsSheet.kt` (+ `LyricsView`)               | redesign |
| 10 Video                            | `ui/player/VideoPlayerSheet.kt` (`VideoPlayerInline`)     | redesign + related list |
| Per te (hub)                        | **NEW** `ui/foryou/ForYouScreen.kt`                       | new screen + nav tab |
| Generated playlist detail           | `ui/playlists/PlaylistDetailScreen.kt` w/ `kind != USER`  | branch on `isAuto` |
| Home "Generata per te" carousel     | `ui/home/HomeScreen.kt` (between recents and made-for-you)| insertion |
| AA · all 13 screens                 | `playback/LibraryTree.kt` + `MediaPlaybackService`        | extend tree + content style |
| Launcher icon                       | `app/src/main/res/mipmap-*` + adaptive XML                 | replace |

---

## 2 · Visual rebrand — design tokens

`ui/theme/Theme.kt` is the choke point. Replace `SpotifyColors` with
`MusicHubColors` (keep the file name to avoid churn elsewhere, or
add an alias):

| Token             | Old (Spotify)              | New (MusicHub)              |
|-------------------|----------------------------|-----------------------------|
| primary / accent  | `#1DB954` Green            | `#A8E04E` Lime              |
| onPrimary         | `#000000`                  | `#0A0A0A`                   |
| background        | `#000000`                  | `#080808` (BG_BOTTOM)       |
| surface           | `#121212`                  | `#1F1F1F` (BG_TOP)          |
| surfaceContainer  | `#181818`                  | `#181818`                   |
| onSurfaceVariant  | `#B3B3B3`                  | `#9A9A9A` (TEXT_LO)         |

Add gradient helper: `Brush.verticalGradient(BG_TOP → BG_BOTTOM)` for
screen backgrounds. Add `JetBrainsMono` font family alongside Inter.
Mockup uses Inter where current uses system default — load
**Inter Variable** and **JetBrains Mono** via `androidx.compose.ui.text.googlefonts`
or bundle TTFs in `res/font/`.

Add `MHTypography` extension: a `mono*` set (`monoLabelSmall` 10sp,
`monoBodySmall` 11sp, etc) for the eyebrow + duration + badge usage.

---

## 3 · Reusable building blocks (build first)

`ui/common/`:

- `MHCover.kt` — generative SVG cover renderer. 10 styles
  (`arc`, `grid`, `moon`, `triangles`, `wave`, `dot`, `stripes`,
  `blob`, `type`, `duotone`, `artist`). Use `androidx.compose.ui.graphics.drawscope`
  for `Path` + `Brush.linearGradient`. Deterministic palette per
  song/album id (hash → palette index) so the same album always
  renders the same cover even without artwork.
- `MHCoverFallback.kt` — wraps `coil3.AsyncImage` with `MHCover` as
  placeholder + error fallback. Drop-in replacement for current
  `SongCover` calls.
- `EyebrowText.kt` — `// SECTION` mono uppercase, lime tint.
- `SectionHeader.kt` — already exists; extend signature with
  `eyebrow: String? = null` + `action: (@Composable () -> Unit)? = null`.
- `LimeButton.kt` + `GhostButton.kt` — primary/secondary CTA with
  the spec sizes (44dp filled lime / 32dp pill).
- `PillChip.kt` — filter chip with selected = lime, unselected =
  `rgba(255,255,255,0.08)`.
- `PlayingBars.kt` — 3-bar animated indicator (`mhpb0/1/2`). Use
  `rememberInfiniteTransition`.
- `GeneratedCover.kt` — 7 family covers (rotation rings, mix big
  number, releases VEN badge, capsule polaroid, radar dot grid,
  mood gradient + NOW pill, next arrow). Map by
  `AutoPlaylistKind` from the backend.

---

## 4 · Phased delivery

Each phase is **one user-visible drop** → bumps `AppVersion.VERSION`,
adds a `ChangelogEntry`, bumps `versionName`/`versionCode`. The
visual rebrand is broken into vertical slices so we never have a
"half-rebranded" build.

### Status — `0.10.1` shipped

Delivered through v0.10.0 + v0.10.1 (2026-05):
- **Phase A** — `MHColors` (lime palette), `MHGradient`,
  `LocalMHMono` typography slot, `MHCover` (11 generative kinds),
  `EyebrowText`, `PillChip`, `PlayingBars`, `GeneratedCover` (7
  family covers + `AutoPlaylistFamily` mapping), `SectionHeader` now
  supports `eyebrow`. `SpotifyColors` left as deprecated alias.
- **Phase B** — Home greeting → logo + Italian greeting + date
  strip + Settings → Profile. Section headers Italian + eyebrows.
  4-tab `BottomNav` (Home / Cerca / **Per te** / Libreria).
- **Phase C** — `ForYouScreen` hub: Rotation hero, daily mix grid,
  weekly carousel, contextual rows, "Come funziona" footer.
  `PlaylistDetailScreen` branches on `isAuto`: metadata strip
  (Aggiornata + Brani) + lime cadence banner.
  `PlaylistDetailDto` now carries `kind` + `lastRefreshedAt`.
- **Phase D (light touch)** — Search: `// SFOGLIA` eyebrow, "Tutti i
  generi" headline, Italian tile labels (Album / Artisti). Library:
  Italian filter chips (Tutto / Playlist / Preferiti) + "Brani
  preferiti" pinned row + "Libreria" header.
- **Phase E (light touch)** — Album / Artist subtitles localized;
  Artist sections gain eyebrows (`// POPOLARI` Più ascoltati,
  `// DISCOGRAFIA` Album).
- **Phase F (light touch)** — Now Playing pill localized
  (`IN RIPRODUZIONE DA`).
- **Phase G** — `ProfileScreen` (avatar + stat cards +
  Account / Riproduzione / App sections + Disconnetti). Hosts the
  changelog + update-check entries that lived on Home dropdown.
- **Phase H** — Android Auto browse tree localized
  (Per te / Brani preferiti / Ascoltati di recente / Tutti i brani /
  Album / Artisti / Playlist).
- **Phase I** — Waveform-lime launcher icon (vector adaptive +
  monochrome layer). Mipmap PNGs for pre-O fallback at all 5
  densities.

Deferred — not yet drawn / built:
- **Full layout rewrites** for Now Playing 300dp cover, Lyrics
  fade-active layout, Video player with related list — current
  screens still use the v0.9 layout under new tokens.
- **Library 4-tab UI** (Playlist / Album / Artisti / Scaricati) —
  drill-downs still use separate routes.
- **Phase J** — Login / Onboarding / FindScreen / SpotifyImport /
  sheets (AddToPlaylist, Queue, Equalizer, SleepTimer, Changelog,
  AppUpdate) / settings sub-pages — 26 net-new mockups requested in
  `missing-mockup.md`. Build batch-by-batch as Claude Design returns
  each set.

### Phase A — Foundation (no UI churn yet)
- Add `MHCover`, fonts, palette swap, mono typography, helper
  composables. Keep all existing screens visually working with the
  new tokens. Bump → patch (e.g. `0.10.x`).

### Phase B — Home + Bottom nav (4 tabs)
- Redesign `HomeScreen` to mockup spec: logo + bell/history/settings
  row, large greeting + mono date strip, filter chips
  (Tutto/Musica/Playlist/Artisti), 2×2 quick-picks tiles, generated
  carousel, "Per te" eyebrow vertical track list, fine-feed footer.
- Add `Routes.FOR_YOU` + `ForYouScreen.kt` (initial: lift the
  "Made for you" data from `HomeViewModel` into a dedicated VM that
  also pulls per-family). Update `BottomNav` to 4 destinations:
  Home / Search / **Per te** / Library. Route ordering matches
  `MHBottomNav` in `mh-shared.jsx`.
- Bump → minor (introduces a new tab).

### Phase C — Per te hub + Generated detail
- Build the For You hub from `mh-foryou.jsx`: hero Rotation card,
  6-mix grid, weekly carousel (Releases / Radar / Capsule), Mood
  + Next "Adesso e in poi" rows, "Come funziona" footer.
- Branch `PlaylistDetailScreen` on `playlist.isAuto`: if true,
  render the generated-detail layout (centered cover, mono cadence
  badge, hero title + sub + desc, metadata strip, "Basata su"
  chips, refresh footer). Otherwise the standard playlist layout.
- Bump → minor.

### Phase D — Search + Library tabs
- Redesign `SearchScreen` to match `mh-screens.jsx` 02:
  search bar with Mic icon, recents list with "Cancella" CTA,
  `// SFOGLIA` eyebrow, 2×2 colored genre tiles with rotated
  generative cover. Wire mic to existing voice intent.
- Convert `PlaylistsScreen` (Library) to tabs:
  Playlist / Album / Artisti / Scaricati. Reuse
  `AlbumListScreen` / `ArtistListScreen` content under their tabs.
  Pinned "Liked songs" row stays sticky at top of Playlist tab.
  Add Filter pill row + Grid toggle as in mockup.
- Bump → minor.

### Phase E — Album / Playlist / Artist detail redesign
- Three screens, same pattern (centered hero, gradient header,
  artist row with verified, mono metadata strip, action row,
  track list). Replace current Spotify-hero with mockup spec.
  Album & Playlist gradient = derived from cover palette
  (use Coil palette extraction or fall back to generative palette).
- Bump → patch.

### Phase F — Now Playing + Lyrics + Video
- `NowPlayingContent` swap: 300dp centered cover with shadow,
  scrubber with 12dp lime thumb, big controls (70dp play),
  Cast / device name strip, Heart filled lime.
- `LyricsView` → mockup spec: large active line (26sp white),
  past lines `rgba(255,255,255,0.35)`, future `0.5`, mono
  `SINCRONIZZATO` badge, mini-track strip on top, big lime play
  pause at the bottom.
- `VideoPlayerInline` → mockup spec: 16:9 hero with center play
  glass, top status pill (`VIDEO 4K`), scrubber overlay, channel
  follow bar, action chips row, related-videos list. The related
  list pulls from existing local catalog (or a backend endpoint
  if available).
- Bump → minor.

### Phase G — Profile / Settings full screen
- New `ui/profile/ProfileScreen.kt` reachable from the gear icon
  on Home + Per Te. Hero (gradient avatar + name + handle),
  3 mono stat cards, Account / Riproduzione / App sections.
  Wire Crossfade and Download offline to actual settings;
  fold the existing "What's new" / "Check for updates" entries
  into the App section instead of the current Home dropdown.
  Disconnetti CTA at the bottom.
- Bump → patch.

### Phase H — Android Auto extension
- `LibraryTree.kt` already exposes Made-for-you, Recents, Liked,
  Playlists, Albums, Artists, All Songs. Add:
  - `for_you` root child (browsable, grid) — children are the
    `AutoPlaylistKind`s pulled from backend, rendered with the
    family-specific icon.
  - Lyrics / video tabs are surfaces of the active session, not
    library tree nodes — register custom layout commands on the
    `MediaSession` (`SessionCommand` for "show lyrics"/"show video")
    and let AA's templated UI render them. Video stays paused on
    AA per driver-safe spec.
- Add `EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT` where needed.
- Bump → minor.

### Phase I — Launcher icon swap
- Drop `mockup/android-icons/waveform-lime/` into
  `app/src/main/res/`:
  - `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml`
    (adaptive — references foreground/background drawables)
  - `drawable/ic_launcher_foreground.xml` (the 8-bar waveform)
  - `drawable/ic_launcher_background.xml` (lime fill)
  - `drawable/ic_launcher_monochrome.xml` (Android 13+ themed)
  - `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png` + round
- Smoke-test: launcher preview, recent-apps card, themed-icon
  picker (Material You).
- Bump → patch.

### Phase J — Missing screens (see `missing-mockup.md`)
- Once Claude Design returns mockups for Login / Onboarding /
  Find / Spotify Import / sheets / settings sub-pages, build
  them in batches. Each batch = patch bump.

---

## 5 · Concrete deltas per existing screen

Below: the surgical diff for each screen, so the executor knows
exactly what changes.

### `HomeScreen.kt`
- Replace `GreetingHeader` with mockup spec: `MHLogo` (lime
  square + circle mark) + Bell/History/Settings icons row.
- Below: greeting headline (`headlineMedium`) + mono date strip
  (`bodySmall` MONO).
- Add filter chip row (Tutto / Musica / Playlist / Artisti).
- Replace current `ShortcutGrid` with the mockup 2×2 quick-picks
  layout: each tile = 48dp generative cover + title + optional
  `NEW` badge mono lime.
- New section `// Generata per te` between quick picks and
  "Made for you" — horizontal carousel of generated playlists
  (Rotation + 6 mix). Tap → generated detail.
- Replace the "Brani consigliati" track list with mockup track
  layout: 2-digit mono index, 44dp cover, title + artist (lime
  if `playing`), heart, mono duration, kebab.
- Footer: `— FINE FEED —` mono.

### `SearchScreen.kt`
- Search bar with Mic icon (right side), trailing icon active
  state on focus.
- Recents: header strip with `// RECENTI` lime mono + "Cancella"
  trailing, divider rows with History icon + label + X.
- `// SFOGLIA` eyebrow, "Tutti i generi" headline, 2×2 colored
  genre tiles with rotated generative cover at corner.

### `PlaylistsScreen.kt`
- Convert into 4-tab layout: Playlist / Album / Artisti /
  Scaricati. Use Compose `ScrollableTabRow` with custom indicator
  (lime pill).
- Filter row above content: chips (Recenti default), grid toggle.
- Pinned Liked row at top with mono pin icon + "Playlist · 247
  brani" sub.
- All other rows: 52dp cover (round for artists), title + sub,
  chevron.

### `AlbumScreen.kt`
- Top: Back + More.
- Centered 200dp cover with lime shadow.
- Title (`headlineMedium`), artist row with 20dp round cover +
  Verified, mono `ALBUM · 2026 · N BRANI · MM MIN`.
- Action row: Heart / Download / Share / spacer / Shuffle /
  big lime Play (52dp).
- Track list: 18dp index column (mono), title + mono
  `N riproduzioni`, optional liked heart, mono duration, kebab.
- Background gradient: derive from album palette
  (`#2A1448 → #0F0820 → #060309` shown in mockup as a sample).

### `ArtistScreen.kt`
- Hero: 320dp full-bleed `MHCover kind=artist` with bottom-up
  gradient overlay → background. Verified row, big artist name
  (36sp 900), mono `2.4M ASCOLTATORI MENSILI`.
- Action row: Segui pill (outlined), Bell, More, spacer,
  Shuffle, lime Play 48dp.
- `// POPOLARI` "Più ascoltati" — track list with 44dp cover.
- `// DISCOGRAFIA` "Album" carousel of 140dp tiles.
- `// INFO` card with bio + mono `2.4M / 184K` highlights.

### `PlaylistDetailScreen.kt`
- Branch on `playlist.isAuto`:
  - `false` → mockup screen 07: 180dp cover, `// PLAYLIST` eyebrow,
    title + Edit, owner row (avatar + "Marco · 7 brani · 25 min"),
    action row (Heart / Download / Plus / Share / Shuffle / lime
    Play), track list.
  - `true` → mockup `GeneratedDetailScreen`: 200dp cover, mono
    cadence badge `// GENERATA · OGNI GIORNO`, title + sub + desc,
    metadata strip (Aggiornata + Brani), `// BASATA SU` chips,
    full-width primary lime CTA (RIPRODUCI) + 44dp shuffle/heart
    pills, track list, refresh footer.

### `NowPlayingSheet.kt` → `NowPlayingContent`
- Replace existing top-bar with mockup: Down icon + centered
  caption (`IN RIPRODUZIONE DA ALBUM` mono + album name) + More.
- 300dp cover, big shadow, gradient background derived from
  album palette.
- Title + artist + Heart row, scrubber with 12dp thumb,
  Shuffle (lime when on) / SkipPrev (32) / Play (70dp lime) /
  SkipNext (32) / Repeat row, Cast / device strip / Share footer.

### `LyricsSheet.kt`
- `LyricsView`: mock spec — past lines fade to 0.35, future to
  0.5, active 26sp white. Auto-scroll active line to center.
- `LyricsSheet`: top bar (Down + `TESTO` mono + track name + More),
  44dp cover + title + lime `SINCRONIZZATO` badge, scroll body,
  bottom dock (Share / 56dp lime Pause / mono `1:32 / 4:02`).

### `VideoPlayerSheet.kt`
- `VideoSurface` overlay: top status pill `VIDEO 4K` red dot,
  center 64dp glass play button.
- Below: title block + mono views/age, channel row + Segui pill,
  action chips (Heart count / Condividi / Salva / Audio only),
  `// VIDEO CORRELATI` list with 110dp 16:9 thumbnails + duration
  badge.

### `MainActivity.kt`
- Bottom nav: 4 destinations. Add Routes.FOR_YOU.
- Wire Settings icon (Home & Per Te headers) → push
  `Routes.PROFILE` instead of opening dropdown menu.
- Keep dropdown content (`What's new` / `Check for updates`) but
  move to ProfileScreen App section.

---

## 6 · Backend / data dependencies

Most of the rebrand is presentation-only. The For You hub and
generated-detail screens need:
- Endpoint that lists auto-playlists with `kind` so the UI can
  pick the right `GenCover` and metadata strip. Memory says
  backend already supports `GET /api/playlists?kind=auto` and
  `AutoPlaylistKind` (Discover Daily, On Repeat, Release Radar,
  Daily Mix 1..6). Map those to the 7 mockup families:
  - `DISCOVER_DAILY` → mood/rotation hybrid
  - `ON_REPEAT` → rotation
  - `RELEASE_RADAR` → releases (VEN badge)
  - `DAILY_MIX_1..6` → mix-1..6
  - (no current backend equivalents for `capsule` / `radar` /
    `next` — ship as **derived client-side** for v1, lobby
    backend later).
- `lastRefreshedAt` is already in `PlaylistDto`. Use for
  "Aggiornata oggi · 06:00" + "Prossimo aggiornamento domani".

Add `AutoPlaylistKind` enum on Android too (mirror backend) so
the UI doesn't string-match on `kind`.

---

## 7 · Risk register

- **Big-bang theme swap.** Mitigation: Phase A lands tokens only;
  every subsequent phase touches one screen family. CI / smoke
  on each.
- **Generative covers vs real artwork.** Coil already loads
  artwork from the catalog; `MHCover` is fallback. Order of
  precedence: real artwork (when present) → palette-derived
  generative cover → static fallback. Don't replace existing
  artwork.
- **Italian copy regression.** Current strings mix EN/IT. Don't
  flip everything — only the strings that the mockup specifies
  (Per te, Brani consigliati, Cerca, Libreria, Aggiornata, etc).
  Keep DTO field names English.
- **Material 3 vs custom shapes.** The mockup is unopinionated
  about Material — keep using `MaterialTheme` for ripples,
  IME insets, system gestures, but override surfaces and
  components with our tokens. Do not remove Material — we'd
  lose a11y wiring.
- **Bottom-nav reshuffle.** Adding a 4th tab is a navigation
  contract change — make sure deep links still resolve. Test
  the `mediaplayer://share/<token>` flow after the change.
- **AA content-style enums** (memory: `reference_aa_content_style`)
  must stay exact strings — don't swap to constants from a
  newer Media3 version that may differ.

---

## 8 · Definition of done

For each phase:
- [ ] Visual matches mockup pixel-by-pixel at 412dp width
      (close enough — tolerate <4px gap, exact colors/typography).
- [ ] No regression in existing flows: playback start, queue add,
      kebab destinations, AA browse.
- [ ] `AppVersion.VERSION` + `versionName` + `versionCode` bumped,
      `ChangelogEntry` added per `CLAUDE.md`.
- [ ] Build succeeds on Pixel 6 emulator + AA DHU.
- [ ] No new lint warnings (`./gradlew lint`).
- [ ] User-facing strings localized in `strings.xml` (Italian).
