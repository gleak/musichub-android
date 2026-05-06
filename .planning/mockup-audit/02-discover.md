# Discover & Import — mockup vs impl

> **Implementation status — 2026-05-05 · COMPLETELY DONE (v0.13.4).**
> FindScreen + SpotifyImportScreen rewritten to MusicHub spec; backend
> async import pipeline with per-track progress + APPROX match bucket
> shipped. Every actionable item in this audit is closed. Kept as
> historical audit trail only.
>
> Android files in play:
>
> - `app/src/main/kotlin/com/mediaplayer/android/ui/find/FindScreen.kt`
> - `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/SpotifyImportScreen.kt`
> - `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/SpotifyImportViewModel.kt`
> - `app/src/main/kotlin/com/mediaplayer/android/data/MediaPlayerApi.kt` *(async + status endpoints)*
> - `app/src/main/kotlin/com/mediaplayer/android/data/dto/SpotifyImportResultDto.kt` *(adds `approx`, new `SpotifyImportJobIdDto`/`SpotifyImportJobStatusDto`)*
> - `app/src/main/res/values/strings.xml` *(70+ new keys for eyebrows, status caps, terminal headlines, stepper labels, suggestion copy)*
> - `app/src/main/kotlin/com/mediaplayer/android/MainActivity.kt` *(threads `onBack` into `FindScreen`)*
>
> Backend files in play (`../backend/src/main/java/com/mediaplayer/backend/spotify/`):
>
> - `SpotifyImportJob.java` *(new — atomic counters per phase)*
> - `SpotifyImportJobRegistry.java` *(new — in-memory, 1h TTL)*
> - `SpotifyImportJobStatus.java` *(new — wire DTO)*
> - `SpotifyImportResult.java` *(adds `approx`)*
> - `SpotifyImportService.java` *(`@Async importCsvAsync`, EXACT/APPROX/NONE match split, per-track progress)*
> - `SpotifyImportController.java` *(adds `POST /spotify/async` + `GET /spotify/jobs/{id}`)*
>
> Sync POST `/api/playlists/import/spotify` preserved for older clients;
> Android now uses the async path with 500ms→2s polling. Backend
> `mvn clean compile` BUILD SUCCESS, Android `gradlew assembleDebug`
> BUILD SUCCESSFUL.
>
> **Intentionally skipped** (no design demand): multi-playlist preview
> (Exportify produces one CSV per playlist), Lifecycle pause/resume
> diagnostic banner (pure debug overlay).

Mockup: `mockup/mh-discover.jsx`
Impl:
- `app/src/main/kotlin/com/mediaplayer/android/ui/find/FindScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/find/FindViewModel.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/SpotifyImportScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/SpotifyImportViewModel.kt`
- `app/src/main/res/values/strings.xml` (find_*)

## Coverage

- **mockup screens drawn:**
  1. `FindScreen` — single result-list state with optional `state==='error'` banner overlay; loading skeleton always renders below the result list (illustrative).
  2. `SpotifyImportScreen` — 5-step wizard with 4 explicit step branches: `instructions` (1/5), `preview` (3/5), `progress` (4/5), `done` (5/5). Step 2 (`file`) and the stepper bar are common chrome.
- **impl screens found:**
  1. `FindScreen` (`FindScreen.kt`) with states: `Idle` (with optional active-requests list), `Searching`, `Error`, `Tracking` (sub-states: SEARCHING / AWAITING_SELECTION / UNLOCKING / DOWNLOADING / IMPORTED / IMPORTED_PARTIAL / FAILED / CANCELED).
  2. `SpotifyImportScreen` (`SpotifyImportScreen.kt`) with states: `Idle`, `FetchingPlaylist`, `Confirming`, `Importing`, `Done`, `Error`.

No other Discover/Find/import surfaces exist (`HomeScreen`, `PlaylistsScreen`, `OnboardingSheet`, `MainActivity` only reference Find/Import as nav targets).

---

## Per-screen findings

### FindScreen

#### [LAYOUT]

