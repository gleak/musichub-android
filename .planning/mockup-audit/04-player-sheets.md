# Player sheets/dialogs — mockup vs impl

Source mockup: `mockup/mh-player-sheets.jsx`
Reference Android sources:
- `app/src/main/kotlin/com/mediaplayer/android/ui/player/QueueSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/player/EqualizerSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/player/MiniPlayer.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/player/NowPlayingSheet.kt` (sleep-timer dropdown lives here)
- `app/src/main/kotlin/com/mediaplayer/android/MainActivity.kt` (PlaybackErrorDialog)
- `app/src/main/kotlin/com/mediaplayer/android/playback/PlaybackViewModel.kt` (PlaybackErrorInfo)
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/AddToPlaylistSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/AddSongsToPlaylistSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/TrackActionSheet.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/common/FlagWrongAction.kt`

## Coverage

| Mockup surface | Implemented? | Android entry point |
|---|---|---|
| QueueSheet | partial | `QueueSheet.kt` |
| EqualizerSheet | partial | `EqualizerSheet.kt` |
| Sleep timer popover (full bottom sheet) | NO — only DropdownMenu exists | `NowPlayingSheet.kt:768` `SleepTimerMenu` |
| Mini-player swipe-to-close | yes | `MiniPlayer.kt:81` `SwipeToDismissBox` |
| Playback-error dialog | partial | `MainActivity.kt:469` |
| Report-wrong-song dialog | partial | `AddToPlaylistSheet.kt:252` (lives inside another sheet) |
| AddToPlaylistSheet | partial | `AddToPlaylistSheet.kt` |
| AddSongsToPlaylistSheet | partial | `AddSongsToPlaylistSheet.kt` |
| TrackActionSheet | partial | `TrackActionSheet.kt` |

## Per-sheet findings

### QueueSheet
Mockup `mockup/mh-player-sheets.jsx:30-71`. Impl `QueueSheet.kt`.

Layout / structure:
- Mockup uses a custom `Sheet` shell with eyebrow `// CODA`, big title `In riproduzione`, three header action chips (`Shuffle`, `Repeat`, `More`) + drag handle. Impl uses Material3 `ModalBottomSheet` with single `Text("Up next")` (`QueueSheet.kt:65`) — no eyebrow, no shuffle/repeat/more buttons in the sheet header at all.
- Mockup section labels are mono-uppercase: `// IN CODA · UTENTE · 2` (count inline) and `// SUCCESSIVI · DA "SLOW HOURS"` with right-aligned `+12` overflow indicator (`mh-player-sheets.jsx:56,59-60`). Impl section labels are `"Now playing"`, `"Next in queue"`, `"Next up"` (`QueueSheet.kt:80,95,110`) — English, plain typography, no source-of-queue label, no item count, no overflow chip.
- Mockup row layout: 44 dp cover with playing-bars overlay on `now`, accent-coloured title for the now-playing row, drag handle on each draggable row. Impl reuses `SongRow` (full kebab row) + an extra `Close` IconButton for remove (`QueueSheet.kt:174-181`); no drag handle, no playing-bars overlay (highlight is just a `secondaryContainer` background tint at 0.4f, `QueueSheet.kt:163`).
- Mockup has a sticky bottom CTA "Cancella coda" (`mh-player-sheets.jsx:67`). Impl has NO clear-queue button anywhere in the sheet.

Copy:
- Italian vs English mismatch: mockup is Italian ("In riproduzione", "IN CODA · UTENTE", "SUCCESSIVI", "Cancella coda"); impl is English ("Up next", "Now playing", "Next in queue", "Next up"). The wider app already mixes Italian (NowPlayingSheet uses `IN RIPRODUZIONE DA` at `NowPlayingSheet.kt:257`).
- Empty state mockup: not shown. Impl: `EmptyState("Queue is empty", "Tap a song to start playback.")` (`QueueSheet.kt:71-75`).

Behavior:
- Mockup implies drag-reorder (drag handle in `QRow`). Impl has no reorder gesture, only remove via X button.
- Mockup header `Shuffle` / `Repeat` toggles are missing from impl's queue sheet (those controls live exclusively in NowPlayingSheet).

