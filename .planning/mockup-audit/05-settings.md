# Mockup audit — Settings sub-pages

Mockup source: `mockup/mh-settings.jsx` (defines `SettingsSubScreen`, `CrossfadeScreen`, `DownloadOfflineScreen`, `ThemeScreen`, `DislikedScreen`).

App version at audit: **v0.13.1**.

Implementation files audited:
- `app/src/main/kotlin/com/mediaplayer/android/ui/profile/settings/SettingsSubScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/profile/settings/CrossfadeScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/profile/settings/DownloadOfflineScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/profile/settings/ThemeScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/profile/settings/DislikedScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/profile/ProfileScreen.kt` (parent / hub)
- (Eventi in coda — diagnostic surface not in `mh-settings.jsx`; mockup lives in `mockup/mh-update.jsx:154-…` `QueuedEventsScreen`. ProfileScreen.kt:266-269 renders only an info-row, no dedicated screen exists.)

---

## 1. SettingsSubScreen — shared chrome

Mockup (`mh-settings.jsx:5-19`) and Android scaffold (`SettingsSubScreen.kt:39-80`).

### [LAYOUT]
- **Mockup**: header is a single Row with back chevron + a two-line title block — eyebrow on top (`// IMPOSTAZIONI`, mono, lime, 10sp letter-spaced 1.5) and bold title 22sp underneath (`mh-settings.jsx:11-13`). Outer padding `60px 16px 20px` (status-bar push + side gutters).
- **Android**: header is a single Row (`SettingsSubScreen.kt:51-70`) with `IconButton(ArrowBack)` + plain `Text(title, style = headlineSmall)`. **No eyebrow above the title.** Padding `horizontal=4dp, vertical=4dp` — no top status-bar offset, sits flush against the chrome edge.
- **Mockup** wraps content in a scrollable region (`overflowY: 'auto'`) and supports an optional `footer` slot pinned beneath a 1px divider (`mh-settings.jsx:15-16`). **Android** does not support a footer slot — content is a `ColumnScope` block with `verticalArrangement = spacedBy(20.dp)`.
- **Mockup** background gradient runs `T.BG_TOP → T.BG_BOTTOM` over 320px (`mh-settings.jsx:7`); Android uses `MHGradient.screenBg()` over the full size — visually similar but the falloff point differs.

### [COPY]
- Mockup eyebrow string: `"// IMPOSTAZIONI"` (`mh-settings.jsx:25, 72, 128`). DislikedScreen uses `"// CONSIGLI"` (`mh-settings.jsx:168`). **Android emits no eyebrow at all** at the screen-header level. (Card-level eyebrows do exist via `SettingsCard(eyebrow=…)`, see below.)
- Mockup back-button has no text label; Android sets `contentDescription = "Indietro"` (`SettingsSubScreen.kt:60`) — fine.

### [BEHAVIOR]
- Mockup back button is decorative (no handler, `mh-settings.jsx:9`); Android wires `onBack` callback — implementation correctly extends mockup intent.
- Mockup divider `1px rgba(255,255,255,0.06)` between content and optional footer never appears in Android.

### Gap summary
- Missing **eyebrow above title** in shared chrome — every Android sub-screen lacks the `// IMPOSTAZIONI` / `// CONSIGLI` lime mono kicker the mockup defines as the screen identity.
- Missing **footer slot** API (no current call site needs it, but the contract differs).

---

## 2. CrossfadeScreen

Mockup `mh-settings.jsx:22-58`; Android `CrossfadeScreen.kt`.

### [LAYOUT]
- **Mockup** — single card, `T.CARD` 14px radius, padding `24px 20px` (`mh-settings.jsx:31`). Contains:
  1. Top row: `"Durata"` mono uppercase label on the left (`mh-settings.jsx:33`) and the **value displayed at 36sp ExtraBold lime** in mono on the right with `s` suffix glued (`{v}s`, `mh-settings.jsx:34`).
  2. Custom slider track with 7 numeric ticks `0,2,4,6,8,10,12s` rendered as text labels under the rail (`mh-settings.jsx:42-44`).
- A **second card** below (`mh-settings.jsx:48-54`) hosts a `"Audizione"` toggle row.

