# Missing mockups — request to Claude Design

Bundle reviewed: `mockup/` (MusicHub — `mh-screens.jsx`, `mh-extras.jsx`,
`mh-foryou.jsx`, `mh-generated.jsx`, `mh-auto.jsx`, launcher icons).

App version at audit time: **v0.13.1**. Many items from the previous
revision of this file are now shipped — this file is **rewritten** to
reflect what's still uncovered after v0.10.1 → v0.13.1 work.

For each entry below, the screen / sheet / dialog **exists in code** but
has **no mockup** in the bundle. Generate matching designs so the visual
rebrand can land on them. Same design system: lime `#A8E04E`, Inter +
JetBrains Mono, generative covers, eyebrow `// SECTION`, gradient
backgrounds. Italian copy.

> **Free app — confirm.** No Premium / Family / payments / audio-quality
> picker. Crossfade + Download-offline stay.
> **Auth — confirm.** Guest mode was removed in v0.12.9. All access
> requires Google sign-in. **Drop the "Continue as guest" path** that
> appeared in the previous revision.

---

## 1. Auth & first-run

### 1.1 `LoginScreen` — auth gate (`ui/auth/LoginScreen.kt`)
- States: NotSignedIn, Loading, SigningIn, Error, SignedIn.
- Single primary CTA: **"Accedi con Google"** (Google branded button).
- Hero: lime monogram tile (the launcher mark) + greeting +
  one-line subtitle ("Per ascoltare la tua libreria, accedi con Google").
- Error banner rendered when `state is Error` (red-ish strip + retry).
- No guest path. No email/password. No backend selector on this screen.

### 1.2 `OnboardingScreen` + `OnboardingSheet` (`ui/onboarding/`)
- First-launch tag picker — **"Cosa ascolti?"** — multi-select chips of
  genres (8+). Footer: skip / continue (lime CTA).
- Welcome sheet variant: shorter, single-screen modal with hero + CTA.
- Both reachable only on fresh install / reset; both write to the
  user's preferred-tags signal that seeds Daily Mix.

### 1.3 Account-switching dialog
- Triggered from Profile → Account → "Cambia account".
- Confirmation dialog before sign-out: title, body explaining cached
  data stays / sync stops, **Annulla** / **Disconnetti** (destructive).
- Companion success state: lands back on `LoginScreen`.

---

## 2. Discover & import

### 2.1 `FindScreen` — Discover/YT-import hub (`ui/find/FindScreen.kt`)
- Idle: search field + retry strip when last fetch failed.
- Loading skeleton row.
- Error state with `retry` button (uses the error-state copy
  standardised in v0.11.5).
- Result rows: candidate match with thumbnail, duration, source,
  CTA "Aggiungi" / status pill.
- Distinct from local Search — Find runs the YT auto-match pipeline.

### 2.2 `SpotifyImportScreen` — Exportify CSV import
  (`ui/playlists/SpotifyImportScreen.kt`)
- Step 1 — instructions panel ("Esporta da Exportify", link/help text).
- Step 2 — file picker entry (paste CSV / upload).
- Step 3 — preview list (name, owner, song count, "Importa N brani").
- Step 4 — progress strip while matching tracks.
- Step 5 — result summary (imported / skipped / failed) + CTA
  "Apri playlist".

---

## 3. Player surface (sheets/dialogs)

### 3.1 `QueueSheet` (`ui/player/QueueSheet.kt`)
- Three sections: **In riproduzione** · **In coda** (user queue,
  `KEY_USER_QUEUED`) · **Successivi** (system queue / source).
- Drag-to-reorder, swipe-to-remove, snackbar undo.
- Header: shuffle + repeat toggles, "Cancella coda".
- AA mockup already exists (`AAQueue`) — keep visual parity but mobile
  spec needed.

### 3.2 `EqualizerSheet` (`ui/player/EqualizerSheet.kt`)
- 10-band sliders, preset dropdown (Flat / Bass Booster / Treble
  Booster / Vocale / Personalizzato), bypass toggle, audio-session
  info strip at bottom.

### 3.3 Sleep-timer popover (inside Now Playing more menu)
- Options: 5 / 10 / 15 / 30 / 45 / 60 min + **"Fine traccia"**.
- Active state with countdown badge `MM:SS` and Annulla CTA.

### 3.4 Mini-player swipe-to-close affordance
  (`ui/player/MiniPlayer.kt`)