### EqualizerSheet
Mockup `mh-player-sheets.jsx:101-168`. Impl `EqualizerSheet.kt`.

Layout / structure:
- Mockup eyebrow `// AUDIO`, title `Equalizzatore`, header action is a green `ATTIVO` pill (mono accent badge). Impl has plain `Text("Equalizer")` + a Material `Switch` for on/off (`EqualizerSheet.kt:71-75`). No eyebrow, no accent pill.
- Mockup preset is a single tappable card showing `Preset` label + `Personalizzato` value + chevron — opens a follow-up picker. Impl is a horizontal `FilterChip` row over all `EqPreset.entries` (`EqualizerSheet.kt:121-137`). Different interaction model and visual.
- Mockup sliders are vertical (10-band column with `+12..-12` head room), bands `[32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000]` Hz, custom track + above-zero in accent / below-zero in dimmed accent, knob has glow shadow, dB readouts below each column. Impl is a Material `Slider` per band laid out horizontally with `freqLabel`/`dbLabel` text columns (`EqualizerSheet.kt:86-113`). Bands come from `band.freqLabel` (system equalizer) — usually 5 bands on most devices, not 10.
- Mockup includes a `// SESSIONE AUDIO` info card with `session_id` and `output` (`mh-player-sheets.jsx:158-164`). Impl shows nothing about audio session or output device.
- Mockup unsupported-state: not shown. Impl falls back to `Text("Equalizer not supported on this device")` (`EqualizerSheet.kt:57`).

Copy:
- "Equalizer" (impl) vs "Equalizzatore" (mockup).
- Preset label "Custom" (chip in impl) vs "Personalizzato" (mockup).
- No `ATTIVO` badge text, no "// AUDIO" eyebrow.

### Sleep-timer popover
Mockup `mh-player-sheets.jsx:171-206` `SleepTimerSheet`. Impl: there is NO bottom sheet — only `SleepTimerMenu` `DropdownMenu` at `NowPlayingSheet.kt:768-788`.

Layout / structure:
- Mockup is a 0.62-height bottom sheet with eyebrow `// TIMER`, title `Timer di sospensione`, an active-state hero card (large mono countdown `27:14`, sub `L'audio si fermerà alle 23:42`, `Annulla` pill), then a 3×2 grid of preset chips for `5, 10, 15, 30, 45, 60` minutes, then a full-width outlined `Fine traccia` button.
- Impl is a tiny `DropdownMenu` anchored to the bedtime icon in NowPlayingSheet's top bar, listing only `15, 30, 60` minutes (`NowPlayingSheet.kt:781`) plus a `"Cancel timer"` row when active (`NowPlayingSheet.kt:777`). No countdown, no end-of-track option, no full sheet.

Copy:
- Mockup options: 5/10/15/30/45/60. Impl: 15/30/60 only — missing 5, 10, 45.
- "Cancel timer" (impl) vs "Annulla" (mockup).
- "Fine traccia" (end-of-track) — completely missing from impl.
- "L'audio si fermerà alle 23:42" / countdown / scheduled-time hint — completely missing from impl.

Behavior:
- Impl just sets a wall-clock timer via `viewModel.setSleepTimer(minutes)` (`NowPlayingSheet.kt:284`); `SleepTimer.kt:18` only supports minutes. There is no end-of-current-track support.
- The sleep-timer flag `viewModel.sleepTimerActive` is exposed (`PlaybackViewModel.kt:104`) but only as a boolean — no remaining-time stream, so the mockup's `27:14` countdown isn't backable today.

### Mini-player swipe
Mockup `mh-player-sheets.jsx:269-315` `MiniPlayerSwipe`. Impl `MiniPlayer.kt:81-102`.