- **Android** — uses two stacked `SettingsCard`s with eyebrows `"Durata transizione"` and `"Come funziona"` (`CrossfadeScreen.kt:50, 98`). Card 1 contents:
  - Centered Row with **value at 56sp ExtraBold lime** + ` sec` separated by a space using mono caption style (`CrossfadeScreen.kt:60-72`).
  - Material3 `Slider` (no custom ticks; `steps=11` adds dots but no numeric labels).
  - Row beneath slider with literal `"Off"` and `"12 sec"` corner labels (`CrossfadeScreen.kt:93-94`).

### [COPY]
- Mockup descriptor (top of screen, before card): `"Sovrappone le tracce in transizione. Una dissolvenza più lunga è ideale per ambient e mix DJ; spegnila per album che chiedono silenzio fra una traccia e l'altra."` (`mh-settings.jsx:28`).
- Android equivalent (in second card, eyebrow `"Come funziona"`): `"Crossfade sovrappone la fine del brano corrente con l'inizio del successivo per la durata scelta. Lascia a 0 per cambi netti."` (`CrossfadeScreen.kt:100-101`).
- Mockup card eyebrow: `"Durata"` (`mh-settings.jsx:33`). Android card eyebrow: `"Durata transizione"` (`CrossfadeScreen.kt:50`).
- Mockup unit: `"{v}s"` (no space, e.g. `6s`). Android unit: `" sec"` with a space (e.g. `6 sec`).

### [STATE]
- **Mockup**: local `useState(6)` only — no persistence (`mh-settings.jsx:23`).
- **Android**: reads `PlayerSettings.crossfadeSeconds` and persists on `onValueChangeFinished` (`CrossfadeScreen.kt:39-46, 78-80`) — implementation correctly extends.

### [BEHAVIOR]
- **Audizione toggle**: Mockup ships a hard-coded `Toggle on={true}` row labelled `"Audizione"` with subtitle `"Riproduce un'anteprima ad ogni cambio valore"` (`mh-settings.jsx:48-54`). **Not implemented in Android** — there is no audition / preview toggle, no auto-preview on slider change.
- **Numeric ticks under the slider**: mockup's 0/2/4/6/8/10/12 mono labels (`mh-settings.jsx:42-44`) are absent — Android only shows boundary labels `Off` / `12 sec`.

