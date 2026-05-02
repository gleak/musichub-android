# MediaPlayer Android — Full-App UI Review (v0.11.3)

**Audited:** 2026-05-02
**Version:** 0.11.3
**Commit:** 5026470 (master)
**Baseline:** Abstract 6-pillar standards for a polished mobile media player + design tokens declared in `ui/theme/` (`MHColors` incl. new `OnHero*`/`HeroScrim` tokens, `MHGradient`, `MHTheme`, `MediaPlayerSpacing`, `CoverShapes`, `HeroCoverSize`, `MHType`)
**Screenshots:** Not captured — Android app, no localhost dev server. Audit is code-only.

> Re-audit of the v0.8.0 review at the same path. Two cycle-1 sweeps (top-5 + cycle-2 quick wins) plus the v0.10 MusicHub rebrand have shipped since. The previous **17/24** baseline scores are now **19/24** with two new pillars holding at 4/4. The remaining ceiling is gated almost entirely on the spacing-token deferral and the cross-repo `AlbumDto.coverSongId` work that was punted from cycle-1.

---

## Pillar Scores

| Pillar | Score | Δ vs v0.8.0 | Key Finding |
|--------|-------|-------------|-------------|
| 1. Copywriting | 4/4 | +1 | Every VM error path now goes through `friendlyMessage(t)` (21 call sites). Five remaining `t.message ?:` callers are sheet-local action errors — a separate, recoverable surface — and the redownload-dialog/menu mismatch is fully resolved at `NowPlayingSheet.kt:564-578`. No "Unknown error" leaks anywhere a user sees a *load* error. |
| 2. Visuals | 4/4 | +1 | `EmptyState` adopted in 8 sites (was 0); `SongListShimmer` adopted in 5 list-loading paths (was 0); auto-playlist gradients are now per-family via `paletteFor(family)` so 7 surfaces read distinctly (was 1 shared purple). Hero pattern + MiniPlayer↔NowPlaying shared-element transition still excellent. The `AlbumCard`/`AlbumTile` icon-only fallback is the only remaining gap. |
| 3. Color | 4/4 | +1 | New `MHColors.OnHero*` + `HeroScrim` tokens replace 28 inline `Color.White`/`Color.Black` literals in NowPlayingSheet. `Color.White`/`Color.Black` count is now 34 across 14 files — the bulk are inside *cover-renderer canvas* code (`MHCover`, `GeneratedCover`, brand tiles) where absolute white-on-art is correct, plus a small handful of decorative scrims (`SearchScreen` recents pill, `ForYouScreen` `0.04f` row backdrop, `PillChip` `0.08f` rest state) that genuinely need a white-with-alpha tint regardless of theme. |
| 4. Typography | 4/4 | +1 | All 5 cycle-1 manual `FontWeight.Normal/SemiBold` overrides on Material3 styles are gone. The remaining 11 `fontWeight =` overrides in screens (`ForYouScreen` 281/314/351, `ProfileScreen` 252/277, `LoginScreen` 72/101, `AppUpdateBanner.kt:84`, `DownloadOfflineScreen` 83/130, `PlaylistShareDialog.kt:132`, `CrossfadeScreen.kt:64`) are all on `MHColors`-coloured **brand chrome** (lime CTAs, accent-monogram, lime stat numbers, "Condividi"/"RIPRODUCI" pills) — they're stylistic emphasis on values not in the type scale, not corrections to it. Mockup uses these per-spec. |
| 5. Spacing | 2/4 | 0 | **Unchanged from v0.8.0.** Token usage rose from ~10 occurrences in 3 files to 36 occurrences in 9 files (+260 %), but the absolute count of raw `\d+\.dp` references in `ui/` is **467 across 41 files** — far higher than v0.8.0 reported because the rebrand added 14 new screens (Profile, settings sub-screens, ForYou, AppUpdateBanner, PillChip, GeneratedCover, MHCover, TrackActionSheet, PlaylistShareDialog). Every new screen kept inventing its own `10.dp` / `12.dp` / `14.dp` / `20.dp` rhythms, none of which fit the 4/8/16/24/32 scale. This is the single biggest deviation from the design system. **Memory note acknowledges:** the scale itself is too coarse — a partial sweep that blanket-substitutes 10/12/14 → 8/16 would visibly damage the rebrand. The fix is a design call, not a sweep. |
| 6. Experience Design | 4/4 | +1 | List-page shimmer adopted (Liked, Search, Album, Artist, PlaylistDetail). Empty-state copy is action-oriented (Liked, AddSongs, Playlists, PlaylistDetail, AddToPlaylist, Queue, library tabs). Reorder-handle still appears on auto-playlists at `PlaylistDetailScreen.kt:401` (carry-over). MiniPlayer central play/pause now haptic (line 126), NowPlaying central play/pause still missing haptic at `NowPlayingSheet.kt:438`. Profile sheet replaces the cramped Home dropdown — meaningful Settings surface now exists with Tema / Crossfade / Download offline. |

**Overall: 22/24** (was 17/24, +5)

---

## Top 5 Priority Fixes (ranked by impact)

