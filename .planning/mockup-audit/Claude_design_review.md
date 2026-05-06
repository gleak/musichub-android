# Design review requests

This file collects mockup-vs-implementation drifts where the impl team
needs a design decision before the audit row can close. Each section is
self-contained and references its source audit row.

Open requests:
1. AA lyric ticker chrome (audit `08-auto-extra.md` D6) — § 1
2. NowPlaying hero cover ceiling (audit `09-core-screens.md` §4) — § 2
3. ForYou hero heart shortcut (audit `09-core-screens.md` §3) — § 3
4. Full-screen Lyrics + Video screens (audit `09-core-screens.md` §5/§6) — § 4
5. Home + Per te top-bar `History` icon (audit `09-core-screens.md` cross-cut §3) — § 5
6. EqualizerSheet 10-band lock (audit `04-player-sheets.md` § Equalizer) — § 6
7. QueueSheet drag-to-reorder (audit `04-player-sheets.md` § QueueSheet) — § 7
8. Mini-player swipe-trail mockup chrome (audit `04-player-sheets.md` § Mini-player) — § 8

---

# § 1 — AA lyric ticker chrome (audit D6)

**Submitted to:** Claude Design
**From:** MediaPlayer Android · audit `08-auto-extra.md` · v0.16.4
**Status:** Blocked on platform constraint — needs design decision
**Mockup at issue:** `mockup/mh-auto-extra.jsx` § 9.2 (lines 58–114)

---

## TL;DR

The AA now-playing lyric ticker mockup specifies a **bordered, accent-tinted
card with a `// ORA` mono eyebrow above the active lyric line, set in
24pt 700 weight Inter**, sitting under the title/subtitle on the right
panel of the AA card. We **cannot ship that visual**. Android Auto's
gearhead app owns the now-playing card chrome end-to-end; Media3 only
lets us hand it `MediaMetadata` (title / subtitle / description /
artworkUri) and the head unit paints those fields with its own design
system.

We need either (a) a revised mockup contract for the AA-side lyric
surface that fits the platform's actual API, or (b) acceptance of the
divergence as permanent platform-imposed drift.

---

## 1 · What the mockup specifies

From `mh-auto-extra.jsx` § 9.2 `AANowPlayingTicker` (relevant slice):

```jsx
{/* Lyric ticker */}
<div style={{
  marginTop: 28,
  padding: '14px 18px',
  background: 'rgba(168,224,78,0.08)',         // lime accent fill
  border: '1.5px solid rgba(168,224,78,0.25)',  // lime accent border
  borderRadius: 14,
  position: 'relative',
  overflow: 'hidden',
}}>
  <div style={{
    fontFamily: T.MONO,                        // JetBrains Mono
    fontSize: 10, fontWeight: 700,
    color: T.ACCENT, letterSpacing: 1.5,
    marginBottom: 6,
  }}>// ORA</div>
  <div style={{
    fontSize: 24, fontWeight: 700,             // body type
    letterSpacing: -0.3, lineHeight: 1.2,
    color: T.TEXT_HI,
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  }}>
    And the tide pulled me under, slow
  </div>
</div>
```

Visual elements the spec requires:

| Element                  | Spec value                                      |
|--------------------------|-------------------------------------------------|
| Container                | rounded card, 14px corners                      |
| Background               | `rgba(168, 224, 78, 0.08)` (lime tint)          |
| Border                   | `1.5px solid rgba(168, 224, 78, 0.25)`          |
| Padding                  | `14px 18px`                                     |
| Eyebrow caption          | `// ORA`, JetBrains Mono 10pt 700 lime         |
| Body line                | Inter 24pt 700 white, single-line + ellipsis    |
| Position in card         | Below title/subtitle, above progress bar        |
| Update cadence           | Once per second, follows playhead               |

---

## 2 · What ships in v0.16.4

We render the active lyric line into `MediaMetadata.description`. AA
paints `description` as **a third caption line** under the title and
subtitle in its standard now-playing card. We get:

- Default AA typography (small, single colour, no weight control)
- No card background, no border, no accent tint
- No eyebrow (we work around with a text prefix `// ORA · `)
- Truncation behaviour delegated to AA's renderer (we cap to 60 chars
  upstream so it doesn't wrap inconsistently across head units)

Code path — `app/src/main/kotlin/com/mediaplayer/android/playback/AALyricsTicker.kt:160-179`.

The functional behaviour matches the mockup's intent: the right
lyric line for the current playhead, updated once per second. The
**visual presentation** is wrong because we cannot inject a styled
view into AA's card.

---

## 3 · Why we can't paint the mockup chrome

