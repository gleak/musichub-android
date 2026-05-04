# Core surfaces — mockup vs implementation parity audit

App version at audit time: **v0.13.1**.
Mockup bundle: `mockup/mh-screens.jsx`, `mockup/mh-extras.jsx`,
`mockup/mh-foryou.jsx`, `mockup/mh-shared.jsx`, `mockup/mh-canvas-app.jsx`.
Tag legend: **[LAYOUT]** geometry/structure · **[COPY]** strings · **[STATE]**
data shape / which states are drawn · **[BEHAVIOR]** gestures and runtime.

> Note (canvas): `mh-canvas-app.jsx` mounts only `HomeScreen` from
> `mh-screens.jsx` for the Home artboard. `LibraryScreen`, `ProfileScreen`
> from the same file are mounted into the legacy "mobile" section but the
> dedicated mockups for v0.10.1+ Library / Profile actually live in
> `mh-library.jsx` / `mh-settings.jsx`. The canvas references those for
> drill-downs. For the core six this audit treats `mh-screens.jsx` as
> authoritative for Home, Search, Now Playing, Album, Artist, Playlist
> Detail; `mh-foryou.jsx` for Per te; `mh-extras.jsx` for Lyrics + Video.

---

## 1. HOME — `ui/home/HomeScreen.kt`

Mockup: `mockup/mh-screens.jsx:9-109` (`HomeScreen`).

### [LAYOUT]
- Mockup top bar: `MHLogo` + trailing `Bell · History · Settings` icons
  (`mh-screens.jsx:35-40`). **Code top bar: greeting only + single
  `Settings` IconButton** (`HomeScreen.kt:451-465`). **No logo
  (`MHLogo`), no `Bell` (notifications), no `History` icon.** Project
  notes (missing-mockup §1, §10) say notifications are out-of-scope, so
  `Bell` parity is intentional. Logo and history icons remain divergent.
- Mockup quick-shortcut grid: 4 fixed mock tiles
  (Echo / Night Mode / Liked / Slow Hours) with `48×48` cover and "NEW"
  monospace badge in lime (`mh-screens.jsx:11-16, 57-72`). Code shortcut
  grid: dynamic, up to 6 tiles in 2-col layout, height `56dp`, no NEW
  badge — content is `Liked + recents + playlists` (`HomeScreen.kt:496-577`).
- Mockup recommended tracks: numbered (01..06) row with `14sp` mono
  index, 44dp cover, heart, 32dp duration mono, more
  (`mh-screens.jsx:81-104`). **Code does not render numbered
  recommendation rows** — the `Recents` track row uses
  `RecentRow → SongCardSquare` (140dp horizontal cards,
  `HomeScreen.kt:633-674`). `// Per te / Brani consigliati` section
  does not exist in code; instead Home shows `Cronologia · Riprodotti
  di recente` (`HomeScreen.kt:223-228`).
- Mockup section header for Made-For-You uses action `"Per te →"`
  (`mh-screens.jsx:74`). Code's `SectionHeader(eyebrow="Generata per
  te", title="Le tue playlist di oggi")` has no trailing action link
  (`HomeScreen.kt:233`).
- Greeting font: mockup `26sp/800/-0.8` (`mh-screens.jsx:42`); code uses
  `MaterialTheme.typography.headlineMedium` (`HomeScreen.kt:454`).
  Visually close but not pinned.

### [COPY]
- Mockup date line: `"Ven 1 Mag · 3 nuove uscite per te"`
  (`mh-screens.jsx:43-45`). **Code drops the "N nuove uscite per te"
  half** — `currentDateLabel` only formats `"Ven 4 Mag"`
  (`HomeScreen.kt:475-486`).
- Filter chip labels match: `Tutto · Musica · Playlist · Artisti`
  (mockup `mh-screens.jsx:48`; code `HomeScreen.kt:151-154`).
- Footer end-marker `"— FINE FEED —"` matches mockup `mh-screens.jsx:106`
  ↔ `HomeScreen.kt:323`.
- `pluralizeSongs` copy: mockup `"Playlist · N brani"`
  (`mh-screens.jsx:191-194`); code `PlaylistListRow`
  (`HomeScreen.kt:894-897`) and `PlaylistCardSquare`
  (`HomeScreen.kt:732-739`) use English "song"/"songs" — **drift to
  English** in the auto-square card label.