- v0.12.6: drag mini-player horizontally to stop + hide. Mockup needs
  to show resting state + the swipe-trail visual / fade.

### 3.5 Playback-error dialog (v0.13.1)
- Replaces toast. Title, error category copy ("file danneggiato",
  "codec non supportato", "server irraggiungibile", etc.), technical
  error code in mono small print, **Chiudi** / **Riprova** /
  **Riscarica** CTAs depending on category.

### 3.6 "Report wrong song" confirmation dialog (v0.12.8)
- Triggered from kebab → "Segnala brano sbagliato".
- Body explains the action is permanent, removes from search/playlists/
  liked/history across all devices, and blocks future re-download.
- **Annulla** + destructive **Segnala**.

---

## 4. Sheets — list & catalog

### 4.1 `AddToPlaylistSheet` (`ui/playlists/`)
- Triggered from kebab on any `SongRow`.
- Search/filter bar at top, list of playlists with cover + song count,
  "Crea nuova playlist" pinned at bottom.

### 4.2 `AddSongsToPlaylistSheet`
- Bulk add inside playlist detail. Catalog search, multi-select with
  checkboxes, footer CTA "Aggiungi N brani".

### 4.3 `TrackActionSheet` (`ui/common/TrackActionSheet.kt`)
- The shared kebab sheet. Rows (some conditional):
  Aggiungi alla coda · Riproduci dopo · Aggiungi a playlist ·
  Mi piace / Rimuovi mi piace · Condividi · Vai all'artista ·
  Vai all'album · Scarica / Rimuovi download · Mostra testo ·
  Mostra video · Sleep timer · **Non consigliarmi questo brano** ·
  **Non consigliarmi questo artista** · **Segnala brano sbagliato** ·
  Rimuovi dalla playlist (only inside playlist context).

---

## 5. Library drill-downs

### 5.1 `AlbumListScreen` (`ui/albums/AlbumListScreen.kt`)
- All-albums grid; sort (Recenti / A-Z), search filter.

### 5.2 `ArtistListScreen` (`ui/artists/ArtistListScreen.kt`)
- Round artist tiles, "X artisti" header, A-Z scrub.

### 5.3 `LikedScreen` (`ui/liked/LikedScreen.kt`)
- Hero gradient cover (`duotone` palette), shuffle + lime Play,
  paginated track list with kebab. Detail of Liked songs — **distinct
  from the pinned Liked row in Library**.

### 5.4 Genre detail / browse-tag results
- v0.10.20 added `Sfoglia · Tutti i generi` 4×2 grid (already
  designed). Tap → list of songs filtered by tag with removable
  pill at top + in-grid digit search. **Detail screen not yet drawn.**

---

## 6. Playlists — collaborative & sharing (v0.13.0)

### 6.1 Collaborative playlist detail variants
- Owner POV: "Condivisa con N persone" strip with avatars +
  "Gestisci membri" entry.
- Member POV: "Condivisa da <nome>" strip under cover; delete CTA
  becomes **"Rimuovi dalla libreria"** (not destructive for owner).
- Playlist row in Library: "Condivisa da <nome>" subtitle when owned
  by someone else.

### 6.2 Playlist auto-sync card (v0.12.1)
- Collapsed under cover: switch + label "Sincronizzazione automatica"
  + sub-line "Scarica i nuovi brani all'apertura dell'app".
- States: off (default), on, on + "Solo Wi-Fi" hint, syncing-now strip.

### 6.3 Generate share-link sheet (`PlaylistShareDialog.kt`)
- Owner-only entry. Shows the **https://** link
  (DuckDNS host — short example), copy CTA, system-share CTA.
- Caption explaining: members get the same playlist (collaborative,
  not a copy); only owner can issue links.

### 6.4 Receive / import shared playlist (`PlaylistShareImporter.kt`)
- Deep-link landing: preview cover + name + owner + song count.
- States: **first-time accept** ("Aggiungi alla mia libreria") ·
  **already a member** ("Apri playlist") · **already owner**
  (auto-route, ephemeral toast).
- Legacy `mediaplayer://share/<token>` URIs are still accepted —
  show identical preview regardless of scheme.

---

## 7. Profile / Settings sub-pages

`ProfileScreen` is drawn (Phase G). The chevron destinations need
mockups:

### 7.1 `CrossfadeScreen` (`ui/profile/settings/CrossfadeScreen.kt`)
- Slider 0–12 sec with mono ticks, value label, audition toggle.