Android Auto's UI runs in a separate process (`com.google.android.projection.gearhead`)
on the head unit. The phone's `MediaLibraryService` exposes a
`MediaSession` over IPC; gearhead consumes that session and paints
the now-playing card using its own templates, which are locked down
by Google's [Auto driver-distraction guidelines](https://developer.android.com/training/cars/media#design-guidelines).

What we can send via `MediaMetadata` (Media3 v1.4):

| Field            | Type     | Where AA paints it                              |
|------------------|----------|-------------------------------------------------|
| `title`          | String   | Large primary line                              |
| `subtitle`       | String   | Secondary line under title                      |
| `artist`         | String   | Often used as subtitle if `subtitle` is unset   |
| `description`    | String   | Tertiary caption (smallest)                     |
| `artworkUri`     | Uri      | Album cover slot (square, fixed position)       |
| `extras`         | Bundle   | Hints to AA (content-style, search support…); not painted as visible UI |

That is the **entire** vocabulary. There is no
`MediaMetadata.lyricBlock`, no `MediaMetadata.accentColor`, no
`MediaMetadata.customCard`, no `MediaSession.setNowPlayingChrome(view)`
API. Custom-layout buttons (`CommandButton`) exist, but they're tap
targets at the bottom of the card, not text frames inside it.

We've already pushed Media3's primitives as far as they go — see
v0.16.4 closure of D7 (length clip), D8 (`// ORA · ` eyebrow as a
text prefix on `description`), D11/D14/D15/D17/D18 (sleep timer
custom-layout chips with live countdown). The lyric *card chrome*
is the one mockup element we cannot reach with these tools.

---

## 4 · Hacks we considered and rejected

### 4.1 — Lyric in the title field

```
title    = "And the tide pulled me under, slow"
subtitle = "Helena Vorr · Slow Hours"
```

Pros: large type, prominent placement, matches the mockup's body weight.

Rejected because:

- Destroys the song title (driver loses primary metadata)
- Voice queries ("what song is this?") get the lyric instead
- Lockscreen / notification / Bluetooth car HUD all show the lyric as
  the song name — every other surface that consumes the session
  metadata gets garbled
- Resume-from-cold-connect chip would say "And the tide…" instead of
  the actual track name

### 4.2 — Lyric overlaid on the cover artwork

Generate a 512×512 PNG per second: cover image with the current
lyric line rendered as text over a lime-tinted gradient at the
bottom. Set `MediaMetadata.artworkUri` to a `content://` URI for
the freshly generated bitmap.

Pros: gives us full visual control — we'd own the type, the colour,
the eyebrow.

Rejected because:

- Driver loses the album cover entirely (the one design element AA
  itself prioritises for at-a-glance recognition)
- Per-second bitmap regeneration thrashes AA's image fetch
  pipeline; gearhead caches and rate-limits artwork URIs (varies
  per head unit firmware) so updates would be jittery / dropped
- 60 PNG writes per minute to internal storage during every car
  session — measurable battery and I/O cost
- Some head units pre-fetch the artwork at track start and ignore
  subsequent updates — the lyric would freeze on the first line
- Accessibility: screen-readers and TalkBack treat artwork as
  non-textual — lyric would be invisible to assistive tech

### 4.3 — Replace AA with our own car-mode UI

Build a fullscreen Activity for car use, bypassing AA. Out of scope
— we'd lose Google's certification, voice integration, and the
distraction-mitigation guarantees that come with Auto.

---

## 5 · What we ask of design

We need a decision on one of two paths.

### Path A — Accept the divergence; redesign the AA-side lyric

The mockup's lyric card is aspirational. Acknowledge that the AA
surface will be a small caption line under the title/subtitle, and
respec it accordingly. Concretely we'd need a new mockup variant of
§ 9.2 that:

- Drops the bordered card primitive
- Drops the accent border + tint background
- Drops the 24pt 700 body weight (AA renders `description` at ~14sp)
- Either (i) keeps the `// ORA · ` text prefix as the only "eyebrow"
  signal, or (ii) drops the eyebrow entirely
- Specifies a max line length (we currently clip at 60 chars; design
  may want a different number)
- Optionally specifies a placeholder when the song has no lyrics
  (current behaviour: the description stays whatever the queue's
  MediaItem originally had — usually empty)

This is the pragmatic ask. It captures what we *can* ship and stops
the audit from carrying a permanently-open red item.

### Path B — Commit to one of the rejected hacks, with the trade-offs

If the lyric chrome is truly load-bearing for the brand identity in
car mode, design + product would need to sign off on one of:

- **B.1** — Title hijack (§4.1): accept that song title is unreadable
  in AA while lyrics are present, and that downstream surfaces
  (lockscreen, BT HUD) inherit the same brokenness. Estimated cost:
  3–4 days to refactor every metadata consumer to disambiguate
  "is this real title or lyric ride-along?".
