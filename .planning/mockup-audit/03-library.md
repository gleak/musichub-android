# Library drilldowns — mockup vs impl

> **Implementation status — 2026-05-05 · DONE (v0.13.5 → v0.14.0 → v0.15.0).**
> Albums/Artists/Liked rebranded with eyebrows + count badges + Italian
> titles, A→Z scrubber on Artists, Liked numeric index + total duration
> + download triplet, GenreDetailScreen shipped, collaborative-playlist
> members card + Gestisci membri + per-track contributor pill, dedicated
> PlaylistShareDialog with copy + system share + revoke link,
> full-screen PlaylistShareImporter modal. Kept as historical audit
> trail only.

Mockup file: `mockup/mh-library.jsx`
Impl files audited:
- `app/src/main/kotlin/com/mediaplayer/android/ui/albums/AlbumListScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/artists/ArtistListScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/liked/LikedScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/PlaylistsScreen.kt` (library landing — mounted at `Routes.LIBRARY` per `MainActivity.kt:651`)
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/PlaylistDetailScreen.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/playlists/PlaylistShareImporter.kt`
- `app/src/main/kotlin/com/mediaplayer/android/ui/search/SearchScreen.kt` (genre handling)

## Coverage

- **Mockup screens:** `AlbumListScreen`, `ArtistListScreen`, `LikedScreen`, `GenreDetailScreen`, `CollabPlaylistOwner`, `CollabPlaylistMember`, `PlaylistShareDialog`, `PlaylistShareImporter`.
- **Impl screens:** `AlbumListScreen`, `ArtistListScreen`, `LikedScreen`, `PlaylistsScreen` (library landing — not depicted in this mockup file but reachable via the Library tab), `PlaylistDetailScreen` (one file covers owner + member + auto), `PlaylistShareImporter` (AlertDialog), system share-sheet (no in-app share dialog).
- **No corresponding impl screen:** `GenreDetailScreen` (genre is a transient filter pill on `SearchScreen`, not a stand-alone destination), `PlaylistShareDialog` (replaced by `Intent.ACTION_SEND` chooser at `PlaylistDetailScreen.kt:159-174`).

## Per-screen findings

### AlbumListScreen (`AlbumListScreen.kt`)

- **[LAYOUT]** Mockup uses a custom in-content header with a back chevron + eyebrow + title + count badge (`mh-library.jsx:18-24`). Impl uses Material3 `TopAppBar` with plain `Text("Albums")` (`AlbumListScreen.kt:154-162`). No eyebrow, no `// LIBRERIA` mono caption, no live count.
- **[COPY]** Mockup title is `"Album · {count}"` with the eyebrow `// LIBRERIA` (`mh-library.jsx:21-22`). Impl title is the English `"Albums"` (`AlbumListScreen.kt:155`). Mockup is Italian; rest of app is Italian; impl is the only English thing here.
- **[COPY]** Mockup empty/placeholder uses Italian; impl shows `"No albums in catalog."` (`AlbumListScreen.kt:174`).
- **[BEHAVIOR]** Mockup has an inline search affordance `"Cerca album…"` and a sort/filter pill `"Recenti"` with a filter glyph (`mh-library.jsx:25-33`). Impl has neither — no in-list search and no sort control.
- **[LAYOUT]** Mockup grid: 2 cols, 14 px gap, cover radius 8, body text 13/11.5 (`mh-library.jsx:34-44`). Impl grid: 2 cols, 12/16 dp arrangement, `CoverShapes.SongRow` clip, `titleSmall`/`bodySmall` (`AlbumListScreen.kt:189-202`, `227-265`). Visually similar but tokens differ.
- **[STATE]** Impl covers loading (spinner), error (`ErrorWithRetry`), empty, paginated load-more spinner. Mockup shows none of these — no shimmer/skeleton.

### ArtistListScreen (`ArtistListScreen.kt`)

