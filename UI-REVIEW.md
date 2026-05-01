# MediaPlayer Android — Full-App UI Review (v0.8.0)

**Audited:** 2026-05-01
**Version:** 0.8.0 (post-v0.4.3 audit drift)
**Baseline:** Abstract 6-pillar standards for a polished mobile media player + design tokens declared in `ui/theme/` (`MediaPlayerSpacing`, `CoverShapes`, `SpotifyColors`, `SpotifyType`)
**Screenshots:** Not captured — Android app, no localhost dev server. Audit is code-only.

> Replaces the v0.4.3 audit (50 findings, all closed). Re-audit was triggered by 4 milestones of post-audit code (Daily Mixes, Release Radar, self-hosted updates, playlist sharing, Spotify-style queue, Liked/Artist/Album pagination). Previous audit history available in version control.

---

## Pillar Scores

| Pillar | Score | Key Finding |
|--------|-------|-------------|
| 1. Copywriting | 3/4 | Strong tone overall; `"Unknown error"` leaks in 7+ places, two redownload dialogs share a confusingly different title for the same intent |
| 2. Visuals | 3/4 | Hero pattern (`SpotifyHero`) is excellent and reused; but `EmptyState` and `SongListShimmer` are defined and never used — every screen rolls its own |
| 3. Color | 3/4 | Brand palette is locked and consistent; `Color.White` / `Color.Black` literals appear 35+ times in `NowPlayingSheet` (cover-overlay context, defensible), and Browse tile colors look right |
| 4. Typography | 3/4 | Type scale is well-defined; **drift:** `SongRow.kt:86` overrides `titleMedium`'s SemiBold with manual `FontWeight.Normal`, three `FindScreen` rows hand-set `FontWeight.SemiBold` on `bodyMedium`, `OnboardingScreen.kt:75` redundantly bolds an already-ExtraBold `headlineMedium` |
| 5. Spacing | 2/4 | **Major drift:** `MediaPlayerSpacing` is used in only 3 of 23 UI files (~10 occurrences) — every other screen uses raw `16.dp / 24.dp / 12.dp / 8.dp` literals (281 `.dp)` matches across 29 files). The spacing scale exists but the codebase ignores it. |
| 6. Experience Design | 3/4 | Loading + error + empty states present everywhere; pagination consistent; kebab pattern enforced. But: redownload dialog has copy-vs-action mismatch, no haptic on play/pause toggle, MiniPlayer like button preserves haptic that Now Playing's `KeyboardArrowDown` close lacks |

**Overall: 17/24**

---

## Top 5 Priority Fixes (ranked by impact)

1. **Adopt `MediaPlayerSpacing` everywhere — the scale is dead-on-arrival.** The token was created in v0.3.0, but post-audit screens (`HomeScreen`, `PlaylistsScreen`, `PlaylistDetailScreen`, `LikedScreen`, `Artist/AlbumScreen`, `ChangelogSheet`, `OnboardingScreen`, all sheets) use raw `16.dp` / `24.dp` / `12.dp` / `8.dp` literals. Drift will compound — each new screen invents its own padding. **Fix:** sweep replace `padding(horizontal = 16.dp)` → `padding(horizontal = MediaPlayerSpacing.M)` across 19 files; add a lint rule (or even just a comment in `Spacing.kt`) to forbid raw integer-`dp` in `padding()`/`Spacer` outside `theme/`. (Affects Pillar 5.)

2. **Replace the 28 inline `RoundedCornerShape(N.dp)` sites with `CoverShapes` constants.** Codebase has 5 distinct cover radii in flight (`4.dp`, `6.dp`, `8.dp`, `10.dp`, `12.dp`) where the design system declares 4 named ones. Notable: `HomeScreen.kt:399` (`RoundedCornerShape(6.dp)` for shortcut tile, should be `CoverShapes.SongRow`), `HomeScreen.kt:552` (`6.dp` for playlist card cover, should be `CoverShapes.MiniPlayer`), `PlaylistsScreen.kt:324/370/414` (all `4.dp` — inconsistent with sibling tile at `:516` using `6.dp`), `AnonymousBanner.kt:36` (`12.dp` magic number for banner). **Fix:** replace each call with the appropriate `CoverShapes.*` value or add a new named token if there's a real design intent (e.g. `CoverShapes.Banner = RoundedCornerShape(12.dp)`).