- **B.2** — Artwork overlay (§4.2): accept the loss of the album
  cover in car mode and the head-unit-dependent jitter. Estimated
  cost: 2–3 days for the bitmap pipeline + FileProvider, plus
  ongoing per-head-unit QA cost.

Neither is recommended. Both materially degrade other parts of the
car experience.

### Path C — Wait

Google evolves the Auto API regularly. A future Media3 release may
introduce a richer now-playing primitive (Apple Music's car UI
already does richer chrome; Google may follow). Design could choose
to leave the mockup as-is, ship the v0.16.4 description-line
implementation, and re-evaluate once a new API surfaces. We'd file
a feature request on the AOSP issue tracker referencing this
mockup.

---

## 6 · Recommendation

**Path A** — respec the AA lyric for the description-line surface.

Reasoning:
- Path B trades a recognisable car HUD for a nicer lyric — bad
  driver-glance trade-off, against AA's own design principles.
- Path C leaves an open red item in the audit indefinitely with no
  forecast on Google delivering a richer API (Auto API has been
  stable on this surface for 5+ years).
- The functional behaviour (right line, right time, eyebrow text
  prefix, ellipsis) already matches the mockup's *intent*. The
  remaining gap is purely stylistic chrome the platform doesn't
  expose.

If design agrees, the deliverable would be a small additional
mockup file (`mh-auto-extra-states.jsx` or a revision of § 9.2)
showing the AA card with the lyric as a third caption line + the
`// ORA · ` text prefix, so the audit can mark D6 ✅ closed and
the artefact stays the source of truth for the AA experience.

---

## 7 · Reference

| Source                                              | Notes                                              |
|-----------------------------------------------------|----------------------------------------------------|
| `mockup/mh-auto-extra.jsx` § 9.2                    | Original mockup spec                               |
| `app/.../playback/AALyricsTicker.kt`                | Current implementation                             |
| `app/.../playback/MediaPlaybackService.kt:262-264`  | Where the ticker is installed against the player   |
| `.planning/mockup-audit/08-auto-extra.md` D6        | Audit row (open)                                   |
| Media3 `MediaMetadata` source                       | `androidx.media3.common.MediaMetadata` — list of fields above |
| AAOS distraction guidelines                         | https://developer.android.com/training/cars/media#design-guidelines |

---

# § 2 — NowPlaying hero cover ceiling

**Submitted to:** Claude Design
**From:** MediaPlayer Android · audit `09-core-screens.md` § 4 + checklist row 1 · v0.16.5
**Status:** Plan-locked drift — needs design decision (re-spec mockup or relax cap)
**Mockup at issue:** `mockup/mh-screens.jsx:355-358` (`NowPlayingScreen` hero)

## TL;DR

Mockup hardcodes the Now Playing hero cover at **300dp × 300dp**. Code
caps at `min(0.92 × screenWidth, 360dp)` per `Shapes.kt:38-43`, which
resolves to **~358dp on a 390dp-wide phone** and **300dp on a 326dp-wide
phone**. The implementation choice was made deliberately — the cover *is*
the screen on the now-playing surface — and a hard 300dp cap looks
postage-stamp-small on modern displays.

We need a design call: keep the 360dp ceiling and re-spec the mockup, or
drop the ceiling back to 300dp.

## What ships in v0.16.5

`Shapes.kt:33-43` defines the cover-size strategy:

```kotlin
// Now-playing hero is *the* screen — it should fill the available
// width comfortably. Cap so very large foldables don't stretch the
// cover past tasteful bounds.
val NowPlayingFraction = 0.92f
val NowPlayingMax = 360.dp
```

Used at `NowPlayingSheet.kt:292-294` to size the cover.

| Device width | Resolved cover |
|--------------|----------------|
| 326 dp (compact) | 300 dp (`0.92 × 326 = 300`) |
| 360 dp (standard) | 331 dp |
| 390 dp (Pixel 9) | 358 dp |
| 412 dp (large) | 360 dp (capped) |

The 300dp mockup matches *only* the smallest compact phones.

## Why we deviated

- The Now Playing surface is intentionally cover-led — title, transport
  and overflow all fit comfortably below a wider hero on modern phones.
- A hard 300dp cap on a 390dp screen leaves a 90dp horizontal letterbox
  per side that looks empty. Existing audit checklist item §10
  flagged this as "current code uses 300dp cover" — the entry was
  written when the spec did match, before the cap moved.
- The shared-element transition from MiniPlayer to Now Playing
  animates between 56dp and the resolved hero size. A 300dp endpoint
  on a 390dp screen would feel cramped at the destination.

## What we ask of design

Pick one:

- **A.** Keep the 360dp cap. Re-spec `mockup/mh-screens.jsx:355-358` to
  reference `min(92vw, 360px)` or split into per-frame sizes
  (`360dp`, `331dp`, `300dp`). Audit row closes.
- **B.** Drop to 300dp. Update `Shapes.kt:38-43` to
  `NowPlayingMax = 300.dp` (or remove the fraction and use a fixed dp).
  Accept the letterbox on ≥360dp devices. Audit row closes.
- **C.** Hybrid — fixed 300dp fraction that grows only on tablets /
  foldables (e.g. ≥600dp). Concrete numbers needed.

Recommendation: **A** — the implementation choice favours mobile-first
glanceability. The mockup was drawn against a 360dp reference frame and
the absolute number is a snapshot of that frame, not a contract.

## Reference

| Source                                              | Notes                                              |
|-----------------------------------------------------|----------------------------------------------------|
| `mockup/mh-screens.jsx:355-358`                     | 300dp cover spec                                   |
| `app/.../ui/theme/Shapes.kt:33-43`                  | Current implementation policy                      |
| `app/.../ui/player/NowPlayingSheet.kt:292-294`      | Where the cover is sized                           |
| `09-core-screens.md` § 4 / checklist row 1          | Audit row (drift)                                  |

---

# § 3 — ForYou hero heart shortcut

**Submitted to:** Claude Design
**From:** MediaPlayer Android · audit `09-core-screens.md` § 3 · v0.16.5
**Status:** Blocked on data model — needs design decision on the heart's target
**Mockup at issue:** `mockup/mh-foryou.jsx:36-67` (`RotationHero`)

## TL;DR

Mockup's "In rotazione" hero card has **two shortcuts**: a primary
`RIPRODUCI` pill and a heart icon to its right. Code only renders the
RIPRODUCI button (`ForYouScreen.kt:200-256`). Adding the heart is
trivial; we need design to confirm what the heart toggles, because
the data model doesn't have an obvious target.

## Possible heart targets

| Option | Toggles | Implementable? | Trade-offs |
|--------|---------|----------------|------------|
| A | Like the **playlist itself** (favourite-playlist concept) | ❌ Not in data model — would need a new `playlist_likes` table + API + sync | Biggest scope; mirrors Spotify's "heart playlist" exactly |
| B | Like the **rotation top track** (the most-played song that drove the playlist generation) | ✅ Possible if we expose the top track id from the playlist endpoint | Discoverability — user expects heart to act on the playlist, not on a single track they didn't choose |
| C | Like **all** songs currently in the rotation playlist | ✅ Bulk like via existing `/api/songs/{id}/like` per track | Surprising side effect; one tap likes 30 songs |
| D | Decorative — heart icon for visual parity, no action | ✅ Trivial | Bad UX (icon-shaped lie) |
| E | Drop the heart from the mockup | ✅ Trivial | Loses parity but unblocks audit |

## What we ask of design

Pick one. Recommendation: **A** if we want to add favourite-playlists as
a feature (logical anchor for "Cose che mi piacciono" → playlists tab),
otherwise **E** to keep the audit clean and avoid a half-implemented
heart that does something unexpected.

## Reference

| Source                                              | Notes                                              |
|-----------------------------------------------------|----------------------------------------------------|
| `mockup/mh-foryou.jsx:36-67`                        | RotationHero with heart shortcut                   |
| `app/.../ui/foryou/ForYouScreen.kt:200-256`         | Current `RotationHero` (no heart)                  |
| `app/.../data/dto/PlaylistDto.kt`                   | No `liked` field on playlist                       |
| `09-core-screens.md` § 3                            | Audit row (drift)                                  |

---

# § 4 — Full-screen Lyrics + Video screens

**Submitted to:** Claude Design
**From:** MediaPlayer Android · audit `09-core-screens.md` § 5 + § 6 + cross-cut §6 · v0.16.5
**Status:** Mockup vs product divergence — needs design decision (retire mockups or build separate screens)
**Mockups at issue:** `mockup/mh-extras.jsx` (`LyricsScreen` 7-73 + `VideoScreen` 78-204)

## TL;DR

`mh-extras.jsx` describes two **dedicated full-screen routes**:

- **LyricsScreen** — gradient backdrop, top bar `Down · TESTO · More`,
  44dp cover row + `SINCRONIZZATO` badge, line list, footer transport
  `Share · Pause · "1:32 / 4:02"`.
- **VideoScreen** — 16:9 hero, title block, follow CTA, action chips
  (`Heart · Condividi · Salva · Audio only`), `// VIDEO CORRELATI`
  related-videos list.

Code intentionally consolidated both into **inline blocks inside
NowPlayingSheet**. There is no full-screen Lyrics or Video route in the
shipping product, and no plan to build one.

## Why we consolidated