- **Mockup top bar** is a custom hero with a back button + monospace eyebrow `// SCOPRI · YT` + bold 22px title `Trova brani` (mh-discover.jsx:15-21). **Impl** has no AppBar/title at all in `FindScreen.kt:71-78`; the screen is just `QueryBar` + `HorizontalDivider` + body. The hosting Scaffold/title is supplied by the parent (not shown here).
- **Mockup search field** is a single chip-style row inside a `T.CARD` rounded card with leading magnifier and trailing `X` clear icon, no separate Search button (mh-discover.jsx:24-30). **Impl** uses a Material `OutlinedTextField` + a separate `Button("Cerca")` (`FindScreen.kt:124-139`). No clear (X) affordance, no card framing.
- **Mockup result row** is 48×48 cover (`MHCover` shape, `radius:4`), title 14px / artist · `mono dur` · `YT` source pill, with a right-side action chip (`Aggiungi` filled accent / `In coda` ghost) (mh-discover.jsx:46-67). **Impl** `CandidateRow` uses a 100×72 thumbnail (landscape), title up to 2 lines, channelName, duration + view-count metadata (`FindScreen.kt:311-357`, `Thumbnail` 100×72 dp at line 367-369). No right-side chip — the whole row is `clickable`; selection is shown by `secondaryContainer` background, not a pill.
- **Mockup "// 4 RISULTATI · YT MATCH" section header** in mono caps (mh-discover.jsx:43). **Impl** has no result-count header on `Tracking`/`AWAITING_SELECTION`; only an `IdleBody` "In corso" header (`FindScreen.kt:155`).
- **Mockup loading skeleton** uses two grey shimmer rows below the result list (mh-discover.jsx:69-79). **Impl** uses a centered `CircularProgressIndicator` (`CenteredSpinner`) for `Searching` (`FindScreen.kt:94`) and a `LinearProgressIndicator` strip while in SEARCHING/UNLOCKING/DOWNLOADING (`FindScreen.kt:218,228`). No skeletons.
- **Mockup back button** sits in the hero; **impl** back button only appears in `StatusHeader` while `Tracking` (`FindScreen.kt:268-273`) and just calls `viewModel.reset()`.

#### [COPY]

- Title: mockup `Trova brani` + eyebrow `// SCOPRI · YT`; impl string `find_title = "Scopri nuova musica"` (strings.xml:19) — different phrasing, and not displayed in the screen body.
- Search hint: impl `find_hint = "Artista, album o brano"` (strings.xml:20). Mockup field has placeholder `Mira Holt` (defaultValue) — no neutral hint.
- Search CTA: mockup has no button; impl `find_button_search = "Cerca"` (strings.xml:21).
- Section header: mockup `// 4 RISULTATI · YT MATCH`. Impl analogue is `find_active_downloads = "In corso"` (strings.xml:32) — different concept (active downloads vs result count).
- Empty: impl `find_empty = "Cerca un video musicale su YouTube da scaricare."` (strings.xml:22). Mockup never draws an empty state.
- Error banner: mockup inline banner `Connessione non riuscita` / `Verifica la rete e riprova` + `Riprova` chip (mh-discover.jsx:32-40). Impl uses full-screen `ErrorWithRetry` with `find_error_prefix = "Ricerca non riuscita:"` + raw exception message and a generic Retry button (`FindScreen.kt:96-99`).
- Status labels (Tracking): impl has 7 (`find_status_searching` "Sto cercando su YouTube…", `awaiting` "Scegli un video qui sotto.", `downloading` "Scarico e importo…", `imported` "Importato. Buon ascolto.", `imported_partial`, `failed` "Errore.", `canceled` "Annullato."). Mockup never renders the imported/failed/canceled/partial states or any status label.
- Action chip copy: mockup has `Aggiungi` and `In coda`. Impl has no per-row action chip — there is no equivalent string.
- Source pill `YT`: mockup decorates each row (mh-discover.jsx:56). Impl has no source pill (everything is YouTube, but the row never says it).
- Back contentDescription: `find_back = "Torna a Scopri"` (strings.xml:33).

#### [STATE]