### Gap summary
- Missing **`Audizione` audition toggle** card and the underlying preview-on-change wiring.
- Missing **mono numeric tick labels** beneath the slider rail.
- Lead-in descriptive paragraph is moved into a second card with eyebrow `"Come funziona"` and rewritten (less colourful copy than mockup's "ambient e mix DJ" line).
- Card eyebrow rename `"Durata"` → `"Durata transizione"` differs from mockup.
- Unit format `s` (no space) vs `" sec"` (with space) — affects also ProfileScreen detail text `"$crossfadeSec sec"` (`ProfileScreen.kt:217`).

---

## 3. DownloadOfflineScreen

Mockup `mh-settings.jsx:69-105`; Android `DownloadOfflineScreen.kt`.

### [LAYOUT]
- **Mockup** has 4 distinct blocks in one scroll:
  1. Storage gauge card (`mh-settings.jsx:75-86`) — eyebrow `"Spazio usato"`, headline `"1.8GB / 8GB"`, 6px progress bar, sub-line `"284 brani · 12 album · 6 playlist"`.
  2. Two `Row` cards — **Solo Wi-Fi** + **Download automatico** toggles (`mh-settings.jsx:89-90`), as **separate cards each in their own pill** (no shared container).
  3. Eyebrow `"// GESTIONE"` (`mh-settings.jsx:93`) followed by 3 chevron rows (`Riscarica da origine`, `Svuota cache locale`, `Forza rigenerazione Daily Mix`).
  4. Full-width destructive button `"Cancella tutti i download"` (`mh-settings.jsx:99-101`) — pill-shaped, red border + red translucent fill + red text.

- **Android** has 3 stacked `SettingsCard`s:
  1. Eyebrow `"Spazio occupato"` (`DownloadOfflineScreen.kt:72`) — large lime number + `MB di X GB` / `GB di X GB` suffix; 8dp progress bar; **no song/album/playlist counts sub-line.**
  2. Eyebrow `"Opzioni"` — both toggles inside one card with a divider (`DownloadOfflineScreen.kt:110-124`).
  3. Eyebrow `"Manutenzione"` — single `Cancella tutti i download` row using red text on the standard card surface (no border, no pill, no translucent fill).

### [COPY]
| Item | Mockup | Android |
|---|---|---|
| Storage card eyebrow | `"Spazio usato"` (`mh-settings.jsx:77`) | `"Spazio occupato"` (`DownloadOfflineScreen.kt:72`) |
| Cap | hardcoded `8GB` | `1 GB` (CACHE_CAP_BYTES = 1 GiB, `DownloadOfflineScreen.kt:40`) |
| Sub-line under bar | `"284 brani · 12 album · 6 playlist"` (`mh-settings.jsx:84`) | absent |
| Wi-Fi toggle subtitle | `"Non scaricare con dati mobili"` (`mh-settings.jsx:89`) | `"Scarica solo quando sei connesso a una rete Wi-Fi"` (`DownloadOfflineScreen.kt:113`) |
| Auto-download toggle subtitle | `"Scarica le novità delle playlist sincronizzate appena online. Disattivato per default da v0.12.6 — ti chiediamo conferma prima di occupare spazio."` (`mh-settings.jsx:90`) | `"Scarica automaticamente ogni brano che ascolti. Disattivato per impostazione predefinita."` (`DownloadOfflineScreen.kt:120`) |
| Maintenance section eyebrow | `"// GESTIONE"` (`mh-settings.jsx:93`) | `"Manutenzione"` (`DownloadOfflineScreen.kt:125`) |
| Wipe-everything CTA | `"Cancella tutti i download"` (red pill button) | `"Cancella tutti i download"` (red text row) |

### [STATE]
- **Mockup**: hardcoded `usedMB=1842, capMB=8000` (`mh-settings.jsx:70`); toggles wired only to local state.
- **Android**: real `PlayerCache.cacheSpace` read on IO; toggles bound to `PlayerSettings.downloadWifiOnly` / `downloadAuto`; `<0.1 GB` falls back to `MB` display (`DownloadOfflineScreen.kt:79-80`).

### [BEHAVIOR]
- **Missing `// GESTIONE` triplet rows** entirely:
  - `"Riscarica da origine"` — "Forza un nuovo fetch dalla sorgente per i brani con audio difettoso" — **NOT IMPLEMENTED**. Per memory `project_redownload_paths`, the code paths exist (`redownloadCurrent` / `refreshLocalDownload`) but are surfaced elsewhere, not as a generic settings row.
  - `"Svuota cache locale"` — "Mantiene i brani scaricati ma cancella i file temporanei" — **NOT IMPLEMENTED** (Android only offers "Cancella tutti").
  - `"Forza rigenerazione Daily Mix"` — `"Ricalcola il Daily Mix di domani al prossimo aggiornamento"` — implemented in `ProfileScreen.kt:225-244` instead, on the parent profile screen, **not inside DownloadOfflineScreen**.
- **Missing destructive button visual treatment** — mockup specifies pill button with red border + red translucent background + lime-style fontWeight 600 (`mh-settings.jsx:99-101`); Android renders just red text on a default card row (`DownloadOfflineScreen.kt:127-130`).
- **Missing storage breakdown sub-line** (`284 brani · 12 album · 6 playlist`).

### Gap summary
- 3 chevron management rows missing (`Riscarica da origine`, `Svuota cache locale`, `Forza rigenerazione Daily Mix` — last one lives elsewhere).
- Storage card lacks the `n brani · n album · n playlist` breakdown.
- Eyebrow rename `Spazio usato` → `Spazio occupato`; `// GESTIONE` → `Manutenzione`.
- Wi-Fi and Auto-download subtitles diverge (Wi-Fi description and the auto-download v0.12.6 reasoning line are softer in code).
- Destructive CTA visual treatment not matched (red pill button vs red text row).
- Cache cap differs (8 GB display value in mockup vs 1 GiB real cap in `PlayerCache`) — likely a mockup error, but worth noting.

---

## 4. ThemeScreen

Mockup `mh-settings.jsx:120-153`; Android `ThemeScreen.kt`.

### [LAYOUT]
- **Mockup**: 3 large clickable cards in a vertical grid (`gap: 12`), each 16px padded with a 56×56 colour preview tile on the left, label centred, lime check icon on the right when selected (`mh-settings.jsx:131-149`). Cards swap to a lime-tinted background `rgba(168,224,78,0.08)` + 1.5px lime inset border when selected (`mh-settings.jsx:135-136`).
- **Android**: simple list of `SettingsRadioRow`s in one `SettingsCard` with eyebrow `"Aspetto"` (`ThemeScreen.kt:27-36`). Selected indication is a tiny 20dp lime circle on the right with a 8dp dark dot — no preview tile, no lime card highlight.

### [COPY]
- Mockup leading paragraph: `"L'app rispetta il tema di sistema per default. Scegli "Chiaro" o "Scuro" per fissarlo."` (`mh-settings.jsx:130`) — printed before the option grid.
- Android secondary paragraph (after options, separate card with eyebrow `"Nota"`): `"MusicHub è progettato per il tema scuro — gradienti, copertine generative e accento lime sono pensati su sfondi profondi. Il tema chiaro è sperimentale."` (`ThemeScreen.kt:38-41`) — different message and placement.
- Option labels: both have `Chiaro`, `Scuro`, `Sistema`. Order differs: mockup `light, dark, system` (`mh-settings.jsx:122-126`); Android `dark, light, system` (`ThemeScreen.kt:21-25`). Mockup default selection `'dark'` (`mh-settings.jsx:121`); Android defaults to `'dark'` from `PlayerSettings.theme` (`ThemeScreen.kt:20`) — values agree.

### [STATE]
- **Mockup**: local `useState`. **Android**: `PlayerSettings.theme` flow — implementation correctly extends.

### [BEHAVIOR]
- Mockup applies the theme on tap (visual only); Android persists to `PlayerSettings.setTheme(id)` (`ThemeScreen.kt:32`) — fine.
- The mockup's `"Sistema"` preview tile uses a diagonal split gradient (`'linear-gradient(135deg,#0A0A0A 50%, #F4F2EC 50%)'`, `mh-settings.jsx:125`) — Android has no preview tiles at all.

### Gap summary
- **Missing 56×56 preview tiles** on each option (the visual hero of the mockup).
- **Missing "selected" full-card lime highlight** treatment.
- Selection indicator differs (lime check icon vs Material radio dot).
- Lead-in paragraph dropped from above the options; replaced post-options by a heavier "experimental" warning that shifts the message tone.
- Order of options inverted (`light, dark, system` vs `dark, light, system`).

---

## 5. DislikedScreen

Mockup `mh-settings.jsx:156-207`; Android `DislikedScreen.kt`.

### [LAYOUT]
- **Mockup**: pill-shaped tabs at the top (`mh-settings.jsx:170-182`) — `Brani · 3` / `Artisti · 2` rendering as lime-filled when active, white-translucent when inactive; **count rendered inline in mono after the bullet separator** (`mh-settings.jsx:179`). Below tabs, plain rows with no card wrapper, separated by `1px rgba(255,255,255,0.05)` bottom borders. Each row opacity 0.7 (visually muted because it's a "removed" list).
- **Android**: Material3 `PrimaryTabRow` inside a `SettingsCard` (`DislikedScreen.kt:81-94`). Tab labels are `"Brani (${songs.size})"` / `"Artisti (${artists.size})"` — count in parentheses, not after a `·`. Lists are wrapped in another `SettingsCard` containing a `LazyColumn` with `HorizontalDivider`s.

### [COPY]
| Item | Mockup | Android |
|---|---|---|
| Screen title | `"Non consigliati"` (`mh-settings.jsx:168`) | `"Non consigliarmi"` (`DislikedScreen.kt:80`) |
| Screen eyebrow | `"// CONSIGLI"` (`mh-settings.jsx:168`) | (none — no shared eyebrow infra) |
| Tracks tab | `"Brani · 3"` (mono count) | `"Brani (3)"` (parenthesised) |
| Artists tab | `"Artisti · 2"` | `"Artisti (2)"` |
| Restore CTA | `"Ripristina"` button per row (lime-bordered pill, `mh-settings.jsx:191`) | `IconButton(Icons.Filled.Restore)` only — text-less, contentDescription `"Ripristina nei consigli"` (`DislikedScreen.kt:200-205`) |
| Artist row subtitle | n/a — under the artist name mockup writes `"Artista"` literal (`mh-settings.jsx:199`) | absent |
| Empty (songs) | n/a in mockup | `"Nessun brano escluso"` + `"Quando segni un brano con \"Non consigliarmi\", appare qui."` (`DislikedScreen.kt:135-137`) |
| Empty (artists) | n/a in mockup | `"Nessun artista escluso"` + same pattern (`DislikedScreen.kt:158-161`) |

### [STATE]
- **Mockup**: hardcoded sample arrays (3 tracks, 2 artists).
- **Android**: `DislikedRepository.dislikedSongs(page=0,size=50)` + `dislikedArtists()` with offline `ReadCache` fallback; loading + error states; un-dislike actions optimistically remove + persist (`DislikedScreen.kt:55-122`).

### [BEHAVIOR]
- Mockup row visual: 44×44 generative `MHCover` thumbnail (`kind: 'grid' | 'wave' | 'duotone'`, palette per item) at 50% opacity (`mh-settings.jsx:186, 196`). Title + artist column at 70% opacity (`mh-settings.jsx:187`). Restore is a **text pill button**, not an icon button.
- Android: 40dp `SongCover` with no opacity dimming; **artists tab uses a generic `PersonOff` icon avatar** rather than a circular generative cover (`DislikedScreen.kt:218-230`).
- **Title naming divergence** — mockup uses `Non consigliati` (the noun form / list); Android uses `Non consigliarmi` (verb form / setting). The parent `ProfileScreen.kt:246` row is also labelled `Non consigliarmi`. Worth confirming canonical naming.

### Gap summary
- **Tab styling** — mockup pill-tabs vs Material `PrimaryTabRow`.
- **Restore action** — mockup uses a text button `"Ripristina"`; Android uses a bare icon (less discoverable, no Italian glyph cue).
- **Visual dimming** of removed items missing (50% / 70% opacity).
- **Artist row** missing the `"Artista"` subtitle line and circular generative cover; falls back to a `PersonOff` chip.
- **Title** differs: `Non consigliati` (mockup) vs `Non consigliarmi` (code).
- **Eyebrow** `// CONSIGLI` cannot be rendered (no chrome support).
- Android adds proper empty-state and error handling — improvement over mockup.

---

## 6. Eventi in coda — diagnostic surface

`mh-settings.jsx` does **not** include this screen. The mockup lives in `mockup/mh-update.jsx:154-…` (`QueuedEventsScreen`, eyebrow `"// PROFILO · DIAGNOSTICA"`, title `"Eventi in coda"`).

Android has **no dedicated screen** for this. The only surface is a non-clickable `SettingsRow` on `ProfileScreen.kt:266-269`:

```kotlin
SettingsRow(
    label = "Eventi in coda",
    detail = if (pending == 0) "Tutto sincronizzato" else "$pending in attesa",
)
```

### [BEHAVIOR]
- Row reflects `EventQueue.pending` live (`ProfileScreen.kt:260`) — read-only counter.
- No drill-in to view event types, retry, or wipe queue.

### Gap summary
- Detailed audit belongs to `mh-update.jsx` — flagged here only because the prompt asked to confirm. **Action item**: when `mh-update.jsx` audit runs, capture the `QueuedEventsScreen` ↔ ProfileScreen detail-row gap (mockup expects an entire sub-screen with per-event-type breakdown; code stops at a single info row).

---

## 7. Cross-cutting observations

- **Eyebrow over screen title** is a defining MusicHub idiom in `mh-settings.jsx` (`// IMPOSTAZIONI`, `// CONSIGLI`, `// PROFILO · DIAGNOSTICA`) — Android's `SettingsSubScreen` chrome doesn't surface it. Adding an `eyebrow: String?` parameter to `SettingsSubScreen` and threading the existing strings would close this for all 4 sub-screens at once.
- **Mockup uses card-level eyebrows sparingly** — only DownloadOffline has an inline `// GESTIONE` between blocks. Android uses `SettingsCard(eyebrow=…)` heavily on every card (`Durata transizione`, `Come funziona`, `Spazio occupato`, `Opzioni`, `Manutenzione`, `Aspetto`, `Nota`). The Android pattern is more rigid; consider whether the explanatory cards (`Come funziona`, `Nota`) should drop the eyebrow to keep visual hierarchy single-tier per the mockup.
- **Mono unit suffix style**: mockup glues short forms (`6s`, `1.8GB`); Android prefers spaced long forms (`6 sec`, `1.8 GB`). One direction or the other should be chosen and applied consistently — `ProfileScreen.kt:217` already uses `sec`, so flipping to `s` would also touch that surface.
- **Destructive CTA visual** treatment is the most user-visible regression — mockup expects a bordered pill with translucent red fill (`mh-settings.jsx:99-101`); current code is a plain row. Pattern recurs anywhere a destructive action exists (Disconnetti row in `ProfileScreen.kt:289-294` uses red text on a card too). Consider promoting a `DestructiveButton` composable to align both.
- **Toggle component**: mockup uses a custom 44×26 lime/dark thumb design (`Toggle`, `mh-settings.jsx:60-66`); Android uses Material3 `Switch` with `MHColors.Lime` track. Visually close enough but not pixel-identical — out of scope to swap unless a generic MH toggle is desired across the app.