- Lyrics and video are *modes of the current track*, not separate
  routes. Toggling them inline keeps the user's playback context
  visible (queue position, transport, sleep timer state).
- A separate route doubles the surface area: every state (loading,
  empty, error, no-lyrics, video-download-in-progress, etc.) would
  need to be drawn once for the inline panel and once for the route.
- The bottom-row icon swap (library-video / music-note) the mockup
  team requested in § 9.10 already lets users switch modes from
  inside Now Playing — same tap, no navigation.

## What we ask of design

Pick one:

- **A.** Retire `mh-extras.jsx`'s LyricsScreen + VideoScreen mockups.
  Re-spec the inline-panel chrome that ships in
  `NowPlayingSheet.kt:294-307` (video) and the inline `LyricsView`
  rendered at `NowPlayingSheet.kt:620-626`. Audit row closes.
- **B.** Commit to building both as separate routes. Estimated cost:
  3-5 days for Lyrics route (most pieces exist as `LyricsView`),
  5-8 days for Video route (related-videos list + follow-artist CTA
  on a video page have no existing infra).
- **C.** Rebadge the existing mockups as "expanded inline" states
  (i.e. the Lyrics/Video panel inside Now Playing, not a route).
  Update the mockup files to remove the route chrome (down arrow,
  page top bar) and clarify they overlay the player.

Recommendation: **C** — preserves the design intent (full-bleed lyric
typography, the SINCRONIZZATO badge, the action chips) without forcing
a route that fights the consolidation already in product.

## Reference

| Source                                              | Notes                                              |
|-----------------------------------------------------|----------------------------------------------------|
| `mockup/mh-extras.jsx:7-73`                         | LyricsScreen mockup                                |
| `mockup/mh-extras.jsx:78-204`                       | VideoScreen mockup                                 |
| `app/.../ui/player/LyricsSheet.kt`                  | Current inline `LyricsView`                        |
| `app/.../ui/player/VideoPlayerSheet.kt`             | Current inline `VideoPlayerInline`                 |
| `09-core-screens.md` § 5, § 6, cross-cut §6         | Audit rows                                         |

---

# § 5 — Home + Per te top-bar `History` icon

**Submitted to:** Claude Design
**From:** MediaPlayer Android · audit `09-core-screens.md` cross-cutting drift §3 · v0.16.5
**Status:** Mockup vs product divergence — needs design decision
**Mockups at issue:** `mockup/mh-screens.jsx:35-40` + `mockup/mh-foryou.jsx:20-25`

## TL;DR

After v0.16.5 the Home and Per te top bars now show **`MHLogo` (left) +
`Settings` (right)**. Mockup specifies **`MHLogo` + `Bell · History ·
Settings`**. Project notes confirm `Bell` (notifications) is permanently
out-of-scope. The `History` icon remains divergent.

## Options