3. **Adopt `EmptyState` and `SongListShimmer` — they exist and are entirely unused.** `States.kt:94` defines a polished `EmptyState(icon, title, subtitle, actionLabel, onAction)` composable; **zero callers** in `app/src/main/kotlin/com/mediaplayer/android/ui/`. Same for `SongListShimmer` at `States.kt:203`. Every list screen falls back to a bare `CenteredSpinner` (Liked, Playlists, Artist, Album, Find, Search) and every empty state is a hand-rolled `Box(...) { Text("No ...") }` (`PlaylistsScreen.kt:305`, `PlaylistDetailScreen.kt:335`, `LikedScreen.kt:184`, `AlbumListScreen.kt:173`, `ArtistListScreen.kt:173`, `AddToPlaylistSheet.kt:190`, `AddSongsToPlaylistSheet.kt:143`, `FindScreen.kt:289`, `QueueSheet.kt:74`). **Fix:** swap `CenteredSpinner` → `SongListShimmer` for list-loading paths (Liked, Search, AddSongs, PlaylistDetail), swap inline empty `Box { Text(...) }` → `EmptyState(...)` with appropriate icons (FavoriteBorder for liked, QueueMusic for playlists, Search for songs, Person for artists). Big perceived-quality win.

4. **Resolve the two redownload dialogs in `NowPlayingSheet.kt:615-659` — copy is contradictory.** Both `confirmRedownload` (line 615) and `confirmMarkBroken` (line 638) bind to overflow menu items "Re-download song" and "Mark song as broken" respectively, but the `confirmMarkBroken` dialog's title at line 641 says **"Re-download song to device?"** — nothing about "broken" appears in the dialog UI. The menu item label "Mark song as broken" therefore mismatches what the dialog actually does (which is a local-cache re-download, per the body text at 643-647). **Fix:** either (a) align the menu label to "Re-download to device" + the title to match, or (b) actually expose a "report broken" path that hits a server-side flag instead of triggering a local re-download. This is a copywriting + experience-design defect.

5. **`SongRow.kt:86` hand-overrides the type scale — and the comment admits why.** The row title sets `style = MaterialTheme.typography.titleMedium` then immediately patches `fontWeight = FontWeight.Normal` to undo `titleMedium`'s SemiBold. The comment says "previous titleSmall (14sp Bold) read as too heavy". This is a sign that the type scale's `titleMedium` (16sp SemiBold) is itself wrong for list rows, OR `bodyLarge` (16sp Normal) should be used directly. Three other `FontWeight.SemiBold` overrides on `bodyMedium` exist in `FindScreen.kt:181/271/329`, plus `SpotifyImportScreen.kt:227`. **Fix:** introduce a dedicated `bodyLargeStrong = bodyLarge.copy(weight = SemiBold)` token (or use `titleSmall` properly and accept its weight), and swap each override to the token. Manual `FontWeight.X` in screens is a smell every time.

---

## Detailed Findings

### Pillar 1: Copywriting (3/4)

**Strong points:**
- `friendlyMessage()` in `States.kt:210-218` is a thoughtful translator that hides 401/403/404/IO stack traces with empathic copy.
- Pluralization respects "1 song" vs "N songs" consistently (`PlaylistsScreen.kt:618`, `ArtistScreen.kt:333`, `AddToPlaylistSheet.kt:279`, `LikedScreen.kt:220`, etc).
- `OnboardingScreen.kt:127` uses a counter-down CTA ("Pick 2 more" → "Continue") that's a polished UX pattern.
- Cold-start CTAs at `HomeScreen.kt:218-228` ("Find new music" / "Import Spotify" with one-line subtitles) hit the right voice.
- Anonymous-banner copy at `AnonymousBanner.kt:49` ("You're playing as a guest. Sign in to sync your library.") is warm and action-oriented.

**Findings:**