### 7.2 `DownloadOfflineScreen` (`ui/profile/settings/DownloadOfflineScreen.kt`)
- Toggles: "Solo Wi-Fi" · **"Download automatico"** (default OFF
  per v0.12.6 — caption explains the behaviour change).
- Storage gauge + "Scaricati N brani · X MB".
- "Cancella tutti i download" destructive CTA.
- Two redownload paths captioned in the "Manage" list (re-fetch from
  origin vs drop local cache).
- "Forza rigenerazione Daily Mix" entry (v0.11.6).

### 7.3 `ThemeScreen` (`ui/profile/settings/ThemeScreen.kt`)
- Light / Dark / System radios with preview chips.

### 7.4 `DislikedScreen` (`ui/profile/settings/DislikedScreen.kt`) — v0.12.0
- Two tabs: **Brani** · **Artisti**.
- List with cover + name + "Ripristina" trailing tap.
- Empty state per tab.

### 7.5 `SettingsSubScreen` (generic chrome) — header pattern
- Standard top-bar (back chevron + mono caption + title) used by all
  the sub-pages above. Define once.

### 7.6 (Optional, not yet in code — confirm scope)
- Notifications · Lingua · Informazioni / About · Backend host.
  These were in the old request but **no Compose screen exists**.
  Skip unless the product wants to add them.

---

## 8. App-update channel

### 8.1 App-update banner (`ui/common/AppUpdateBanner.kt`)
- Non-modal banner pinned on Home: "Aggiornamento disponibile ·
  v<old> → v<new>", install CTA, dismiss.
- Progress state (downloading APK, % strip).
- Failure state with retry.

### 8.2 ChangelogSheet (`ui/changelog/ChangelogSheet.kt`)
- Auto-opens when stored `last_seen_version ≠ AppVersion.VERSION`.
- Body: title + bullets of `ChangelogEntry.highlights`.
- Footer: "Continua" CTA. Optional pager when multiple
  unseen entries.

### 8.3 Profile → "Eventi in coda" diagnostic (v0.11.6)
- Shows pending offline-sync events count (Mi piace / Segui artista /
  riproduzioni / dislike). Live counter that drops to 0 once flushed.

---

## 9. Android Auto — incremental

The 13 AA screens drawn in `mh-auto.jsx` cover the main tree. Still
missing visual specs:

### 9.1 AA Genres tile (v0.10.20)
- Same 8 genres as mobile (Indie / Elettronica / Hip-hop / Jazz /
  Classica / Ambient / Rock / Pop). Browse-grid hint.

### 9.2 AA now-playing description-line lyric ticker (v0.10.20)
- Shows the current lyric line in the now-playing card description.
- No scroll (driver-safe). Spec the typography + truncation rule.

### 9.3 AA custom layout commands
- Like (heart) and Sleep timer chips on the AA player surface.
- Spec disabled-while-driving treatment for sleep-timer numeric input.

> **Skip:** AA login / AA equalizer fine sliders. Auth happens on
> phone; equalizer remains phone-only.

---

## 10. Cross-cutting — already shipped, mockup parity check only

These are **drawn** but worth a re-audit pass against current code
because the implementation evolved after the original mockup:

- **Now Playing** (mh-screens) — current code uses 300dp cover; check
  closable mini-player gesture is consistent.
- **Lyrics** (mh-extras) — current `LyricsView` is fade-active. AA
  ticker line shares this spec.
- **Video** (mh-extras) — confirm: when video active, audio scrubber
  + audio controls **are hidden** (v0.12.5) and the action-bar icon
  swaps (library-video / playing-note).
- **PlaylistDetailScreen** — confirm long-press to reorder
  (v0.12.7) replaces the dedicated drag handle.
- **SongRow** — confirm v0.12.7 layout: subtitle is "Artista • 3:42"
  only (no album), heart sits next to kebab.

---

## Notes for Claude Design

- Lime accent + Inter + JetBrains Mono. No new brand colors.
- App is fully free. No Premium / payments / audio-quality picker.
- **No guest mode.** All non-auth screens assume signed-in user.
- Italian copy on user-visible strings. English fallback OK.
- iPhone frame for previews is fine — aesthetic only; the device is
  Android.
- For each new screen: eyebrow `// SECTION` mono, lime CTA primary,
  `rgba(255,255,255,0.08)` secondary, generative SVG covers, dock =
  `MHPlayerBar` + 4-tab `MHBottomNav` (Home / Cerca / Per te /
  Libreria). Profile is reached via gear icon in section headers.