- Idle (no active requests): impl shows `CenteredMessage(find_empty)` (`FindScreen.kt:148-150`). Mockup never depicts this — the mockup's idle equivalent is the result list itself.
- Idle (with active requests): impl renders `IdleBody` with "In corso" header + `ActiveRequestRow` per request showing `"<query>"` + status text + linear progress (`FindScreen.kt:144-205`). Mockup has no concept of "ongoing background requests when not searching"; the entire active-requests list/poll model is impl-only.
- Searching: impl shows `CenteredSpinner` (full-screen). Mockup shows skeleton rows under the (existing) results.
- Error: impl shows `ErrorWithRetry` full-screen replacing the body. Mockup shows an inline 12px banner above the result list — non-blocking.
- Tracking → IMPORTED / IMPORTED_PARTIAL / FAILED / CANCELED: impl renders `StatusHeader` + the candidate list (greyed-out, not clickable) (`FindScreen.kt:236-244`). Mockup has no terminal state — there is no success/failure UI for a Find request.
- Pull-to-refresh: impl wraps the `Idle` body in `PullToRefreshBox` (`FindScreen.kt:83-92`). Mockup has no such affordance.

#### [BEHAVIOR]

- Selection model: mockup row's primary CTA is a per-row `Aggiungi`/`In coda` button — implies the whole row's tap target is the metadata, the chip handles the action. Impl makes the **entire row** clickable (`FindScreen.kt:325`) and has no chip.
- Polling: impl polls active requests + the tracked request with exponential backoff (POLL_MS=2s → MAX_POLL_MS=10s, FindViewModel.kt:206-208) and gates polling on lifecycle (`pause`/`resume`, FindViewModel.kt:68-91). Mockup is purely visual — no behavior expressed.
- After terminal status, impl auto-returns to `Idle` after `TERMINAL_LINGER_MS = 2_000L` (FindViewModel.kt:166-171). Not depicted in the mockup.
- Empty query: impl `submit()` ignores blank queries (FindViewModel.kt:98-99) and the Cerca button is `enabled = query.isNotBlank()`. Mockup has no submit button so this gate is moot.
- Clear-input (X icon) in mockup field has no impl equivalent — the OutlinedTextField has no trailing clear button.

---

### SpotifyImportScreen

#### [LAYOUT]

- **Chrome:** mockup uses the shared `SettingsSubScreen` wrapper with `eyebrow="// IMPORTAZIONE"` and `title="Importa da Spotify"` (mh-discover.jsx:88). Impl uses an inline `Row` with `IconButton` + `Text("Importa da Spotify", titleLarge)` (`SpotifyImportScreen.kt:55-71`). No eyebrow, no shared SettingsSubScreen.
- **Stepper bar:** mockup draws a 5-segment progress bar at the top of every step (mh-discover.jsx:91-96). Impl has no stepper at all — none of the 6 states render any progress segments.
- **Step number eyebrow:** every mockup step has a mono caps eyebrow `// PASSO N / 5 · …` (e.g. `// PASSO 1 / 5 · ESPORTA CSV` mh-discover.jsx:100). Impl has no step labels anywhere.
- **Instructions step (mockup `instructions`, impl `Idle`):** mockup is a numbered `<ol>` with 3 steps and a single `Apri Exportify ↗` ghost button (mh-discover.jsx:98-108). Impl uses `StepRow` rows ("1", "2", "3" + text) (`SpotifyImportScreen.kt:120-122`) **and** an extra primary `Scegli file CSV` button to launch the file picker (`SpotifyImportScreen.kt:136-141`). Mockup has no explicit "pick file" button — the flow expects step 2 (`file`) to handle that.
- **Preview step (mockup `preview`, impl `Confirming`):** mockup renders the filename `liked-songs.csv` 16/700, a mono summary `284 brani · 3 playlist rilevate`, then a list of **playlists** (each with cover, name, "n brani · owner", trailing accent check) (mh-discover.jsx:111-130). Impl renders just `"<N> brani trovati"` + an `OutlinedTextField` with label `Nome playlist` + `Annulla` / `Avvia import` buttons + a `LazyColumn` of **track previews** (title + artist) (`SpotifyImportScreen.kt:166-214`).
- **Progress step (mockup `progress`, impl `Importing`):** mockup shows a card with the current track, mono `147 / 284` counter, a 4px progress bar at 52%, and a 3-stat grid (`Trovati` / `Approx` / `Saltati`) (mh-discover.jsx:138-156). Impl shows a generic full-screen `CircularProgressIndicator` + label `"Importo la playlist…"` (`SpotifyImportScreen.kt:88, 295-308`). No counter, no current track, no stats grid.
- **Done step:** mockup shows a 64px accent-tint circle with check, headline `Importazione completata`, subtitle `3 playlist · 279 brani · 5 saltati`, a 3-stat grid (`Importati` / `Saltati` / `Errori`), and a primary CTA `Apri Slow Hours` (mh-discover.jsx:159-175). Impl shows no icon, headline `Import completato`, multi-line body with matched/queued/failed lines, then `Apri playlist` and `Torna alle playlist` (`SpotifyImportScreen.kt:240-272`). No stat grid.