### [STATE]
- Mockup is purely static. Code adds states the mockup never draws:
  `Loading` (`CenteredSpinner`), `Error` (`ErrorWithRetry`),
  cold-start CTAs `Iniziamo / Scopri musica / Importa Spotify`
  (`HomeScreen.kt:368-404`). Cold-start has no mockup equivalent.

### [BEHAVIOR]
- `PullToRefreshBox` (`HomeScreen.kt:120`) and `ON_RESUME` silent
  refresh (`HomeScreen.kt:107-117`) — no mockup affordance.

---

## 2. SEARCH — `ui/search/SearchScreen.kt`

Mockup: `mh-screens.jsx:114-179` (`SearchScreen`).

### [LAYOUT]
- Mockup search field: pill `borderRadius:10`, `Search` leading + `Mic`
  trailing, gap=10 (`mh-screens.jsx:132-141`). Code uses M3 `TextField`
  with `RoundedCornerShape(10.dp)` and `Color.White.alpha(0.08f)`
  background, leading Search and trailing `[Close (when query)] + Mic`
  (`SearchScreen.kt:264-313`). Drift: code adds an **inline `Close`
  affordance** that the mockup omits.
- Mockup adds a "Cerca" headline above the field
  (`mh-screens.jsx:128-131`). Code matches but **adds a trailing
  `Settings` IconButton** to the title row (`SearchScreen.kt:139-159`)
  not present in the mockup.
- Recents block — mockup eyebrow `// RECENTI` + trailing "Cancella" tap,
  rows have `History · text · X` with bottom divider
  (`mh-screens.jsx:143-156`). Code matches almost exactly
  (`SearchScreen.kt:328-352, 372-409`). Eyebrow text from
  `R.string.search_section_recents`.
- Genre grid 2-col `90dp` tiles with rotated 60×60 cover at bottom-right
  (`mh-screens.jsx:162-176`). Code `GenreGrid` mirrors this — height
  90dp, rotation 20°, color list matches almost 1:1 with the mockup
  (`SearchScreen.kt:422-485`). All 8 genres + colors line up:
  `Indie/Elettronica/Hip-hop/Jazz/Classica/Ambient/Rock/Pop`
  with `#3A0CA3 / #06B6D4 / #FF4D2E / #FFC857 / #E8DCC4 / #0B3D2E /
  #5C2D8C / #F0A6B0`.

### [COPY]
- Mockup placeholder `"Brani, artisti, playlist"` (`mh-screens.jsx:138`);
  code uses `R.string.search_hint` — value not inspected here, but
  call-site shows `stringResource(R.string.search_hint)`
  (`SearchScreen.kt:271`).
- Mockup recents list contains `["marina vega","long way home",
  "tobi akin","glasshouse"]` — pure mock data; code wires
  `viewModel.recentQueries`. Match is structural only.
- Browse section: mockup eyebrow `// SFOGLIA` + title `"Tutti i
  generi"` (`mh-screens.jsx:159-161`). Code uses
  `R.string.search_section_browse` and `R.string.search_browse_title`
  (`SearchScreen.kt:355-361`).

### [STATE]
- Mockup draws only the idle state. Code wires
  `Idle / Loading (shimmer) / Success / Error`
  (`SearchScreen.kt:170-220`) — none of these has a mockup.
- `GenreFilterPill` (`SearchScreen.kt:487-511`) — lime pill on top of
  results when a genre filter is active; **not in mockup**. Same for
  `RecentlyPlayedCarousel` (`SearchScreen.kt:515-560`).

### [BEHAVIOR]
- Voice mic launches `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`
  (`SearchScreen.kt:127-137`). Mockup is static.
- AddToPlaylistSheet on kebab matches §10 SongRow contract
  (`SearchScreen.kt:230-250`).

---

## 3. PER TE / FOR YOU — `ui/foryou/ForYouScreen.kt`

Mockup: `mockup/mh-foryou.jsx:8-127` (`ForYouScreen`).

### [LAYOUT]
- Mockup header: `MHLogo` + trailing `History + Settings` icons
  (`mh-foryou.jsx:20-25`). **Code header has neither logo nor history
  icon** — only the `Settings` IconButton in the title row
  (`ForYouScreen.kt:117-131`).