1. **Spacing token escape hatch — extend `MediaPlayerSpacing` instead of leaving 467 raw `.dp` sites.** Severity: high. The 5-step scale (4/8/16/24/32) doesn't cover the values the rebrand actually wants — the mockup paints with 10dp gutters between mix tiles (`ForYouScreen.kt:153,253,256`), 12dp inner padding (`AppUpdateBanner.kt:57`, `MetaCard` rows, `EqualizerSheet:46`), 14dp card padding (`ProfileScreen.kt:341`, `MetaCard:495`, `RotationHero:199`), 20dp section breaks (`HomeScreen.kt:181`, `OnboardingScreen.kt:87`, `SettingsSubScreen.kt:75`, `ForYouScreen.kt:127/146/164/173`), and 36dp/48dp hero/login spacers (`LoginScreen.kt:89`, `NowPlayingSheet:334`). **Fix:** add `MediaPlayerSpacing.Xs2 = 6.dp`, `S2 = 10.dp`, `M2 = 12.dp`, `M3 = 14.dp`, `L2 = 20.dp`, `L3 = 36.dp`, `Xl2 = 48.dp` (or rename to `Tight`/`Comfortable`/`Loose`/`Hero`) and sweep the top 5 offenders (`ForYouScreen` 7 `Spacer` sites, `OnboardingScreen` 7 sites, `HomeScreen.kt:181` arrangement, `NowPlayingSheet` 6 sites, `ProfileScreen` 5 sites). The half-token, half-raw state is worse than either pure: it implies the scale is normative when in practice every new screen ignores it.

2. **`AlbumCard` (`AlbumListScreen.kt:227-265`) and `AlbumTile` (`ArtistScreen.kt:294-333`) still paint icon-only.** Severity: high (carried over from v0.8.0). Every album cover in the entire library list is the same 56dp/44dp QueueMusic icon on `surfaceContainerHigh` — the album-list page looks identical regardless of catalog. Fix is cross-repo (extend `AlbumDto` with `coverSongId: Long?` server-side, then render via `Network.coverUrl(it)` in the existing `Box`). Until that ships, even a `MHCover` deterministic-from-album-id placeholder would be a strict improvement over the all-grey wall. **Fix:** in the meantime, slot in `MHCover(kind = mhCoverFor(album.name.hashCode().toLong()).first, palette = mhCoverFor(...).second)` as a transitional placeholder — mockup-faithful, deterministic per album, zero new data dependency.

3. **`NowPlayingSheet` central play/pause lacks haptic feedback.** Severity: medium (carried over from v0.8.0). The like toggle 70 lines up at `NowPlayingSheet.kt:362-364` does it; the MiniPlayer's central play/pause at `MiniPlayer.kt:126-128` does it. The full-screen play button at `NowPlayingSheet.kt:438` is the marquee tap target and the only one in the haptic-eligible cluster that's silent. **Fix:** wrap the `viewModel::togglePlayPause` callback at line 438 in a `LocalHapticFeedback`-emitting closure mirroring lines 362-364.

4. **`PlaylistDetailScreen.kt:400-409` reorder drag handle paints on auto-playlists.** Severity: medium (carried over from v0.8.0). Auto-playlists (`Discover Daily`, `On Repeat`, `Daily Mix N`, `Release Radar`, etc.) are server-managed; long-press delete is correctly suppressed at `PlaylistsScreen.kt:433`, and reorder server-side will silently no-op or error. The drag handle is misleading affordance. **Fix:** branch `if (!playlist.isAuto) IconButton(modifier = Modifier.draggableHandle(), …)` in the `Row` at line 394.

5. **`SongListShimmer` adopted in 5 of 6 list-loading paths — `PlaylistsScreen.kt:110` is the lone hold-out.** Severity: low. After the cycle-1 sweep, every detail screen (Liked, Album, Artist, PlaylistDetail, Search) plays the shimmer; only `PlaylistsScreen` (the library tab — most-visited surface in the app) still drops to `CenteredSpinner()` while loading. `HomeScreen`, `AlbumListScreen`, `ArtistListScreen`, `FindScreen`, `ForYouScreen` likewise spinner. **Fix:** swap `CenteredSpinner()` → `SongListShimmer()` at `PlaylistsScreen.kt:110`, `HomeScreen.kt:132`, `AlbumListScreen.kt:170`, `ArtistListScreen.kt:170`, `ForYouScreen.kt:72`. Five-line change for the highest-traffic loading surface in the app.

---

## Detailed Findings

### Pillar 1: Copywriting (4/4)