#### [COPY]

- Title: matches (`Importa da Spotify`).
- Eyebrow `// IMPORTAZIONE`: missing in impl.
- IdleContent header: impl `Come esportare la playlist da Spotify:`. Mockup says (in the instructions step heading) `Esporta da Exportify`.
- Step copy: impl steps are
  - `"Tocca il pulsante qui sotto per aprire Exportify"`
  - `"Accedi con Spotify ed esporta la playlist in CSV"`
  - `"Salva il file sul telefono, poi importalo qui"`
  Mockup steps are
  - `"Apri watsonbox.github.io/exportify e accedi con Spotify."`
  - `"Per ogni playlist, premi Export per scaricare il CSV."`
  - `"Torna qui e seleziona il file dal tuo dispositivo."`
  Different phrasing and the mockup explicitly cites the URL `watsonbox.github.io/exportify` (impl opens `https://exportify.net` — different domain — `SpotifyImportScreen.kt:128`).
- Buttons (idle): mockup `Apri Exportify ↗` (with arrow). Impl `Apri Exportify` (no arrow) + extra primary `Scegli file CSV`.
- Fetching label: impl `"Leggo il file…"`. Mockup has no analogue (no separate "fetching" state — goes straight from instructions through file pick to preview).
- Confirming label: impl `"<N> brani trovati"`. Mockup says `<filename> · <N> brani · <N> playlist rilevate`.
- Confirming buttons: impl `Annulla` / `Avvia import`. Mockup primary `Importa 284 brani` (count baked in), no cancel.
- Confirming text field label `Nome playlist`: missing in mockup (mockup expects multiple playlists to be detected and picked via toggle/check, not a single rename).
- Importing label: impl `"Importo la playlist…"`. Mockup `Sto cercando i brani su YouTube…` + per-track current line.
- Done headline: impl `Import completato`. Mockup `Importazione completata`.
- Done body: impl Italian-pluralizes ("brano/brani aggiunto/aggiunti a \"<name>\"" + optional `<N> in scaricamento — saranno aggiunti quando pronti` + `<N> non trovati`, `SpotifyImportScreen.kt:254-263`). Mockup uses summary line + grid.
- Done CTAs: impl `Apri playlist` + `Torna alle playlist`. Mockup `Apri Slow Hours` (single CTA, name interpolated).
- Error retry button: impl `Try Again` (English!) (`SpotifyImportScreen.kt:289`) — outlier vs the all-Italian UI.
- Error CSV-empty message: impl `"No tracks found. Make sure this is an Exportify CSV file."` (`SpotifyImportViewModel.kt:64`) — English; not localised.
- File-read failure: impl `"Could not read file."` (`SpotifyImportViewModel.kt:58, 94`) — English.
- Default playlist name fallback: `"Imported Playlist"` (`SpotifyImportViewModel.kt:126`) — English.
- IconButton contentDescription `"Indietro"` is hard-coded (`SpotifyImportScreen.kt:63`) — not localised in strings.xml.

#### [STATE]