- **`AlbumScreen.kt:83/94`, `ArtistScreen.kt:111/146`, `AlbumListScreen.kt:108`, `ArtistListScreen.kt:107`, `AddToPlaylistSheet.kt:91`, `AddSongsToPlaylistSheet.kt:85`** — Severity: medium. Eight call sites fall back to literal `"Unknown error"` when `t.message` is null. `States.kt:210` already provides `friendlyMessage(t)` which does this correctly. **Fix:** replace each `t.message ?: "Unknown error"` with `friendlyMessage(t)`.

- **`NowPlayingSheet.kt:641`** — Severity: high. Dialog title "Re-download song to device?" is bound to overflow item "Mark song as broken" (line 570). The two strings disagree on intent. **Fix:** see Top-5 #4.

- **`AddSongsToPlaylistSheet.kt:143`** ("No songs found.") — Severity: low. The string is shown in two semantically different states: empty catalog (initial query="") and zero matches for a typed query. **Fix:** branch the empty text on whether `query` is empty: empty catalog → "Your library is empty"; non-empty query → `"No songs match \"$query\""`.

- **`AddToPlaylistSheet.kt:190`** ("No playlists yet. Create one above.") — Severity: low. Gestures at the `NewPlaylistRow` directly above, but the row sits inside a scrolling `LazyColumn` and the empty-message Box is a sibling to it. "Above" is true at first paint only. **Fix:** "Tap **+ New playlist** to create one."

- **`HomeScreen.kt:323`** ("You're on the latest version") — Severity: low. Toast copy is fine but slightly inconsistent with the rest of the app's voice — elsewhere it says "What's new", this should likely echo "You're up to date".

- **`HomeScreen.kt:327`** — Severity: low. Update-error toast uses raw `r.message` — not run through `friendlyMessage`. Could surface a stack-trace-like string to user.

- **`PlaylistDetailScreen.kt:207`** ("Removed from playlist") — Severity: low. Unique playlist context is lost; the add-to flow at line 231 says "Added to $playlistName". **Fix:** "Removed from \"<name>\"" mirroring the add path.

- **`SpotifyImportScreen.kt:259`** — Severity: low. The string `"\n${state.queued} downloading — will be added when ready"` mixes copywriting with raw line breaks; renders as one paragraph in `bodyMedium`. **Fix:** put each line in its own `Text` row inside a `Column(spacedBy(4.dp))` for readable hierarchy.

### Pillar 2: Visuals (3/4)

**Strong points:**
- `SpotifyHero` (`SpotifyHero.kt`) is excellent: cover, palette-derived gradient, title/subtitle, play+shuffle action row, optional `extraActions` slot. Used by Liked, Album, Artist, Playlist Detail. Consistent visual identity for every detail screen.
- `NowPlayingSheet` cover-driven backdrop (`NowPlayingSheet.kt:196-227`) with the dual-gradient overlay is the standout visual moment in the app.
- `MiniPlayer` ↔ `NowPlayingSheet` shared-element cover transition (`NowPlayingSheet.kt:310-317`, `MiniPlayer.kt:96`) is a high-polish touch.
- Browse tiles (`SearchScreen.kt:228-274`) with the Spotify pink/purple Albums/Artists pair land Spotify-faithfully.
- Liked-songs purple gradient anchor is consistent across Home (`HomeScreen.kt:443`), Playlists (`PlaylistsScreen.kt:325`), and the Liked hero (`LikedScreen.kt:170`).

**Findings:**

- **`States.kt:94` `EmptyState` and `States.kt:203` `SongListShimmer` are unused** — Severity: high. See Top-5 #3. Every screen ships its own loading + empty pattern instead. Polish gap.

- **`PlaylistsScreen.kt:397-487` `PlaylistRow` is dead code.** Severity: medium. The current grid layout uses `PlaylistTile` (line 491). `PlaylistRow` is private and has no callers — it's been left behind by the v0.4.3 grid migration. **Fix:** delete (or guard with a comment if it's expected to come back).

