# MusicHub mockup vs implementation — audit report

**App version at audit:** v0.13.1 · **Date:** 2026-05-04 · **Last update:** 2026-05-06 (area 7 closed in v0.16.3, area 8 closed in v0.16.4, area 9 polish landed in v0.16.5)
**Mockups:** `mockup/*.jsx` (MusicHub design system — lime `#A8E04E`, Inter + JetBrains Mono, Italian copy, generative covers, eyebrow `// SECTION`)
**Goal:** find every place where the shipped Android UI diverges from the design contract.

This file is the executive overview. Each section has a per-area detail file.

> **2026-05-05 — state-mockup pass landed.** Three new files (`mockup/mh-auth-states.jsx`, `mh-discover-states.jsx`, `mh-library-states.jsx`) close the prior "Missing in mockup" gaps for areas 1/2/3. Per-area detail files have been updated with the new contract (search for `### Now covered by state mockups`). Highest-impact gaps in §1–§3 stay hot — those tracked impl-side gaps, not mockup gaps.

| # | Area | Detail file | Severity |
|---|------|-------------|----------|
| 1 | Auth & first-run | [01-auth.md](01-auth.md) | ✅ Done (v0.13.3) |
| 2 | Discover & Spotify import | [02-discover.md](02-discover.md) | ✅ Done (v0.13.4) |
| 3 | Library drilldowns + sharing | [03-library.md](03-library.md) | ✅ Done (v0.15.0) |
| 4 | Player sheets / dialogs | [04-player-sheets.md](04-player-sheets.md) | High |
| 5 | Profile / settings sub-pages | [05-settings.md](05-settings.md) | Medium |
| 6 | App-update + changelog + event-queue | [06-update.md](06-update.md) | ✅ Done (v0.16.0) |
| 7 | Ringtone trim editor | [07-trim.md](07-trim.md) | ✅ Done (v0.16.3) |
| 8 | Android Auto extras | [08-auto-extra.md](08-auto-extra.md) | ✅ Done (v0.16.4) |
| 9 | Core screens parity (Home/Search/ForYou/NowPlaying/Lyrics/Video/PlaylistDetail/SongRow) | [09-core-screens.md](09-core-screens.md) | ✅ Done (v0.16.5) — actionable polish landed; 3 design-locked drifts remain |

---

## Cross-cutting findings (apply to most areas)

### C. Eyebrow / mono caption pattern missing everywhere
Mockup design system requires `// SECTION` JetBrains-Mono uppercase captions above section titles. Impl ships none of them:
- Auth: no `// CAMBIA ACCOUNT`, `// PASSO 1 / 1`.
- Discover: no `// SCOPRI · YT`, `// 4 RISULTATI · YT MATCH`, `// PASSO N / 5`.
- Library: no `// LA TUA LIBRERIA`, count badges absent.
- Settings shared chrome: missing `// IMPOSTAZIONI` / `// CONSIGLI` above titles.
- Update banner / changelog: missing `// AGGIORNAMENTO`, `// NOVITÀ · vX.Y.Z`.

### Area 9 closure (v0.16.5)

Actionable parity gaps from `09-core-screens.md` shipped:
- **LyricsView tri-state fade** — active line lime + larger (`headlineSmall` vs `bodyLarge`), past 0.35α, future 0.5α (`LyricsSheet.kt:143-167`).
- **Lyrics empty state Italian** — `"Testo non disponibile"` / `"Testo non trovato"` replace the English drift (`LyricsSheet.kt:99`).
- **Home greeting tail** — `"· N nuove uscite per te"` appended to the date line when Release Radar is non-empty (`HomeScreen.kt:182-200, 471-510`).
- **PlaylistCardSquare Italian** — `"N brani"` / `"Generata per te"` replace English `"songs"` / `"Made for you"` (`HomeScreen.kt:736-740`).
- **SongRow separator** — `· ` middle-dot replaces the bullet `•` to match mockup glyph (`SongRow.kt:156`).
- **Per te `OGGI` badge** — lime mono trailing badge on the mix-grid SectionHeader (`ForYouScreen.kt:148-152`, `SectionHeader.kt:32-69`).
- **MHPlayingBars + lime active title on every list** — wired via `LocalNowPlaying` CompositionLocal in `MainActivity.kt`, consumed by `SongRow.kt:60-66`. Active row across Home Music filter, Search, Liked, Album, Artist, Genre, Playlist Detail and Queue now lights up lime + animated bars.
- **MHLogo at top of Home + Per te** — brand lockup pinned above the greeting / "Per te" title per mockup (`HomeScreen.kt:456-477`, `ForYouScreen.kt:118-138`).