- Mockup hero "In rotazione" card: 100dp cover left, lime tinted
  background, lime border, `RIPRODUCI` button + heart shortcut
  (`mh-foryou.jsx:36-67`). Code `RotationHero` matches the chrome
  (lime alpha 0.10 fill, 1.5dp lime border, 100dp `GeneratedCover`,
  RIPRODUCI button) (`ForYouScreen.kt:200-256`). **Heart shortcut from
  the mockup is missing in code** — only the Play CTA is rendered.
- Mockup mix grid: 6 daily mixes in 2-col `1:1` aspect with `170dp`
  reference cover; subtitle is mockup field `m.sub` ("PER TE · 2026"
  variants from MHGenerated) (`mh-foryou.jsx:79-93`). Code subtitle is
  `"${pl.songCount} brani"` (`ForYouScreen.kt:299-306`) — **mockup's
  generated subtitle (`m.sub`) is not preserved**.
- Mockup weekly carousel: `releases / radar / capsule` via `GenCardLarge`
  (`mh-foryou.jsx:97-102`). Code mirrors with `WeeklyCard` 170dp
  (`ForYouScreen.kt:310-338`) — same family filter set.
- Mockup `Mood + Next` rendered as `GenCardSmall` rows
  (`mh-foryou.jsx:104-109`). Code's `ContextRow` uses 56dp cover with
  faint white background (`ForYouScreen.kt:340-373`).
- Mockup metadata strip on detail page (`AGGIORNATA / BRANI`) +
  refresh footer (`mh-foryou.jsx:163-244`) — **detail screen lives in
  `PlaylistDetailScreen.kt` `AutoPlaylistMetaStrip`** which exposes
  `AGGIORNATA + BRANI` cards (`PlaylistDetailScreen.kt:556-567`) and a
  lime "Aggiornata automaticamente" strip (`:568-589`). Detail parity
  is good; mockup's "Prossimo aggiornamento domani · 06:00" caption
  has no equivalent — code shows `"Aggiornata automaticamente —
  ${family.label}"` instead.

### [COPY]
- Mockup eyebrow `// GENERATA DAL SISTEMA` + title `"Per te"`
  (`mh-foryou.jsx:27-29`). Code matches (`ForYouScreen.kt:116, 119`).
- Mockup subtitle: `"${GEN_PLAYLISTS.length} playlist · aggiornate
  oggi"` (`mh-foryou.jsx:30-32`). Code: `"${autoPlaylists.size}
  playlist · aggiornate oggi"` (`ForYouScreen.kt:132-135`). ✅
- Mockup mix-grid eyebrow `// 6 MIX` + title `"I tuoi mix giornalieri"`
  (`mh-foryou.jsx:73-77`). Code matches (`ForYouScreen.kt:148`).
- Mockup `OGGI` mono micro-label on grid header (`mh-foryou.jsx:77`)
  — **not in code**.
- Mockup `// COME FUNZIONA` card body matches code copy
  (`mh-foryou.jsx:114-120` ↔ `ForYouScreen.kt:388-398`). ✅
- Mockup hero eyebrow `// IN ROTAZIONE`, code matches verbatim
  (`ForYouScreen.kt:223`).

### [STATE]
- Mockup static. Code adds `Loading / Error / Ready` plus
  `PullToRefreshBox` (`ForYouScreen.kt:71-87`). Empty state when no
  auto playlists exist not drawn in mockup.

### [BEHAVIOR]
- Hero CTA in mockup is `RIPRODUCI` (start playback). Code wires the
  whole hero `Box` clickable to navigate to playlist detail
  (`ForYouScreen.kt:209`) — not direct play. The button itself shares
  the navigation target.

---

## 4. NOW PLAYING — `ui/player/NowPlayingSheet.kt`

Mockup: `mh-screens.jsx:337-406` (`NowPlayingScreen`).

### [LAYOUT]
- Mockup hero cover: `300dp × 300dp`, `boxShadow:0 30px 80px`
  (`mh-screens.jsx:355-358`). Code uses
  `HeroCoverSize.NowPlayingFraction = 0.92f` capped at
  `NowPlayingMax = 360.dp` (`Shapes.kt:38-44`,
  `NowPlayingSheet.kt:292-294`). On a 390dp width frame this resolves
  to `~358dp` — **larger than mockup's 300dp**. Audit checklist §10
  said "current code uses 300dp cover" — **stale**, code went up to
  360dp ceiling. (Plan-locked because the cover IS the screen — see
  the comment at `Shapes.kt:33-37`.)
