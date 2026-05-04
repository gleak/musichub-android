# MusicHub mockup vs implementation — audit report

**App version at audit:** v0.13.1 · **Date:** 2026-05-04
**Mockups:** `mockup/*.jsx` (MusicHub design system — lime `#A8E04E`, Inter + JetBrains Mono, Italian copy, generative covers, eyebrow `// SECTION`)
**Goal:** find every place where the shipped Android UI diverges from the design contract.

This file is the executive overview. Each section has a per-area detail file.

| # | Area | Detail file | Severity |
|---|------|-------------|----------|
| 1 | Auth & first-run | [01-auth.md](01-auth.md) | High |
| 2 | Discover & Spotify import | [02-discover.md](02-discover.md) | High |
| 3 | Library drilldowns + sharing | [03-library.md](03-library.md) | High |
| 4 | Player sheets / dialogs | [04-player-sheets.md](04-player-sheets.md) | High |
| 5 | Profile / settings sub-pages | [05-settings.md](05-settings.md) | Medium |
| 6 | App-update + changelog + event-queue | [06-update.md](06-update.md) | Medium |
| 7 | Ringtone trim editor | [07-trim.md](07-trim.md) | Feature gap |
| 8 | Android Auto extras | [08-auto-extra.md](08-auto-extra.md) | Medium |
| 9 | Core screens parity (Home/Search/ForYou/NowPlaying/Lyrics/Video/PlaylistDetail/SongRow) | [09-core-screens.md](09-core-screens.md) | Low (5/7 ok) |

---

## Cross-cutting findings (apply to most areas)

### A. Italian / English copy drift
Mockups are **fully Italian**. Impl ships English in many places — especially user-facing strings outside the core localised flows.

- Discover: `"Try Again"`, `"Could not read file."`, `"No tracks found…"`, `"Imported Playlist"` (02-discover §SpotifyImport).
- Library drilldowns: titles `"Albums"`, `"Artists"`, `"Liked Songs"` (03-library).
- Onboarding sheet: `"Welcome to MediaPlayer"` plus 3 English feature lines (01-auth §1.3).
- Lyrics + overflow + empty states (09-core-screens §cross-cutting).
- AA custom-command labels (`Like` / `Sleep 30m` — 08-auto-extra).
- DislikedScreen tab title `Non consigliati` vs mockup `Non consigliarmi questo` (05-settings §7.4).

### B. Brand identity is stale
Mockups specify the **MusicHub** rebrand with the equalizer-bars monogram on a lime-tinted radial. Impl still uses:
- `Text("M")` placeholder for the monogram (LoginScreen).
- App name string `"MediaPlayer"` in OnboardingSheet hero.
- Generic gradient backgrounds instead of the lime-tinted radial.
- No `MHLogo` / `MHPlayingBars` indicator in top bars / NowPlaying (09-core-screens).

### C. Eyebrow / mono caption pattern missing everywhere
Mockup design system requires `// SECTION` JetBrains-Mono uppercase captions above section titles. Impl ships none of them:
- Auth: no `// CAMBIA ACCOUNT`, `// PASSO 1 / 1`.
- Discover: no `// SCOPRI · YT`, `// 4 RISULTATI · YT MATCH`, `// PASSO N / 5`.
- Library: no `// LA TUA LIBRERIA`, count badges absent.
- Settings shared chrome: missing `// IMPOSTAZIONI` / `// CONSIGLI` above titles.
- Update banner / changelog: missing `// AGGIORNAMENTO`, `// NOVITÀ · vX.Y.Z`.

### D. State coverage gaps
Impl frequently ships only the happy path; mockup specs all states.

- AppUpdateBanner — only `available`; missing `progress` and `failed` (06-update).
- Playback-error dialog has only OK, no `Riprova` / `Riscarica` (04-player-sheets).
- ChangelogSheet shows historical entries with English heading vs mockup's curated single-release hero with pager dots (06-update).
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
3. **AppUpdateBanner missing 2/3 states** — progress + failed not rendered. Download progress only in system shade. (06-update)
4. **ChangelogSheet structure mismatch** — impl shows full history in English; mockup specs curated single-release hero with `vX → vY` lime diff and pager dots. Returning users land on a markedly off-brand screen.
5. **Collaborative-playlist UI** — owner/member strips, `Gestisci membri` entry, per-track contributor badge, `Rimuovi dalla libreria` for non-owners are all missing (03-library §6.1).
6. **PlaylistShareDialog + Importer** — both bypassed for stock Android dialogs; loses the `https` link card, copy CTA, system-share CTA, deep-link landing preview.
7. **LoginScreen brand identity** — `Text("M")` instead of equalizer monogram, lime pill instead of Google-G pill, raw `e.message` instead of boxed error panel. First impression for every user.
8. **Spotify import** — single-playlist + opaque progress vs mockup's 5-step wizard, multi-playlist preview, rich progress card, English error strings throughout.
9. **DownloadOfflineScreen** — missing storage breakdown, `// GESTIONE` triplet, destructive pill, regen-Daily-Mix entry.
10. **AA custom layout** — only 2 chips (Like + Sleep 30m default) vs mockup's 4 (`Coda`, `cast`); like/sleep labels English; no preset picker for sleep timer.

---

## Pure feature gaps (not polish — code does not exist)

- **Ringtone trim editor** (`mh-trim.jsx`) — fully absent. Impl `RingtoneExporter.exportAsAlarm` exports the entire track; no IN/OUT model, no waveform, no slice-encode, no nudge / fade / A-B / zoom. Treat as new feature, not polish. (07-trim)
- **GenreDetailScreen** — `mh-library.jsx` specifies a dedicated screen with removable pill + count + grid; impl reuses search filter only. (03-library)
- **Eventi-in-coda diagnostic sub-screen** — mockup specs full screen (hero count, typed events list with icons + ×N pills, flush countdown); impl is one settings row with aggregate count. (06-update)
- **AA Genres tile artwork** — `LibraryTree.kt:519-528` passes `artworkSongId = null`, so per-genre covers from mockup are unreachable. (08-auto-extra)
- **AA description-line lyric ticker** — uses `MediaMetadata.description` (`AALyricsTicker.kt:171-179`); mockup's accent-bordered `// ORA` block is unreachable in AA's content-style spec. (08-auto-extra)

---

## Confirmed parity (already shipped correctly)

From the §10 checklist in `missing-mockup.md`:

- ✅ Mini-player swipe-to-close (v0.12.6) — visual + behavior consistent.
- ✅ Video surface hides audio scrubber/controls (v0.12.5).
- ✅ Video action-bar icon swap (library-video / playing-note).
- ✅ PlaylistDetail long-press reorder (v0.12.7) — no drag handle.
- ✅ SongRow v0.12.7 layout: subtitle `Artista • 3:42` only, heart next to kebab.

Two drifts inside the same checklist:

- ⚠ NowPlaying cover capped at **360dp** (`Shapes.kt:42-43`), mockup specifies **300dp**.
- ⚠ LyricsView fade is binary not tri-state per `LyricsSheet.kt:147-150`.

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
