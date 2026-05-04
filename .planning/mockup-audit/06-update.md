# Mockup audit — Update channel + Changelog + Queued events

Mockup: `mockup/mh-update.jsx`
Scope: AppUpdateBanner (3 states), ChangelogSheet, Eventi-in-coda diagnostic.

---

## 1. AppUpdateBanner — Home embed

Mockup: `mockup/mh-update.jsx:6-89`
Code:    `app/src/main/kotlin/com/mediaplayer/android/ui/common/AppUpdateBanner.kt:41-104`

### State coverage

[STATE] **Mockup defines THREE banner states** (`mh-update.jsx:6` `state = 'available' | 'progress' | 'failed'`):
1. **available** — lime gradient card with "Installa" CTA + X dismiss (`mh-update.jsx:8-21`)
2. **progress** — neutral card, spinner, percent, byte counter, progress bar (`mh-update.jsx:22-39`)
3. **failed** — red-tinted card with "Riprova" CTA, no dismiss (`mh-update.jsx:40-52`)

[STATE] **Code only renders the `available` state** (`AppUpdateBanner.kt:41-104`). There is no `progress` and no `failed` banner. Download progress in code is delegated to the system DownloadManager notification (`AppUpdateInstaller.kt:49` `setNotificationVisibility(VISIBILITY_VISIBLE_NOTIFY_COMPLETED)`); failure surfaces only as the `error` text inside `AppUpdateDialog` (`AppUpdateDialog.kt:50-56`) — not in the banner. **The banner has no in-app awareness of the download being in flight or having failed.**

### "Available" state — visual diff

[LAYOUT] Mockup uses **lime gradient** background `linear-gradient(135deg, rgba(168,224,78,0.16) 0%, rgba(168,224,78,0.04) 100%)` (`mh-update.jsx:10`); code uses **flat tint** `MHColors.Lime.copy(alpha = 0.10f)` (`AppUpdateBanner.kt:54`). No gradient.

[LAYOUT] Mockup outer `borderRadius: 14`, padding `12px 14px` (`mh-update.jsx:10`); code `RoundedCornerShape(12.dp)`, padding `12.dp horizontal/12.dp vertical` (`AppUpdateBanner.kt:53,57`). 14 vs 12 corner radius minor.

[LAYOUT] Mockup icon container is `borderRadius: 10` (rounded-square), `36×36`, solid `T.ACCENT` (`mh-update.jsx:11`). Code uses `CircleShape`, `36.dp`, solid `MHColors.Lime` (`AppUpdateBanner.kt:62-64`). **Shape mismatch — squircle vs circle.**

[LAYOUT] Mockup has explicit X icon-button on the trailing edge for dismiss (`mh-update.jsx:19`). Code has IconButton + X (`AppUpdateBanner.kt:95-102`). Match — but the X tint differs: mockup `T.TEXT_LO`, code `MHColors.TextLo`. OK.

[LAYOUT] Mockup has the **"Installa" pill button** between text and X (`mh-update.jsx:18`: `padding 8px 14px`, `background T.ACCENT`, `borderRadius 999`, `color #0A0A0A`, `fontWeight 700`, `fontSize 12`). Code has **no pill button** — the entire row is `clickable(onClick = onInstall)` (`AppUpdateBanner.kt:56`). Tap-anywhere behavior, no explicit CTA chip.

[LAYOUT] Mockup margin: `0 16px 14px` (centered, 14 below). Code: `padding(horizontal = 16.dp, vertical = 8.dp)` (`AppUpdateBanner.kt:52`). 14 vs 8 below — minor.

### "Available" state — copy diff

[COPY] Eyebrow:
- Mockup `// AGGIORNAMENTO` (`mh-update.jsx:15`)
- Code `// AGGIORNAMENTO DISPONIBILE` (`AppUpdateBanner.kt:77`)
**Code is more verbose; mockup is shorter.**

[COPY] Title row:
- Mockup `v0.13.1 → v0.14.0` with the new version highlighted in `T.ACCENT` + `T.MONO` (`mh-update.jsx:16`)
- Code `v$fromVersion → v$toVersion` plain titleMedium, **no accent split, no mono on the new version** (`AppUpdateBanner.kt:80-88`)