- **A.** Add a `History` icon to both top bars. Tap navigates to
  Cronologia (already exists as the "Cronologia · Riprodotti di
  recente" section on Home). Cost: ~1h to add a route + icon.
  Concern: redundant with the existing Home section header that
  already lists recents.
- **B.** Drop `History` from the mockup. Concrete reason: the
  recents section IS the history, on the same screen. A separate
  route is duplicate navigation. Audit row closes.
- **C.** Add `History` only to **Per te** (Per te has no inline
  recents list — the icon is a real shortcut there). Leave Home
  without it (its recents are inline). Audit closes with rationale.

Recommendation: **C** — gives Per te a concrete shortcut, keeps Home
clean, and matches the data shape (Home shows history inline; Per te
doesn't).

## Reference

| Source                                              | Notes                                              |
|-----------------------------------------------------|----------------------------------------------------|
| `mockup/mh-screens.jsx:35-40`                       | Home top bar with `Bell · History · Settings`     |
| `mockup/mh-foryou.jsx:20-25`                        | Per te top bar with `History · Settings`           |
| `app/.../ui/home/HomeScreen.kt:445-510`             | Current GreetingHeader (logo + settings only)      |
| `app/.../ui/foryou/ForYouScreen.kt:114-145`         | Current header (logo + settings only)              |
| `09-core-screens.md` cross-cutting drift §3         | Audit row (open)                                   |

---

# § 6 — EqualizerSheet 10-band lock (audit `04-player-sheets.md` Equalizer)

**Submitted to:** Claude Design
**From:** MediaPlayer Android · audit `04-player-sheets.md` · v0.16.7
**Status:** Blocked on platform constraint — needs design decision
**Mockup at issue:** `mockup/mh-player-sheets.jsx:101-168` § 3.2 `EqualizerSheet`

---

## TL;DR

The EqualizerSheet mockup specifies a **fixed 10-band vertical-slider grid**
at frequencies `[32, 64, 125, 250, 500, 1k, 2k, 4k, 8k, 16k] Hz` with `+12/-12 dB`
head-room. We **cannot ship a hard-coded 10-band layout** because Android's
system Equalizer API (`android.media.audiofx.Equalizer`) reports band count
and centre frequencies *per device*. Most phones expose 5 bands; some OEMs
expose 10; Pixel "Adaptive Sound" exposes 0 (the API is disabled entirely).

We need either (a) acceptance that the band count is system-driven and the
mockup's "10 bands" is illustrative, or (b) a design call to ship our own
DSP chain (BiquadFilter graph + AudioTrack write loop) so we can guarantee
10 bands across all devices.

In v0.16.7 we ship **vertical sliders bound to whatever bands the system
reports**. The visual chrome (vertical layout, lime accent thumb, dB
read-out per column, mono freq label below) matches the mockup exactly —
only the column count varies.

---

## 1 · What the mockup specifies

From `mh-player-sheets.jsx:118-150`:

```jsx
const bands = [32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000];
// 10 vertical sliders, +12 dB headroom, -12 dB floor
// Above-zero in lime, below-zero in dimmed lime
// Per-column dB read-out above the slider, mono freq label below
```

| Element                  | Spec                                                  |
|--------------------------|-------------------------------------------------------|
| Band count               | **10, fixed**                                         |
| Frequencies              | `[32, 64, 125, 250, 500, 1k, 2k, 4k, 8k, 16k] Hz`     |
| Range                    | `+12 dB` to `-12 dB`                                  |
| Slider orientation       | Vertical                                              |
| Track                    | Lime above 0 dB, dimmed lime below 0 dB               |
| Label above              | `"+5.2 dB"` mono                                      |
| Label below              | `"125 Hz"` / `"4 kHz"` mono                           |

---

## 2 · What ships in v0.16.7

`app/src/main/kotlin/com/mediaplayer/android/ui/player/EqualizerSheet.kt`
now renders a **vertical-slider grid where each column is one of the
system-reported `BandInfo` entries** (`EqualizerController.kt:81-110`,
`snapshot()` at `:186-201`). The visual chrome is the mockup's:
- Vertical orientation via `Modifier.layout {}` swap + `Modifier.rotate(-90f)`
- Per-band dB label above (lime when ≥ 0 dB, lo-text otherwise)
- Per-band freq label below (`band.freqLabel` mono)
- Lime accent thumb + 20%-alpha inactive track

What's **device-dependent**:
- The number of columns. Pixel 7: 5. Some Sony XPERIA: 10. Some OnePlus: 5.
- The centre frequencies. The 5-band devices typically expose
  `[60, 230, 910, 3600, 14k] Hz`; the 10-band devices match the mockup
  spec almost exactly.
- The dB range. Reported by `Equalizer.getBandLevelRange()` — usually
  `±15 dB`, sometimes `±12 dB`, occasionally `±9 dB`.

The frontend only needs the band count + frequencies + range to render —
the audit row is technically "covered" for any user on a 10-band device,
"approximated" on a 5-band device.

---

## 3 · Why we can't force 10 bands

Android's audio-effects API is a thin shim over the device's effects
library. `Equalizer(priority, audioSessionId)` returns whatever the
device's `effect_factory_libraries` config emits — there's no parameter
to request a specific band count. The only knob is which preset to apply
to those bands.

To ship a guaranteed 10-band EQ on all devices we'd need to:
1. Replace `android.media.audiofx.Equalizer` with our own `BiquadFilter`
   chain (10 cascaded peaking-EQ filters at the 10 mockup centre
   frequencies).
2. Tap into Media3's `AudioProcessor` SPI to inject the chain into the
   audio pipeline before output.
3. Write a fixed-point implementation for performance (Java/Kotlin double
   maths blow the per-buffer budget on low-end devices).
4. Ship a tone-control DSP-test plan to verify against
   `android.media.audiofx.Equalizer`'s reference behaviour on each ROM
   we support.

This is roughly **a 2-3 week effort** with maintenance ongoing. Worth it
only if the design team commits to "10 bands across all devices, no
exceptions."

---

## 4 · Hacks we considered and rejected

### 4.1 — Pad the UI to 10 columns regardless

Show 10 columns but only 5 are interactive on 5-band devices. Bad UX:
the disabled columns either (a) look broken, or (b) confuse users who
think they can adjust 16k Hz when they actually can't.

### 4.2 — Use 10-band only on supported devices

Detect `Equalizer.numberOfBands == 10` and render the spec layout when
it's true; fall back to 5 columns otherwise. This is what we already do
*by accident* — `s.bands.forEach { … }` produces 10 columns on 10-band
devices. Documenting this behaviour as the contract closes the audit row
without any new code.

---

## 5 · What we need from design

A decision on **one of**:

A. **Accept device-driven band count.** The current 5-or-10 column behaviour
   is the contract; mockup's 10-band figure is illustrative. Audit row
   closes.

B. **Commit to custom DSP chain.** Engineering ships the Biquad chain and
   guarantees 10 bands. ~2-3 weeks of work + ongoing DSP maintenance.

C. **Drop the EQ feature on 5-band devices.** Show "Equalizzatore non
   supportato — non abbastanza bande disponibili" message. Aggressive,
   probably not what the user expects.

We strongly prefer (A): the perceived audio difference between 5-band
and 10-band parametric EQ is small for most listeners, and the
maintenance burden of a custom chain is high.

## Reference

| Source                                                            | Notes                                  |
|-------------------------------------------------------------------|----------------------------------------|
| `mockup/mh-player-sheets.jsx:101-168`                             | EqualizerSheet 10-band spec             |
| `app/.../ui/player/EqualizerSheet.kt`                             | Current vertical-slider grid (v0.16.7) |
| `app/.../playback/EqualizerController.kt:81-110`                  | System-EQ binding                      |
| `04-player-sheets.md` § EqualizerSheet                            | Audit row (open)                       |

---

# § 7 — QueueSheet drag-to-reorder (audit `04-player-sheets.md` QueueSheet)

**Submitted to:** Claude Design
**From:** MediaPlayer Android · audit `04-player-sheets.md` · v0.16.7
**Status:** Blocked on missing Compose primitive — needs design decision
**Mockup at issue:** `mockup/mh-player-sheets.jsx:30-71` § 3.1 `QueueSheet`

---

## TL;DR

The QueueSheet mockup shows a per-row drag handle (six-dot ⋮⋮ glyph) and
implies long-press-and-drag reordering of user-queued items. **Compose
has no first-party drag-to-reorder primitive in `material3` or
`foundation`**. The community libraries we evaluated either:
- Drop frames on lists with > ~30 items (the Burnoutcrew `reorderable`
  library uses raw `pointerInput` recomposition per drag tick).
- Don't support `LazyColumn` headers / sticky bottom CTA simultaneously
  (most libs assume the whole list is reorderable).
- Force shape mismatches with our existing `SongRow` layout.

We need a design decision: ship without drag-to-reorder (current state),
adopt a community library and accept its trade-offs, or budget the
~1-week custom-impl effort to write our own reorder modifier on top of
`Modifier.pointerInput` + manual offset accounting.

---

## 1 · What the mockup specifies

From `mh-player-sheets.jsx:35-58`:

```jsx
{rows.map((r, i) => (
  <QRow key={i} song={r} draggable={r.userQueued}>
    {/* leading: drag handle (⋮⋮) only when draggable */}
    {/* tap-and-hold the handle → drag to reorder */}
    {/* drop = commits new position */}
  </QRow>
))}
```

Behaviour the mockup implies:
- Drag-to-reorder applies to **user-queued rows only**. Source-of-queue
  rows are immutable (the source's natural order is preserved).
- Reorder commits on drop; a haptic fires on lift.
- The "now playing" row never moves.
- Reordering preserves the user-queued grouping (a user-queued row can't
  be dropped into the source-ahead section).

---

## 2 · What ships in v0.16.7

`app/src/main/kotlin/com/mediaplayer/android/ui/player/QueueSheet.kt`
renders the queue with no reorder gesture. Per-row remove (`X` button)
is the only mutation. Our `PlaybackViewModel` exposes `removeFromQueue`
and `clearQueue` but no `moveQueueItem(fromIndex, toIndex)` — adding it
is straightforward (`controller.moveMediaItem(from, to)` is a one-liner)
but has no UI affordance to invoke it.

---

## 3 · Why no first-party primitive

Compose Material3 ships `LazyColumn` and `LazyListState` but no
reorder API. The `material3` team has discussed it (see
[issue 181822458](https://issuetracker.google.com/issues/181822458))
but as of `material3:1.4.x` there is no public API. Workarounds:

| Approach                                  | Trade-offs                                                             |
|-------------------------------------------|------------------------------------------------------------------------|
| `org.burnoutcrew.composereorderable`      | 3rd-party. Adds 280 KB. Works for ≤ 30 items.                          |
| `sh.calvin.reorderable:reorderable`        | 3rd-party. ~150 KB. Mature; well-tested. New dep + transitive risk.    |
| Custom `Modifier.pointerInput` + offsets   | No new deps. ~300 LoC. Need to handle scroll-while-dragging, recomposition cost. ~1 week to ship + test on slow devices. |
| Separate "Edit queue" full-screen mode     | UX deviation from mockup. Power-user feature only.                     |

---

## 4 · What we need from design

A decision on **one of**:

A. **Ship without drag-reorder** (current state). The `X` remove button
   covers most cases — users who want to reorder can remove and re-add.
   Audit row closes as "deferred indefinitely."

B. **Adopt `sh.calvin.reorderable`.** ~150 KB dep, ~1 day of integration.
   Audit row closes within a sprint.

C. **Custom reorder modifier.** ~1 week of work + ongoing maintenance.
   Best long-term but slowest to ship.

D. **"Edit queue" mode.** New full-screen surface with reorder UI. Out
   of scope for this milestone.

We recommend (B) for a v0.17.x patch — the dep is small, the API is
clean, and we can swap to (C) later if maintenance becomes a problem.

## Reference

| Source                                            | Notes                                          |
|---------------------------------------------------|------------------------------------------------|
| `mockup/mh-player-sheets.jsx:30-71`               | QueueSheet drag-handle spec                     |
| `app/.../ui/player/QueueSheet.kt`                 | Current sheet (v0.16.7) — remove only           |
| `app/.../playback/PlaybackViewModel.kt:606-640`   | `skipToQueueItem`, `removeFromQueue`, `clearQueue` |
| `04-player-sheets.md` § QueueSheet                | Audit row (open)                                |

---

# § 8 — Mini-player swipe-trail chrome (audit `04-player-sheets.md` Mini-player)

**Submitted to:** Claude Design
**From:** MediaPlayer Android · audit `04-player-sheets.md` · v0.16.7
**Status:** Soft drift — partial implementation needs review
**Mockup at issue:** `mockup/mh-player-sheets.jsx:269-315` § 3.7 `MiniPlayerSwipe`

---

## TL;DR

The mini-player swipe-to-dismiss mockup shows three visual elements:
(a) a fading trail behind the dragged card, (b) a `// GESTO · Trascina
per chiudere · Da v0.12.6` annotation overlay, and (c) a `Rilascia per
fermare` hint that fades in once the user has dragged past the threshold.
v0.16.7 ships **(a) and (c)** via the `SwipeToDismissBox.backgroundContent`
slot, but **(b) — the build-version annotation overlay — is omitted**:
ahead-of-time annotations like that don't fit a shipped app.

We need confirmation that the version-annotation overlay is
mockup-only documentation chrome (not user-visible) so we can close the
audit row without further work.

---

## 1 · What the mockup specifies

From `mh-player-sheets.jsx:280-310`:

```jsx
<MiniPlayerCard offset={dragOffset}>
  {/* leading fade trail */}
  <FadeTrail opacity={dragOffset / 200} />
  {/* annotation tag pinned top-left */}
  <Annotation>// GESTO · Trascina per chiudere · Da v0.12.6</Annotation>
  {/* hint text fades in past 25% drag */}
  {dragOffset > 0.25 && <Hint>Rilascia per fermare</Hint>}
</MiniPlayerCard>
```

The `// GESTO …` annotation tag uses the same `// SECTION` mono font
the rest of the mockup uses for **author commentary on prototype
canvases** — the kind of pin you'd see in a Figma flow showing "this is
the gesture, available since v0.12.6."

---

## 2 · What ships in v0.16.7

`app/src/main/kotlin/com/mediaplayer/android/ui/player/MiniPlayer.kt`
shipped:
- ✅ Fade trail bg (lime tint, fades in with drag intent).
- ✅ "Rilascia per fermare" hint shown when `dismissState.progress > 0.25f`.
- ✅ Brand gradient outline on the foreground card.
- ✅ Accent-filled play button (mockup chrome).
- ✅ Album line under artist when metadata available.

Not shipped:
- ❌ The `// GESTO · Trascina per chiudere · Da v0.12.6` annotation
  overlay. We're treating it as **prototype-canvas chrome** (designer's
  notes on the mockup, not user-visible UI).

---

## 3 · What we need from design

Confirmation that the `// GESTO · Trascina per chiudere · Da v0.12.6`
annotation is mockup-only — i.e. it shouldn't be painted in the shipped
app. If design intends it to be visible to end-users (an in-app
"new feature" pin?), we'll need:
- A trigger (first-launch on v0.12.6+? Once-per-session? Always?).
- A dismiss path.
- A version-bump strategy (the "Da v0.12.6" string would have to update
  with the codebase).

If yes-it's-canvas-chrome, the audit row closes as covered.

## Reference

| Source                                            | Notes                                  |
|---------------------------------------------------|----------------------------------------|
| `mockup/mh-player-sheets.jsx:269-315`             | MiniPlayerSwipe spec                    |
| `app/.../ui/player/MiniPlayer.kt`                 | Current mini-player (v0.16.7)          |
| `04-player-sheets.md` § Mini-player swipe         | Audit row (open)                        |
