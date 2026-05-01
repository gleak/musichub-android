# Missing mockups — request to Claude Design

Bundle reviewed: `mockup/` (MusicHub, 8 mobile + 2 extras + 13 AA + launcher icons).

The following Android features are **already implemented** in the app but
have **no mockup coverage**. Please generate matching screens so the
visual rebrand can land on them too. Same design system (lime `#A8E04E`,
Inter + JetBrains Mono, generative covers, eyebrow `// SECTION`,
gradient backgrounds).

---

## Onboarding & auth

### 1. `LoginScreen` — auth gate
- States: signed-out, signing-in, anonymous-upgrade.
- Elements: hero/logo, email + password fields, "Continue as guest"
  secondary, error banner, link to backend selection.
- Note: app supports anonymous sessions; mockup must show the
  guest path (no Premium / no payments — app is fully free).

### 2. `OnboardingScreen` / `OnboardingSheet` — first-run setup
- Multi-step: welcome → optional Spotify import / pick interests →
  done. Shown once per fresh install.
- Style: full-screen, lime CTA, mono caption strip.

### 3. Anonymous banner + cold-start CTAs (mobile Home)
- Inline banner pinned under greeting prompting sign-in.
- Two cold-start tiles ("Find new music" / "Import Spotify") shown
  when both recents and playlists are empty.
- Already partially in `HomeScreen.kt`. Needs visual treatment in the
  new lime/mono system (current uses Material `surfaceContainerHigh`).

---

## Discover (YouTube import flow)

### 4. `FindScreen` — Discover hub
- Idle state: query bar with mic, list of in-flight import requests,
  empty-state copy.
- Active request row: progress, status pill (queued/searching/ready),
  cancel.
- Tracking state: status header, candidate list with thumbnails,
  duration, view count, source channel; "Pick this one" CTA.
- This is **distinct from Search** (Search browses local catalog;
  Find runs YT auto-match). Both currently exist.

### 5. `SpotifyImportScreen` — Spotify playlist import
- Paste-URL field, fetch metadata, preview list (cover, name,
  songCount, owner), "Import as playlist" CTA, progress while
  matching tracks, success summary.

---

## Sheets / dialogs (mobile)

### 6. `AddToPlaylistSheet` — kebab destination
- Bottom sheet from any `SongRow` kebab (`onMore`).
- Search/filter playlists at top, list of playlists with cover +
  song count, "Create new playlist" row at bottom.
- **Pattern:** every list site shows kebab → opens this sheet.

### 7. `AddSongsToPlaylistSheet` — bulk add inside playlist detail
- Search local catalog, multi-select with checkboxes, footer with
  "Add N songs" CTA.

### 8. `QueueSheet` — Now-playing queue (mobile)
- Currently exists but no mockup. AA already has Queue.
- Sections: "Now playing", "Up next" (system queue), "User queue"
  (queued by user, marked with `KEY_USER_QUEUED` extras), "From
  this album/playlist".
- Drag-to-reorder, swipe-to-remove, shuffle/repeat toggles.

### 9. `EqualizerSheet` — audio EQ
- 5–10 band sliders, presets dropdown (Flat / Bass / Vocal / Custom),
  bypass toggle, audio session info strip.

### 10. `Sleep timer` menu (inside Now Playing more menu)
- Options: 5/10/15/30/45/60 min + "End of track", visible countdown
  when active.

### 11. `ChangelogSheet` — "What's new"
- Auto-shown when stored `last_seen_version` ≠ `AppVersion.VERSION`.
- List of `ChangelogEntry` (version, date, bullet highlights),
  "Got it" CTA.

### 12. App update available — banner / dialog
- Self-hosted update channel: when `AppUpdateChecker` finds a newer
  APK on the operator's host, surface a non-modal banner on Home
  ("Update available · v0.10.2 → v0.10.3 · install"), tap → trigger
  `AppUpdateInstaller`.

---

## Sharing

### 13. Playlist share — generate / receive link
- Share menu inside playlist detail: shows `mediaplayer://share/<token>`,
  "Copy link" + system share-sheet entry.
- Receive flow: deep-link landing inside app → confirm import sheet
  (preview the shared playlist, "Add to my library" CTA). One-shot
  copy semantics — not collaborative — explain in caption.

---

## Library (drill-downs)

### 14. `AlbumListScreen` — full album library
- All-albums grid with sort (recent/A-Z), search filter chip.
  Mockup currently shows Library → Album tab inline list, but the
  full album browse screen is its own destination.

### 15. `ArtistListScreen` — full artist library
- Round artist tiles, "X artists" count, A-Z scroll bar.

### 16. `LikedScreen` — Liked-songs detail
- Hero: gradient cover (mockup uses `duotone` palette). Header,
  shuffle + play, song list with kebab. Already shown as a "pinned"
  row in Library mockup but the **detail view** needs a screen.

---

## Settings (granular)

The mockup `ProfileScreen` is the entry point. The following sub-screens
are reached via chevrons but **not yet drawn**:

### 17. Settings → Notifications
- Toggles per channel (new releases, playlist updates, AA-only mode).

### 18. Settings → Crossfade
- Slider 0–12 sec, preview toggle.

### 19. Settings → Download offline
- Storage gauge, manage downloaded items list, "Clear all" CTA.
- Two redownload paths: re-fetch from origin vs drop local cache.

### 20. Settings → Lingua
- Language picker (Italiano / English / +).

### 21. Settings → Tema
- Light / Dark / System (currently locked Dark — confirm scope).

### 22. Settings → Informazioni / About
- Version, build, backend host, open-source licenses, privacy.

### 23. Settings → Backend host / server selection
- For self-hosted operator. Field for backend URL, connection test.

---

## Android Auto — incremental

Most AA screens are covered. Missing:

### 24. AA · Onboarding / connect prompt
- First-time: "Sign in on phone" or "Continue as guest", QR or
  short-code if needed.

### 25. AA · Equalizer (driver-safe)
- Large preset chips (Flat / Bass / Vocal / Custom), no fine sliders
  while driving. Disabled state during navigation.

---

## Icon families inside the app

The 6 user-invocable kebab actions (per `SongRow`) and the various
toolbar overflow menus are not specified. Icon set in `mh-shared.jsx`
covers most — confirm:

### 26. Per-row action menu (kebab → bottom sheet)
- Add to queue (top), Add to playlist, Like / Unlike, Share, Go to
  artist, Go to album, Download / Remove download, Re-download,
  View lyrics, View video, Sleep timer, Show details.

---

## Notes for Claude Design

- Keep the **lime accent + Inter + Mono** system. Don't introduce new
  brand colors.
- App is **fully free** — no Premium, no payments, no family plan,
  no audio quality picker. Crossfade + Download offline stay.
- Italian copy where possible (matches existing screens). English
  fallback acceptable.
- Frame: keep the iPhone frame (user already chose iOS aesthetic on
  Android — visual style only, the device is Android).
- For each new screen, follow the existing pattern:
  - eyebrow `// SECTION` mono
  - lime CTA primary, `rgba(255,255,255,0.08)` secondary
  - generative SVG cover for any artwork placeholder
  - bottom dock = MHPlayerBar + 4-tab BottomNav