- **[LAYOUT]** Same eyebrow + count header in mockup (`mh-library.jsx:63-69`); impl uses Material3 `TopAppBar` with `Text("Artists")` (`ArtistListScreen.kt:154-162`).
- **[COPY]** Mockup title `"Artisti · {count}"` (`mh-library.jsx:67`). Impl title `"Artists"` (English). Empty: `"No artists in catalog."` (`ArtistListScreen.kt:174`).
- **[LAYOUT/BEHAVIOR]** Mockup has a vertical **A–Z scrubber rail** down the right edge with letters dimmed unless represented in the list (`mh-library.jsx:86-93`). Impl has no scrubber.
- **[COPY]** Mockup row subtitle reads `"Artista"` in mono (`mh-library.jsx:79`). Impl computes `"{n} album(s) · {n} song(s)"` in English (`ArtistListScreen.kt:257-261`).
- **[LAYOUT]** Both use a 56 dp circular avatar; mockup uses a generated cover, impl shows a stock `Person` icon over `surfaceContainerHigh`.
- **[BEHAVIOR]** Mockup has a trailing `Chevron` on every row (`mh-library.jsx:81`); impl has no trailing chevron — the row is just clickable.

### LikedScreen (`LikedScreen.kt`)

- **[LAYOUT]** Mockup uses a hero gradient `linear-gradient(180deg, #3A0CA3 0%, #F72585 30%, BG_BOTTOM 60%)` (`mh-library.jsx:112`). Impl uses `MHColors.LikedGradientStart to LikedGradientEnd` via `SpotifyHero` (`LikedScreen.kt:179`). Direction/stops differ but intent matches.
- **[COPY]** Mockup eyebrow `// LIBRERIA · MI PIACE`, title `"Brani che ti piacciono"`, subtitle `"284 brani · 18h 42m"` (`mh-library.jsx:121-123`). Impl title `"Liked Songs"`, subtitle `"Playlist • {count} song(s)"` (`LikedScreen.kt:175-178, 222-223`). All English; impl has no total duration.
- **[LAYOUT]** Mockup hero CTAs are a triplet: shuffle (left, ghost), big lime Play (center, 56 dp), download (right, ghost) (`mh-library.jsx:125-131`). Impl `SpotifyHero` exposes `onPlay` + `onShuffle` only — no per-screen download CTA on Liked.
- **[LAYOUT]** Mockup track rows show **leading numeric index** (mono, right-aligned), 44 dp cover, currently-playing track highlighted in lime + tiny equalizer bars next to title, trailing **filled heart** (lime) + `More` (`mh-library.jsx:135-149`). Impl `SongRow` shows cover only (no numbered index) and the same heart/more affordances.
- **[COPY]** Mockup track meta uses `artist · dur` in mono. Impl `SongRow` formats the duration as `M:SS` but renders it as text body, not mono.
- **[COPY]** Empty-state copy: impl `"No liked songs yet"` / `"Heart tracks from the Search tab to find them here."` (`LikedScreen.kt:189-192`). Mockup has no empty state.
- **[STATE]** Impl has `LikedUiState.Loading` (shimmer), `Error`, pull-to-refresh, paginated load-more (`LikedScreen.kt:99-120`); mockup has none of these.
- **[BEHAVIOR]** Impl wires the kebab to `AddToPlaylistSheet` with dislike + flag-wrong actions (`LikedScreen.kt:127-138`); mockup `More` is decorative.

### Library landing (`PlaylistsScreen.kt`)

> Not present as a screen in `mh-library.jsx`, but Albums / Artisti tabs in this impl screen route into the lists above. Recording differences for completeness.

- **[LAYOUT]** Top bar in impl: avatar/initial + `"Libreria"` + Search + Add + Settings (`PlaylistsScreen.kt:149-215`). Mockup library hub is in another mockup file; the back-chevron headers in this mockup all read `// LIBRERIA` and route directly into Album/Artist drill-downs.
- **[BEHAVIOR]** Impl has filter chips `Playlist | Album | Artisti | Scaricati` (`PlaylistsScreen.kt:218-247`). Selecting `Album`, `Artisti`, or `Scaricati` shows a `TabPlaceholder` saying e.g. `"Apri \"Album\" dal menu di ricerca per sfogliare la libreria completa."` (`PlaylistsScreen.kt:300-319`) instead of routing to `AlbumListScreen` / `ArtistListScreen`. The mockup directly lands on those drill-downs from somewhere else, so the filter-tab → placeholder pattern is not represented in the mockup at all.
- **[COPY]** `LikedSongsRow` title is `"Brani preferiti"` / subtitle `"Playlist"` (`PlaylistsScreen.kt:362-371`); the Liked drill-down (above) titles itself `"Liked Songs"` instead — internal copy inconsistency vs the mockup spec `"Brani che ti piacciono"`.