- Mockup top bar: `Down · centered "IN RIPRODUZIONE DA ALBUM /
  Slow Hours" · More` (`mh-screens.jsx:345-352`). Code top bar:
  `Down · centered "IN RIPRODUZIONE DA / album-or-artist" · Bedtime
  (sleep)` (`NowPlayingSheet.kt:241-288`). **Drift — the trailing
  slot is the Sleep Timer entry, not the More overflow** (overflow
  was relocated to the bottom action row at `:567-617`).
- Mockup transport row order: `Shuffle (lime active) · SkipPrev ·
  Play(70dp lime) · SkipNext · Repeat` (`mh-screens.jsx:384-394`).
  Code matches: shuffle, skipPrev (56dp button, 40dp icon), play
  (`72dp` filled lime, 40dp icon), skipNext, repeat
  (`NowPlayingSheet.kt:421-485`). Sizes drift by 2dp on the play
  button (mockup 70 vs code 72).
- Mockup bottom row: `Cast · "iPhone di Marco" · Share`
  (`mh-screens.jsx:397-401`). **Code bottom row is completely
  different**: `[Lyrics] [Video toggle / VideoFile + Tune] [Equalizer]
  [Queue] [Overflow]` (`NowPlayingSheet.kt:490-618`). Cast
  affordance does not exist; Share does not exist; device-name label
  does not exist.
- Heart icon position: mockup is to the right of the title block,
  inline with title row (`mh-screens.jsx:362-368`). Code matches —
  heart sits right of the `(title + artist)` Column in the title Row
  (`NowPlayingSheet.kt:338-374`). ✅

### [COPY]
- Mockup top eyebrow: `"IN RIPRODUZIONE DA ALBUM"` (uppercase, mono
  letter-spacing 1) (`mh-screens.jsx:348`). Code: `"IN RIPRODUZIONE
  DA"` (no qualifier suffix) (`NowPlayingSheet.kt:257`).
- Title font sizes drift: mockup title 24/800/-0.5, artist 14
  (`mh-screens.jsx:364-365`). Code uses
  `MaterialTheme.typography.headlineSmall` for title and
  `bodyLarge` for artist (`NowPlayingSheet.kt:343-352`).
- Code-only labels (no mockup equivalent): `"Re-download from
  source"`, `"Refresh local copy"`, `"Save as alarm sound"`,
  `"Report wrong song"` overflow items (`NowPlayingSheet.kt:579-616`).

### [STATE]
- Mockup is single static state. Code wires every transport state +
  scrubbing + shared-element transition + 6 alert dialogs
  (`NowPlayingSheet.kt:640-764`).
- Inline video state (`showVideo`) replaces the cover with a 16:9
  PlayerView and **hides the audio scrubber + transport row**
  (`NowPlayingSheet.kt:294-307, 384-488`). This satisfies missing-mockup
  §10 "video active → audio scrubber + audio controls hidden".

### [BEHAVIOR]
- Audit checklist (§10) item: **"action-bar icon swap library-video /
  playing-note"**. Code does swap: `Icons.Filled.MusicNote` when video
  active vs `Icons.Filled.VideoLibrary` when not
  (`NowPlayingSheet.kt:506-507`). ✅
- Sleep timer popover (mockup §3.3 unspecified). Implementation only
  offers `15 / 30 / 60 minutes + Cancel timer` — **mockup spec wants
  `5 / 10 / 15 / 30 / 45 / 60 + "Fine traccia"` plus countdown badge**
  (missing-mockup §3.3). Mockup gap unrelated to this audit but flagged.

---

## 5. LYRICS — `ui/player/LyricsSheet.kt`

Mockup: `mh-extras.jsx:7-73` (`LyricsScreen`).

### [LAYOUT]
- Mockup is full-screen Lyrics surface: gradient bg matching Now
  Playing (`#1E3A8A → #060309`), top bar `Down · "TESTO" / Undertow ·
  More` (`mh-extras.jsx:21-34`), 44dp cover row + `SINCRONIZZATO`
  badge (`mh-extras.jsx:36-49`), the lines list, and a footer with
  `Share · Pause(56dp lime) · "1:32 / 4:02" mono`
  (`mh-extras.jsx:62-70`).
- **Code has no full-screen Lyrics analogue.** `LyricsView`
  (`LyricsSheet.kt:39-158`) is an inline panel rendered **inside
  NowPlayingSheet** when `showLyrics` is toggled
  (`NowPlayingSheet.kt:620-626`). `LyricsSheet` (`:160-177`) wraps
  the same `LyricsView` in a `ModalBottomSheet` but is not the
  in-Player toggle path.

