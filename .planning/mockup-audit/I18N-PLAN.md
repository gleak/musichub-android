# Italian-only migration plan

**Goal:** every user-visible string in Italian. No English leakage.
**Scope:** strings rendered in Compose / Material dialogs / AA custom commands /
notifications / browse-tree / changelog / update flows. Code identifiers and
log lines stay English. Code comments stay English (devs).
**Strategy:** hardcoded Italian strings inline (matches current convention —
no `strings.xml` infrastructure exists yet, and adding it is out of scope).

## Phases

Ordered easy → hard. Each phase commits independently. Bug-fix-only commits
do not need a version bump (CLAUDE.md), but the **final phase** lands as a
single user-visible polish drop with one bump (PATCH).

### Phase 1 — Leaf strings (easy, mechanical, no logic change)
Swap English → Italian for isolated `Text(...)` / `contentDescription = ...` /
`title = ...` / `placeholder = ...` / `label = ...` calls that are pure UI
labels. Each swap is one line. Risk: very low.

**Files:**
- `ui/common/States.kt` — `friendlyMessage` table + `"Retry"`.
- `ui/changelog/ChangelogSheet.kt` — `"What's new"` + `"Version X"`.
- `ui/onboarding/OnboardingSheet.kt` — full hero + 3 features + CTA.
- `update/AppUpdateDialog.kt` — title / body / buttons.
- `ui/albums/AlbumListScreen.kt` — `"Albums"`.
- `ui/artists/ArtistListScreen.kt` — `"Artists"` (+ `"Album"` already Italian).
- `ui/liked/LikedScreen.kt` — `"Liked Songs"` + `pluralizeSongs`.
- `ui/playlists/PlaylistsScreen.kt` — `"New"`, `"New playlist"`, `"Playlist name"`,
  `"Create"`, `"Cancel"`, `"Delete"`, `pluralizeSongs`.
- `ui/playlists/AddToPlaylistSheet.kt` — `"Cancel"`, `"Report"`, `"Report wrong song?"`,
  `"New playlist"`, `"Playlist name"`, `"Download"` label.
- `ui/playlists/AddSongsToPlaylistSheet.kt` — `"Search songs"`.
- `ui/playlists/SpotifyImportScreen.kt` — `"Try Again"`.
- `ui/player/NowPlayingSheet.kt` — Cancel / Report / Refresh dialog buttons,
  `"Refresh local copy"`, `"Refresh local copy?"`, `"Report wrong song"`,
  `"Report wrong song?"`, `"Video download failed"`, `"Video reinitialize failed"`,
  `"Saved as alarm sound"`, `"Cancel timer"`, `"Lyrics"`/`"Equalizer"`/`"Queue"` cd.
- `ui/player/EqualizerSheet.kt` — `"Equalizer"` title.
- `ui/player/LyricsSheet.kt` — `"Lyrics"` header.
- `ui/player/MiniPlayer.kt` — `"Like"`/`"Unlike"` cd.
- `ui/search/SongRow.kt` — `"Like"`/`"Unlike"` cd.
- `MainActivity.kt` — `"OK"` (kept), `"Home"` cd, `OfflineBadge` cd
  `"No network — playing only downloaded songs"`.

### Phase 2 — Sheet / dialog body copy (medium)
Multi-line strings that need translation choices, not just literal swaps.

- `ui/onboarding/OnboardingScreen.kt` — pluralization helpers, comment-only.
- `ui/playlists/SpotifyImportScreen.kt` — error states, button row, instructions
  (already partly Italian — finish the job).
- `ui/playlists/PlaylistShareImporter.kt` — already Italian, audit only.
- `ui/playlists/PlaylistShareDialog.kt` — already Italian, audit only.
- `ui/profile/settings/CrossfadeScreen.kt` — `"Off"` is fine (universal); audit
  rest of subtree (`DownloadOfflineScreen`, `ThemeScreen`, `DislikedScreen`,
  `SettingsSubScreen`).
- `update/AppUpdateChecker.kt` / `AppUpdateRepository.kt` — banner state text
  if any user-visible.

### Phase 3 — Service / browse / AA (medium-high)
Strings that surface in Android Auto, the system shade, or the media browse
tree. Risk: AA UI is hard to test; verify on device or via logcat.

- `playback/MediaPlaybackService.kt` — custom command `setDisplayName`
  (`"Like"`/`"Unlike"`, `"Sleep 30m"`).
- `playback/LibraryTree.kt` — section folder titles only need a sweep — the
  user-visible ones (`"Playlist"`, `"Album"`) are already Italian-compatible;
  audit `"Artisti"` / `"Generi"` / etc. for English leakage.
- `playback/AALyricsTicker.kt` — only renders song lyrics; audit any fallback
  copy.
- Notification / foreground-service text (grep `setContentText`, `setContentTitle`
  in `playback/`).

### Phase 4 — friendlyMessage / error mapping (medium)
Centralize the Italian copy for error states. The `friendlyMessage` function
in `States.kt` is reached by every screen via `ErrorWithRetry`. Already
covered in Phase 1 leaf swap, but Phase 4 adds:
- 401 / 403 / 404 / network / generic copy.
- Verify all `ErrorWithRetry` callers pass meaningful messages.

### Phase 5 — Audit + version bump (closeout)
- `Grep` for remaining English-shaped tokens (`"Try"`, `"Could"`, `"Failed"`,
  `"Unable"`, `"Loading"`, `"Search"`, `"New"`, etc.).
- Manual sweep of the screens listed in `01-auth.md` … `09-core-screens.md`.
- Bump `AppVersion.VERSION` (PATCH) + `versionName` + `versionCode`.
- Add `ChangelogEntry` summarizing the polish drop.

## Out of scope
- Full design rebrand items in the audit reports (eyebrows, lime gradient,
  pill chips, generative covers). Those are visual phases, separate from
  i18n.
- Code comments / log messages / docstrings.
- Changelog `ChangelogEntry` body strings (already Italian for releases that
  matter; old English entries can stay as historical artefacts).
- `strings.xml` extraction — defer until plurals or context-sensitive
  translation becomes necessary.

## Verification per phase
After every phase: `./gradlew :app:assembleDebug` (compile check) +
spot-load the affected screens on emulator. After Phase 5: full grep
audit + visual pass on every screen listed in the audit reports.