### Genre detail (`GenreDetailScreen` mockup)

- **[STATE]** Mockup has a **dedicated Genre detail destination** with: back nav, eyebrow `// SFOGLIA · GENERE`, title `"Indie"`, removable lime pill, a `"Più popolari"` filter pill, in-list search `"Cerca dentro Indie…"`, full-width `"Riproduci tutti"` button + shuffle icon, a count caption `// 247 BRANI`, and a list of tracks (`mh-library.jsx:166-216`).
- **[BEHAVIOR]** Impl has **no genre detail screen**. Genre selection on `SearchScreen` shows results inline with a removable pill (`SearchScreen.kt:488-511`) but: no dedicated route, no in-list search, no `"Riproduci tutti"` / `"Shuffle"` CTAs, no popularity sort, no count caption.
- **[COPY]** Italian strings absent in impl: `"Cerca dentro Indie…"`, `"Più popolari"`, `"Riproduci tutti"`, `// 247 BRANI`.

### Collaborative playlist detail (mockup `CollabPlaylistOwner`/`CollabPlaylistMember`)

> Maps to `PlaylistDetailScreen.kt` for both roles. Impl branches on `playlist.isOwner` / `playlist.isAuto` rather than rendering a distinct screen.

- **[LAYOUT]** Mockup: rounded 36 dp pill back + `More` buttons floating on a green gradient (`mh-library.jsx:238-241`); centered hero (180 dp cover + eyebrow + title + count); members strip card with overlapping avatars + `"Gestisci"` button (owner) or chevron (member); auto-sync card with toggle; CTA row Play + Shuffle (+ Share for owner); for member: extra ghost CTA `"Rimuovi dalla libreria"`; `// BRANI` caption above list. Each row shows the contributor name in lime mono uppercase (e.g. `LUCA`).
- **[LAYOUT]** Impl uses `SpotifyHero` (no eyebrow) + a single `AutoSyncCard` row (`PlaylistDetailScreen.kt:400-405`, `491-538`). No members strip, no per-track contributor badge, no `"Gestisci"` CTA, no separate `"Rimuovi dalla libreria"` ghost button (members hit the row long-press dialog `"Rimuovi dalla libreria?"` from the list view at `PlaylistsScreen.kt:519-540` instead).
- **[COPY]** Mockup eyebrow `// PLAYLIST · COLLABORATIVA` is missing in impl. Mockup `"Condivisa con 4 persone"` / `"Condivisa da Luca"` line is shown by impl in subtitle text (`PlaylistDetailScreen.kt:343-350`) — same string but rendered as part of the comma-separated subtitle, not as a dedicated members card.
- **[COPY]** Auto-sync card mockup: `"Sincronizzazione automatica"` / `"Solo Wi-Fi · 8 nuovi brani"` (`mh-library.jsx:285-286`). Impl: `"Sincronizzazione automatica"` / `"Scarica i nuovi brani all'apertura dell'app. Disattivata per impostazione predefinita."` (`PlaylistDetailScreen.kt:515-522`). Different value-prop copy and impl doesn't surface "8 nuovi brani" delta.
- **[COPY]** Mockup Play CTA copy: `"Riproduci"` (`mh-library.jsx:297`). Impl uses the SpotifyHero's iconic Play button (no text label).
- **[BEHAVIOR]** Mockup Share button is owner-only inline beside Play/Shuffle (`mh-library.jsx:300`). Impl shows the share icon in the `TopAppBar` actions row (`PlaylistDetailScreen.kt:152-190`), gated on `isOwner`, and triggers the system `Intent.createChooser`.
- **[BEHAVIOR]** Mockup row contributor pill (`MARTA`, `LUCA`, etc.) (`mh-library.jsx:319`) — no equivalent in impl; per-track contributor data isn't surfaced.
- **[STATE]** Impl handles loading shimmer, error retry, refresh button, swipe-to-remove, drag-reorder, auto-playlist meta strip — none of these states are shown in the mockup.