### [COPY]
- Mockup section title `"TESTO"` (`mh-extras.jsx:30`). Code title is
  `"Lyrics"` (`LyricsSheet.kt:82`) — **English drift, no Italian
  copy**.
- `SINCRONIZZATO` lime badge (`mh-extras.jsx:44-48`) — **not in code**.
- No-lyrics empty state copy: code `"No lyrics available"` /
  `"No lyrics found"` / button `"Download lyrics"` /
  `"Try again"` / `"Downloading…"` (`LyricsSheet.kt:99-134`).
  **All English**, no mockup equivalent (the mockup never models the
  empty state).

### [STATE]
- Mockup draws synced-active state only. Code adds `loading` spinner,
  `noLyrics` empty + import action, `importFailed` retry.
  (`LyricsSheet.kt:46-65, 87-136`).

### [BEHAVIOR]
- §10 audit "fade-active": mockup uses
  `color: l.active ? T.TEXT_HI : (l.past ? rgba(255,255,255,0.35)
  : rgba(255,255,255,0.5))` and font sizes 26 (active) vs 22
  (`mh-extras.jsx:53-58`). **Code fade is binary, not tri-state**:
  active line uses `colorScheme.primary`, every other line uses
  `onSurfaceVariant` (`LyricsSheet.kt:147-150`). Past/future lines
  share the same colour — **no fade-out for past lines**, no
  enlargement on the active line, font is `bodyLarge` for all rows.
- Mockup auto-scrolls smoothly; code calls
  `listState.animateScrollToItem(activeIndex)` on activeIndex change
  (`LyricsSheet.kt:74-78`). ✅
- Footer transport (`Share / Pause / "1:32 / 4:02"`) does not exist
  in code — controls are inherited from the surrounding NowPlaying
  surface.

---

## 6. VIDEO — `ui/player/VideoPlayerSheet.kt` (`VideoPlayerInline`)

Mockup: `mh-extras.jsx:78-204` (`VideoScreen`).

### [LAYOUT]
- Mockup is a full-screen video page with a 16:9 hero, title block,
  artist+follow strip, action chips (`Heart 24K · Condividi · Salva ·
  Audio only`), and a `// VIDEO CORRELATI` related-videos list
  (`mh-extras.jsx:78-204`).
- **Code has no equivalent screen.** `VideoPlayerInline`
  (`VideoPlayerSheet.kt:46-100+`) is an inline 16:9 PlayerView
  embedded inside `NowPlayingSheet` (`NowPlayingSheet.kt:293-307`)
  with a fullscreen Dialog escape hatch (`:101-`). No metadata block,
  no action chips, no related videos.

### [COPY]
- Mockup `VIDEO 4K` red-dot live pill (`mh-extras.jsx:103-110`),
  artist follow CTA `"Segui"` (`mh-extras.jsx:159-162`),
  `"X.X visualizzazioni · X mesi fa"`, `// VIDEO CORRELATI` —
  **none of these strings exist in code**.

### [STATE]
- Mockup single static. Code states: `loading`, `playing`,
  `fullscreen`, `download in progress` (`videoDownloading`),
  `download error`, `reinitializing`, `reinit error`
  (`NowPlayingSheet.kt:514-552, 719-740`).

### [BEHAVIOR]
- §10 audit "audio scrubber + audio controls hidden when video
  active": ✅ confirmed (`NowPlayingSheet.kt:384`). Code pauses the
  audio player when entering video and resumes on exit
  (`:180-190`).
- §10 audit "action-bar icon swap library-video / playing-note": ✅
  confirmed (`NowPlayingSheet.kt:506-512`).
- Mockup's "Audio only" chip toggle has its analogue in code's
  video-toggle IconButton — same end behavior, different chrome.

---

## 7. PLAYLIST DETAIL — `ui/playlists/PlaylistDetailScreen.kt`

Mockup: `mh-screens.jsx:541-626` (`PlaylistScreen`); detail variant
in `mh-foryou.jsx:132-247` (`GeneratedDetailScreen`).