[COPY] Subtitle:
- Mockup has **none** — relies on the visible "Installa" pill as the affordance.
- Code shows `"Tocca per installare"` body smalltext (`AppUpdateBanner.kt:90`). **Code adds an instruction line because there is no pill.**

### "Progress" state (missing in code)

[STATE/LAYOUT] Spec (`mh-update.jsx:22-39`):
- Card `T.CARD` neutral background with subtle border `rgba(255,255,255,0.06)`.
- 36×36 squircle icon slot with **lime-tinted bg `rgba(168,224,78,0.14)`** holding a Spinner (`mh-update.jsx:91-98` — circle stroke `rgba(168,224,78,0.25)` + 90° arc in `T.ACCENT`, strokeWidth 2.4).
- Eyebrow `// SCARICAMENTO APK` (mono, lime).
- Title `v0.14.0 · 4.2 / 12.8 MB` (where the byte counter is mono, `T.TEXT_LO`).
- Trailing label: `33%` (mono, weight 800, lime, fontSize 16).
- Bottom: 4px-tall progress bar, track `rgba(255,255,255,0.08)`, fill `T.ACCENT`, radius 2.

[BEHAVIOR] No equivalent in code. Suggests the banner should subscribe to a `DownloadProgress` flow exposing `bytesDownloaded`, `totalBytes`, `percent`. Currently the user must drop into the system shade.

### "Failed" state (missing in code)

[STATE/LAYOUT] Spec (`mh-update.jsx:40-52`):
- Background `rgba(225,72,72,0.08)`, border `rgba(225,72,72,0.3)` — **rose tint**.
- Icon slot `rgba(225,72,72,0.18)` containing a triangle-warning glyph stroked `#FF7A7A`, strokeWidth 1.8.
- Eyebrow `// AGGIORNAMENTO FALLITO` (mono, color `#FF7A7A`).
- Title `Scaricamento interrotto`.
- Trailing pill `Riprova` — `padding 8px 14px`, neutral bg `rgba(255,255,255,0.08)`, color `T.TEXT_HI`, fontWeight 600.
- **No X dismiss** in failed state — only retry.

[BEHAVIOR] Code has no failed-banner concept. After a download error, `AppUpdateDialog` shows an inline error string (`AppUpdateDialog.kt:50-56`) but the banner itself never reflects failure — the user could miss the failure entirely if they were in the dialog and dismissed it.

---

## 2. ChangelogSheet — bottom sheet "What's new"

Mockup: `mockup/mh-update.jsx:101-152`
Code:    `app/src/main/kotlin/com/mediaplayer/android/ui/changelog/ChangelogSheet.kt:28-110`
Data:    `app/src/main/kotlin/com/mediaplayer/android/data/AppVersion.kt:23-213`

### Layout

[LAYOUT] **Sheet height** — mockup `height: 90%` of viewport, custom rounded top `20px 20px 0 0` (`mh-update.jsx:110`). Code uses M3 `ModalBottomSheet` with `skipPartiallyExpanded = true` — defaults to ~90% on phone, but corner radius and grab-handle styling are M3 defaults. Roughly equivalent in shape.

[LAYOUT] **Hero block** — mockup has a distinct hero card (`mh-update.jsx:116-126`):
- Top-down lime gradient backdrop `linear-gradient(180deg, rgba(168,224,78,0.16) 0%, rgba(24,24,24,0) 100%)` over the title section.
- Eyebrow `// NOVITÀ · v0.14.0` (mono, lime, letterSpacing 1.5).
- **Two-line large headline** (fontSize 28, weight 800, letterSpacing -0.7) — copy: `Quattro cose\nche ti piaceranno.` (a marketing-style line, not the version title).
- Mono subline showing version diff: `v0.13.1 → v0.14.0`, with `→ v0.14.0` portion in lime.

Code (`ChangelogSheet.kt:53-67`):
- No hero gradient, no eyebrow.
- Title `"What's new"` in `headlineSmall` (English).
- Subtitle `"Version ${AppVersion.VERSION}"` (label medium).
- **No "from→to" version diff visible.**

[LAYOUT] **Entry rows** — mockup (`mh-update.jsx:128-137`):
- Numbered list `01`, `02`, ... in mono lime, weight 700, fontSize 13, fixed 18px column.
- Each entry has a 16/700 title and a 13/lo body, separated by 1px hairline `rgba(255,255,255,0.06)`.
- Padding `14px 0` per row.

