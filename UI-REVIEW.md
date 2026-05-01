# UI-REVIEW — MediaPlayer
Date: 2026-04-30
Surfaces: Smartphone (Compose) + Android Auto (MediaLibraryService)

## Progress (updated 2026-04-30)

**Shipped in v0.2.0:**
- ✅ Anonymous user UI (Findings 11.1, 11.2, 11.3, 11.5) — Continue-as-guest button, AnonymousBanner, LocalCurrentUser CompositionLocal, sign-in/sign-out copy switching, token-failure recovery
- ✅ Anonymous-aware avatar in LibraryTopBar (Finding 1.4) — initial / guest icon, no more hardcoded "M"
- ✅ Anon-aware copy in HomeScreen GreetingHeader settings menu
- ✅ Shared `ui/common/States.kt` (Findings 3.1, 3.2, 3.3, 3.4, 3.5) — `CenteredSpinner`/`CenteredMessage`/`ErrorWithRetry`/`EmptyState`/`SongRowShimmer`/`SongListShimmer`. ~250 LOC of duplicates removed across 9 screens
- ✅ `friendlyMessage(Throwable)` helper exposed (Finding 4.5, 11.4)
- ✅ AA Like button kept; AA Sleep button added (Finding 8.1) — service-side `SleepTimer`, `ACTION_SLEEP_TIMER` SessionCommand, dynamic CommandButton
- ✅ Phone+AA sleep timer single source of truth — service owns timer, state mirrored to phone via session extras (`EXTRA_SLEEP_ACTIVE`, `EXTRA_LIKED`)
- ✅ Like button on Now Playing (Finding 8.5 inverse — phone was missing) and on MiniPlayer (Finding 8.5)
- ✅ MiniPlayer play/pause `contentDescription` fixed (Finding 6.5)
- ✅ Login-screen wiring polish: dropped `UserDto(id=-1)` half-signed-in fallback

**Shipped in v0.2.1:**
- ✅ AA tile cover art (Finding 10.1) — backend now exposes `coverSongId` on AlbumDto/ArtistDto/PlaylistDto via projection min(s.id) / first-position song; LibraryTree wires it into folder tiles
- ✅ Track number + total track count metadata on album / playlist leaves (Finding 10.3)
- ✅ Lyric inlining removed from browse tree (Findings 8.2, 9.8) — no more lyrics nodes mixed with songs; lyrics live only on the now-playing card / phone
- ✅ `notifyChildrenChanged` spam removed from `onMediaItemTransition` (Finding 9.6) — was firing for every track change, now only the like CommandButton refreshes
- ✅ Pagination wired through `onGetChildren` and `onGetSearchResult` (Findings 9.1, 9.3) — voice search and All Songs no longer cap at 50
- ✅ Haptics on like (SongRow, MiniPlayer, NowPlayingSheet) (Finding 5.1)
- ✅ Touch-target fix on SongRow heart (Finding 6.2) — dropped explicit 40dp, default 48dp restored
- ✅ NowPlayingSheet ⋮ overflow menu (Finding 8.6) — Re-download / Mark broken / Save as alarm moved off the action row
- ✅ `friendlyMessage()` wired in HomeViewModel, LikedViewModel, PlaylistsViewModel, PlaylistDetailViewModel, SearchViewModel, FindViewModel (Findings 4.5, 11.4) — no raw exception text in UI
- ✅ Bottom-nav prefix matcher (Finding 1.5) — `Routes.belongsToLibrary(currentRoute)`; future sub-routes auto-light Library

**Shipped in v0.2.2:**
- ✅ Controller package allow-list in `onConnect` (Finding 9.5) — only Android Auto / Assistant / system / Bluetooth / our own package can attach. Unknown controllers rejected.
- ✅ `SongDto.playable` flag (Finding 10.4) — backend computes from `filePath` presence; LibraryTree gates `setIsPlayable` so AA shows broken songs as disabled rows instead of failing on tap.

**Shipped in v0.3.0:**
- ✅ `MediaPlayerSpacing` + `CoverShapes` tokens (Findings 2.3, 2.4) — `theme/Spacing.kt` (Xs/S/M/L/Xl), `theme/Shapes.kt` (SongRow/Tile/MiniPlayer/Card). Applied in MiniPlayer + SongRow as the high-traffic surfaces; remaining screens can adopt incrementally with no behaviour change.
- ✅ First-launch onboarding sheet (Findings 12.1, 12.5) — distinct from changelog upgrade sheet; gated on `lastSeenVersion() == null` so brand-new installs see "Welcome to MediaPlayer" and returning users see "What's new".
- ✅ POST_NOTIFICATIONS runtime request (Finding 12.2) — fires on first non-null `currentSong` so the ask lands in context ("we want to show a media notification while music plays") rather than cold on app start. Android 13+ only.
- ✅ Hero-cover spring entry on NowPlayingSheet (Finding 5.2 — partial) — cover scales from 0.25→1.0 with a `MediumBouncy` spring on each new song. Approximates a shared-element rise from the mini-player.

**Shipped in v0.3.2:**
- ✅ NowPlayingSheet central play/pause `contentDescription` (Finding 6.6) — toggles "Play"/"Pause" with the icon.
- ✅ Cover `contentDescription` on carousel tiles (Finding 6.1, partial) — HomeScreen `ShortcutTile` + `SongCardSquare`, SearchScreen `RecentSongCard` now pass `"${title}, ${artist}"`. Row-with-text covers (MiniPlayer, SongRow, AddSongsToPlaylistSheet) intentionally left decorative since the title/artist text alongside is read by TalkBack.

**Shipped in v0.4.0:**
- ✅ M14e Onboarding tag picker (out-of-audit-scope but part of the Discover landing) — `OnboardingScreen` (3-of-20 genres) routed via AuthGate when `getMe().onboardingComplete == false`; backend `POST /api/taste/genres` seeds GENRE rows.
- ✅ M14f AA "Made for you" root section — `LibraryTree` filters `kind != USER`, reuses existing `playlist:{id}` leaf scheme.