Deferred — see `Claude_design_review.md` §§ 2-5 for design decisions:
- **NowPlaying cover 360dp ceiling** — mockup says 300dp; impl uses 92% of width capped at 360dp. Plan-locked. Awaiting design call. (Review §2)
- **ForYou hero heart shortcut** — needs decision on what the heart toggles (no data model). (Review §3)
- **Full-screen Lyrics + Video mockups** (`mh-extras.jsx`) — both consolidated as inline blocks inside Now Playing. Awaiting decision: retire / rebadge / build routes. (Review §4)
- **`History` icon in Home + Per te top bars** — `Bell` is permanently out-of-scope per project notes; `History` remains divergent. (Review §5)

### D. State coverage gaps
Impl frequently ships only the happy path; mockup specs all states.

- ~~AppUpdateBanner — only `available`; missing `progress` and `failed`.~~ ✅ Closed v0.16.0 — all three states render with live `DownloadManager` polling; required updates render as a full-screen blocking overlay.
- Playback-error dialog has only OK, no `Riprova` / `Riscarica` (04-player-sheets).
- ~~ChangelogSheet shows historical entries with English heading vs mockup's curated single-release hero with pager dots.~~ ✅ Closed v0.16.0 — Italian hero with lime gradient + version diff, numbered highlights, `HorizontalPager` with dot indicator, sticky `Continua` pill, latest entry only.
- DownloadOfflineScreen missing `// GESTIONE` triplet (re-fetch / drop cache / regen Daily Mix), storage breakdown, destructive red-pill (05-settings).

### E. CTAs vs Material defaults
Impl reaches for stock Material widgets where mockup specifies bespoke pill / chip / sheet patterns.

- Discover row uses full-row tap; mockup wants per-row `Aggiungi` / `In coda` chips + `YT` source pill.
- Account-switch is a stock `AlertDialog`; mockup wants account-preview row + filled red destructive pill.
- AddSongsToPlaylistSheet auto-commits on single tap; mockup specs batched multi-select with `Aggiungi N brani` footer.
- DislikedScreen uses Material tabs + icon button; mockup wants pill tabs + `Ripristina` text button.
- EqualizerSheet uses horizontal Material sliders; mockup wants vertical 10-band, preset card, audio-session info.
- PlaylistShareDialog is fully replaced with `Intent.createChooser` (03-library §6.3).
- PlaylistShareImporter is a plain `AlertDialog` instead of full-screen mockup (03-library §6.4).

---

## Highest-impact gaps (suggested priority for design rebrand follow-up)

1. **Sleep timer surface** — mockup specs full bottom sheet with countdown + 6 presets + `Fine traccia`. Impl is a 3-item `DropdownMenu` (`NowPlayingSheet.kt:768`). Affects mobile + AA. (04-player-sheets, 08-auto-extra)
2. **Playback-error dialog CTAs** — mockup has `Riprova` / `Riscarica` per error category; impl has only OK (`MainActivity.kt:517`). Loses the v0.13.1 dialog promise.
3. ~~**AppUpdateBanner missing 2/3 states**~~ ✅ Closed v0.16.0.
4. ~~**ChangelogSheet structure mismatch**~~ ✅ Closed v0.16.0.
5. **Collaborative-playlist UI** — owner/member strips, `Gestisci membri` entry, per-track contributor badge, `Rimuovi dalla libreria` for non-owners are all missing (03-library §6.1).
6. **PlaylistShareDialog + Importer** — both bypassed for stock Android dialogs; loses the `https` link card, copy CTA, system-share CTA, deep-link landing preview.
7. **LoginScreen brand identity** — `Text("M")` instead of equalizer monogram, lime pill instead of Google-G pill, raw `e.message` instead of boxed error panel. First impression for every user.
8. **Spotify import** — single-playlist + opaque progress vs mockup's 5-step wizard, multi-playlist preview, rich progress card, English error strings throughout.
9. **DownloadOfflineScreen** — missing storage breakdown, `// GESTIONE` triplet, destructive pill, regen-Daily-Mix entry.
10. ~~**AA custom layout** — only 2 chips (Like + Sleep 30m default) vs mockup's 4 (`Coda`, `cast`); like/sleep labels English; no preset picker for sleep timer.~~ ✅ Closed v0.16.4 — sleep chip is now four quick-set presets `Sospendi tra 15m / 30m / 60m` + `Fine traccia` (collapses to live `Annulla · N min` / `Annulla · fine traccia` cancel button while armed); like/sleep labels were already Italian (audit was stale on D12/D13). `Coda` chip can't be implemented as a now-playing button (custom commands can't navigate AA's UI), but D9 was closed by adding a `Coda corrente` browse-tree folder under the AA root that lists the live player timeline (current row marked `▸`) and jumps to the chosen position on tap. `BMW Audio` (cast) remains AA-owned audio-route chrome.

