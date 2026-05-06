# AA Extra (mockup `mh-auto-extra.jsx`) vs current implementation

Mockup file: `mockup/mh-auto-extra.jsx` — three AA increments:
9.1 Genres tile, 9.2 Now-playing lyric ticker, 9.3 Sleep-timer driver-safe.

> **2026-05-06 — closed in v0.16.4.** Implementable diffs landed:
> - **D1** — 8 bundled `res/drawable/genre_*.xml` gradient drawables
>   matching the mockup MHCover palettes; surfaced via `android.resource://...`
>   artworkUri so AA fetches locally (no LAN dependency).
> - **D2** — genre tile subtitle `"Genere"` dropped.
> - **D7** — lyric line clipped to 60 chars + ellipsis before reaching
>   `MediaMetadata.description` (consistent across head units).
> - **D8** — lyric line prefixed with `// ORA · ` so AA's third metadata
>   line reads as a lyric, not a generic caption.
> - **D9** — new `Coda corrente` folder at the AA browse root: lists
>   the current player timeline (`▸` marker on the active row), tap
>   jumps to that index without rebuilding the queue. The mockup's
>   chip-in-now-playing-card variant remains unimplementable
>   (custom-layout buttons cannot navigate AA's UI), but the browse-tree
>   surface delivers the same intent — driver gets a glanceable
>   queue list in the MusicHub design language instead of AA's
>   generic queue affordance.
> - **D11** — live `Annulla · N min` countdown chip via new
>   `SleepTimer.remainingMs` minute-boundary tick.
> - **D14** — three minute presets (`Sospendi tra 15m / 30m / 60m`) +
>   `Fine traccia` chip in the AA custom layout instead of one fixed
>   `Sospendi tra 30m`.
> - **D15** — `SleepTimer.setEndOfTrack(player, onExpire)` listens for
>   AUTO/REPEAT transition; `EXTRA_SLEEP_END_OF_TRACK` published on
>   session extras; chip label `Annulla · fine traccia` while armed.
> - **D17** — `EXTRA_SLEEP_REMAINING_MS` (Long) on session extras.
> - **D18** — cancel chip relabeled `Annulla · N min` / `Annulla · fine traccia`
>   so the cancel intent is explicit alongside the countdown.
>
> D12/D13 were already shipped Italian — audit was stale on them.
> Stays open and routed to design: **D6** (lyric block chrome —
> Media3 has no card-styling primitive; design review filed at
> `Claude_design_review.md`). Stays open by platform constraint:
> **D10** (cast chip — AA-owned audio-route picker), **D16** (driver-
> safety warning — no `CarConnection` listener wired, intentional skip).
> See updated verdict per row in the summary table at the bottom.

Source files inspected:
- `app/src/main/kotlin/com/mediaplayer/android/playback/LibraryTree.kt`
- `app/src/main/kotlin/com/mediaplayer/android/playback/AALyricsTicker.kt`
- `app/src/main/kotlin/com/mediaplayer/android/playback/MediaPlaybackService.kt`

---

## 9.1 — AA Genres tile

### Mockup spec (mh-auto-extra.jsx:24-55)

8 genre tiles in a 4x2 grid with custom `MHCover` artwork (kind +
duotone palette per tile), shown under a `Cerca` tab with a back
button and the mono header `// SFOGLIA · TUTTI I GENERI`.

Genre list (display name + visual kind/palette):

| # | Name | kind | palette |
|---|---|---|---|
| 1 | `Indie` | duotone | `#3A0CA3` / `#F72585` |
| 2 | `Elettronica` | blob | `#1E3A8A` / `#06B6D4` |
| 3 | `Hip-hop` | triangles | `#1A1A1A` / `#FF4D2E` |
| 4 | `Jazz` | stripes | `#FFC857` / `#1A1A1A` |
| 5 | `Classica` | wave | `#5C2D8C` / `#F0A6B0` |
| 6 | `Ambient` | dot | `#0B3D2E` / `T.ACCENT` |
| 7 | `Rock` | arc | `#FF6B5B` / `#3A1F8A` |
| 8 | `Pop` | grid | `#0E1F3A` / `T.ACCENT` |

### Implementation

`LibraryTree.GENRES` (LibraryTree.kt:87-96) — same 8 names, same
order. Tags hard-wired to backend taxonomy:

```
"Indie" to "indie",
"Elettronica" to "electronic",
"Hip-hop" to "hip-hop",
"Jazz" to "jazz",
"Classica" to "classical",
"Ambient" to "ambient",
"Rock" to "rock",
"Pop" to "pop",
```

`genreTiles()` (LibraryTree.kt:519-528) renders each as a folder
tile with `subtitle = "Genere"`, `artworkSongId = null`, and
crucially `grid = false` — i.e. **LIST** not GRID.

`sectionFolder(GENRES_ID, "Generi", grid = true …)` (LibraryTree.kt:409-410)
sets the parent folder's child hint to **GRID**, so AA does try to
render the children as a grid. But the children themselves emit
`CONTENT_STYLE_BROWSABLE_HINT = LIST` (folderTile, LibraryTree.kt:639-643)
which propagates to *their* children, not to themselves.

### Diffs

- **D1 — No per-genre cover artwork.** Mockup gives each genre a
  `MHCover` palette/kind; impl passes `artworkSongId = null` so
  AA falls back to its generic placeholder. The driver-glance
  visual identity is missing; tiles are colour-less name labels.
  No backend-provided genre cover URI is wired either.
- **D2 — Section subtitle says `Genere` literally on every tile.**
  Mockup has no subtitle (just a large name label over the cover
  with a bottom gradient). Italian noun-on-each-row is redundant
  noise.
- **D3 — Tile order partially differs from mockup grid reading
  order.** Mockup row 1: Indie / Elettronica / Hip-hop / Jazz;
  row 2: Classica / Ambient / Rock / Pop. Implementation order
  matches exactly — **no diff**.
- **D4 — Path discoverability.** Mockup shows Genres tab under
  *Cerca* (search). In AA the genres folder lives at the browse
  root directly (`rootChildren()`, LibraryTree.kt:396-413), one
  of 8 root sections. Acceptable AA-side, just noting.
- **D5 — Empty-state copy mismatch.** `genreSongs(...)` returns
  `infoItem("Nessun brano per questo genere")` (LibraryTree.kt:543)
  on empty result; mockup never shows an empty state for this view.
  Not a regression — just absent from spec.

---

## 9.2 — AA now-playing with lyric ticker

### Mockup spec (mh-auto-extra.jsx:58-114)

- 46% cover panel + right info panel.
- Title `Undertow` 44pt, subtitle `Helena Vorr · Slow Hours`.
- Lyric ticker block under the title:
  - Padding `14px 18px`, accent-tinted background, accent border.
  - Mono uppercase eyebrow `// ORA`.
  - Single line, **24pt 700 weight**, `whiteSpace: nowrap`,
    `overflow: hidden`, `textOverflow: ellipsis`.
  - Sample text: `And the tide pulled me under, slow`.
- Custom layout chip strip under the playback controls:
  - `Mi piace` (heart, **active**)
  - `Timer · 27 min` (sleep, **active**)
  - `Coda` (queue, neutral)
  - `BMW Audio` (cast, neutral, right-aligned)

### Implementation

`AALyricsTicker.kt:38-184` writes the active lyric line into
`MediaMetadata.description` via `Player.replaceMediaItem` once a
second (`POLL_MS = 1_000L`, AALyricsTicker.kt:182).

Active line resolved with `lines.indexOfLast { it.positionMs <= pos }`
(AALyricsTicker.kt:164) — synced lyric, not free-running ticker.
Ticker only runs while at least one AA / Automotive controller is
connected (`aaConnected` gate, AALyricsTicker.kt:62, MediaPlaybackService.kt:451-469).

Custom layout (MediaPlaybackService.kt:344-366) is **2 buttons**, not 4:

1. `buildLikeButton(liked)` — display `"Like"` / `"Unlike"`,
   icon `R.drawable.ic_favorite` ↔ `ic_favorite_border`.
2. `buildSleepButton(active)` — display `"Sleep ${defaultSleepMinutes}m"`
   = `"Sleep 30m"` (defaultSleepMinutes = 30, line 150) or
   `"Cancel sleep"`. Icon `ic_bedtime` ↔ `ic_bedtime_off`.

`onCustomCommand` handles `ACTION_TOGGLE_LIKE` and
`ACTION_SLEEP_TIMER` only (MediaPlaybackService.kt:471-507). No
queue-view command, no cast-target command.

### Diffs

- **D6 — Lyric ticker uses `MediaMetadata.description`, not a
  dedicated visual block.** AA renders description as the third
  metadata line under title/subtitle in its standard now-playing
  card. The mockup's accented bordered panel with `// ORA`
  eyebrow cannot be expressed via Media3 session metadata —
  AA owns the visual chrome. Implementation is the only realistic
  option, but the rendered result will look nothing like the
  mockup: smaller type, no border, no eyebrow.
- **D7 — Truncation behaviour delegated to AA.** Mockup explicitly
  specifies `whiteSpace: nowrap; overflow: hidden; textOverflow: ellipsis`.
  Implementation writes the full line into `description`; whether
  AA single-line-clips or wraps depends on AA's renderer, not us.
  Long lyrics may wrap or truncate inconsistently. No active
  truncation in code (`applyDescription`, AALyricsTicker.kt:171-179).
- **D8 — Eyebrow `// ORA` cannot be added.** No way to tag the
  description as "current lyric"; AA users see a bare line with
  no provenance. Could prepend `▸ ` or similar but not currently
  done.
- **D9 — No `Coda` (queue) chip in custom layout.** Mockup
  requires four chips; implementation ships two. Queue view in
  AA is reachable via the standard AA queue affordance, not a
  custom chip — so this may be intentional, but the mockup
  shows a chip.
- **D10 — No `cast` chip (`BMW Audio`).** Audio-route picker is
  also AA-owned chrome (system output switcher); custom command
  here would duplicate. Mockup chip is decorative but not
  representable via session commands.
- **D11 — Sleep chip label diverges.** Mockup: `Timer · 27 min`
  (live countdown of remaining minutes). Impl: when active label
  flips to `"Cancel sleep"`, no remaining-minutes countdown.
  `SleepTimer.isActive` is a `StateFlow<Boolean>`, not duration
  (MediaPlaybackService.kt:331). The custom layout is rebuilt
  only on active/inactive transition, not every second, so the
  label cannot show running minutes anyway. Trade-off
  acknowledged: per-second `setCustomLayout` would thrash the
  AA UI.
- **D12 — Like chip label `Mi piace` (mockup) vs `Like` / `Unlike`
  (impl, line 353).** App is otherwise localised in Italian.
  Mismatch: heart label is the only English string in the AA
  custom strip. Should be `Mi piace` / `Non mi piace` for
  consistency.
- **D13 — Sleep chip label `Sleep 30m` (impl, line 362) is also
  English** vs mockup `Timer · 27 min` (Italian + dynamic).
  Same localisation gap.

---

## 9.3 — AA driver-safe sleep timer panel

### Mockup spec (mh-auto-extra.jsx:144-189)

Two-column custom layout:

- **Active column** — eyebrow `// ATTIVO`, large mono `27:14`
  remaining (96pt), copy `Si fermerà tra 27 minuti`, `Annulla`
  button.
- **Preset chip column** — `// CAMBIA · TAP RAPIDO` eyebrow,
  3x2 grid of preset minutes `[5, 10, 15, 30, 45, 60]`,
  highlighted bottom row `Fine traccia` (end-of-track),
  warning banner: *Input numerico libero disabilitato in
  marcia · solo preset*.

### Implementation

There is **no UI surface for a sleep-timer picker** on the AA
side. The session command `ACTION_SLEEP_TIMER` accepts a
`"minutes"` int (MediaPlaybackService.kt:493-497):

```
val minutes = if (args.containsKey("minutes")) {
    args.getInt("minutes")
} else {
    defaultSleepMinutes  // 30
}
```

But there is no AA browse or custom-action UI that lets the
driver *pick* minutes — pressing the sleep chip in the custom
layout sends the command with no `args`, so it always uses the
30-minute default (or cancels if a timer is active). The phone
ViewModel (`PlaybackViewModel.kt:883, 893`) sends specific
minute values from the phone sleep sheet, but AA cannot do this.

### Diffs

- **D14 — No preset chip selector exposed to AA.** Mockup shows
  six presets + `Fine traccia`. Impl: only a binary
  on(30-min)/off chip. Drivers in the car cannot pick 5/10/15/45/60.
- **D15 — No `Fine traccia` (end-of-track) mode.** `SleepTimer`
  takes minutes only — no "stop at end of current song" mode is
  implemented anywhere. Phone sheet also lacks it (out of scope
  for this audit; relevant because mockup highlights it).
- **D16 — No driver-safety warning surface.** Mockup explicitly
  warns that free numeric input is disabled in motion; impl has
  no driving-mode awareness at all (no
  `CarConnection.getType()` / `parking-brake` listener). Defaults
  are restrictive enough that this is moot, but spec language is
  not reflected.
- **D17 — No countdown surface.** Mockup hero displays `27:14`
  ticking down. Impl publishes only `EXTRA_SLEEP_ACTIVE` boolean
  to session extras (MediaPlaybackService.kt:73, 338-342). No
  remaining-ms or remaining-minutes extra is emitted, so AA UI
  could not render a countdown even if it wanted to.
- **D18 — `Annulla` reachable only by re-tapping sleep chip.**
  Mockup shows a discrete `Annulla` button in the active column;
  impl reuses the same chip, whose label flips to
  `"Cancel sleep"` (MediaPlaybackService.kt:362) when active.
  Functional parity — UX label gap.

---

## Summary table

| ID | Surface | Severity | Note |
|---|---|---|---|
| D1 | Genres tile artwork | ✅ v0.16.4 | 8 bundled `genre_*.xml` gradient drawables, `android.resource://` URIs |
| D2 | Genres tile subtitle | ✅ v0.16.4 | `subtitle = null` in `genreTiles()` |
| D3 | Genres order | OK | Matches mockup |
| D4 | Genres root path | OK | Acceptable |
| D5 | Genres empty state | OK | Not in spec, harmless |
| D6 | Lyric ticker chrome | Design review | Routed — see `Claude_design_review.md` |
| D7 | Lyric truncation | ✅ v0.16.4 | Clipped to ~60 chars + ellipsis before reaching AA |
| D8 | Lyric eyebrow `// ORA` | ✅ v0.16.4 | `// ORA · ` text prefix on `MediaMetadata.description` |
| D9 | Custom queue surface | ✅ v0.16.4 | `Coda corrente` browse-tree folder at AA root (chip-in-card variant unreachable) |
| D10 | Custom strip — cast chip | OK | AA-owned audio-route picker, intentional |
| D11 | Sleep label countdown | ✅ v0.16.4 | `Annulla · N min` ticks at minute boundaries |
| D12 | Like label localisation | ✅ stale | Already `Mi piace` / `Rimuovi mi piace` pre-audit |
| D13 | Sleep label localisation | ✅ stale | Already `Sospendi tra Nm` / `Annulla timer` pre-audit |
| D14 | Sleep preset chips | ✅ v0.16.4 | 15 / 30 / 60 + `Fine traccia` quick-set presets |
| D15 | `Fine traccia` mode | ✅ v0.16.4 | `SleepTimer.setEndOfTrack(player, onExpire)` |
| D16 | Driver-safety warning | Skip | Per audit owner — no `CarConnection` listener wired |
| D17 | Countdown extra | ✅ v0.16.4 | `EXTRA_SLEEP_REMAINING_MS` (Long) on session extras |
| D18 | `Annulla` label | ✅ v0.16.4 | Cancel chip relabeled `Annulla · N min` / `Annulla · fine traccia` |

### Highest-impact gaps

1. ~~**D14** — Sleep timer in AA has no minute picker; only the
   30-minute default fires.~~ ✅ Closed v0.16.4 — three minute
   presets (`15m / 30m / 60m`) + `Fine traccia` chip when no
   timer is armed; single live countdown chip while armed.
2. **D6** — Lyric ticker visual block in mockup is unreachable
   via Media3 metadata. The implementation is functionally
   correct (line cycling) but visually doesn't match the spec.
   **Routed to design review** — `Claude_design_review.md`
   walks through the platform constraint, the rejected hacks
   (title hijack, artwork overlay), and asks design to either
   respec the AA-side lyric for the description-line surface
   (Path A, recommended) or sign off on the trade-offs of one
   of the hacks (Path B). Partial relief in v0.16.4: the line
   is now prefixed with `// ORA · ` (D8) and clipped to ~60
   chars (D7), so AA's third metadata line at least reads like
   a lyric eyebrow rather than a generic caption.
3. ~~**D1** — Genre tiles are visual-identity-less; mockup invests
   heavily in per-genre covers and impl renders blank
   placeholders.~~ ✅ Closed v0.16.4 — 8 bundled `genre_*.xml`
   gradient drawables matching the mockup MHCover palettes,
   referenced via `android.resource://...` artworkUri (no LAN
   backend dependency, no FileProvider gymnastics).
4. ~~**D11/D17** — Live remaining-minutes UI is impossible without
   either a per-second `setCustomLayout` thrash (rejected) or a
   new session-extras + AA-card binding scheme.~~ ✅ Closed
   v0.16.4 — `SleepTimer.remainingMs` ticks only at minute
   boundaries (no per-second thrash); custom layout rebuilds
   once per minute with the new label, and `EXTRA_SLEEP_REMAINING_MS`
   + `EXTRA_SLEEP_END_OF_TRACK` are published on session extras
   for any controller that wants to render its own countdown.
5. ~~**D12/D13** — AA custom-strip labels are English in an
   otherwise Italian app.~~ Stale at audit time — labels were
   already `Mi piace` / `Sospendi tra 30m` pre-v0.16.4.