Code (`ChangelogSheet.kt:78-109`):
- Per-version block: `v0.13.1` (in primary color) + entry title side-by-side as titleMedium.
- Highlights are bullet rows `•` + body text — Material bullets, not numbered.
- No row dividers.
- Spacing: `Arrangement.spacedBy(20.dp)` between version blocks (`ChangelogSheet.kt:51`).

[LAYOUT] **Pager dots** — mockup has bottom-of-list pager `mh-update.jsx:139-143` showing 1-active + 2-inactive dots (suggesting **multiple paginated versions**). Code is a single LazyColumn — all versions shown by scroll, no paging.

[LAYOUT] **Footer CTA** — mockup has a sticky lime pill button `Continua` full-width, padding `14px`, weight 700 (`mh-update.jsx:146-148`). Code has **no footer / no CTA** — user dismisses by drag.

### Copy / data shape

[COPY] **Mockup shows ONE version's entries flat-listed (no version label per entry)** — entries are just numbered novelty cards (`mh-update.jsx:102-107`):
- `Audio nitido come mai prima` / `Nuovo decoder Opus a 96kHz; ...`
- `Mini-player a portata di pollice` / `Trascina di lato per fermare la riproduzione e nasconderlo.`
- `Errori di playback più chiari` / `Niente più toast: ora un dialogo ti dice cosa fare (riprova / riscarica).`
- `Per te, ma davvero per te` / `Daily Mix usa anche i tuoi "non consigliarmi" per affinare le scelte.`

[COPY] **Code shows ALL historical entries cumulatively** (`AppVersion.kt:23-211`, currently 16 entries from 0.10.20 → 0.13.1). Entries have full version + bulleted highlights. No "novelties of this release only" filter.

[COPY] **Hero is generic in code** ("What's new" + version), **marketing/curated in mockup** ("Quattro cose che ti piaceranno." — number + emotional teaser).

[COPY] Mockup mixes English with Italian — but the displayed strings are all Italian. Code header is **English** (`"What's new"`, `"Version ..."`). Inconsistent with the rest of the app which is Italian.

[COPY] Mockup version diff: `v0.13.1 →` muted, `v0.14.0` lime. Code: only current version shown, no diff. The "from-version" semantic exists in `ChangelogPreferences` (`ChangelogSheet.kt:34` `markSeen(AppVersion.VERSION)`) but is never displayed.

### Behavior

[BEHAVIOR] Mockup CTA is a **Continua** confirm button — implies modal commitment / dismiss-via-button. Code dismisses via system back / drag-down only (`ChangelogSheet.kt:38` `onDismissRequest = onDismiss`). No explicit dismiss button.

[BEHAVIOR] Mockup pager dots imply paginated reveal of features (3 pages?). Code scrolls all entries continuously. Different UX paradigm.

[BEHAVIOR] `LaunchedEffect(Unit)` in code marks the version as seen on open (`ChangelogSheet.kt:33-35`) — fine. Mockup doesn't show that mechanism but it's implied.

---

## 3. "Eventi in coda" — diagnostic screen

Mockup: `mockup/mh-update.jsx:155-215` (`QueuedEventsScreen`, exported as `MHUpdate.QueuedEventsScreen`)
Code:    `app/src/main/kotlin/com/mediaplayer/android/ui/profile/ProfileScreen.kt:266-269`

### Coverage

[STATE] **Mockup defines a full sub-screen** — eyebrow `// PROFILO · DIAGNOSTICA`, title `Eventi in coda`, with:
- Hero card showing the **total queued count** in 56px mono lime numerals + label `eventi pronti` + caption "Le azioni offline ... si svuotano da sole quando torni online. Nessun dato perso." (`mh-update.jsx:166-176`).
- Detail eyebrow `// DETTAGLIO`.
- List of typed events with icon, title, subtitle, count pill (`mh-update.jsx:179-188`):
  - `Mi piace · Citrine` / `Mira Holt` / ×1
  - `Segui artista · Iso Tide` / ×1
  - `Riproduzioni` / `Tre tracce ascoltate offline` / ×3
  - `Non consigliarmi · Hollow Ave` / `Lana Verdier` / ×1
  - `Aggiunta a playlist · Slow Hours` / `+2 brani` / ×2