---

## Pure feature gaps (not polish — code does not exist)

- ~~**Ringtone trim editor** (`mh-trim.jsx`) — fully absent.~~ ✅ Closed v0.16.0 → v0.16.3 — full-screen `TrimScreen.kt` with dual-timeline canvas, IN/OUT handles, ±1s/±.1 nudge boxes, ×8 long-press zoom, A/B preview loop, snap-to-silence on real PCM peaks (decoded via MediaExtractor + MediaCodec, cached on disk), 0.5s fade-in/out re-encoded server-side via `afade`. Backend `POST /api/songs/{id}/cut` (ffmpeg `-c copy` or libmp3lame q2 + afade) creates a first-class master row with shifted lyrics + duplicated cover; `POST /api/playlists/replace-song` swaps the original across the user's accessible playlists when the saved-toast `Sì` is tapped.
- **GenreDetailScreen** — `mh-library.jsx` specifies a dedicated screen with removable pill + count + grid; impl reuses search filter only. (03-library)
- ~~**Eventi-in-coda diagnostic sub-screen**~~ ✅ Closed v0.16.0 — full sub-screen at `profile/queued-events` with 56sp lime hero count, `// DETTAGLIO` per-row events (squircle icon + label + ×N lime pill), per-event titles via new `display_label` column on `pending_events` (DB v3 migration), and `MM:SS` countdown to next retry sourced from `MIN(next_attempt_at)` ticking once per second.
- ~~**AA Genres tile artwork** — `LibraryTree.kt:519-528` passes `artworkSongId = null`, so per-genre covers from mockup are unreachable.~~ ✅ Closed v0.16.4 — 8 bundled `genre_*.xml` gradient drawables matching the mockup's MHCover palettes, surfaced via `android.resource://...` artworkUri so AA fetches them locally without depending on the LAN backend.
- **AA description-line lyric chrome** — `MediaMetadata.description` is the only field AA renders below title/subtitle; the mockup's accent-bordered `// ORA` card chrome cannot be expressed via Media3 session metadata. (08-auto-extra D6) — AA owns the chrome. *Partial closure v0.16.4*: D7 (line clipped to ~60 chars) + D8 (`// ORA · ` text prefix) shipped.
- ~~**AA `Fine traccia` (end-of-track) sleep mode**~~ ✅ Closed v0.16.4 — `SleepTimer.setEndOfTrack(player, onExpire)` listens for the next AUTO/REPEAT transition and pauses; surfaced as a `Fine traccia` preset chip in the AA custom layout next to the minute presets.

---

## Confirmed parity (already shipped correctly)

From the §10 checklist in `missing-mockup.md`:

- ✅ Mini-player swipe-to-close (v0.12.6) — visual + behavior consistent.
- ✅ Video surface hides audio scrubber/controls (v0.12.5).
- ✅ Video action-bar icon swap (library-video / playing-note).
- ✅ PlaylistDetail long-press reorder (v0.12.7) — no drag handle.
- ✅ SongRow v0.12.7 layout: subtitle `Artista • 3:42` only, heart next to kebab.

Two drifts inside the same checklist:

- ⚠ NowPlaying cover capped at **360dp** (`Shapes.kt:42-43`), mockup specifies **300dp**. Plan-locked — the cover *is* the screen on mobile widths.
- ✅ LyricsView fade tri-state landed in v0.16.5: active = lime + headlineSmall, past = onSurface @ 0.35α, future = @ 0.5α.

---

## Notes on mockup → impl mapping

- `mh-canvas-app.jsx` is the canvas index — confirm any mockup mounted there but missing from this audit before discussing.
- `mh-shared.jsx` defines `MHCover`, `MHPlayerBar`, `MHBottomNav`, `MHLogo`, `MHPlayingBars` — primitives. Impl has rough equivalents under `ui/common/` (e.g. `MHCover.kt`) but `MHLogo` / `MHPlayingBars` analogues are missing.
- The 4-tab `MHBottomNav` (Home / Cerca / Per te / Libreria) and Profile-via-gear-in-headers pattern are partially implemented; cross-check the navigation host.

---

## How to use this report

For each area:
1. Open the matching `0X-*.md` file.
2. Triage findings as **Brand-critical** (logo / monogram / lime gradient / eyebrows) vs **Copy** (Italian leakage) vs **State** (missing loading/error/progress) vs **Behavior** (CTAs / multi-select / drag reorder).
3. Decide which become phases under the next milestone (continuation of `project_design_rebrand` Phase J — the 26 missing mockups now exist, so Phase J can finally land).