### PlaylistShareDialog (mockup-only)

- **[LAYOUT]** Mockup is a full bottom sheet with a drag handle, eyebrow `// CONDIVIDI · COLLABORATIVA`, name, an explanatory paragraph (`"Chi apre il link entra come collaboratore della stessa playlist — non viene creata una copia. Solo tu, come creatore, puoi generare nuovi link."`), a copyable link box `https://mh.duckdns.org/p/x7Q-9FN`, a system-share button `"Condividi via sistema"`, a footer with `"4 membri attivi · revoca in Gestisci"` and a destructive `"Revoca link"` button (`mh-library.jsx:331-367`).
- **[BEHAVIOR]** Impl has no share dialog at all — `PlaylistDetailScreen.kt:159-174` mints the link and immediately fires `Intent.createChooser`. There is no UI for: showing/copying the URL, displaying the rationale, listing active members, or revoking a link. Members management (`Gestisci`) is also absent.

### PlaylistShareImporter (mockup `state = first | member | owner`)

- **[LAYOUT]** Mockup is a full-screen modal with green gradient, X dismiss, eyebrow `// LINK CONDIVISO`, large 220 dp hero cover, big title `"Casa · Cena"`, owner caption `"Playlist collaborativa di Luca R."`, `"62 BRANI · 4 MEMBRI"` caption in mono, members preview avatars, and a contextual body line + CTA per state (`mh-library.jsx:370-421`).
- **[LAYOUT]** Impl is a Material3 `AlertDialog` titled `"Aggiungi alla libreria?"` with a single text body and Confirm/Dismiss buttons (`PlaylistShareImporter.kt:69-127`). Visually completely different.
- **[COPY]** Mockup body (state=first): `"Aggiungendola alla libreria potrai aggiungere brani e tutti i collaboratori vedranno le tue modifiche."` (`mh-library.jsx:399`). Impl body: `"${ownerName} ha condiviso \"${playlistName}\" ($songs).\nVedrai gli aggiornamenti in tempo reale e potrai modificarla insieme a ${ownerName}."` (`PlaylistShareImporter.kt:88-92`). Different framing.
- **[COPY]** Mockup state=`member` shows the lime pill `// GIÀ NELLA TUA LIBRERIA` and CTA `"Apri playlist"` (`mh-library.jsx:402-411`). Impl handles this branch silently — `alreadyAccessible` skips the dialog and routes straight in (`PlaylistShareImporter.kt:55-60`). Functionally equivalent but no confirmation/peek for the user.
- **[COPY]** Mockup CTA `"Aggiungi alla mia libreria"` (`mh-library.jsx:411`). Impl button label `"Aggiungi"` / dismiss `"Chiudi"` (`PlaylistShareImporter.kt:119, 125`).
- **[BEHAVIOR]** Impl `LaunchedEffect` triggers `previewShare` on mount, errors surface as red text inline. Mockup has no error/loading variant.
- **[LAYOUT]** Mockup shows the URL footer `mh.duckdns.org/p/x7Q-9FN` below the CTA in mono (`mh-library.jsx:413-417`); impl never shows the URL.

## Missing in impl

1. Genre detail destination (entire `GenreDetailScreen`) — mockup positions it as a first-class browse target with its own play / shuffle / sort / inner search.
2. Eyebrow caption pattern (`// LIBRERIA`, `// LIBRERIA · MI PIACE`, `// SFOGLIA · GENERE`, `// PLAYLIST · COLLABORATIVA`, `// LINK CONDIVISO`, `// CONDIVIDI · COLLABORATIVA`) on every drill-down header — no equivalent in any impl screen.
3. Live count badge (`Album · 8`, `Artisti · 8`) next to titles.
4. A–Z scrubber rail on `ArtistListScreen`.
5. Inline list search affordances: `"Cerca album…"`, `"Cerca dentro Indie…"`.
6. Sort/filter pill on Albums (`"Recenti"`).
7. Numeric track index and currently-playing equalizer indicator on `LikedScreen` rows.
8. Total-duration line on `LikedScreen` (`"284 brani · 18h 42m"`); current subtitle is just count.
9. Triple-CTA layout on `LikedScreen` hero (Shuffle + Play + Download).
10. Members avatar strip + `"Gestisci"` action on collaborative playlist detail.
11. Per-track contributor badge (`LUCA`, `MARTA`) on collaborative playlist tracks.
12. Inline `"Rimuovi dalla libreria"` ghost CTA for members on the playlist detail screen.
13. Dedicated `PlaylistShareDialog` (URL preview + copy + system share + member count + revoke) — currently bypassed via system intent.
14. Member-management UI (`Gestisci`) and revoke-link UI.
15. Full-screen `PlaylistShareImporter` per the mockup design (large hero, member preview, member/owner state branches).
16. Italian-language drill-down titles: impl uses `"Albums"`, `"Artists"`, `"Liked Songs"` while the rest of the app speaks Italian.