- Footer info row with spinner + `Prossimo flush automatico tra 00:42` (lime countdown) (`mh-update.jsx:190-193`).
- Five icon kinds: `heart`, `user`, `play`, `thumb`, `plus` (`EventIcon` `mh-update.jsx:199-215`), all in lime-tinted 36px squircle slots.

[STATE] **Code has NO sub-screen** — only a single SettingsRow on the profile screen showing `label = "Eventi in coda"` with a one-line `detail` of `"Tutto sincronizzato"` or `"$pending in attesa"` (`ProfileScreen.kt:266-269`). No tap target, no breakdown by event type, no countdown, no copy explaining what queued events mean.

[BEHAVIOR] **Major gap.** Mockup spec implies the row should navigate into a diagnostic sub-screen with grouped event details. Code currently gives the user only an aggregate count — no way to see what is queued, no way to trigger a manual flush, no countdown to next sync attempt.

[BEHAVIOR] Mockup uses the **same Spinner element** as the banner-progress state (`mh-update.jsx:91-98`), tying together "in-flight async work" iconography. Code has neither.

---

## Summary of gaps (priority ordered)

| # | Gap | Severity |
|---|---|---|
| 1 | AppUpdateBanner has no `progress` state — user can't see in-app whether the APK download is in flight or how far along | high |
| 2 | AppUpdateBanner has no `failed` state with a Riprova action — failures only show inside dialog body text | high |
| 3 | `Eventi in coda` is a single line in profile, mockup specs a full diagnostic sub-screen with breakdown + flush countdown | high |
| 4 | ChangelogSheet has no hero/eyebrow/version-diff/curated headline — looks generic vs marketing-grade mockup | medium |
| 5 | ChangelogSheet shows ALL historical entries; mockup shows curated novelties of the latest release only | medium |
| 6 | ChangelogSheet has no `Continua` confirm CTA at the bottom | medium |
| 7 | ChangelogSheet header is in English (`"What's new"`) inconsistent with Italian app | medium |
| 8 | AppUpdateBanner uses circle icon, mockup uses squircle (10px radius) | low |
| 9 | AppUpdateBanner has no explicit pill `Installa` button — relies on whole-row tap | low |
| 10 | AppUpdateBanner has flat tint instead of 135° gradient | low |
| 11 | AppUpdateBanner eyebrow is `// AGGIORNAMENTO DISPONIBILE` vs mockup's shorter `// AGGIORNAMENTO` | low |
| 12 | AppUpdateBanner shows only current version in title; mockup colors+monos the new version | low |

## Files referenced

Mockup:
- `C:\Users\Antonio\Documents\Claude\Projects\MediaPlayer\android\mockup\mh-update.jsx`

Android sources:
- `C:\Users\Antonio\Documents\Claude\Projects\MediaPlayer\android\app\src\main\kotlin\com\mediaplayer\android\ui\common\AppUpdateBanner.kt`
- `C:\Users\Antonio\Documents\Claude\Projects\MediaPlayer\android\app\src\main\kotlin\com\mediaplayer\android\update\AppUpdateDialog.kt`
- `C:\Users\Antonio\Documents\Claude\Projects\MediaPlayer\android\app\src\main\kotlin\com\mediaplayer\android\update\AppUpdateChecker.kt`
- `C:\Users\Antonio\Documents\Claude\Projects\MediaPlayer\android\app\src\main\kotlin\com\mediaplayer\android\update\AppUpdateRepository.kt`
- `C:\Users\Antonio\Documents\Claude\Projects\MediaPlayer\android\app\src\main\kotlin\com\mediaplayer\android\update\AppUpdateInstaller.kt`
- `C:\Users\Antonio\Documents\Claude\Projects\MediaPlayer\android\app\src\main\kotlin\com\mediaplayer\android\ui\changelog\ChangelogSheet.kt`
- `C:\Users\Antonio\Documents\Claude\Projects\MediaPlayer\android\app\src\main\kotlin\com\mediaplayer\android\data\AppVersion.kt`
- `C:\Users\Antonio\Documents\Claude\Projects\MediaPlayer\android\app\src\main\kotlin\com\mediaplayer\android\ui\profile\ProfileScreen.kt` (lines 264-270)