Layout / structure:
- Mockup illustrates the gesture: trailing fade trail behind the dragged card, an `// GESTO · Trascina per chiudere · Da v0.12.6` annotation, and a `Rilascia per fermare` hint at the bottom. Impl just wraps the bar in `SwipeToDismissBox` with `backgroundContent = {}` (`MiniPlayer.kt:96-98`) — no trail, no instructional copy, no release hint.
- Mockup mini-player visual: gradient outline frame, 10 px corner radius, accent-filled circular Pause button (`mh-player-sheets.jsx:294-305`). Impl uses `MaterialTheme.colorScheme.surfaceContainerHigh` background (`MiniPlayer.kt:107`), `CoverShapes.Card` clip, no gradient outline, plain `IconButton` for play/pause.
- Mockup shows progress as a thin underline inside the card (`mh-player-sheets.jsx:300-302`). Impl renders `LinearProgressIndicator` of height 2 dp at the bottom edge (`MiniPlayer.kt:161-173`) — close enough.
- Mockup has cover + title/artist/album-line (`Helena Vorr · Slow Hours`); impl shows only `current.artist` (`MiniPlayer.kt:131-137`), no album.
- Mockup does NOT show a like button on the mini-player. Impl includes a heart `IconButton` (`MiniPlayer.kt:139-149`).

Behavior:
- Both directions trigger dismissal in impl: `StartToEnd || EndToStart → viewModel.dismissPlayback()` (`MiniPlayer.kt:84-90`). Mockup arrow points right; copy says "scorri il mini-player verso destra (o sinistra)" — both directions OK in mockup too.

### Playback-error dialog
Mockup `mh-player-sheets.jsx:209-235` `PlaybackErrorDialog`. Impl `MainActivity.kt:469-523`.

Layout / structure:
- Mockup is a centered modal card on a 60% scrim, `1A1A1A` bg with red-tinted border, top-left red triangle icon + eyebrow `// ERRORE PLAYBACK`, large title (`Codec non supportato`), description, mono `CODE` badge with `player/codec-unsupported · opus@48k`, and three-button footer `Chiudi / Riprova / Riscarica` — last button is the green accent CTA (`mh-player-sheets.jsx:228-231`).
- Impl is a stock Material `AlertDialog` with `CloudOff` icon (`MainActivity.kt:478`), title `"Impossibile riprodurre"`, body shows `info.songTitle` + `info.reason` + optional `info.recoveryHint` + `"Codice: ${info.errorCodeName}"`, and a single `OK` `TextButton` (`MainActivity.kt:517-521`).

Copy:
- Mockup title: `Codec non supportato` (specific). Impl: `Impossibile riprodurre` (generic) — the specific reason is in the body.
- Mockup eyebrow `// ERRORE PLAYBACK` — missing from impl.
- Mockup code formatted as `CODE | player/codec-unsupported · opus@48k` (mono pill). Impl: `Codice: ERROR_CODE_*` plain text (`MainActivity.kt:511`).
- Recovery copy in impl is descriptive paragraph (`PlaybackViewModel.kt:303,314`); mockup leaves recovery to the buttons.

Behavior / state:
- Mockup CTAs: `Chiudi`, `Riprova`, `Riscarica` (close / retry / re-download). Impl: only `OK`. No Retry button, no Re-download button — the auto-fix path is implicit (`PlaybackViewModel.kt:298-307`) but the user can't trigger it from the dialog.
- Mockup uses red triangle warning icon. Impl uses `CloudOff` (network icon) regardless of error class.

### Report-wrong-song dialog
Mockup `mh-player-sheets.jsx:238-266` `ReportSongDialog`. Impl: `AddToPlaylistSheet.kt:252-275` (the dialog) + `FlagWrongAction.kt` (the action).

Layout / structure:
- Mockup is a centered standalone card with eyebrow `// SEGNALA · DEFINITIVO` (red mono), title `Brano sbagliato?`, embedded track preview (cover + title + artist), descriptive paragraph, two pill buttons `Annulla` / red `Segnala`. Impl is stock Material `AlertDialog` (`AddToPlaylistSheet.kt:253`) with title `"Report wrong song?"`, plain text body, two `TextButton`s `Cancel` / `Report` — no eyebrow, no track preview, no red CTA styling.