### [LAYOUT]
- Mockup hero: `180dp` square cover centered, gradient backdrop
  `#1F0833 → #060309`, title 26/800, owner avatar `M` + "Marco · 7
  brani · 25 min", action row `Heart · Download · Plus(add) · Share
  · Shuffle · Play(52dp lime)` (`mh-screens.jsx:552-602`).
- Code hero: `SpotifyHero` composable (used at
  `PlaylistDetailScreen.kt:358-384`); per `Shapes.kt:39-40`
  `DetailFraction = 0.6f`, `DetailMax = 280.dp` — at 390dp this is
  `234dp` cover, **larger than mockup's 180dp**. Tone OK but size
  drifts up.
- Mockup track row: 44dp cover + title + `artist` (no album), mono
  duration, `More` (`mh-screens.jsx:605-622`). Code uses
  `SongRow` (see §8 below) inside a `SwipeToDismissBox` +
  `ReorderableItem` (`PlaylistDetailScreen.kt:417-481`) with
  `HorizontalDivider` between rows. Layout drift in the row itself
  is covered in §8.
- Mockup top bar `Back · More` (`mh-screens.jsx:557-560`); code top
  bar is `Back + Refresh + Share + Add`
  (`PlaylistDetailScreen.kt:127-198`). **No mockup equivalent for
  Refresh / Share / Add icons** (mockup hides everything behind
  the trailing `More`).

### [COPY]
- Mockup playlist eyebrow `// PLAYLIST` (`mh-screens.jsx:570-572`).
  Code subtitle is dynamically built — `"Playlist · N brani"` or
  `"<family> · N brani"` for auto-playlists, plus collab strings
  `"Condivisa da X"` / `"Condivisa con N persone"`
  (`PlaylistDetailScreen.kt:332-352`). Eyebrow string itself isn't
  rendered as a separate `// PLAYLIST` mono line.
- Mockup empty state line (none drawn). Code: `"No songs yet" / "Add
  some from the Search tab or tap +."` (`:412-413`) — **English,
  no mockup**.
- Auto-sync card copy: `"Sincronizzazione automatica" /
  "Scarica i nuovi brani all'apertura dell'app. Disattivata per
  impostazione predefinita."` (`PlaylistDetailScreen.kt:514-523`).
  Mockup has no auto-sync card (missing-mockup §6.2 deferred).
- Auto-meta strip eyebrows: `AGGIORNATA · BRANI` mono. Mockup
  `mh-foryou.jsx:166-172` matches verbatim. ✅
- "Aggiornata automaticamente — ${family.label}" strip
  (`PlaylistDetailScreen.kt:584-587`) replaces mockup's "Prossimo
  aggiornamento domani · 06:00".

### [STATE]
- Mockup static. Code: `Loading (shimmer) / Error (retry) / Success`
  (`PlaylistDetailScreen.kt:202-235`). `share-link` minting in-flight
  (`sharing` flag) and download / removal toasts also unmodelled.

### [BEHAVIOR]
- §10 audit "long-press to reorder, no dedicated drag handle":
  ✅ confirmed. Each row's gesture modifier is
  `Modifier.longPressDraggableHandle()` injected via
  `rowGestureModifier` for non-auto playlists
  (`PlaylistDetailScreen.kt:475-477`). No drag handle icon is
  rendered. Drag state managed by
  `rememberReorderableLazyListState` (`:302-308`); server commit
  fires once dragging settles (`:316-326`).
- Swipe-to-remove via `SwipeToDismissBox` end-to-start
  (`:443-466`); auto-playlists disable the affordance
  (`enableDismissFromEndToStart = !playlist.isAuto`).

---

## 8. SONG ROW — `ui/search/SongRow.kt`

Mockup: track-row instances in
`mh-screens.jsx:81-104` (Home), `:605-622` (Playlist),
`mh-foryou.jsx:208-227` (For You detail).

### [LAYOUT]
- §10 audit "v0.12.7 layout: subtitle 'Artista • 3:42', heart next to
  kebab". Code:
  - 44dp cover + 12dp gap + flexible title/subtitle column (`SongRow.kt:59-81`).
  - Subtitle row: `[downloaded badge?] artist " • " duration`
    (`SongRow.kt:119-160`). ✅ subtitle structure matches checklist.
  - Heart sits at the end before kebab (`SongRow.kt:85-104`,
    `:106-115`). ✅ matches "heart next to kebab".
- Mockup's per-screen rows use 44dp cover + duration on the right
  + kebab at end. Visual order in mockup is
  `[index?] cover title/artist heart? duration more`
  (`mh-screens.jsx:83-103`). **Code drops the duration column on
  the right** — duration is folded into subtitle as `"artista •
  3:42"` (`SongRow.kt:147-158`). For the Home recommended list this
  is a structural divergence from `mh-screens.jsx:100`, which renders
  duration as a separate 32-wide mono cell.
