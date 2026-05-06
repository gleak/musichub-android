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