Copy (mockup → impl):
- `Brano sbagliato?` → `Report wrong song?`.
- `// SEGNALA · DEFINITIVO` red eyebrow — missing.
- Mockup body: `Verrà rimosso da ricerca, playlist, brani che ti piacciono e cronologia su tutti i tuoi dispositivi. Il match non verrà ri-scaricato in futuro.` Impl body: `"…will be removed from your playlists, likes, and history, and the file will be deleted from the server. This is permanent."` (`AddToPlaylistSheet.kt:258-260`). Different scope: mockup mentions "ricerca" (search) and emphasises "su tutti i tuoi dispositivi" + "Il match non verrà ri-scaricato"; impl emphasises server-side file deletion.
- Buttons: `Annulla / Segnala` → `Cancel / Report`.
- Track preview row (cover + title + artist inside the dialog) — missing in impl; user only sees the `songTitle` quoted in the body.

Behavior:
- Both fire-and-confirm. Mockup's red `Segnala` would map to a destructive button style; impl uses default Material `TextButton` colors for both (no danger emphasis).

### AddToPlaylistSheet
Mockup `mh-player-sheets.jsx:318-358`. Impl `AddToPlaylistSheet.kt`.

Layout / structure:
- Mockup eyebrow `// AGGIUNGI A`, title `Le mie playlist`, search field row (`Cerca playlist…`), playlist rows with 48 dp `MHCover`, song count (`142 brani`), trailing radio/check circle (selected = filled accent dot), and a sticky bottom outlined-dashed CTA `+ Crea nuova playlist` in accent.
- Impl uses Material `ModalBottomSheet` with `Text("Add to playlist")` + a sub `Text(songTitle)` (`AddToPlaylistSheet.kt:128-139`); NO search field; NO sticky bottom CTA — instead the "New playlist" row is inline at the top of the list (`AddToPlaylistSheet.kt:198`).
- Impl injects a stack of additional action rows BEFORE the playlist list when callbacks are provided: `Play next`, `Add to queue`, `Download`, `Non consigliarmi questo brano`, `Non consigliarmi questo artista`, `Report wrong song` (`AddToPlaylistSheet.kt:143-196`). Mockup's `AddToPlaylistSheet` is purely a playlist picker — those extra actions live in `TrackActionSheet`. The two surfaces overlap heavily in impl.
- Impl playlist row uses a generic `QueueMusic` icon on a `surfaceVariant` square (`AddToPlaylistSheet.kt:317-322`); mockup uses each playlist's distinctive `MHCover` artwork.
- Impl row trailing is just a clickable area; mockup trailing is a radio-style selection circle.

Copy:
- Mockup Italian (`Cerca playlist…`, `142 brani`, `Crea nuova playlist`); impl mixes English (`"Add to playlist"`, `"New playlist"`, `"1 song"/"X songs"` `AddToPlaylistSheet.kt:336`) and Italian (`"Non consigliarmi questo brano"` `:169`, `"Non consigliarmi questo artista"` `:177`) — inconsistent.
- Impl's `"Report wrong song"` label (`:185`) is English; the confirm dialog is also English.

Behavior:
- Mockup search filters playlists locally; impl has no playlist-search field at all.
- Mockup "+ Create" is a sticky footer; impl has it inline above the list with a `+` icon row.

### AddSongsToPlaylistSheet
Mockup `mh-player-sheets.jsx:361-404`. Impl `AddSongsToPlaylistSheet.kt`.

Layout / structure:
- Mockup eyebrow `// AGGIUNGI A · SLOW HOURS` (target playlist baked into eyebrow), title `Aggiungi brani`, search field with X-clear (showing query `mira`), rows are: leading checkbox, 44 dp cover, title/artist + duration. Bottom sticky CTA `Aggiungi N brani` — accent-pill (`mh-player-sheets.jsx:397-401`). Multi-select pattern.
- Impl uses Material `ModalBottomSheet` with title `"Add songs"` (`AddSongsToPlaylistSheet.kt:99-103`), `OutlinedTextField` search with `placeholder = "Search songs"` and trailing `Close` icon when query is non-empty (`:104-120`). Rows use a 40 dp cover (real `AsyncImage`), title/artist (no duration shown), and a per-row `IconButton` that toggles between `Add` and `Check` icons (`:224-231`). No multi-select; each tap fires `playlistRepository.addSong` immediately. No sticky `Aggiungi N brani` CTA.