## Missing in mockup

**Update 2026-05-05:** new state file `mockup/mh-library-states.jsx` (mounted in `mh-canvas-app.jsx:78-91` as `lib-loading`, `lib-pull`, `lib-error`, `lib-loadmore`, `lib-empty-liked`, `lib-empty-albums`, `lib-kebab`, `lib-auto`, `lib-gestures`, `lib-topbar`, `lib-landing-plus`, `lib-scaricati`, `lib-update`, `lib-offline`) closes most prior gaps. Remaining truly impl-only items at the bottom.

### Now covered by state mockups
1. ~~**Loading / shimmer / error-retry / pull-to-refresh / paginated load-more**~~ →
   - `LibraryLoadingScreen` — full shimmer (filter chips + 8 list rows, `mhShimmer` keyframes, 200% bg-pos slide).
   - `LibraryPullRefreshScreen` — top progress band with spinner + mono `// AGGIORNO LIBRERIA…`, content translated 4px down at 0.85 opacity.
   - `LibraryErrorRetryScreen` — 64dp red triangle glyph, red eyebrow `// ERRORE · NETWORK`, title `"Server irraggiungibile"`, copy `"mh.duckdns.org non risponde. I brani offline restano disponibili dalla scheda Scaricati."`, mono diagnostic chip `ECONNREFUSED · retry 3/5 · backoff 4s`, dual CTA `Riprova` lime + `Solo offline` ghost.
   - `LibraryLoadMoreScreen` — Paged liked-songs list with mono header `// PAGE 4 OF 26 · 100/1284`, leading numeric index col (`fontFamily: T.MONO`, right-aligned), bottom load-more card (spinner + `"Carico altri brani…"` + mono `page=5 limit=100 · ETA 0.6s` + `1184 / 1284` counter).
2. ~~**Empty states**~~ →
   - `LikedEmptyScreen` — purple→bg gradient hero, 200dp dashed-border heart placeholder, eyebrow `// LIBRERIA · MI PIACE`, title `"Nessun brano che ti piace"`, body `"Tocca il cuore su qualunque traccia: la troverai qui, sempre offline-ready."`, lime CTA `Sfoglia consigli`.
   - `AlbumsEmptyScreen` — back chevron + eyebrow `// LIBRERIA` + title `Album · 0`, 96dp dashed disc icon, italian copy referencing `MusicHub` + Exportify path, dual CTA `Importa da Spotify` lime + `Cerca` ghost.