- **Mockup steps (5):** `instructions` → `file` → `preview` → `progress` → `done`. Step 2 (`file`) is implicit (file picker between instructions and preview) and not drawn beyond a stepper segment.
- **Impl states (6):** `Idle` (= instructions+file picker fused) → `FetchingPlaylist` (parsing locally) → `Confirming` (= preview, but track-level not playlist-level) → `Importing` (= progress, but no per-track detail) → `Done` (= done, but with different layout) + `Error`.
- Mockup has no error state at all in the import flow. Impl exposes one (`SpotifyImportUiState.Error` / `ErrorContent`).
- Mockup `preview` lists **playlists** with selection checkboxes (multi-playlist import). Impl `Confirming` lists **tracks** of a single playlist with a rename field and no selection. The product models diverge.
- Mockup `progress` exposes per-track granularity (current track + counter + Trovati/Approx/Saltati stats). Impl is opaque — just a spinner. The backend may surface this via `SpotifyImportResultDto.matched/queued/failed` but it's only available at `Done`.
- `FetchingPlaylist` (parse-CSV) state is impl-only.

#### [BEHAVIOR]

- Multi-playlist import: mockup expects the CSV to contain N playlists and lets the user pick which to import via toggle checks. Impl supports a single playlist per CSV (Exportify exports one playlist per file) and lets the user only rename it.
- Approx-match bucket: mockup shows `Approx` (#FFC857 amber) as a separate category. Impl response only carries `matched`/`queued`/`failed` (`SpotifyImportResultDto`) — no notion of "approximate match".
- Done CTA target: impl explicitly opens the created playlist (`onPlaylistCreated(playlistId)`) and offers a secondary back nav. Mockup only has primary `Apri <name>`.
- File picker: impl uses `ActivityResultContracts.GetContent()` with `*/*` MIME (`SpotifyImportScreen.kt:106-108, 137`) — no CSV filter. Mockup just shows the result.
- Pull-to-refresh / cancel during import: impl doesn't expose cancel during `Importing` — the mockup's `progress` card likewise has no cancel.

---

## Missing in impl (present in mockup, not implemented)

- **Find — section header `// 4 RISULTATI · YT MATCH`** (mono caps, result count + source).
- **Find — per-row action chip** (`Aggiungi` filled / `In coda` ghost with check) replacing whole-row tap target.
- **Find — `YT` source pill** on each candidate row.
- **Find — inline error banner** (non-blocking) instead of full-screen `ErrorWithRetry`.
- **Find — skeleton-row loading** (vs full-screen spinner).
- **Find — search field clear (X) icon** trailing the input.
- **Find — hero with `// SCOPRI · YT` eyebrow + `Trova brani` title** (mockup uses 22px bold; impl has no in-screen header).
- **Find — `MHCover` thumbnail shape** (48×48 with shape-based cover); impl uses 100×72 raw thumbnail.
- **Spotify Import — full 5-segment stepper** at top of every step.
- **Spotify Import — per-step `// PASSO N / 5 · …` mono eyebrow**.
- **Spotify Import — `SettingsSubScreen` wrapper** with eyebrow `// IMPORTAZIONE`.
- **Spotify Import — multi-playlist preview** (filename + "N brani · M playlist rilevate" summary, list of playlists with covers/owner/check toggle).
- **Spotify Import — rich progress card** (current track, "X / Y" counter, progress bar, Trovati/Approx/Saltati stats grid).
- **Spotify Import — done stat grid** (Importati/Saltati/Errori) and accent-tint check circle.
- **Spotify Import — primary CTA with count baked in** (`Importa 284 brani`).
- **Spotify Import — `Apri Exportify ↗` arrow glyph**.
- **Spotify Import — citation of `watsonbox.github.io/exportify`** in instructions (impl uses `exportify.net`).
- **Spotify Import — single-CTA done screen `Apri <playlistName>`** (impl has two buttons).
- **`Approx` match bucket** in import progress/done stats.

## Missing in mockup (present in impl, not depicted)

**Update 2026-05-05:** new state file `mockup/mh-discover-states.jsx` (mounted in `mh-canvas-app.jsx:100-120` as `find-idle-*`, `find-pull`, `find-searching`, `find-unlocking`, `find-downloading`, `find-imported`, `find-imp-partial`, `find-failed`, `find-canceled`, `find-life-pause`, `find-life-resume`, `sp-idle`, `sp-fetch`, `sp-error`, `sp-confirm`, `sp-done-plural`, `sp-done-singular`) closes most prior gaps. Remaining truly impl-only items at the bottom.

### Find — now covered by state mockups
- ~~**`Idle` state with active-requests poll list**~~ → `FindIdleEmpty` + `FindIdleActive`. Empty variant has 64dp circular search glyph, italian hero copy `"Trova nuovi brani"`, mono badge `// FINDVIEWMODEL · IDLE`. Active variant draws `// 4 IN CORSO · BACKGROUND` + `POLL · 2s` mono caption, then `ActiveRequestRow` per query (28dp lime tile icon + `"<query>"` + uppercase status label `RICERCA SU YT` / `SBLOCCO STREAM` / `DOWNLOAD · {pct}%` colour-coded amber for unlock/lime for download + 2px progress bar indeterminate or determinate + trailing X cancel) plus `// OGGI · COMPLETATE` section using `TerminalRow` (IMPORTED/IMPORTED_PARTIAL/FAILED/CANCELED).
- ~~**Pull-to-refresh on Idle list**~~ → `FindIdlePullRefresh`. 32dp dark-blur capsule with lime spinner + mono `AGGIORNO…` label, content translated 8px down to imply pull.
- ~~**Terminal-status visuals**~~ → `FindTerminalScreen kind="IMPORTED" | "IMPORTED_PARTIAL" | "FAILED" | "CANCELED"`. Per-kind colour-tinted radial gradient (lime / amber / red / grey), 88dp circular icon, italian title (`"Aggiunto alla libreria"` / `"Importato · parzialmente"` / `"Brano non trovato"` / `"Ricerca annullata"`), 3-row meta card (mono key/value pairs — Sorgente/Bitrate/Durata, Audio/Video/Motivo, Tentati/Match max/Soglia, Stato/Progresso/Pulizia), dual CTA stack (primary `"Apri brano"`/`"Riprova"` + secondary `"Trova un altro"`/`"Affina la ricerca"` etc.).
- ~~**`Searching` global state**~~ → `FindSearching`. `StatusHeader` with `"Mira Holt"` + `RICERCA SU YT…` mono status + indeterminate 2px lime strip; `// INTERROGAZIONE INDICE` section caption; 5 dimmed (opacity 0.5) skeleton rows (48dp grey square + two grey lines); footer banner `"La ricerca continua se chiudi questa schermata."` lime mono. **Mockup `mh-discover.jsx` skeleton model is superseded — pure searching view is the new contract.**
- ~~**`StatusHeader` with back + query + status + progress strip**~~ → reusable `StatusHeader` component used across `FindSearching`, `FindCandidatesSelected`. 56pt top padding, `rgba(0,0,0,0.25)` band, ellipsised `"<query>"` 15px/700, status mono caps below (lime / amber / red), 2px strip indeterminate (`mh-indet` keyframes) or determinate.
- ~~**Selection highlight on chosen candidate**~~ → `FindCandidatesSelected phase="unlocking" | "downloading"`. Selected row uses `rgba(168,224,78,0.10)` bg + lime border (or amber while unlocking), 48dp `MHCover` overlaid with dark scrim + colour-coded spinner; trailing pill swaps from disabled `Aggiungi` (40% opacity) to colour-tinted mono pill `SBLOCCO` / `64%`. Section header `// 4 RISULTATI · YT MATCH`.
- ~~**Lifecycle pause/resume of polling**~~ → `FindLifecycle paused={true|false}`. Active rows dimmed to 0.45 when paused; bottom diagnostic banner with paused glyph (amber `Polling in pausa` + mono `lifecycle/onPause · jobs interrotti`) or play glyph (lime `Polling ripreso` + `lifecycle/onResume · 3 jobs riavviati`).

### Spotify Import — now covered by state mockups
- ~~**`FetchingPlaylist` (CSV parse) state**~~ → `SpotifyImportFetching`. `// PASSO 3 / 5 · LETTURA FILE` step badge, hero `"Leggo il file…"`, card with 56dp lime-tinted file icon + corner spinner, filename `liked-songs.csv` + mono `412 KB · parsing`, indeterminate 2px lime strip, 2-col stat tiles (RIGHE/COLONNE), mono footer `spotify/csv-parse · uri-extract`.
- ~~**`Error` state**~~ → `SpotifyImportError`. Red eyebrow `// ERRORE · LETTURA FILE`, italian title `"Non riesco a leggere il file"`, red panel with bullet + `"Header mancanti: Track URI, Artist Name"` + mono stack box (`spotify/csv-parse:42` etc.), `// SUGGERIMENTI` card with 3-bullet recovery list, primary `Riprova` lime pill + secondary `Scegli un altro file` text button. **Replaces impl's English `Try Again`.**
- ~~**Playlist rename `OutlinedTextField` + `Annulla`**~~ → `SpotifyImportConfirming`. M3 outlined field with floating `Nome playlist` label (lime border), helper row `Originale: liked-songs` + mono `10 / 60` counter, summary card (44dp cover + name + mono `284 brani · da liked-songs.csv` + 3-stat row Brani/Durata/Artisti), `"Mantieni privata"` toggle row, dual CTAs `Annulla` ghost flex-1 + lime `Importa 284 brani` flex-2.
- ~~**`Torna alle playlist` secondary CTA on done**~~ + ~~**pluralized body line**~~ + ~~**`<N> in scaricamento` line**~~ → `SpotifyImportDone variant="plural"|"singular"`. Eyebrow `// PASSO 5 / 5 · COMPLETATO`, 72dp lime check circle, italian title `"Importazione completata"`, body uses `bran<o|i> aggiunt<o|i>` (`"279 brani aggiunti"` / `"1 brano aggiunto"`) + mono `"{n} in scaricamento"` lime accent, 3-stat grid Importati/Saltati/Errori, highlight playlist row with `MHCover` + mono `"{n} brani · creata ora"`, dual CTAs `Apri Slow Hours` lime + `← Torna alle playlist` ghost.
- ~~**Explicit `Scegli file CSV` primary CTA**~~ → `SpotifyImportIdle`. Eyebrow `// PASSO 2 / 5 · SCEGLI FILE`, hero `"Seleziona il CSV esportato"`, dashed lime drop-zone tile (44dp lime icon + `"Nessun file selezionato"` + mono `.csv · max 10 MB`), primary lime pill `Scegli file CSV` with leading upload icon, secondary text `↩ Torna alle istruzioni`.

### Still impl-only (behaviour, no UI counterpart needed)
- **Find — view-count metadata** ("1.2M views"). The new `FindCandidatesSelected` row matches impl's view-count line (mono, after duration), so this is now consistent — no longer impl-only.
- **Find/Spotify — lifecycle pause/resume of polling jobs** (FindViewModel.resume/pause, lines 68-91) — mockup `FindLifecycle` covers the visual state but the actual job orchestration remains code-only.

## Cross-cutting notes

- **English leakage in Spotify Import:** `Try Again` (`SpotifyImportScreen.kt:289`), `"No tracks found…"` (VM:64), `"Could not read file."` (VM:58/94), `"Failed to read file."` (VM:69), `"Import failed."` (VM:115), `"Imported Playlist"` fallback (VM:126), and IconButton CD `"Indietro"` (Screen:63) are all hard-coded — not in `strings.xml`. Mockup is fully Italian.
- **Typographic divergence:** mockup uses a strong dual-typeface system (sans `T.FONT` + monospace `T.MONO` for technical metadata, durations, counts, eyebrows); impl uses Material `typography.titleSmall/bodySmall/labelMedium` exclusively — no monospace.
- **Accent system:** mockup uses `T.ACCENT` (lime `#A8E04E`-ish from siblings) for primary CTAs and the second-tone amber `#FFC857`. Impl uses default Material 3 primary/secondary roles.
- **`MHCover` shape system:** mockup covers are programmatic shapes (`arc`/`wave`/`triangles`/`blob`/`dot`). Impl uses raw `AsyncImage` thumbnails with no fallback art.