Copy:
- Mockup `Aggiungi brani` → impl `"Add songs"`.
- Mockup `Cerca` placeholder → impl `"Search songs"`.
- Mockup row trailing CTA aggregate `Aggiungi N brani` — missing entirely from impl.
- Impl shows song duration nowhere; mockup row has `dur` (`mh-player-sheets.jsx:390`).

Behavior:
- Mockup is a batched commit (`Aggiungi N brani` button); impl is one-at-a-time auto-commit on tap. Different mental model.
- Mockup eyebrow shows the target playlist name; impl never shows it inside the sheet.
- Empty state for impl: `"Your library is empty"` / `"No songs match \"$query\""` (`:142`); mockup has none.

### TrackActionSheet
Mockup `mh-player-sheets.jsx:407-465`. Impl `TrackActionSheet.kt`.

Layout / structure:
- Mockup eyebrow `// AZIONI`, title is `Citrine · Mira Holt` (single styled line). Impl renders a full track header row: cover + title + artist (`TrackActionSheet.kt:95-124`); no eyebrow, no `// AZIONI` label.
- Mockup row layout: just `Icon + label`, 16 px gap, no cover/border (icon tinted by category). Impl row layout matches that pattern (`TrackActionSheet.kt:159-185`) — single icon + label, divided into the dim/highlight tints.

Copy / item list — mockup order vs impl order:

| # | Mockup label | Impl label | Notes |
|---|---|---|---|
| 1 | Aggiungi alla coda | Aggiungi alla coda (`:128`) | match (Italian) |
| 2 | Riproduci dopo | Riproduci dopo (`:127`) | match — impl renders BEFORE Add-to-queue, mockup is REVERSE order |
| 3 | Aggiungi a playlist | Aggiungi a playlist (`:129`) | match |
| 4 | Mi piace (accent heart) | Aggiungi ai preferiti / Rimuovi dai preferiti (`:133`) | mockup uses single `Mi piace` always; impl toggles label based on `isLiked` |
| 5 | Condividi | Condividi (`:144`) | match |
| 6 | Vai all'artista | Vai all'artista (`:142`) | match |
| 7 | Vai all'album | Vai all'album (`:143`) | match |
| 8 | Scarica | Scarica (`:138`) | match. Impl also has `Rimuovi download` (`:139`) for `onRemoveDownload` — not in mockup |
| 9 | Mostra testo | Mostra testo (`:140`) | match |
| 10 | Mostra video | Apri video (`:141`) | "Mostra" vs "Apri" — different verb |
| 11 | Timer di sospensione | Timer di spegnimento (`:145`) | "sospensione" vs "spegnimento" |
| div | divider | (no divider in impl) | mockup separates destructive group |
| 12 | Non consigliarmi questo brano (muted) | (NOT in TrackActionSheet) | Impl puts this in `AddToPlaylistSheet:169` instead |
| 13 | Non consigliarmi questo artista (muted) | (NOT in TrackActionSheet) | Impl puts this in `AddToPlaylistSheet:177` instead |
| 14 | Segnala brano sbagliato (danger red) | (NOT in TrackActionSheet) | Impl puts this in `AddToPlaylistSheet:185` instead |
| — | (no entry) | Rimuovi dalla playlist (red, `:147-153`) | impl-only, contextual |

Behavior:
- Impl re-uses `MaterialTheme.surfaceContainer` defaults for sheet bg; mockup uses fixed `#181818`. (Impl actually pins `containerColor = Color(0xFF161616)` at `TrackActionSheet.kt:81` — close to mockup.)
- Impl callbacks all run via `onClick = { onDismiss(); it() }` (`:127-145`) — sheet closes BEFORE the action fires. Mockup just shows the row, no behavior spec.
- The dislike + report actions are split between two surfaces in impl (TrackActionSheet does NOT host them; they only appear via the kebab → `AddToPlaylistSheet` path). Mockup keeps them in TrackActionSheet under a divider.