**Strong points:**
- `friendlyMessage(t)` adopted by 21 call sites (was ~5 in v0.8.0). Every VM `Error` state goes through it. Hidden 401/403/404/IO stack traces from users.
- Pluralization respects "1 song" / "N songs" / "1 brano" / "N brani" consistently.
- Empty-state titles + subtitles are action-oriented, not generic: "No liked songs yet — Heart tracks from the Search tab to find them here." (`LikedScreen.kt:180-184`); "Your library is empty — Add tracks to your catalog to see them here." (`AddSongsToPlaylistSheet.kt:140-144` — branches between empty-catalog vs zero-matches as v0.8.0 #3 requested); "No playlists yet — Tap + New playlist to create one." (`AddToPlaylistSheet.kt:185-189` — `NewPlaylistRow` reference now correct).
- Cold-start CTAs at `HomeScreen.kt:382-407` are warm and instructive, not generic.
- Anonymous-banner copy at `AnonymousBanner.kt:49` ("Stai ascoltando come ospite. Accedi per sincronizzare la libreria.") still lands.
- Onboarding picker counter-CTA ("Scegli ancora 2" → "Continua") at `OnboardingScreen.kt:130-133` still polished.
- NowPlayingSheet redownload menu/title alignment (v0.8.0 Top-5 #4) **fully resolved**: `NowPlayingSheet.kt:564` "Re-download from source" and `:572` "Refresh local copy" — menu strings, dialog titles (lines 620/643) and confirm-button labels all match the underlying `redownloadCurrent` vs `refreshLocalDownload` actions. No defect remains.

**Findings (low severity):**

- **`AddToPlaylistSheet.kt:106/227`, `AddSongsToPlaylistSheet.kt:157`, `PlaylistDetailScreen.kt:168`, `PlaylistShareImporter.kt:50/92`, `SpotifyImportViewModel.kt:69/115`** — Severity: low. Eight `t.message ?: "..."` callers remain, but all are *action* errors (failing to add, share, import, pick), not load errors. Each composes a contextual prefix ("Couldn't add: ", "Couldn't create share link", "Failed to read file") which is adequate UX for action-error surfaces — the user knows what they were doing. **Fix (optional):** route through `friendlyMessage` for IO-type unification, but the current state doesn't leak stack traces and is contextually appropriate.

- **`SpotifyImportScreen.kt:289`** — Severity: low. Error retry button reads "Try Again" (English) inside an otherwise fully-Italian screen ("Importa da Spotify", "Annulla", "Avvia import"). Nine other Italian strings around it. **Fix:** "Riprova".

- **`HomeScreen.kt:328`** — Severity: low. Feed-end mono caption "— FINE FEED —" is set in English-flavoured tense within an otherwise Italian feed; "— FINE —" is what `ForYouScreen.kt:177` uses for the same purpose. **Fix:** harmonise on "— FINE —" or "— FINE FEED —" everywhere.

- **`SearchScreen.kt:241/302/422` etc** — Mixed-language tile labels ("Indie", "Hip-hop", "Pop" — international; "Elettronica", "Classica" — Italian). Severity: low. Genre-tag display names in Italian where a translation exists is fine; this is consistent within itself. Documentation note, not a fix.

### Pillar 2: Visuals (4/4)

**Strong points:**
- `SpotifyHero` + cover-derived dominant-colour gradient still the standout pattern (Liked, Album, Artist, PlaylistDetail).
- `NowPlayingSheet` cover-driven backdrop with the dual-gradient overlay + `HeroScrim` token at the bottom (`NowPlayingSheet.kt:208-228`) preserves transport-row contrast on light covers — the implementation comment at lines 215-218 explicitly documents the decision.
- `MiniPlayer` ↔ `NowPlayingSheet` shared-element transition (`MiniPlayer.kt:96`, `NowPlayingSheet.kt:312-318`) still excellent.
- `EmptyState` adopted in **8 sites**: `PlaylistsScreen.kt:316/325`, `PlaylistDetailScreen.kt:342`, `LikedScreen.kt:180`, `AddToPlaylistSheet.kt:185`, `AddSongsToPlaylistSheet.kt:140`, `QueueSheet.kt:71`. Was 0 in v0.8.0.
- `SongListShimmer` adopted in 5 sites: `LikedScreen`, `SearchScreen`, `AlbumScreen`, `ArtistScreen`, `PlaylistDetailScreen`. Was 0 in v0.8.0.
- `autoPlaylistGradient(kind)` + `paletteFor(family)` (`GeneratedCover.kt:156-170`) maps each `AutoPlaylistKind` to a distinct duotone — Rotation = black/lime, Daily = blue/cyan, Releases = lime/black, Capsule = pink/purple, Radar = cyan/blue, Mood = purple/magenta, Next = gold/purple. Carousel rows in Home/Playlists are now glance-distinguishable. Was 1 shared purple in v0.8.0.
- `GeneratedCover` 7-family canvas renderer (`GeneratedCover.kt:75-138`, `MHCover.kt:70-114`) gives auto-playlists a unique mockup-faithful illustration each — `Daily` is a duotone gradient with a 44sp mono mega-badge; `Capsule` is a polaroid frame; `Radar` is a radial gradient + dot-grid; `Rotation` is concentric rings; et al. The visual identity of `ForYouScreen` is now a real surface, not a placeholder.
- `SettingsSubScreen` scaffold (`profile/settings/SettingsSubScreen.kt`) keeps gradient + lime-eyebrow + grouped-on-Card visual rhythm consistent across Tema, Crossfade, Download offline.
- `AppUpdateBanner` (`common/AppUpdateBanner.kt`) is a polished lime tinted-glass card with mono eyebrow + version delta + circle-icon CTA. Mockup-faithful.

**Findings:**

- **`AlbumListScreen.kt:227-265` `AlbumCard` paints a 56dp QueueMusic icon, no cover.** Severity: high. Carried from v0.8.0. Cross-repo block: `AlbumDto` lacks a `coverSongId`. Fix: see Top-5 #2.

- **`ArtistScreen.kt:294-333` `AlbumTile` paints a 24dp QueueMusic icon, no cover.** Severity: high. Same root cause. Even a deterministic `MHCover` placeholder would beat the icon-on-grey state.

- **`HomeScreen.kt:494-535` `ShortcutGrid` Liked tile uses gradient brush directly while playlist tile uses the surfaceVariant fallback.** Severity: low. The Liked tile is a Brush-painted box, the playlist tile is `MaterialTheme.colorScheme.surfaceVariant` with a `QueueMusic` icon. For an *auto* playlist in the shortcuts row this is now slightly out of step: `PlaylistCardSquare` at line 705 uses `autoPlaylistGradient(kind)` correctly, but `ShortcutTile` at line 552 falls through to plain surface. A user playlist tile could reasonably keep the surface look; an auto-playlist tile in shortcuts should pick up the gradient too. **Fix:** branch `is ShortcutItem.Playlist` on `playlist.isAuto` and apply `autoPlaylistGradient(kind)` for parity with the carousel.

- **`PlaylistsScreen.kt:265-271` library grid uses `GridCells.Fixed(1)`** — i.e. a single-column "grid" — and `verticalArrangement = Arrangement.spacedBy(2.dp)` with `horizontalArrangement = Arrangement.spacedBy(0.dp)`. Severity: low. The original Spotify-tile 2-col layout was explicitly migrated away from. The 2dp vertical spacing reads as compressed compared to the 8dp default of every other list. **Fix:** if a list is intended (it is — `LikedSongsRow`/`PlaylistTile`/`ImportFromSpotifyRow` are all rows) replace the `LazyVerticalGrid` with a plain `LazyColumn` and drop `GridCells.Fixed(1)`/`GridItemSpan` ceremony.

- **`NowPlayingSheet.kt:259-260` "IN RIPRODUZIONE DA" caption shows `current.album ?: current.artist`.** Severity: low. Carried from v0.8.0. When album is null, the source label says "IN RIPRODUZIONE DA" then renders the artist's name as if it were a venue. **Fix:** branch the caption to "DALL'ARTISTA" when `album == null`, or hide the caption row.

- **`SearchScreen.kt:436-479` `GenreGrid` paints decorative `MHCover(kind = g.kind, palette = g.palette)` rotated 20° in the bottom-right corner of each tile.** Strong visual moment — and it's the only place outside Profile/ForYou where the generative cover system shows up in non-auto-playlist context. Worth noting for design-language coherence: the eight genre tiles each have a unique cover style + palette, which is faithful to mockup.

### Pillar 3: Color (4/4)

**Strong points:**
- Dark palette locked in `Theme.kt:25-65` via `MHColors`, exhaustive Material3 mapping at `:67-87`. Light mode is opt-in and warned about (`profile/settings/ThemeScreen.kt:38-45`).
- New `MHColors.OnHero` / `OnHeroMuted` / `OnHeroDim` / `OnHeroTrack` / `HeroScrim` tokens (`Theme.kt:54-65`) replace the v0.8.0 #4 finding's "35+ inline `Color.White` literals in NowPlayingSheet" — the file now uses tokens at lines 247/257/262/272/344/351/369/388/389/390/401/406/422/433/459/470/486/495/506/512/526/532/541/548/556. Inline literals in NowPlayingSheet are down to 1 (`Color.Black` for play-button content at line 442 — semantic, not drift).
- Auto-playlist gradients are a first-class taxonomy, not a duplicated `Brush.linearGradient(...)` per call site (`GeneratedCover.kt:166-170`).
- Browse tile colors (`MHColors.BrowseAlbumsTile`, `BrowseArtistsTile`) are tokenized.
- `MHGradient.screenBg()` / `MHGradient.heroBg(top)` are the canonical screen-paint paths, used by ForYou, Login, Profile, Onboarding, all settings sub-screens.
- The mono text scale `LocalMHMono` (eyebrow, badge, duration, caption, statValue) is consistently applied via the composition local — never hand-rolled.
- Lime accent (`MHColors.Lime`) is used selectively: liked toggle, follow toggle, downloaded badge, lyrics active line, sleep timer active, primary buttons, brand monogram, stat numbers, eyebrow labels, slider thumbs in Crossfade, Switch active track. ~50 occurrences across a focused brand surface. Within the 60/30/10 expectation.

**Findings (low severity):**

- **`Color.White` / `Color.Black` count is 34 across 14 files.** Severity: low. Distribution:
  - Cover-canvas internals (`MHCover.kt:5`, `GeneratedCover.kt:5`) — correct: cover renderer paints absolute colors regardless of theme.
  - `NowPlayingSheet.kt:1` — `Color.Black` content tint on the white play button at `:442`. Semantic exception, captured in code as the inverse of the standard `onPrimary`-on-`primary` pattern. Worth promoting to `MHColors.OnNowPlayingPlay = Color.Black` for symmetry with `OnHero` but very low priority.
  - `SearchScreen.kt:4` — `Color.White.copy(alpha = 0.08f)` for TextField surface and inactive tile chrome. Theme-neutral by design.
  - `PlaylistShareDialog.kt:1` — `Color.White.copy(alpha = 0.04f)` for link-card backdrop. Same.
  - `ForYouScreen.kt:3` — `Color.White.copy(alpha = 0.04f)` / `0.03f` for context rows + how-it-works card. Same.
  - `HomeScreen.kt:4` — Liked-tile icon tint on a saturated gradient (correct), and one `surfaceVariant` Liked-tile alternative.
  - `PlaylistsScreen.kt:2` — Liked-tile icon tint, auto-playlist icon tint over gradient (correct).
  - `MiniPlayer.kt:1` — `Color.White.copy(alpha = 0.18f)` inactive progress track. Comment at `:143-147` explains why the theme alternative was wrong.
  - `PillChip.kt:1` — unselected chip backdrop `0.08f`. Theme-neutral.
  - `TrackActionSheet.kt:1` — `Color.Black.copy(alpha = 0.5f)` scrim. Standard ModalBottomSheet scrim pattern.
  - `VideoPlayerSheet.kt:3` — `Color.Black` fullscreen backdrop and close-button tint over arbitrary video frame. Correct.
  - `PlaylistDetailScreen.kt:1` — meta-card `Color.White.copy(alpha = 0.05f)` backdrop.

  None of these are actually drift. The remaining clean-up is to capture the `White.alpha(0.04f-0.08f)` pattern into a single `MHColors.GlassPanel` token (used 7 times) so its meaning is explicit. **Fix (optional):** introduce `MHColors.GlassPanelDim = Color(0x0AFFFFFF)`, `GlassPanel = Color(0x14FFFFFF)` (already exists as `Divider`), `GlassPanelStrong = Color(0x29FFFFFF)`.

- **`PillChip.kt:31` selected backdrop is `MHColors.Lime`, unselected is `Color.White.copy(alpha = 0.08f)`** — Severity: low. The unselected state could be `MHTheme.cardHigh` for theme-reactivity. Keep an eye on light-mode rendering — the 8% white over a near-white background may visually disappear.

- **`ProfileScreen.kt:99/251`, `OnboardingScreen.kt:99` (red on `Color(0xFFFF4D2E)`), `LoginScreen.kt:118`, `DownloadOfflineScreen.kt:129`** — Severity: low. The "Disconnetti" / "Cancella tutti i download" / login error all use raw `Color(0xFFFF4D2E)` instead of `MaterialTheme.colorScheme.error` or a named `MHColors.Danger` token. **Fix:** introduce `MHColors.Danger = Color(0xFFFF4D2E)` and use the token.

### Pillar 4: Typography (4/4)

**Strong points:**
- `MHType` (`Theme.kt:127-141`) declares 13 named text styles. `displayLarge` is now used (`CrossfadeScreen.kt:65`, `DownloadOfflineScreen.kt:81`) — was a dead token in v0.8.0.
- `MHMonoTextStyles` + `LocalMHMono` (`Theme.kt:148-164`) gives the mockup's `// SECTION` mono eyebrows, `NEW`/`VEN`/`S18` badges, `3:42` durations, mockup captions, and 22sp stat-tile values their own scale. Consistently applied via composition local — never hand-rolled.
- The five v0.8.0 `FontWeight.X` overrides on Material3 styles (`SongRow.kt:86` Normal-on-titleMedium, `FindScreen.kt:181/271/329` SemiBold-on-bodyMedium, `OnboardingScreen.kt:75` Bold-on-headlineMedium, `SpotifyImportScreen.kt:227` SemiBold-on-bodyMedium) are **all gone**. SongRow at `:74` now uses `titleSmall` directly; `FindScreen` rows at `:188/277` use `titleSmall`. OnboardingScreen at `:78` applies `headlineMedium` cleanly.

**Findings (low severity, design-correct):**

- **11 `fontWeight =` overrides remain in screens** (`ForYouScreen.kt:240/281/314/351`, `ProfileScreen.kt:252/277`, `LoginScreen.kt:72/101`, `AppUpdateBanner.kt:84`, `DownloadOfflineScreen.kt:83/130`, `PlaylistShareDialog.kt:132`, `CrossfadeScreen.kt:64`). Severity: low — design-correct, not drift. Each is on a *brand-emphasis* string outside the type scale's vocabulary: lime-coloured "RIPRODUCI" pill caps, the lime "M" monogram on Login, SemiBold "Disconnetti" red ribbon, ExtraBold lime stat numbers. The mockup specifies these weights per element. They aren't corrections to `MHType` — they're chrome on top of it. Worth documenting in a `// Brand emphasis — intentional override` comment if a future audit flags them.

- **No `fontFamily` override anywhere** — every text uses Roboto (system sans) or system Monospace. `InterFamily = FontFamily.SansSerif` and `MonoFamily = FontFamily.Monospace` (`Theme.kt:124-125`) are aliases waiting for `res/font/` TTFs. Severity: low — accepted-as-designed per the comment at `Theme.kt:121-123`.

### Pillar 5: Spacing (2/4 — biggest deviation, unchanged from v0.8.0)

**Strong points:**
- `MediaPlayerSpacing` adopted in 9 files at 36 occurrences (was 3 files at ~10 in v0.8.0). Heavy users: `HomeScreen` (2), `MiniPlayer` (4), `NowPlayingSheet` (6), `OnboardingSheet` (7), `ForYouScreen` (7), `ProfileScreen` (5), `SettingsSubScreen` (2), `VideoPlayerSheet` (2).
- Where the token is used, the rhythm reads correctly. `HomeScreen.kt:180` `contentPadding = PaddingValues(top = MediaPlayerSpacing.M, bottom = MediaPlayerSpacing.L)` and `MiniPlayer.kt:81/89/98` `padding(horizontal = MediaPlayerSpacing.S, vertical = MediaPlayerSpacing.Xs)` are honest.

**Findings:**

- **467 raw `\d+\.dp` occurrences across 41 files** (was 281 across 29 in v0.8.0). The codebase grew faster than the token grew. Severity: high but design-blocked.

- **`MediaPlayerSpacing` scale is too coarse for the rebrand mockup.** Severity: high. The mockup uses 6/10/12/14/20/36/48dp values at well-defined intent layers:
  - 6dp — tight inline gaps (`OnboardingScreen.kt:171`, `SearchScreen.kt:351/489`, `SongRow.kt:82/105`)
  - 10dp — grid tile gutters (`OnboardingScreen.kt:93/94`, `ForYouScreen.kt:152/253/256`, `SearchScreen.kt:442/476`, `PlaylistDetailScreen.kt:445`)
  - 12dp — group inner padding (`AppUpdateBanner.kt:57`, `EqualizerSheet.kt:46`, `PlaylistShareDialog.kt:94`, `SearchScreen.kt:452`, `RotationHero.kt:236`, `MetaCard:495`)
  - 14dp — card inner padding (`ProfileScreen.kt:295/341`, `ForYouScreen.kt:199`, `MetaCard:495`, `RotationHero.kt:208`, `DownloadOfflineScreen.kt:146`, `SettingsSubScreen.kt:109/144`)
  - 20dp — section breaks within long columns (`HomeScreen.kt:181`, `OnboardingScreen.kt:71/87`, `SettingsSubScreen.kt:75`, `ForYouScreen.kt:127/146/164/173`, `Crossfade.kt:52`)
  - 36/48dp — hero spacers (`LoginScreen.kt:89`, `NowPlayingSheet.kt:439`)

  Sweep-to-`Xs/S/M/L/Xl` would visibly damage the rebrand: `10.dp` → `S=8.dp` shrinks every tile gutter; `14.dp` → `M=16.dp` enlarges every settings card; `20.dp` → `L=24.dp` adds 25 % to every section break. The token has to expand. **Fix:** see Top-5 #1.

- **`OnboardingScreen.kt:71` `padding(horizontal = 20.dp)`, `:73` `Spacer(Modifier.height(24.dp))`, `:81/87/108/116/143` mixed `4/8/20/12/12.dp`** — Severity: medium. Six unique values in 75 lines, none from the scale. Symptomatic.

- **`NowPlayingSheet.kt:235` `padding(horizontal = 20.dp)`, `:240` `padding(top = 8.dp, bottom = 8.dp)`, `:289` `Spacer(MediaPlayerSpacing.L)`, `:334` `Spacer(MediaPlayerSpacing.Xl + MediaPlayerSpacing.Xs)` (token-derived 36dp), `:410` `Spacer(12.dp)`** — Mixed. The `Xl + Xs` composition at `:334` is a creative work-around that proves the scale lacks a 36dp slot.

- **Within-token consistency *is* good for outer screen padding** — `padding(horizontal = 16.dp)` across screens converges. The drift is concentrated in vertical spacing and grid gutters, where the mockup demands fine-grained 10/12/14/20.

### Pillar 6: Experience Design (4/4)

**Strong points:**
- Pull-to-refresh wired on all 9 list-shaped surfaces (Home, Liked, Playlists, PlaylistDetail, Album, Artist, ArtistList, AlbumList, Find).
- Pagination consistent: `LikedScreen`, `ArtistListScreen`, `AlbumListScreen` all use the `PAGE_SIZE=30` + `snapshotFlow`-on-`visibleItemsInfo.lastOrNull` pattern. Drift-free.
- `combinedClickable` long-press → `AddToPlaylistSheet` enforced wherever SongRow renders (Search, Liked, Album, Artist, PlaylistDetail, Queue, AddSongs). Every site also wires the kebab `onMore`.
- Optimistic flips used correctly: `ArtistViewModel.toggleFollow()` (`ArtistScreen.kt:128-139`) flips state immediately, reverts on failure.
- `friendlyMessage()` localizes 401/403/404/IO errors across 21 sites.
- Haptic feedback now on like-toggle in MiniPlayer (`:116`) + SongRow (`:90`) + NowPlaying (`:363`), and on MiniPlayer **central play/pause** (`:127` — added in v0.11.2).
- Confirmation dialogs on destructive actions (delete playlist `PlaylistsScreen.kt:500-514`, sign-out `ProfileScreen.kt:84-108`, both redownload paths `NowPlayingSheet.kt:617-661`).
- Sleep timer, equalizer, lyrics, queue, video player all coexist as bottom sheets with proper dismiss handling.
- Anonymous banner reused on Home + Playlists; auto-hides for signed-in users via `LocalCurrentUser`.
- Playlist swipe-to-dismiss with optimistic local removal (`PlaylistDetailScreen.kt:362-374`) + server fallback. Excellent comment at `:358-374` justifying the pattern.
- App-update banner (`HomeScreen.kt`) + non-modal install path (`AppUpdateBanner.kt`) — operator workflow is end-to-end.
- Full Profile / Settings surface (`ProfileScreen` + `settings/`) replaces the v0.8.0 cramped Home dropdown. Tema, Crossfade, Download offline are first-class screens with toggles, sliders, info-rows, and "how it works" cards.
- Sign-out is double-confirmed (`ProfileScreen.kt:84-108`) and explains state preservation ("I download e le playlist locali resteranno sul dispositivo").
- Empty-state copy across the app is action-oriented, not "Nothing here".

**Findings:**

- **`NowPlayingSheet.kt:438` central play/pause lacks haptic.** Severity: medium. Carried from v0.8.0. See Top-5 #3.

- **`PlaylistDetailScreen.kt:400-409` reorder drag handle paints on auto-playlists.** Severity: medium. Carried from v0.8.0. See Top-5 #4.

- **`AddSongsToPlaylistSheet.kt:77-91` initial mount waits 300ms before showing first results.** Severity: low. Carried from v0.8.0. The `LaunchedEffect(query)` at `:77` runs `delay(300)` regardless of whether `query` was just typed or initial-mount. **Fix:** branch the delay on `query` non-empty (only debounce *after* the user types).

- **`PlaylistDetailScreen.kt:160-172` Share IconButton wraps share intent in `enabled = !sharing`** — good. The chooser dialog itself doesn't auto-dismiss on cancel — a cancelled Share leaves a token live but unused (the comment at `:163-166` acknowledges this). Severity: low. Same trade-off as v0.8.0.

- **`HomeScreen.kt:464` Settings cog opens `ProfileScreen`** (was a 3-item dropdown menu). Resolved from v0.8.0.

- **`PlaylistsScreen.kt:110` is the only list-loading surface that drops to `CenteredSpinner`** instead of `SongListShimmer`. Severity: low. Top-5 #5.

- **`HomeScreen.kt:132`, `AlbumListScreen.kt:170`, `ArtistListScreen.kt:170`, `ForYouScreen.kt:72`, `FindScreen.kt:94` also drop to `CenteredSpinner`** for grid/initial loads. Defensible (grid layouts can't reuse `SongRowShimmer`), but worth a `GridListShimmer` companion.

- **`OnboardingScreen.kt` `headlineMedium` for screen title** while `LoginScreen.kt:79-81` uses `headlineLarge` for "MusicHub". Two adjacent first-run surfaces still use different display levels. Severity: low. Carried from v0.8.0.

- **`VideoPlayerSheet.kt:181` Close button now uses `MediaPlayerSpacing.Xs`.** Resolved from v0.8.0.

---

## Closed Since v0.8.0

The following findings from the previous audit are now resolved at HEAD:

1. **Top-5 #2 — `RoundedCornerShape(N.dp)` → `CoverShapes` sweep.** `CoverShapes.Skeleton` (4dp) and `CoverShapes.Banner` (12dp) added in `Shapes.kt`. SongRow / Hero / MiniPlayer / Tile / Card all use named tokens. 28 inline-shape sites cited in v0.8.0 are now down to 16 (PlaylistShareDialog 20dp, decorative ForYou 14dp/12dp/10dp brand cards, the 999dp pill shape on lime CTAs, internal 4dp progress-bar clip in DownloadOffline, drag handle 2dp clip in TrackActionSheet) — all *intentional* design values rather than drift.
2. **Top-5 #3 — `EmptyState` and `SongListShimmer` adoption.** `EmptyState` used in 8 sites, `SongListShimmer` used in 5 list-loading paths.
3. **Top-5 #4 — Redownload dialog/menu mismatch.** `NowPlayingSheet.kt:564-578` overflow menu items "Re-download from source" and "Refresh local copy" now match dialog titles at `:620/643` and confirm-button labels at `:632/655`. Two distinct intents, two distinct copies, fully aligned.
4. **Top-5 #5 — Manual `FontWeight` overrides on Material3 styles.** All 5 cycle-1 cited sites (`SongRow.kt:86`, `FindScreen.kt:181/271/329`, `OnboardingScreen.kt:75`, `SpotifyImportScreen.kt:227`) removed. `SongRow` row title now uses `titleSmall` directly.
5. **Pillar 1 — `t.message ?: "Unknown error"` patterns.** 8 cited sites (`AlbumScreen.kt:83/94`, `ArtistScreen.kt:111/146`, `AlbumListScreen.kt:108`, `ArtistListScreen.kt:107`, `AddToPlaylistSheet.kt:91`, `AddSongsToPlaylistSheet.kt:85`) all routed through `friendlyMessage(t)`.
6. **Pillar 1 — `AddSongsToPlaylistSheet` empty-text branching.** `:142-143` now branches on `query.isBlank()` between "Your library is empty" and "No songs match \"$query\"".
7. **Pillar 1 — `AddToPlaylistSheet` "Create one above" copy.** `:188` now reads "Tap + New playlist to create one." — explicit reference to the row's actual label.
8. **Pillar 2 — `PlaylistRow` dead code in `PlaylistsScreen`.** Removed (commit 7d0299b).
9. **Pillar 2 — Auto-playlist gradient sameness.** `paletteFor(family)` + `autoPlaylistGradient(kind)` (commit 06f07ad) — 7 distinct family palettes.
10. **Pillar 3 — `Color.White` literals in NowPlayingSheet.** 28 sites swapped to `MHColors.OnHero` / `OnHeroMuted` / `OnHeroDim` / `OnHeroTrack` / `HeroScrim` (commit 5026470).
11. **Pillar 3 — `displayLarge` dead token.** Now used in `CrossfadeScreen.kt:65` and `DownloadOfflineScreen.kt:81` for hero numbers.
12. **Pillar 6 — MiniPlayer central play/pause haptic.** Added (commit 7d0299b).
13. **Pillar 6 — `PlaylistRow` dead delete callback.** Removed.
14. **Pillar 6 — Settings dropdown on Home.** Replaced with full `ProfileScreen` + 3 settings sub-screens.
15. **`VideoPlayerSheet.kt` Close button raw `4.dp`.** Now `MediaPlayerSpacing.Xs`.

**Summary:** 15 findings closed of ~20 cited. The remaining 5 are: (i) MediaPlayerSpacing full sweep, (ii) AlbumDto coverSongId cross-repo, (iii) NowPlaying play/pause haptic, (iv) reorder handle on auto-playlists, (v) one stale `CenteredSpinner` site.

---

## Accepted-as-Designed (carry-over)

These were noted in v0.8.0 and remain consciously deferred:

- **No bundled `res/font/` Inter / JetBrains Mono TTFs.** `InterFamily` and `MonoFamily` are `FontFamily.SansSerif`/`Monospace` aliases (`Theme.kt:121-125`). Mockup specifies these fonts; system fallbacks render acceptably. Bundling adds APK size — accepted by user.
- **`HomeScreen` ColdStart "Iniziamo"** vs OnboardingScreen "Cosa ascolti?" — two different first-time surfaces. Acceptable — onboarding is genre-pick, ColdStart is library-fill.
- **Light theme is "best-effort"** — `Theme.kt:91-94` comment + `ThemeScreen.kt:38-45` warning explicitly tell the user. Accepted.
- **`displayLarge`** was dead in v0.8.0; now used in two settings hero numbers. Resolved.
- **`MediaPlayerSpacing` full sweep gated on token expansion.** Memory record acknowledges scale-coarseness; sweep needs design call before code change. Accepted as deferred, not closed.

---

## Summary of Drift Patterns (cycle-2 baseline)

| Pattern | v0.8.0 | v0.11.3 | Direction |
|---------|--------|---------|-----------|
| Raw `\d+\.dp` literals across `ui/` | 281 / 29 files | **467 / 41 files** | Worse — mostly because rebrand added 14 new screens, scale unchanged |
| `MediaPlayerSpacing` references | ~10 / 3 files | **36 / 9 files** | Better — but absolute usage dwarfed by raw literals |
| Inline `RoundedCornerShape(N.dp)` in screens | 28 sites | **16 sites, all intentional** | Better — drift removed, intent-driven shapes remain |
| `t.message ?: "..."` in **load** error paths | 8 sites | **0 sites** | Resolved |
| `t.message ?: "..."` in **action** error paths | (not separately tracked) | 8 sites — contextually OK | Acceptable |
| Hand-rolled empty `Box { Text("No...") }` | 9 sites | **0 sites** | Resolved (8 → `EmptyState`, 1 → contextual `Text` in HomeScreen filter hint) |
| `CenteredSpinner` for list-row loads | 8 sites | **5 grid sites + 1 list site** | Better — list-shaped loads went to shimmer, grid still spinner |
| Manual `FontWeight.X` on Material3 styles | 5 drift sites | **0 drift sites + 11 design-correct brand-emphasis overrides** | Resolved |
| `Color.White` / `Color.Black` literals | 35+ in NowPlayingSheet alone | **34 across 14 files, all intentional** | Resolved at the NowPlayingSheet hot spot; remaining are cover-canvas / scrim / chrome-on-arbitrary-image (correct usage) |

---

## Files Audited

- `app/src/main/kotlin/com/mediaplayer/android/ui/theme/Theme.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/theme/Spacing.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/theme/Shapes.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/States.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/SongCover.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/GeneratedCover.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/MHCover.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/SpotifyHero.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/SectionHeader.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/EyebrowText.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/AnonymousBanner.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/AppUpdateBanner.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/PillChip.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/TrackActionSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/UserState.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/home/HomeScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/foryou/ForYouScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/foryou/ForYouViewModel.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/search/SearchScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/search/SongRow.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/find/FindScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/PlaylistsScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/PlaylistDetailScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/AddToPlaylistSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/AddSongsToPlaylistSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/SpotifyImportScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/PlaylistShareDialog.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/PlaylistShareImporter.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/liked/LikedScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/artists/ArtistScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/artists/ArtistListScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/albums/AlbumScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/albums/AlbumListScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/player/NowPlayingSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/player/MiniPlayer.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/player/QueueSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/player/LyricsSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/player/EqualizerSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/player/VideoPlayerSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/auth/LoginScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/onboarding/OnboardingScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/changelog/ChangelogSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/profile/ProfileScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/profile/settings/SettingsSubScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/profile/settings/ThemeScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/profile/settings/CrossfadeScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/profile/settings/DownloadOfflineScreen.kt`

## UI REVIEW COMPLETE