- Mockup playing-bars indicator (`MHPlayingBars` in
  `mh-shared.jsx:282-298`) renders next to the title for the
  currently-playing row (`mh-screens.jsx:91-95`). **Code has no
  playing-bars indicator** — the active row is not visually
  highlighted in `SongRow`.

### [COPY]
- Subtitle separator: mockup uses `" · "` middle-dot
  (`mh-screens.jsx:97`); code uses `" • "` bullet
  (`SongRow.kt:148`). Different glyphs. (Note: For You + playlist
  subtitle structure in mockup is just `artist` without duration —
  the audit checklist's "Artista • 3:42" is code-only.)

### [STATE]
- Code states: liked (filled vs outlined heart, primary tint),
  downloaded (12dp `FileDownloadDone` icon prefix), basicMarquee on
  long titles. Mockup models liked + playing only.

### [BEHAVIOR]
- Long-press on row delegated to caller via `rowGestureModifier`
  (used by playlist detail for reorder).
- Heart tap fires `HapticFeedbackType.LongPress`
  (`SongRow.kt:91`).
- Kebab opens `AddToPlaylistSheet` at every call site (Search, Home
  Music filter, PlaylistDetail).

---

## Summary — checklist resolution against missing-mockup §10

| Checklist item | Status | Reference |
| --- | --- | --- |
| Now Playing 300dp cover | **Drift** — code uses fraction 0.92 capped at 360dp (effective 290–360dp on common widths) | `Shapes.kt:42-43`, `NowPlayingSheet.kt:292-294` |
| Mini-player swipe-to-close consistent | ✅ — `SwipeToDismissBox` both directions trigger `dismissPlayback` | `MiniPlayer.kt:81-101` |
| LyricsView fade-active | **Drift** — fade is binary (active vs everything else), no past-line dim, no font-size bump | `LyricsSheet.kt:147-150` |
| Video surface hides audio scrubber + controls when active | ✅ | `NowPlayingSheet.kt:384-488` |
| Video action-bar icon swap (library-video / music-note) | ✅ | `NowPlayingSheet.kt:506-512` |
| PlaylistDetail long-press reorder, no drag handle | ✅ | `PlaylistDetailScreen.kt:475-477, 302-326` |
| SongRow v0.12.7 layout (subtitle "Artista • 3:42", heart next to kebab) | ✅ | `SongRow.kt:119-160, 85-115` |

## Cross-cutting drifts worth flagging to design

1. **Bottom nav contract**. Mockup `MHBottomNav`
   (`mh-shared.jsx:358-378`) advertises **4 tabs**: Home / Cerca / Per
   te / Libreria. App bottom nav (`MainActivity.kt:807` "Per te")
   includes `Per te` as a tab — confirmed parity. Logo is **not** a
   nav surface, but mockup screens still render `MHLogo` in the top
   bar; code never does.
2. **Italian-only copy promise**. Lyrics surface still emits English
   strings (`"Lyrics"`, `"Download lyrics"`, `"No lyrics available"`).
   PlaylistDetail empty state ("No songs yet"). Now Playing overflow
   menu (`"Re-download from source"` etc.). PlaylistCardSquare ("song"
   / "songs"). All flagged for localisation pass.
3. **Logo + history icon**. Mockup pins `MHLogo` and `History` icon
   in top bars (Home and Per te). Code uses the screen title text +
   Settings only. Decision needed: drop from mockup or add to code.
4. **Now Playing bottom row mismatch**. Mockup's `Cast · device-name
   · Share` row vs code's `Lyrics · Video · EQ · Queue · Overflow`.
   Mockup is stale — the cast/share bottom row no longer reflects
   shipping product. Recommendation: refresh mockup, not code.
5. **Track row indicator**. The mockup-canon `MHPlayingBars`
   indicator is not implemented anywhere. Active-row highlight in
   code lives only inside the player surfaces, not in lists.
6. **No standalone full-screen Lyrics or Video screen**. Both live as
   inline blocks inside Now Playing. The dedicated full-screen
   mockups in `mh-extras.jsx` describe a route that does not exist
   in the app. If the app intentionally consolidated, the mockups
   should be retired or rebadged as "expanded inline" states.