## Missing in impl

- Sleep-timer bottom sheet entirely — only a 3-item dropdown (`15/30/60`) exists. No 5/10/45 presets, no "Fine traccia" (end-of-track) option, no countdown view, no scheduled-end-time hint, no `Annulla` pill.
- QueueSheet `Cancella coda` (clear queue) sticky CTA.
- QueueSheet drag-to-reorder gesture and per-row drag handles.
- QueueSheet header chips (`Shuffle / Repeat / More`).
- QueueSheet section labels with source name (`// SUCCESSIVI · DA "SLOW HOURS"`) and `+N` overflow badge.
- QueueSheet now-playing row playing-bars overlay on the cover.
- EqualizerSheet vertical-slider band visualization with 10 bands (impl uses horizontal Material sliders, band count is whatever the system reports).
- EqualizerSheet preset card (single tappable card → picker) — impl uses chips.
- EqualizerSheet `// SESSIONE AUDIO` info card showing session id + active output device.
- EqualizerSheet accent `ATTIVO` pill in the header.
- Mini-player swipe trail / instructional annotation / "Rilascia per fermare" hint.
- Mini-player gradient outline frame and accent-filled play button.
- Mini-player album-line beneath artist.
- Playback-error dialog: `Riprova` (retry) and `Riscarica` (re-download) action buttons; only `OK` exists. Specific-title pattern (e.g. "Codec non supportato"). Mono `CODE` pill formatting. Red triangle icon (impl uses `CloudOff` for all errors).
- Report-wrong-song dialog: red eyebrow, embedded track-preview row, destructive-styled `Segnala` button. Italian copy.
- AddToPlaylistSheet: search field, `MHCover` artwork on rows, radio-circle selection trailing, sticky bottom outlined-dashed `+ Crea nuova playlist` CTA.
- AddSongsToPlaylistSheet: target-playlist eyebrow (`// AGGIUNGI A · SLOW HOURS`), batched multi-select with aggregate `Aggiungi N brani` CTA, song duration in row meta.
- TrackActionSheet: `// AZIONI` eyebrow, divider before destructive group, `Non consigliarmi questo brano/artista` and `Segnala brano sbagliato` rows (currently routed through AddToPlaylistSheet instead).

## Missing in mockup

- QueueSheet: empty state ("Queue is empty / Tap a song to start playback."), per-row `Close` (X) remove button, per-row kebab (`onMore` opening `AddToPlaylistSheet`).
- EqualizerSheet: equalizer-not-supported fallback text, system band-count fallback (5/10 dynamic), full preset list (`EqPreset.entries`).
- Sleep timer: scope of impl is just minutes via `SleepTimer.kt`; mockup specifies an end-of-track mode that has no backing model.
- Mini-player: like button (`Favorite` toggle) — impl has it, mockup doesn't.
- Playback-error dialog: localized recovery hints (`PlaybackViewModel.kt:303`,`:314`) and per-error reason taxonomy (`humanReason` `:326`).
- Report-wrong-song: server-file-deletion mention ("file will be deleted from the server").
- AddToPlaylistSheet: extra action rows injected when `onPlayNext / onAddToQueue / onDownload / onDislikeSong / onDislikeArtist / onFlagWrong` callbacks are provided. Loading + error states. Empty-state composable.
- AddSongsToPlaylistSheet: `AsyncImage` real-cover loading, debounced search (300 ms), already-added Check vs Add icon, empty/error/loading states.
- TrackActionSheet: full track header row (cover + title + artist), `Rimuovi download`, `Rimuovi dalla playlist` (contextual, red).
- App-update dialog, alarm-export dialog, redownload-confirm, mark-broken-confirm — all referenced from `NowPlayingSheet.kt:160-164` and `MainActivity.kt:453-457`, none have a mockup counterpart in this file.