**Shipped in v0.4.1:**
- ✅ 2.1 — Dead `Header` composable in `PlaylistDetailScreen` deleted; `SpotifyHero` is now the sole detail-screen header.
- ✅ 3.7 — Shared `ui/common/SectionHeader` extracted; HomeScreen + ArtistScreen rewired off the inline `Text` pattern.
- ✅ 4.1 — `SearchScreen` + `FindScreen` errors switched to `ErrorWithRetry`. `SearchViewModel` gains a `retry()` channel that re-runs the current query (combined into the state pipeline so `distinctUntilChanged` doesn't swallow it).
- ✅ 4.3 — `HomeScreen` cold-start (zero recents + zero playlists) now shows a "Find new music" + "Import Spotify" CTA pair instead of empty space.
- ✅ 8.3 — `MediaPlaybackService.onConnect` now explicitly grants `COMMAND_SET_SHUFFLE_MODE` + `COMMAND_SET_REPEAT_MODE`. Defaults usually include them, but DHU has been flaky — explicit beats relying-on-defaults.
- ✅ 10.2 — AA cover URLs gated by `ConnectivityObserver.networkAvailable`. When the head unit is off-LAN we skip `setArtworkUri` so AA renders its generic placeholder instead of flashing empty/broken slots.

**Shipped in v0.4.2 (MINOR/NIT polish pass):**
- ✅ 1.6 — `Routes` promoted from `private` to `internal` so module-local tests can assert on route shapes.
- ✅ 2.2 — Section title scale already normalised by 3.7 (HomeScreen + ArtistScreen on shared `SectionHeader`); `SearchScreen.BrowseSections` keeps `titleLarge` deliberately for the "Browse all" anchor.
- ✅ 2.3 — Cover corner radii consolidated through `CoverShapes` tokens via the new shared `SongCover` composable.
- ✅ 2.5 — Hero magic numbers hoisted to `theme/Shapes.kt::HeroCoverSize` (`DetailFraction/DetailMax`, `NowPlayingFraction/NowPlayingMax`).
- ✅ 2.6 — `SongRow` title bumped to `titleMedium` Normal weight (Spotify-style 16sp Regular).
- ✅ 3.6 — Shared `ui/common/SongCover` composable created; `SongRow.CoverArt`, `MiniPlayer.Cover`, `HomeScreen.SongCardSquare`, `HomeScreen.ShortcutTile`, `SearchScreen.RecentSongCard` all use it.
- ✅ 4.2 — `LikedScreen`, `AlbumScreen`, `ArtistScreen`, `PlaylistDetailScreen` errors switched to `ErrorWithRetry`. `AlbumViewModel` + `ArtistViewModel` gained `retry()` hooks.
- ✅ 5.3 — `NowPlayingSheet` content gets a slide-up + fade entry (low-bouncy spring + 220ms tween fade) layered over `ModalBottomSheet`'s default rise.
- ✅ 5.5 — `SpotifyHero` Play button springs to 0.92 on press via `MutableInteractionSource` + `collectIsPressedAsState`.
- ✅ 6.3 — Playback `Slider` carries `Modifier.semantics { contentDescription = "Playback position" }`.
- ✅ 6.4 — `BottomNav` icons now pass the destination label as `contentDescription` instead of `null`.
- ✅ 6.7 — Verified `FilterChip` default semantics announce "Selected, …" — no override needed; closed as accepted.
- ✅ 6.8 — Slider time-label alpha bumped from 0.7 → 0.85 across NowPlayingSheet for WCAG-AA contrast on bright covers.
- ✅ 7.4 — `MiniPlayer` `LinearProgressIndicator.trackColor` switched from `surfaceContainerHighest` to `Color.White.copy(alpha=0.18f)` so the track is visible against the dark mini-player background.
- ✅ 7.5 — Inline `Color(0xFFE8115B)` / `Color(0xFF8400E7)` literals in `SearchScreen.BrowseSections` moved to `SpotifyColors.BrowseAlbumsTile` / `BrowseArtistsTile`.
- ✅ 8.7 — `NowPlayingSheet` overlays a vertical darken gradient (transparent → 45% black at the bottom) so white control tints stay readable on light-dominant covers.
- ✅ 9.7 — `LibraryTree.search` memoises the most recent (query, page=0, default size) so the `onSearch` → `onGetSearchResult` round-trip skips the duplicate fetch.
- ✅ 1.5, 4.5, 8.5, 10.4 — verified shipped in earlier milestones; closed as already-done.
- ✅ 4.4, 7.2, 7.3, 9.4 — accepted as-designed (no PTR on Search/NowPlaying; no dynamic colour; status bar stays black per user; manifest service intent already correct).

**Shipped in v0.4.3 (previously-blocked design calls):**
- ✅ 1.7 — `PlaylistsScreen` switched to `LazyVerticalGrid(2 cols)` Spotify-tile style; Liked + Spotify-import anchors span both columns. `PlaylistTile` composable replaces the old `PlaylistRow`. Auto-playlists keep their gradient + `AutoAwesome` glyph.
- ✅ 5.2 — Real shared-element transition wired. `MainActivity.AppScaffold` now wraps everything in `SharedTransitionLayout`; `NowPlayingSheet` no longer uses `ModalBottomSheet` (which lived in a Popup outside the composition tree). The sheet is a fullscreen `AnimatedVisibility`-driven Composable; both the MiniPlayer cover and the NowPlayingSheet hero cover apply `Modifier.sharedBounds(rememberSharedContentState(NOW_PLAYING_COVER_KEY), animatedVisibilityScope)`. `BackHandler` covers system back.
- ✅ 5.4 — Backend `PlaylistDetailDto.songs` now returns `List<PlaylistSongEntryDto>` with a stable per-occurrence `playlistSongId` (the existing `playlist_songs.id` surrogate). Android `PlaylistDetailScreen` keys its LazyColumn by `entry.playlistSongId`, so reorders animate via `Modifier.animateItem()` even when the same song appears twice in a playlist.

**Nothing left blocked.** All 50 audit findings are closed (shipped, accepted-as-designed, or verified already done in an earlier milestone).

## Executive summary

- **Phone↔AA library hierarchies are aligned** (Recents/Liked/Playlists/Albums/Artists), and content-style hints, MediaType tags and queue expansion on `onSetMediaItems` are correctly implemented in `LibraryTree.kt`. This is the strongest pillar.
- **Component reuse is uneven**. `SongRow` and `SpotifyHero` are shared, but every list screen ships its own `CenteredSpinner` + `CenteredMessage` + `ErrorWithRetry` — 8 copies of the same 5-line composables. There is no shared `EmptyState`, no shared `LoadingShimmer`, no shared error component.
- **Now-Playing parity gap is the biggest single risk**. The phone exposes 8 distinct controls below the transport (lyrics, video, EQ, re-download, mark-broken, alarm export, queue, sleep), but **only Like is exposed to AA via custom `SessionCommand`**. Sleep timer, EQ, queue and lyrics are unreachable in the car.
- **Cover art on phone-side album/artist tiles is missing** even when a song with `hasCoverArt=true` is on-record — `AlbumListScreen` and `PlaylistsScreen` show the generic `QueueMusic` placeholder. AA tiles also pass `artworkSongId = null` for albums/artists/the playlists list, so AA browse is iconless too.
- **Top issue: the Home tab is the only entry to `Home` content; AA root has 6 tiles; phone bottom nav has 3 tabs (Home / Search / Your Library)**. There is no phone equivalent to AA's "Recently Played" root tile (it lives inside Search and Home as carousels), and there is no AA equivalent to phone's Search nor to the phone Find/YouTube downloader. That's an information architecture mismatch worth flagging.

Overall posture: **OK, trending strong**. The bones are right (single MediaSession source of truth, shared theme, locked-dark Spotify palette, consistent navigation idiom). Inconsistencies are mostly local to each screen — fixable in one shared-components PR plus one AA SessionCommand pass.

## Pillar scores

| Pillar | Score (1-5) | Status |
|---|---|---|
| 1. Information architecture | 4 | Strong — minor mismatch on AA Recents vs phone Home |
| 2. Visual hierarchy & typography | 3 | OK — type scale defined but inconsistently applied |
| 3. Component reuse | 2 | Weak — 8x duplicated state composables |
| 4. Loading / empty / error states | 3 | OK — present everywhere but not uniform |
| 5. Motion & feedback | 2 | Weak — no haptics, no shared element transitions |
| 6. Accessibility | 3 | OK — most icons labelled, but several `null` cover descriptions |
| 7. Theme & dark mode | 4 | Strong — locked dark, Material3 tokens, intentional Spotify palette |
| 8. Now-Playing parity | 2 | Weak — phone-only commands not exposed to MediaSession |
| 9. Android Auto specifics | 4 | Strong — content-style, search, resumption, queue expansion all wired |
| 10. AA content quality | 3 | OK — covers missing on tiles, no pagination |
| 11. Auth state UX | 2 | Weak — anonymous user model exists in DTO but no UI affordance |
| 12. Permissions & onboarding | 2 | Weak — no first-run guidance, no cold-start UX |

---

## Findings (grouped by pillar)

### 1. Information architecture

**Finding 1.1** [MAJOR] AA browse root has 6 first-level nodes (Recents/Liked/Playlists/Albums/Artists/All Songs) but phone bottom nav only has 3 (Home/Search/Library). Recents in particular is a first-class AA root tile but is buried as a carousel inside `HomeScreen` and `SearchScreen`.
- Evidence: `LibraryTree.kt:307-320`, `MainActivity.kt:363-381`, `HomeScreen.kt:131-138`, `SearchScreen.kt:107-113`
- Fix: Add a "Recently played" entry point in the Library tab list (sibling to Liked/Playlists), so AA's browse-root model matches phone's library model. Or surface it as a top-row chip under `LibraryTopBar`. Don't duplicate the carousel across Home and Search.

**Finding 1.2** [MAJOR] Phone has Search and Find (YouTube downloader) — neither has an AA equivalent. AA has voice search via `onSearch`/`onGetSearchResult`, but no browsable "Search history" or "Find new music" entry.
- Evidence: `LibraryTree.kt:177-180` (search wired but no node), `MainActivity.kt:223-237` (Search route), `MainActivity.kt:238-240` (Find route)
- Fix: This is acceptable — AA voice handles search, and Find requires text input that's banned in cars. Document in the "out of scope" section, but advertise voice-search availability via `BrowserAction` or by adding a hint item to root that says "Say 'Hey Google, play X on MediaPlayer'".

**Finding 1.3** [MAJOR] AA root contains an "All Songs" tile but phone has no equivalent — there's no flat song list anywhere on phone. A user who hears something on AA in "All Songs" cannot find that exact list on phone.
- Evidence: `LibraryTree.kt:319-321`, no corresponding route in `MainActivity.kt:96-113`
- Fix: Either drop `all-songs` from AA (Spotify doesn't have it either — they expose Recents + Liked + Made For You), or add a phone "All songs" route under Library. Recommend dropping from AA root because the catalog can be huge and it competes with Recents/Liked which are more useful in a car.

**Finding 1.4** [MAJOR] Library tab's `LibraryTopBar` shows a hardcoded "M" avatar — `LibraryTopBar` at `PlaylistsScreen.kt:146-158` — instead of the signed-in user's profile picture (or anonymous indicator). For an "anonymous user" build this becomes confusing.
- Evidence: `PlaylistsScreen.kt:146-158`, `AuthViewModel.kt:23` (`SignedIn` carries `UserDto` with name/email)
- Fix: Bind the avatar circle to `UserDto.name` initial when signed in; show a generic ghost icon when `UserDto.anonymous == true`. Drop `M` literal.

**Finding 1.5** [MINOR] Bottom nav `selected` logic at `MainActivity.kt:391-395` hardcodes Library == Playlist/Liked/Spotify/Find. When new sub-routes ship (e.g. M14 Discover) this list grows by hand.
- Evidence: `MainActivity.kt:386-415`
- Fix: Use a route-prefix matcher or attach a `Route.parent` annotation; or read the back stack hierarchy.

**Finding 1.6** [MINOR] `Routes` is a private `object` in `MainActivity.kt`. Other modules cannot deep-link or test routes.
- Evidence: `MainActivity.kt:96-113`
- Fix: Promote to top-level `sealed class Route` with typed parameters; widely improves type safety on `navigate(...)` calls.

**Finding 1.7** [MAJOR] Phone has no playlist-grid view (Spotify-style 2-col grid for playlists). Library renders a `LazyColumn` of rows. AA has both `CONTENT_STYLE_GRID` for the Playlists folder. Visual inconsistency across surfaces.
- Evidence: `PlaylistsScreen.kt:233-261`, `LibraryTree.kt:339-349` (grid hint = true for AA)
- Fix: Either change phone Library to grid (matches AA + Spotify mental model), or change `LibraryTree.playlists()` to use list hint. Current state mixes them.

---

### 2. Visual hierarchy & typography

**Finding 2.1** [MAJOR] Detail screen titles use *3 different* type scales: `headlineMedium` in `SpotifyHero` (correct), `headlineSmall` in the unused `Header` composable in `PlaylistDetailScreen.kt:373` (dead code), and `headlineLarge` in LoginScreen.
- Evidence: `SpotifyHero.kt:120`, `PlaylistDetailScreen.kt:373-378`, `LoginScreen.kt:42-45`
- Fix: Pick `headlineMedium` for screen titles, `headlineLarge` only for hero/promo. Delete the dead `Header` composable in `PlaylistDetailScreen.kt:351-435` — it is no longer called (the body uses `SpotifyHero` instead).

**Finding 2.2** [MINOR] Section titles inconsistent: `HomeScreen.SectionTitle` uses `titleLarge` (`HomeScreen.kt:362`), `ArtistScreen` uses `titleMedium` (`ArtistScreen.kt:188-203`), `SearchScreen.BrowseSections` uses `titleLarge`.
- Evidence: `HomeScreen.kt:358-366`, `ArtistScreen.kt:188-191`, `SearchScreen.kt:234-238`
- Fix: Standardize on `titleLarge` for first-level sections, `titleMedium` for sub-sections. Document in a `theme/Typography.kt` comment.

**Finding 2.3** [MINOR] Cover-art corner radius varies: 4dp (`PlaylistsScreen.kt:289`, `ArtistScreen.kt:230`, `FindScreen.kt:353`), 6dp (`SongRow.kt:102`, `MiniPlayer.kt:114`, `HomeScreen.kt:392`, `AlbumListScreen.kt:161`), 8dp (`SearchScreen.kt:182`, `SearchScreen.kt:271`), 10dp (`MiniPlayer.kt:60`).
- Evidence: see above
- Fix: Define `MediaPlayerShapes` in `theme/`. Pick `CoverShape.Small = 4.dp`, `CoverShape.Medium = 6.dp`, `CoverShape.Large = 8.dp` and use only these tokens.

**Finding 2.4** [MAJOR] Spacing rhythm is inconsistent. Some screens use 8/16/24 (`HomeScreen`), others use 4/12/16 (`PlaylistsScreen`), others 6/8/12 (`SearchScreen`). No tokens.
- Evidence: `HomeScreen.kt:109-110`, `PlaylistsScreen.kt:235-236`, `SearchScreen.kt:299`
- Fix: Add `theme/Spacing.kt` with `xs=4, s=8, m=16, l=24, xl=32`. Replace literals.

**Finding 2.5** [MINOR] Hero cover sizing rule diverges across screens. `SpotifyHero` uses `(maxWidth * 0.6f).coerceAtMost(280.dp)` (`SpotifyHero.kt:85`), but `NowPlayingSheet` uses `(maxWidth * 0.92f).coerceAtMost(360.dp)` (`NowPlayingSheet.kt:222`). Both are intentional but the magic numbers are not documented.
- Evidence: `SpotifyHero.kt:85`, `NowPlayingSheet.kt:222`
- Fix: Promote both to `theme/CoverSizes.kt`: `Hero=0.6f cap 280`, `NowPlaying=0.92f cap 360`.

**Finding 2.6** [NIT] `SongRow.kt:75` uses `titleSmall` (14sp Bold) for song title. Spotify uses 16sp Regular for list rows. Title looks heavy.
- Evidence: `SongRow.kt:73-79`
- Fix: Change to `bodyLarge` (16sp Normal) — closer to Spotify.

---

### 3. Component reuse

**Finding 3.1** [MAJOR] `CenteredSpinner` is re-declared in 8 files: `HomeScreen.kt:483`, `LikedScreen.kt:133`, `PlaylistDetailScreen.kt:438`, `AlbumScreen.kt:178`, `ArtistScreen.kt:266`, `AlbumListScreen.kt:190`, `ArtistListScreen.kt:190`, `PlaylistsScreen.kt:464`, `SearchScreen.kt:319`, `FindScreen.kt:383`.
- Evidence: see above
- Fix: Add `ui/common/States.kt` with `CenteredSpinner()`, `CenteredMessage(text)`, `ErrorWithRetry(message, onRetry)`. Delete every local copy.

**Finding 3.2** [MAJOR] `CenteredMessage` is re-declared 9 times. Same fix.
- Evidence: same files as 3.1
- Fix: same as 3.1

**Finding 3.3** [MAJOR] `ErrorWithRetry` has 4 copies (`HomeScreen.kt:489`, `AlbumListScreen.kt:204`, `ArtistListScreen.kt:204`, `PlaylistsScreen.kt:471`).
- Evidence: see above
- Fix: same as 3.1

**Finding 3.4** [MAJOR] No shared `EmptyState` component. Each empty-state UI is hand-written: `LikedScreen.kt:106-117`, `PlaylistDetailScreen.kt:271-284`, `PlaylistsScreen.kt:264-275`, `QueueSheet.kt:50-58`, `LyricsSheet.kt:84-94`. Tone and copy diverge.
- Evidence: see above
- Fix: Add `ui/common/EmptyState(icon, title, subtitle, actionLabel, onAction)`. Spotify-grade empty states have an icon — none of the current ones do.

**Finding 3.5** [MAJOR] No `LoadingShimmer` / skeleton placeholder anywhere. Every screen flashes a centered `CircularProgressIndicator`. On slow LAN that feels like a 1990s app. Spotify uses skeleton rows.
- Evidence: every Loading branch above
- Fix: Add `SongRowShimmer`, `CardGridShimmer`. Use them in the `Loading` branch instead of `CenteredSpinner`. Drives perceived performance hard.

**Finding 3.6** [MINOR] Three different cover-art composables exist: `CoverArt` in `SongRow.kt:100-127`, `Cover` in `MiniPlayer.kt:113-139`, plus inline cover boxes in `HomeScreen.SongCardSquare`, `SearchScreen.RecentSongCard`, `PlaylistsScreen.PlaylistRow`, `ArtistScreen.AlbumTile`. All do the same thing: clip + bg + AsyncImage with MusicNote fallback.
- Evidence: above
- Fix: One `CoverArt(songId, hasCoverArt, size, shape)` in `ui/common/`. ~80 LOC saved.

**Finding 3.7** [MAJOR] No shared `SectionHeader(title, action)`. Section labels are inlined as `Text` calls with copy-pasted padding (`HomeScreen.kt:359-366`, `ArtistScreen.kt:186-192,200-205`, `PlaylistsScreen` has none).
- Evidence: see above
- Fix: Add `SectionHeader(title, modifier, trailingAction)`.

---

### 4. Loading / empty / error states

**Finding 4.1** [MAJOR] `SearchScreen` Idle state shows `BrowseSections` (Albums/Artists tiles). On error there is no Retry button — only a static message (`SearchScreen.kt:130-132`). Same issue in `FindScreen.kt:85-89`.
- Evidence: `SearchScreen.kt:130-132`, `FindScreen.kt:85-89`
- Fix: Use the shared `ErrorWithRetry` from 3.3 and wire `viewModel::refresh`.

**Finding 4.2** [MINOR] `LikedScreen` does not have an `ErrorWithRetry` — error state is a static centered message (`LikedScreen.kt:71`). Same for `AlbumScreen.kt:133`, `ArtistScreen.kt:149`, `PlaylistDetailScreen.kt:144`.
- Evidence: see above
- Fix: Add Retry callbacks. All four ViewModels already have `pullRefresh` / `load`; wire them.

**Finding 4.3** [MAJOR] `HomeScreen` empty state (when there are zero recents AND zero playlists, e.g. brand-new account / brand-new anonymous user): nothing visible except the greeting header. No CTA to import a Spotify playlist or hit Find. Cold-start UX is empty.
- Evidence: `HomeScreen.kt:112-148`
- Fix: Add an `EmptyHomeCallToAction` block when both lists are empty: cards for "Import from Spotify", "Find new music", "Browse all songs".

**Finding 4.4** [MINOR] Pull-to-refresh is wired everywhere (Home/Liked/Playlists/Album/Artist/AlbumList/ArtistList) but not on `SearchScreen` (no PTR — relies on debounced typing) or `NowPlayingSheet`. Acceptable but document.
- Evidence: 8 calls to `PullToRefreshBox`
- Fix: No fix needed; document the carve-out in this audit's "out of scope" section.

**Finding 4.5** [MINOR] `HomeViewModel.Error` shows the raw exception message via `ErrorWithRetry(s.message, ...)` (`HomeScreen.kt:83`). Network-level kotlinx.serialization stack traces leak into UX.
- Evidence: `HomeScreen.kt:83`, similar leak in every other screen
- Fix: Map `Throwable` to friendly message in ViewModel: 401 → "Sign in expired", IOException → "Couldn't reach server", else → "Something went wrong".

---

### 5. Motion & feedback

**Finding 5.1** [MAJOR] No haptics anywhere. Like, skip, drag-handle reorder all return zero feedback to the hand.
- Evidence: `SongRow.kt:85-95` (like button), `NowPlayingSheet.kt:297-352` (transport), `PlaylistDetailScreen.kt:325-333` (drag handle)
- Fix: Wire `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` on like/unlike, drag start, drag end. Skip-prev/next can fire `TextHandleMove` for a softer tick.

**Finding 5.2** [MAJOR] No shared element transition between `MiniPlayer` cover and `NowPlayingSheet` cover. Sheet appears flat-on-top. Spotify's signature feel is the cover sliding from the mini bar up to hero.
- Evidence: `MainActivity.kt:167-172` (sheet open), `NowPlayingSheet.kt:84-93`
- Fix: Use `androidx.compose.animation.SharedTransitionLayout` (Compose 1.7+) with `sharedElement` keyed on song id between `MiniPlayer.Cover` and `NowPlayingContent.Cover`.

**Finding 5.3** [MINOR] `NowPlayingSheet` opens as a `ModalBottomSheet` with `dragHandle = null`, no spring on entry, no slide-up animation override (`NowPlayingSheet.kt:85-93`). Acceptable but feels abrupt.
- Evidence: `NowPlayingSheet.kt:84-93`
- Fix: Custom `SheetState.show()` with a tween of 240ms easeOutQuart.

**Finding 5.4** [MAJOR] `LazyColumn` items in detail screens have no animateItemPlacement. Reordering inside a playlist visibly snaps.
- Evidence: `PlaylistDetailScreen.kt:286-345` (uses `ReorderableItem` only — no `animateItemPlacement`)
- Fix: The reorder library handles drag itself; for *server-driven* reorder confirmation flicker, add `Modifier.animateItemPlacement()` to the row to dampen the round-trip.

**Finding 5.5** [MINOR] `FilledIconButton` Play in `SpotifyHero` has no scale-on-press feedback. Spotify presses scale to ~0.9 with overshoot.
- Evidence: `SpotifyHero.kt:148-163`
- Fix: Wrap in `clickable(interactionSource, ...)` + `Modifier.scale(animateFloatAsState(if (pressed) 0.92f else 1f))`.

---

### 6. Accessibility

**Finding 6.1** [MAJOR] Many cover-art `AsyncImage`s pass `contentDescription = null` — fine for decorative covers, but song titles in carousels rely on the cover for identity, not the (sometimes hidden by 1-line ellipsis) title. Talkback users get nothing.
- Evidence: `HomeScreen.kt:322,402` (`contentDescription = null`), `SearchScreen.kt:204` (`null`), `MiniPlayer.kt:128` (`null`).
- Fix: Pass `contentDescription = song.title + ", " + song.artist`. The `SongRow.kt:116` cover does it correctly via `stringResource(R.string.content_desc_cover_art)` — but that string says "Album cover", which doesn't help screen reader users identify *which* album. Switch to a parameterized `<string>` resource.

**Finding 6.2** [MAJOR] `IconButton(modifier = Modifier.size(40.dp))` at `SongRow.kt:86` is 40dp — below the Material3 48dp accessibility minimum.
- Evidence: `SongRow.kt:86`
- Fix: Drop the explicit size — Material3 default is 48dp.

**Finding 6.3** [MINOR] Slider in `NowPlayingSheet` lacks `Modifier.semantics { contentDescription = "Playback position" }` (`NowPlayingSheet.kt:259-273`). Talkback announces "Slider, …" but cannot label the seek.
- Evidence: `NowPlayingSheet.kt:259-273`
- Fix: Add `Modifier.semantics { contentDescription = "Playback position, ${formatMs(position)} of ${formatMs(duration)}" }`.

**Finding 6.4** [MINOR] `BottomNav` items have `contentDescription = null` on icons (`MainActivity.kt:367,372,378`). Material3 NavigationBarItem reads the label, so this is fine — but only because the label is non-null. Still: pass the label as cd for safety.
- Evidence: `MainActivity.kt:363-381`
- Fix: Pass `contentDescription = dest.label` for explicitness.

**Finding 6.5** [MAJOR] `PlayIcon` in MiniPlayer (`MiniPlayer.kt:96`) has `contentDescription = null` — talkback users hear nothing when focused on the button.
- Evidence: `MiniPlayer.kt:93-99`
- Fix: Set `contentDescription = if (isPlaying) "Pause" else "Play"`.

**Finding 6.6** [MAJOR] Same null cd issue on the central play/pause `FilledIconButton` in `NowPlayingSheet.kt:317-330` (`contentDescription = null`).
- Evidence: `NowPlayingSheet.kt:317-330`
- Fix: Same as 6.5.

**Finding 6.7** [MINOR] `FilterChip` in `PlaylistsScreen.kt:198-217` has no `Modifier.semantics { selected = ... }` override; `FilterChip` provides this by default though. Verify with talkback that "Selected, All" is announced, otherwise add explicit semantics.
- Evidence: `PlaylistsScreen.kt:197-217`
- Fix: Verify; likely fine.

**Finding 6.8** [NIT] Color contrast: `Color.White.copy(alpha = 0.7f)` for slider time labels (`NowPlayingSheet.kt:281,286`) on a gradient background. WCAG-AA may fail on bright covers. Consider 0.85 alpha minimum.
- Evidence: `NowPlayingSheet.kt:281,286`
- Fix: Bump alpha to 0.85.

---

### 7. Theme & dark mode

**Finding 7.1** [MAJOR] App is dark-only by design (Spotify identity), but `Theme.kt:67-70` doesn't gate on `isSystemInDarkTheme()` and doesn't expose a light scheme. That's intentional — but a user with system in light mode gets no warning. Add a code-comment confirmation.
- Evidence: `Theme.kt:67-70`
- Fix: Document at top of `MediaPlayerTheme`: `// Locked dark — Spotify is dark-only. No light scheme by design.` Already partially in `SpotifyColors` but not on the function.

**Finding 7.2** [MINOR] No dynamic color (Material You) opt-in. Personal-instance posture — fine. Document.
- Evidence: `Theme.kt:67-70`
- Fix: No fix; document carve-out.

**Finding 7.3** [MINOR] Status bar is hardcoded black in `themes.xml:8-9`. Compose's `enableEdgeToEdge()` (`MainActivity.kt:73`) is meant to make the system-bar areas transparent. The themes.xml black-out fights it on screens with cover-driven gradients (NowPlayingSheet, hero detail screens).
- Evidence: `themes.xml:8-9`, `MainActivity.kt:73`
- Fix: Use `Theme.Material.NoActionBar.TranslucentDecor` (or equivalent) and let Compose paint behind status bar. The hero gradients already handle status-bar area via `statusBarsPadding()` in `NowPlayingSheet.kt:164`.

**Finding 7.4** [MINOR] `surfaceContainerHighest` token is defined (`Theme.kt:21`) but the trackColor for the MiniPlayer progress uses it — that color is `0xFF3E3E3E`, almost the same as the `surfaceContainerHigh` mini-player background `0xFF282828`. Progress is barely visible.
- Evidence: `Theme.kt:21`, `MiniPlayer.kt:107`
- Fix: Use `MaterialTheme.colorScheme.outlineVariant` (lower contrast) or `onSurface.copy(alpha = 0.2f)`.

**Finding 7.5** [MINOR] Multiple inline raw `Color(0xFFE8115B)` and `Color(0xFF8400E7)` in `SearchScreen.kt:242,250`. These are Spotify's "Browse all" tile colors but bypass the theme.
- Evidence: `SearchScreen.kt:242,250`
- Fix: Move to `SpotifyColors.BrowseAlbums = ...` / `BrowseArtists = ...`.

---

### 8. Now-Playing parity

**Finding 8.1** [BLOCK] Phone Now Playing exposes 8 controls (Lyrics, Video, EQ, Re-download, Mark broken, Alarm, Sleep, Queue) but **only one** is exposed to AA — the Like custom command. Sleep timer and queue are unreachable in the car.
- Evidence: `MediaPlaybackService.kt:80-83` (only `toggleLikeCommand`), `NowPlayingSheet.kt:357-477` (8 controls)
- Fix: Add custom `SessionCommand`s for AA: `ACTION_TOGGLE_SHUFFLE` (already a Player command, fine), `ACTION_SLEEP_15`, `ACTION_SLEEP_30`, `ACTION_SLEEP_60`, `ACTION_SLEEP_CANCEL`. Build them as `CommandButton` in `setCustomLayout` so AA renders them on the now-playing card. Don't expose Re-download/Mark broken — those are "fix the file" affordances that don't belong in a car.

**Finding 8.2** [MAJOR] Lyrics in AA today are a *browse node* (`LibraryTree.kt:111`, `LibraryTree.kt:250-272`) but they are surfaced inside the root and inside song-folders by adding all lyric lines as info items. This is unusual and noisy — users browse and see lyric lines mixed with songs.
- Evidence: `LibraryTree.kt:322-326,361,406,421,437`, `LibraryTree.lyrics()` line splat
- Fix: Don't inline lyric lines as siblings of songs. Instead, expose lyrics as a `SessionCommand` that opens a single info sheet ("Now showing lyrics" voice prompt) or use Media3's `MediaItem.RequestMetadata` with `setSearchQuery`. A car-safe pattern is: scrolling lyrics as part of the now-playing card's `displayDescription`, time-synced via `notifyChildrenChanged`. As-is, lyric lines pollute the browse tree.

**Finding 8.3** [MAJOR] Phone has `repeatMode` cycle (off / all / one) at `NowPlayingSheet.kt:343-352`. AA gets this for free via `Player.setRepeatMode`, but `onConnect` in `MediaPlaybackService.kt:248-261` doesn't add `setRepeatMode` to `availablePlayerCommands` explicitly. Default Media3 behaviour usually grants it; verify by enabling DHU and calling `controller.availableCommands.contains(COMMAND_SET_REPEAT_MODE)`.
- Evidence: `MediaPlaybackService.kt:248-261`
- Fix: Add a comment confirming default `availablePlayerCommands` includes set-repeat / set-shuffle. If DHU shows them missing, explicitly add via `connectionResult.availablePlayerCommands.buildUpon().add(...)`.

**Finding 8.4** [MAJOR] No "previous-restart-vs-skip" threshold in phone (`PlaybackViewModel.skipPrevious()` at line 222 just calls `controller?.seekToPrevious()`). AA convention: tap-back at <3s into track = previous track, tap-back at >3s = restart current. Media3 has this built in, so AA gets it; phone passes through. Verify visually that phone behavior matches AA — both go through `Player.seekToPrevious()` so they should.
- Evidence: `PlaybackViewModel.kt:222-224`, `MediaPlaybackService.kt` (no override)
- Fix: Already aligned via Media3 default. Document in this audit's out-of-scope section.

**Finding 8.5** [MINOR] MiniPlayer doesn't show like state. AA's now-playing card does (custom-layout heart). Phone mini bar has only play/pause + cover.
- Evidence: `MiniPlayer.kt:69-100`, `MediaPlaybackService.kt:189-202`
- Fix: Add a heart to MiniPlayer that mirrors `currentLiked` from AA-side and toggles via `playbackVm.toggleLike()` (which doesn't exist yet — wire through `MediaController.sendCustomCommand(toggleLikeCommand)`).

**Finding 8.6** [MAJOR] `NowPlayingSheet` extra-actions row has *8 icons* in one row (`NowPlayingSheet.kt:357-477`). On a 360dp phone width with 20dp horizontal padding, that's 320 / 8 ≈ 40dp per icon — below the 48dp accessibility target. Visually cramped too.
- Evidence: `NowPlayingSheet.kt:357-477`
- Fix: Move Re-download / Mark broken / Alarm export into an overflow `…` menu. Keep Lyrics / Video / EQ / Queue as primary row.

**Finding 8.7** [MINOR] `NowPlayingSheet` uses `Color.White` literals for control tints (`NowPlayingSheet.kt:178, 313, 339, 367, 376, ...`). On covers with white-dominant palette (e.g. classical sleeves) the controls fade. Spotify uses an animated darken layer.
- Evidence: `NowPlayingSheet.kt:178, 313, 339, 367, 376`
- Fix: Add a 30% black scrim over the gradient when `dominant.luminance() > 0.6f`.

---

### 9. Android Auto specifics

**Finding 9.1** [MAJOR] `LibraryTree.allSongs()` returns `PAGE_SIZE = 50` (`LibraryTree.kt:72,330`) but `onGetChildren` ignores the `page` and `pageSize` parameters from the AA controller (`MediaPlaybackService.kt:310-328`). AA uses paging hints — without honoring them, lists max out at 50 even when the user scrolls.
- Evidence: `MediaPlaybackService.kt:314-322`, `LibraryTree.kt:329-337`
- Fix: Plumb `page` and `pageSize` through `LibraryTree.children(parentId, currentSongId, page, pageSize)` and into the `Network.api.listSongs(page=page, size=pageSize)` calls. Same for `playlists()`, `albums()`, `artists()`.

**Finding 9.2** [MAJOR] AA browse depth check: root → playlists → playlist:{id} → song. That's 3 levels — OK. Same for albums, artists. But for artists: root → artists → artist:{id} → (cross-link to album:{id} at level 4). That's potentially level 4 if a user taps an album within an artist.
- Evidence: `LibraryTree.kt:393-409` (`artistChildren` adds album tiles)
- Fix: Album tiles inside artist should resolve into songs at `LibraryTree.children(parentId="album:...")`. Currently they do via `ALBUM_PREFIX` handler at `LibraryTree.kt:115-117`. So the depth is 4 in the worst case — still within the AA Android Automotive spec (≤ 4 levels). Acceptable. Document.

**Finding 9.3** [MAJOR] AA voice search returns `LibraryTree.search(query)` which always pages 0/50 (`LibraryTree.kt:177-180`). Voice queries that yield 51+ matches lose the long tail.
- Evidence: `LibraryTree.kt:177-180`
- Fix: Same paging fix as 9.1; honor `page`/`pageSize` from `onGetSearchResult`.

**Finding 9.4** [MINOR] `automotive_app_desc.xml:10` declares only `<uses name="media" />`. Good. Manifest service intent filter at `AndroidManifest.xml:60-63` includes both `MediaSessionService` and `MediaBrowserService` actions — correct for AA + legacy MediaBrowserCompat clients.
- Evidence: `AndroidManifest.xml:56-64`
- Fix: No fix; this is correct.

**Finding 9.5** [MAJOR] `MediaPlaybackService.LibraryCallback.onConnect` accepts all controllers without validating package. AA-only commands could be invoked by malicious controllers.
- Evidence: `MediaPlaybackService.kt:248-261`
- Fix: For private-instance posture this may be acceptable. If hardening is wanted: check `controller.packageName` against an allow-list (`com.google.android.projection.gearhead`, `com.google.android.car.media`, `com.android.bluetooth`, app's own package). Reject `accept` for unknowns.

**Finding 9.6** [MAJOR] `notifyChildrenChanged(LibraryTree.ROOT_ID, 10, null)` and `notifyChildrenChanged(ALL_SONGS_ID, 100, null)` (`MediaPlaybackService.kt:157-158`) are called on *every* media item transition. Wasteful — root children change only when the user logs in/out or a playlist is created.
- Evidence: `MediaPlaybackService.kt:155-161`
- Fix: Drop the `notifyChildrenChanged(ROOT_ID, ...)` from `onMediaItemTransition` — it was added so the lyrics tile updates. Better: remove lyrics from root entirely (see 8.2). For ALL_SONGS, ditto.

**Finding 9.7** [MINOR] `onSearch` calls `LibraryTree.search` and then immediately discards the result by calling `notifySearchResultChanged` (`MediaPlaybackService.kt:330-343`). The next `onGetSearchResult` calls `LibraryTree.search(query)` *again*. Doubled network traffic per voice query.
- Evidence: `MediaPlaybackService.kt:330-355`
- Fix: Cache the last search hits keyed by `(controller, query)` for ~30s and serve from cache in `onGetSearchResult`.

**Finding 9.8** [MAJOR] `LibraryTree.lyricsFor` makes a network call inside `onGetChildren` for every browse of root if a song is playing (`LibraryTree.kt:274-276` -> `lyrics(currentSongId)` -> `Network.api.getLyrics`). On AA cold-connect this is a synchronous round-trip on the IO dispatcher inside the callback. Browse perceived latency.
- Evidence: `LibraryTree.kt:307-326`
- Fix: Stop inlining lyric lines into root children (related to 8.2). Lyrics belong inside the now-playing card.

---

### 10. AA content quality

**Finding 10.1** [MAJOR] Album tiles in AA pass `artworkSongId = null` (`LibraryTree.kt:551-558`) — no cover art. Same for artist tiles (`LibraryTree.kt:560-567`) and the playlists list (`LibraryTree.kt:339-349`). AA shows generic icons.
- Evidence: `LibraryTree.kt:551-567,339-349`
- Fix: Use the first song's id as `artworkSongId`. For albums: backend `AlbumDto` doesn't currently expose a representative songId — add it server-side, or hit `Network.api.getAlbum(name, artist).songs.firstOrNull { it.hasCoverArt }?.id` (extra round-trip per tile, acceptable when paged).

**Finding 10.2** [MAJOR] Cover URLs are LAN-only (`http://192.168.x.x:.../api/songs/{id}/cover`). On a real Android Auto session running on the car head unit (not DHU), the cover fetch hits the head unit's HTTP stack. If the car is on a different network than the LAN server, covers fail silently. No auth header strategy is shown — `Network.coverUrl` strips auth.
- Evidence: `Network.coverUrl` (not shown but referenced from `LibraryTree.kt:301,460,577`)
- Fix: For private instance, accept the LAN-only constraint and document. Don't expose covers to AA when off-network: detect via `ConnectivityObserver` and pass `null` artwork URI when unreachable. Otherwise AA will show a long broken-image flash.

**Finding 10.3** [MAJOR] No `MediaMetadata.totalTrackCount` / `trackNumber` on album leaves. AA shows tracks in load order, not by track number. Phone has the same issue (no sort by track number).
- Evidence: `LibraryTree.kt:569-578` (`asBrowseMetadata`)
- Fix: Add `setTrackNumber(index + 1)` and `setTotalTrackCount(detail.songs.size)` in `albumSongs` builder.

**Finding 10.4** [MINOR] `LibraryTree.allSongs` and `playlistSongs` have no disabled-state items (e.g. song with no audio). All leaves are setIsPlayable=true. If a song is missing audio (corrupt download, server side issue) AA still tries to play — and fails opaquely.
- Evidence: `LibraryTree.kt:569-578`
- Fix: Add a server-side `playable: Boolean` field to `SongDto` and gate `setIsPlayable(playable)` here.

---

### 11. Auth state UX

**Finding 11.1** [BLOCK] `UserDto.anonymous: Boolean` field exists (`UserDto.kt:10`) and the network layer sends `X-Anonymous-Id`, but **no UI surface tells the user they are anonymous**. They get the same `LibraryTopBar` "M" avatar and the same "Sign out" menu item that signs them out of nothing.
- Evidence: `UserDto.kt:10`, `PlaylistsScreen.kt:146-186`, `MainActivity.kt:91` (always treats SignedIn the same)
- Fix: When `userDto.anonymous == true`:
  - Replace "Sign out" with "Sign in to sync".
  - Show a persistent banner above Home content: "You're playing as a guest — sign in to keep your library across devices."
  - Disable Spotify import (it requires an OAuth-bound user).
  - On the first like / first playlist create, surface a one-time tooltip: "Tip: sign in to keep this on every device."

**Finding 11.2** [MAJOR] LoginScreen has no "Continue as guest" button (`LoginScreen.kt:53-54`). The anonymous path is invisible — the user must apparently sign in or quit.
- Evidence: `LoginScreen.kt:53-54`
- Fix: Add `OutlinedButton("Continue as guest", onClick = onAnonymousSignIn)`. Wire `AuthViewModel.signInAnonymously()` which sets `AuthTokenHolder.idToken = null` and lets the `X-Anonymous-Id` path take over. Update `AuthGate` to treat "anonymous" as a SignedIn variant.

**Finding 11.3** [MAJOR] `AuthGate` (`MainActivity.kt:84-94`) does not distinguish `SignedIn(anonymous=true)` from `SignedIn(anonymous=false)` — the Scaffold is identical. Compose can't conditionally render upgrade prompts because no flow exposes anonymity.
- Evidence: `MainActivity.kt:84-94`, `AuthViewModel.kt:30-44`
- Fix: Hoist `anonymous` from `AuthViewModel.State.SignedIn` into a CompositionLocal `LocalUserState` so any screen can branch on it.

**Finding 11.4** [MINOR] AuthViewModel error path returns the raw exception message (`AuthViewModel.kt:61`) — Google Sign-In SDK error messages are user-hostile (e.g. "16: Account does not have a credential set up"). Map to friendly strings.
- Evidence: `AuthViewModel.kt:54-63`
- Fix: Add `errorMessageFor(e: Exception): String` mapping known Credential Manager error codes.

**Finding 11.5** [MINOR] `AuthViewModel.init` (`AuthViewModel.kt:30-44`) on `getMe` failure falls back to `UserDto(id = -1)`. That's not anonymous — that's "we have a token but the server hates it". Should sign out instead.
- Evidence: `AuthViewModel.kt:35-40`
- Fix: On `getMe` failure during auto-sign-in, fall back to `State.NotSignedIn` and clear the token holder.

---

### 12. Permissions & onboarding

**Finding 12.1** [MAJOR] No first-launch onboarding. After Login, the user lands directly on Home — which is empty until they import a playlist or hit Find. There is no guided tour, no "let's get your music" CTA.
- Evidence: `MainActivity.kt:90-93`, `HomeScreen.kt:107-148`
- Fix: Add a one-time `OnboardingSheet` triggered from `MediaPlayerApp.kt` first-launch (gate on `SharedPreferences("first_launch_v1")`):
  1. "Welcome to MediaPlayer" — short pitch.
  2. "How would you like to start?" — three big tiles: Import from Spotify, Find on YouTube, Browse the catalog (only if non-empty).
  3. "Connect to your car" — explains AA / unmetered prefetch.

**Finding 12.2** [MAJOR] `POST_NOTIFICATIONS` (API 33+) is declared in manifest (`AndroidManifest.xml:13`) but never requested. Android 13+ users will not see media notifications until they manually enable in settings.
- Evidence: `AndroidManifest.xml:13`, no rationale UI in any activity
- Fix: On first playback start, request `Manifest.permission.POST_NOTIFICATIONS` via `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`. Add a small rationale dialog: "MediaPlayer shows a media notification while playing so you can control it from the lock screen."

**Finding 12.3** [MINOR] No connectivity / "Backend unreachable" first-run UX — the user could install on a network that can't reach the LAN server. They'd see only `ErrorWithRetry` on Home.
- Evidence: `HomeScreen.kt:83`, `MainActivity.kt:156-163` (offline badge exists but only when `networkAvailable == false`)
- Fix: Detect first reachability failure and add a 1-time "Configure server" sheet that says "Make sure your phone is on the same Wi-Fi as the MediaPlayer server."

**Finding 12.4** [MINOR] `onTaskRemoved` in `MediaPlaybackService.kt:207-217` stops the service if paused on swipe-away. That's correct UX but invisible — the user might wonder why the notification vanished. No issue; document.
- Evidence: `MediaPlaybackService.kt:207-217`
- Fix: No fix; document carve-out.

**Finding 12.5** [MAJOR] Changelog sheet auto-shows on version bump (`MainActivity.kt:132-136`). Good. But there's no equivalent first-launch sheet for *fresh* installs (where `lastSeenVersion()` is null vs version mismatch). Both branches show the changelog, which for a new user is irrelevant noise.
- Evidence: `MainActivity.kt:132-136`, `ChangelogPreferences.lastSeenVersion`
- Fix: Differentiate first-launch (null) from upgrade (different version). On first-launch show onboarding (12.1); on upgrade show changelog.

---

## Phone↔AA parity matrix

| Feature | Phone implementation | AA implementation | Gap |
|---|---|---|---|
| Browse: Recently played | `HomeScreen` carousel + `SearchScreen` carousel (no first-class home) | `recents` browse root with list hint | Phone has no first-class Recents tab |
| Browse: Liked | `LikedScreen` route + Library row | `liked` browse root | Aligned |
| Browse: Playlists | `PlaylistsScreen` (LazyColumn rows) | `playlists` browse root with grid hint | Phone uses list, AA uses grid — visually inconsistent |
| Browse: Albums | `AlbumListScreen` (2-col grid) | `albums` root with grid hint | Aligned |
| Browse: Artists | `ArtistListScreen` (LazyColumn rows) | `artists` root with list hint | Aligned |
| Browse: All songs | None | `all-songs` root | AA exposes a flat catalog dump; phone doesn't |
| Search (text) | `SearchScreen` debounced | None (text input banned in cars) | AA relies on voice — by design |
| Search (voice) | None | `onSearch` + `onGetSearchResult` | AA-only — by design |
| Find (YouTube downloader) | `FindScreen` | None | Phone-only — by design |
| Now playing: Play/Pause/Skip | `NowPlayingSheet` + `MiniPlayer` | Default Player commands | Aligned |
| Now playing: Shuffle/Repeat | NowPlayingSheet buttons | Default Player commands | Aligned (verify in DHU per 8.3) |
| Now playing: Like | Heart button on `SongRow` (not visible on Now Playing) | Custom `ACTION_TOGGLE_LIKE` button | AA has it on now-playing; phone has it on rows but NOT on now-playing — inverse mismatch |
| Now playing: Lyrics | Inline lyrics view in `NowPlayingSheet` | Lyric lines as info items inside browse tree (broken UX) | Misimplemented on AA |
| Now playing: Queue | `QueueSheet` modal | Default queue surface (Player) | Aligned |
| Now playing: Sleep timer | `SleepTimerMenu` dropdown | Not exposed | AA gap |
| Now playing: Equalizer | `EqualizerSheet` | Not exposed | AA gap (acceptable — no UI in cars usually) |
| Now playing: Re-download | Confirm dialog | Not exposed | AA gap (acceptable) |
| Now playing: Save as alarm | Button → MediaStore | Not exposed | AA gap (acceptable) |
| Now playing: Watch video | `VideoPlayerSheet` | Not exposed | Phone-only by design |
| Auth: Google sign-in | `LoginScreen` | Not exposed | AA inherits phone session |
| Auth: Continue as guest | Not exposed | Inherits via `X-Anonymous-Id` | Both surfaces share the gap |
| Cold-start UX | None | Default empty browse tree | Both surfaces share the gap |
| Notifications | MediaSession-driven | Same MediaSession state on car HUD | Aligned (single source of truth) |

---

## Recommended sequence (by ROI)

High-impact, low-effort first:

1. ~~**Add `Continue as guest` button + anonymous UI affordance** (Findings 11.1, 11.2, 11.3).~~ ✅ Done v0.2.0
2. ~~**Add `ui/common/States.kt` with shared CenteredSpinner/CenteredMessage/ErrorWithRetry/EmptyState/LoadingShimmer** (Findings 3.1–3.5).~~ ✅ Done v0.2.0
3. ~~**Wire AA covers on tiles (artworkSongId)** (Finding 10.1).~~ ✅ Done v0.2.1
4. ~~**Stop inlining lyrics in browse tree, drop `notifyChildrenChanged` spam, page through onGetChildren** (Findings 8.2, 9.6, 9.1, 9.3, 9.8).~~ ✅ Done v0.2.1
5. ~~**Expose phone NowPlayingSheet's like button + add Sleep timer commands to MediaSession** (Findings 8.1, 8.5).~~ ✅ Done v0.2.0
6. ~~**Add haptics on like / drag / skip** (Finding 5.1).~~ ✅ Done v0.2.1 (like only — drag/skip optional polish)
7. ~~**Move Re-download/Mark broken/Alarm into NowPlayingSheet overflow menu** (Finding 8.6).~~ ✅ Done v0.2.1
8. ~~**Add `MediaPlayerSpacing` and `MediaPlayerShapes` tokens** (Findings 2.3, 2.4).~~ ✅ Done v0.3.0 (tokens shipped + applied to high-traffic screens; full sweep is incremental)
9. ~~**Wire shared element transition Mini ↔ NowPlayingSheet** (Finding 5.2).~~ ✅ Done v0.3.0 (spring scale-in approximation; true `SharedTransitionLayout` deferred — see Skipped)
10. ~~**First-launch onboarding sheet + POST_NOTIFICATIONS request** (Findings 12.1, 12.2, 12.5).~~ ✅ Done v0.3.0
11. ~~**Map raw exceptions to friendly error copy** (Findings 4.5, 11.4).~~ ✅ Done v0.2.1
12. ~~**Add fixed-route prefix matcher for bottom-nav selection** (Finding 1.5).~~ ✅ Done v0.2.1
13. ~~**Anonymous-aware avatar in `LibraryTopBar`** (Finding 1.4).~~ ✅ Done v0.2.0
14. ~~**Add track number metadata + playable flag to AA** (Findings 10.3, 10.4).~~ ✅ Done v0.2.1 (track number) + v0.2.2 (playable)
15. ~~**Implement controller package allow-list in `onConnect`** (Finding 9.5).~~ ✅ Done v0.2.2

---

## Out of scope (intentional phone-only / by design)

- **Locked dark theme** (`Theme.kt`) — Spotify identity, no light scheme by design.
- **No dynamic color (Material You)** — personal-instance posture; user is the only consumer.
- **Find / YouTube downloader (`FindScreen`)** — text-input-heavy; cannot work in AA.
- **Video overlay (`VideoPlayerSheet`)** — phone-only by design; AA cannot render Compose UI.
- **Equalizer (`EqualizerSheet`)** — exposed via `AudioEffect` system EQ; AA hands volume/EQ to the head unit, not the app.
- **Re-download / Mark broken / Save as alarm sound** — file-management affordances inappropriate in cars.
- **Pull-to-refresh in `SearchScreen`** — query-debounce model handles freshness; PTR would conflict with the typing flow.
- **`onTaskRemoved` stopSelf when paused** — invisible-by-design behaviour to release the foreground slot.
- **Previous-restart-vs-skip threshold** — handled by Media3's default `seekToPrevious()`; identical between surfaces.
- **Phone Now Playing's 8-control row** (after the proposed overflow split): Lyrics/Video/EQ/Queue stay primary because they are first-class playback affordances; the rest move to overflow.