3. ~~**Kebab `AddToPlaylistSheet` with dislike + flag**~~ → `LibraryTrackKebabSheet`. Bottom sheet with eyebrow `// AZIONI · LIBRERIA`, track header (56dp `MHCover` + title + `Artista · Album`), action list: `Aggiungi a playlist`, `Aggiungi alla coda`, `Riproduci dopo`, **muted heart row `Tolto da Mi piace` with trailing mono `OFF` indicator**, divider, two thumb-down rows (`Non consigliarmi questo brano` / `…questo artista`), red flag row `Segnala brano sbagliato` (`#FF7A7A`).
4. ~~**Auto-playlist variant of `PlaylistDetailScreen`**~~ → `AutoPlaylistDetailScreen`. Lime gradient hero, 180dp cover with absolute-positioned `AUTO` badge (sparkle glyph + mono caption), eyebrow `// PER TE · GENERATA`, title `"Mix · 02"`, italian generation copy, **2-up meta strip cards** `// AGGIORNATA` (`"2 giorni fa"` + mono `Prossimo: mer 11/05`) and `// BRANI` (`"32 · 1h 54m"` + mono `+8 nuovi questa settimana`), CTA row with no edit button, section caption `// BRANI · ORDINE GENERATO`, per-track `NEW` ribbon mono badge (`fontSize: 8.5`, lime tinted bg) for fresh tracks + `MHPlayingBars` for currently-playing row.
5. ~~**Swipe-to-dismiss + drag-reorder gestures**~~ → `PlaylistGesturesScreen`. Eyebrow `// PLAYLIST · MODIFICA`, mono helper line `7 brani · trascina per riordinare · scorri per rimuovere`. **Three gesture states inline:**
   - swiping row — pulled left -92px, red `Rimuovi` affordance with trash icon revealed behind on red gradient (`linear-gradient(90deg, transparent 30%, rgba(225,72,72,0.85) 100%)`).
   - dragging row — lifted, lime tint bg + 12px box-shadow + `0 0 0 1px rgba(168,224,78,0.3)` outline + scale(1.01) + lime drag handle + lime title.
   - drop-target gap — 2px lime line with lime glow above the dimmed (0.55) target row.
6. ~~**Refresh / Add-songs `TopAppBar` actions**~~ → `PlaylistDetailWithTopBar`. Top bar shows spinning lime refresh icon (`mhSpin` keyframes) + lime plus + more. Sync banner directly below: `Spinner` + `"Sincronizzo da MusicHub Server…"` + mono `checking 7 tracks · last sync 12m ago` on lime-tinted card. Eyebrow `// PLAYLIST · TOP-BAR ACTIONS`, mono subtitle `5 brani · 19 min · ↻ refresh · ＋ aggiungi`.
7. ~~**Profile / Settings entry point on library landing**~~ + ~~**`Routes.SPOTIFY_IMPORT` row**~~ → `LibraryLandingPlusScreen`. 36dp circular avatar gradient (`#A8E04E → #3A0CA3`) with initial `M` + 2px lime ring, sits left of `Libreria` title; first-class **Spotify import row** as 52dp green-disc icon + `Importa da Spotify` / `Porta le tue playlist nella libreria` + lime chevron, on dashed lime-tinted card; rest of library list (pinned + playlists + albums + artists) follows.
8. ~~**`LibraryFilter.Scaricati` placeholder**~~ → `ScaricatiEmptyScreen`. Filter chips with `Scaricati` selected (lime), 88dp lime-tinted download icon, eyebrow `// LIBRERIA · SCARICATI`, title `"Tutto in streaming, per ora"`, italian copy with inline `<I.Download/>` glyph, settings hint card linking to `Impostazioni · Download` (`Scarica solo via Wi-Fi`).
9. ~~**App-update dialog overlay**~~ → `AppUpdateDialog`. Modal 320px max-w, eyebrow `// AGGIORNAMENTO · v0.13.0` + lime tile glyph, title `"Nuova versione disponibile"`, italian release-note copy, 3-bullet changelog list (lime `+` for additions, red `×` for fixes), version diff chip `v0.12.6 → v0.13.0` + size `14.2 MB`, dual CTAs `Più tardi` ghost (flex 1) + `Aggiorna ora` lime (flex 1.4).
10. ~~**Offline badge / global indicator**~~ → `LibraryOfflineBadgeScreen`. Sticky orange (`#E1A048`) banner under status bar with pulsing dot + `"Sei offline · solo i brani scaricati sono riproducibili"` + mono `OFFLINE` chip; non-downloaded items dim to 0.45 opacity + trailing mono `N/A` chip; downloaded items get inline lime check glyph next to title.

### Still impl-only (behaviour, no UI counterpart needed)
- **Playback error dialog** — generic overlay still not depicted in this audit slice (covered in `04-player-sheets.md`).
- **Per-row offline `dl: true` checkmark** in `LibraryOfflineBadgeScreen` is depicted but impl `PlaylistsScreen` still doesn't surface a per-row download badge — feature gap, not state-mockup gap.