- **`AlbumListScreen.kt:226-264` `AlbumCard` skips cover art entirely.** Severity: medium. The card draws a `surfaceContainerHigh` box with a `QueueMusic` icon — never an actual album cover, even though every song in the album has a `hasCoverArt` flag and `Network.coverUrl(songId)` would render. The album list page consequently looks identical regardless of catalog. Compare `PlaylistsScreen.kt:536-541` which does fetch a cover when `playlist.coverSongId != null`. **Fix:** join the first song's cover into `AlbumDto` (or fetch lazily) and render it instead of the placeholder.

- **`ArtistScreen.kt:292-330` `AlbumTile` (artist's album list) also paints only an icon, no cover.** Severity: medium. Same fix.

- **`HomeScreen.kt:548-580` PlaylistCardSquare — auto-playlist gradient is the same purple gradient as Liked.** Severity: medium. All 7 auto-playlist surfaces (Discover Daily, On Repeat, Release Radar, Daily Mix 1-6) get the same purple-blue gradient as the Liked tile. They become visually indistinguishable on the Home carousel. **Fix:** distinguish auto-playlist kinds — server-side `kind` field could drive a per-kind gradient pair (e.g. Daily Mix 1 = pink/red, On Repeat = green/teal, Release Radar = blue/cyan). Spotify itself does this.

- **`NowPlayingSheet.kt:236-284` PLAYING FROM caption shows `current.album ?: current.artist`.** Severity: low. The label says "PLAYING FROM" but for a song with no album, it shows the artist's name, which doesn't read as a "from" source. **Fix:** when album is null, switch the label to "PLAYING FROM ARTIST" or hide the label entirely.

### Pillar 3: Color (3/4)

**Strong points:**
- Locked dark palette declared exhaustively in `Theme.kt:14-31` via `SpotifyColors`. No app-level light mode complications.
- All semantic Material3 roles wired up correctly (`primary`, `surface`, `surfaceContainerHigh`, `onSurfaceVariant`).
- Browse tile colors (`BrowseAlbumsTile = 0xFFE8115B`, `BrowseArtistsTile = 0xFF8400E7`) are first-class tokens.
- Liked gradient (`LikedGradientStart` / `LikedGradientEnd`) is a token, not duplicated as literals.
- Equalizer/Slider/FilterChip color overrides defer to theme roles where possible (`PlaylistsScreen.kt:239-244`).

**Findings:**

- **`Color.White` / `Color.Black` appear 35+ times in `NowPlayingSheet.kt`** (lines 245, 255, 260, 270, 342, 367, 386, 387, 388, 399, 404, 420, 431, 439, 440, 457, 468, 484, 493, 504, 510, 524, 530, 539, 546, 554; plus alpha variants). Severity: low — defensible because the cover-derived gradient backdrop can be any color (light pastel, dark vibrant) so the transport row needs absolute white on a darkened scrim, not theme `onSurface`. But this should be a design *decision* captured as a token (`SpotifyColors.OnHero = Color.White`) rather than 35 raw `Color.White` references that future devs might localize incorrectly. **Fix:** introduce `SpotifyColors.OnHero` and `SpotifyColors.OnHeroMuted = Color.White.copy(alpha = 0.85f)` and use those.

- **`Color.White` in `SearchScreen.kt:233/241/268` Browse tiles** — Severity: low. Tile background is a saturated brand color; `onPrimary` would be wrong (it's black for the green primary). Same fix as above — introduce `SpotifyColors.OnBrowseTile = Color.White`.

- **`MiniPlayer.kt:145` `Color.White.copy(alpha = 0.18f)`** for inactive progress track — Severity: low. The 6-line comment justifies it well (surfaceContainerHighest is too close to the mini-player background). The fix is the same — introduce a token (`OnSurfaceFaint`) so the literal isn't naked.

- **`NowPlayingSheet.kt:439-440`** the play/pause `FilledIconButton` uses `containerColor = Color.White, contentColor = Color.Black`. This is *intentional* (Spotify's white round play button on the player) but it's the only place in the app where `onPrimary` (black on green) is overridden to white-on-black. **Fix:** capture as `SpotifyColors.NowPlayingPlay` / `OnNowPlayingPlay` so it's clear this is a deliberate exception, not drift.

- **No accent overuse.** `MaterialTheme.colorScheme.primary` (Spotify Green) is used selectively: liked toggle, follow toggle, downloaded badge, lyrics active line, sleep timer active, primary buttons, brand mark in changelog. ~30 occurrences total — within the 60/30/10 expectation for a focused brand color.

### Pillar 4: Typography (3/4)

**Strong points:**
- `SpotifyType` (`Theme.kt:55-69`) declares 13 named text styles — display, headline, title, body, label — each with size + weight, no missing roles. Cleanly mirrors Spotify's hierarchy.
- ~99% of `Text` composables use `MaterialTheme.typography.X` (200+ matches across 19 files).

**Findings:**

- **`SongRow.kt:86` `fontWeight = FontWeight.Normal`** overriding `titleMedium`'s SemiBold — Severity: high. The comment at line 83-85 says "Spotify-style row title — 16sp Regular, lighter than the previous titleSmall (14sp Bold)". This means the type scale lacks a "16sp Regular" row style and the row resorts to manual override. **Fix:** see Top-5 #5. Either use `bodyLarge` (16sp Normal — matches exactly) or introduce a token. Manual `FontWeight.X` should be a code-review red flag.

- **`FindScreen.kt:181, 271, 329`, `SpotifyImportScreen.kt:227`** — `bodyMedium` paired with `fontWeight = FontWeight.SemiBold` four times. Severity: medium. Same drift pattern. The right Material3 mapping for "14sp SemiBold" is `titleSmall` (`Theme.kt:62`). **Fix:** swap each to `MaterialTheme.typography.titleSmall`.

- **`OnboardingScreen.kt:75`** — `style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold`. Severity: low. `headlineMedium` is already 26sp ExtraBold (`Theme.kt:58`); manually setting `Bold` is *weaker* than the style's default ExtraBold and silently demotes the heading weight. **Fix:** drop the `fontWeight =` line.

- **No `displayLarge` use anywhere in the app.** `displayLarge` (36sp ExtraBold) is declared in `Theme.kt:56` but never referenced. Either it's a dead token or there's a missing hero moment that should use it (`LoginScreen.kt:46` "MediaPlayer" mark uses `headlineLarge` instead — could be `displayLarge` for stronger brand presence). **Fix:** either use it for LoginScreen brand or remove from the type scale.

- **No fontFamily token.** Every text uses the system default (Roboto). Spotify's identity is partly its custom Circular font. No on-disk fonts under `app/src/main/res/font/`. This is fine for a personal app but a clear gap from "polished Spotify-style" intent. (Accept-as-designed unless the user wants to ship a font.)

### Pillar 5: Spacing (2/4 — biggest deviation from spec)

**Strong points:**
- `MediaPlayerSpacing` (`Spacing.kt`) is a clean 5-step scale (Xs/S/M/L/Xl) with documented intent for each step.
- Where it's used (`SongRow`, `MiniPlayer`, `OnboardingSheet`) the result reads correctly: `padding(horizontal = MediaPlayerSpacing.M, vertical = MediaPlayerSpacing.Xs + 2.dp)` is more honest than `padding(16.dp, 6.dp)`.

**Findings:**

- **`MediaPlayerSpacing` is used in only 3 of 23 UI source files** (≈10 total references) — Severity: high. Every other screen uses raw `dp` literals: `HomeScreen.kt` has 28 `.dp)` matches, `PlaylistsScreen.kt` has 28, `PlaylistDetailScreen.kt` has 4, `NowPlayingSheet.kt` has 17, `SearchScreen.kt` has 15, etc. **Fix:** see Top-5 #1. The token was introduced to stop drift; drift continued. Sweep + lint.

- **`HomeScreen.kt:129-130` `contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)`** — three different spacing values in five lines, none from the scale. **Fix:** `top = MediaPlayerSpacing.M, bottom = MediaPlayerSpacing.L, spacedBy(MediaPlayerSpacing.L)` (or similar based on intent).

- **`NowPlayingSheet.kt:286, 332, 372, 408, 473, 603` use `Spacer(Modifier.height(24.dp))` / `36.dp` / `16.dp` / `12.dp`** — six unique vertical-spacing values in one screen. Inconsistent rhythm. **Fix:** map each to a scale step. The 36dp gap (line 332) between hero cover and title row is intentional but should be `MediaPlayerSpacing.Xl + MediaPlayerSpacing.Xs` if a 36dp slot is wanted, or scale up to `Xl` (32dp).

- **`PlaylistsScreen.kt:269` `contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp)`** — four unique values, none from scale. The 96dp bottom is FAB clearance, fine; the 4dp top is suspicious (would normally be `Xs=4` or `S=8`). **Fix:** named `MediaPlayerSpacing.FabClearance = 96.dp` if reused, the rest from scale.

- **`OnboardingScreen.kt:90/91 horizontalArrangement = Arrangement.spacedBy(10.dp)`** — `10.dp` is not in the scale. Most likely should be `MediaPlayerSpacing.S` (8dp).

- **`SpotifyHero.kt:103` `padding(top = 24.dp, bottom = 16.dp)` and `:115` `Spacer(Modifier.height(16.dp))`** — defines the hero's internal rhythm with raw values. Even the design-system component leaks raw `dp`.

- **Horizontal padding does converge to `16.dp` across 19 files** (41 matches via grep). At least *that* convergence is correct — but `MediaPlayerSpacing.M` would make the convention legible and prevent the next screen from inventing 14.dp.

### Pillar 6: Experience Design (3/4)

**Strong points:**
- Pull-to-refresh wired on Home, Liked, Playlists, PlaylistDetail, Album, Artist, ArtistList, AlbumList, Find. Every list-shaped surface refreshes.
- Pagination consistent: `LikedScreen`, `ArtistListScreen`, `AlbumListScreen` all use the `PAGE_SIZE=30` + `snapshotFlow` threshold trigger pattern — drift-free across new screens.
- `combinedClickable` long-press → `AddToPlaylistSheet` enforced everywhere SongRow renders (Search, Liked, Album, Artist, PlaylistDetail, Queue, AddSongs).
- Optimistic flips used correctly: `ArtistViewModel.toggleFollow()` (line 127) flips state immediately, reverts on failure.
- `friendlyMessage()` localizes 401/403/404/IO errors.
- Haptic feedback on like-toggle in MiniPlayer (line 116) + SongRow (line 99) + NowPlaying (line 361).
- Confirmation dialogs on destructive actions (delete playlist twice — `PlaylistsScreen.kt:471/569` — and re-download).
- Sleep timer, equalizer, lyrics, queue, video player all coexist as bottom sheets with proper dismiss handling.
- Anonymous banner (`AnonymousBanner.kt`) is reused on Home, Playlists; auto-hides for signed-in users via `LocalCurrentUser`.
- Playlist swipe-to-dismiss with optimistic local removal (`PlaylistDetailScreen.kt:354-367`) is well-considered — including the comment justifying the choice.
- Reorder library (`PlaylistDetailScreen.kt:284-294`) handles drag-end via snapshotFlow on `isAnyItemDragging` — clean.

**Findings:**

- **No skeleton/shimmer on list-loading paths.** Severity: high. `SongListShimmer` is built (`States.kt:203`) but `LikedScreen.kt:99`, `SearchScreen.kt:106`, `AlbumScreen.kt:143`, `ArtistScreen.kt:211`, `PlaylistDetailScreen.kt:191`, `PlaylistsScreen.kt:109`, `ArtistListScreen.kt:169`, `AlbumListScreen.kt:169` all show `CenteredSpinner` instead. A list-page spinner is the lowest-effort affordance; shimmer is the polished one. See Top-5 #3.

- **`NowPlayingSheet.kt:436` central play/pause lacks haptic feedback** — Severity: low. The like toggle 25 lines below it has it. Spotify-style polish is haptic on the primary play/pause too. **Fix:** wrap `viewModel::togglePlayPause` in a haptic-emitting closure as the like button does.

- **`PlaylistDetailScreen.kt:392-411` Reorder drag handle is shown ALWAYS, even on auto-playlists.** Severity: medium. Auto-playlists are server-managed (the long-press delete is correctly suppressed at line 507), but the reorder handle still appears, and `onReorderSongs` will fail server-side or silently no-op. **Fix:** suppress `IconButton(modifier = Modifier.draggableHandle())` when `playlist.isAuto`.

- **`AddSongsToPlaylistSheet.kt:75-88`** debounces with `delay(300)` then re-runs the search — fine. But on the *initial* mount the `LaunchedEffect(query)` fires too, so first paint also waits 300ms before showing results. Severity: low. **Fix:** branch initial fetch (no delay) vs query-change (300ms).

- **`HomeScreen.kt:280-340` GreetingHeader Settings dropdown has three items: "What's new", "Check for updates", "Sign in/out".** Severity: low. The greeting bar's Settings cog is tied to a 3-item menu where 2 of 3 items are version/update related; a real "Settings" surface (theme/playback/storage/account) could live here later. (Accept-as-designed for v0.8.0.)

- **`NowPlayingSheet.kt:309-329` Hero cover uses `CoverShapes.MiniPlayer` (`6.dp` corner radius) for a 280-360dp cover.** Severity: low. The shape was named for the mini-player; reusing it for the hero is technically fine but reads as a naming mismatch. **Fix:** rename `CoverShapes.MiniPlayer` to `CoverShapes.SmallSquare` or introduce `CoverShapes.Hero` (likely the same `6.dp` but semantically clear).

- **`PlaylistDetailScreen.kt:147-170` Share IconButton wraps the entire share intent in a button-disable (`enabled = !sharing`)** — good. The chooser dialog itself doesn't auto-dismiss on cancel — a cancelled Share leaves a token live but unused (the comment acknowledges this at line 161). The user gets no toast/snackbar on cancel. Severity: low. **Fix:** show a "Share dismissed" snackbar on chooser-cancel detection if you want belt-and-braces; not strictly needed.

- **`OnboardingScreen.kt:56-141` re-bumps to `headlineMedium` for the screen title**, while `LoginScreen.kt:46` uses `headlineLarge` for "MediaPlayer". Two adjacent first-run surfaces use different display levels; not strictly wrong but inconsistent.

- **`VideoPlayerSheet.kt:160-188` `VideoSurface` Close button at TopEnd has `padding(4.dp)`** — should be `MediaPlayerSpacing.Xs`.

---

## Summary of Drift Patterns (Most Common)

| Pattern | Occurrences | Fix |
|---------|-------------|-----|
| Raw `dp` literal in `padding()` instead of `MediaPlayerSpacing.*` | 41 in `padding(horizontal = N.dp)` alone; 281 `.dp)` total across 29 files | Sweep; lint rule |
| Inline `RoundedCornerShape(N.dp)` instead of `CoverShapes.*` | 28 sites | Replace with named token |
| `t.message ?: "Unknown error"` | 8 sites | Replace with `friendlyMessage(t)` |
| Hand-rolled `Box(...) { Text("No ...") }` empty state | 9 sites | Replace with `EmptyState(...)` |
| `CenteredSpinner` on list-page load | 8 sites | Replace with `SongListShimmer()` |
| Manual `fontWeight = FontWeight.X` overriding a typography style | 5 sites | Use the right style |
| `Color.White` / `Color.Black` literal | 35+ sites in NowPlaying alone | Define `OnHero`/`OnHeroMuted` tokens |

---

## Files Audited

- `app/src/main/kotlin/com/mediaplayer/android/ui/theme/Theme.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/theme/Spacing.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/theme/Shapes.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/home/HomeScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/search/SearchScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/search/SongRow.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/find/FindScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/PlaylistsScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/PlaylistDetailScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/AddToPlaylistSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/AddSongsToPlaylistSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/SpotifyImportScreen.kt`
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
- `app/src/main/kotlin/com/mediaplayer/android/ui/onboarding/OnboardingSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/changelog/ChangelogSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/States.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/SectionHeader.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/SongCover.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/SpotifyHero.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/AnonymousBanner.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/UserState.kt`
- `app/src/main/res/values/strings.xml`

## UI REVIEW COMPLETE
