# MusicHub — new screens for review

**For Claude Design.** All screens below were drawn directly in
Compose by the coding agent because no mockup was available. Same
design system as the bundle you sent (`mockup/musichub`): lime accent
`#A8E04E`, dark gradient backgrounds (`#1F1F1F → #080808`), Inter +
JetBrains Mono, generative covers, eyebrow `// SECTION`, Italian
copy.

Please evaluate against your design contract — flag anything that
drifts from the visual language so we can correct it next round, or
generate proper mockups for screens you'd rather redesign.

---

## Snapshot

| # | Screen | File | Trigger |
|---|--------|------|---------|
| 1 | Settings · Notifiche | `ui/profile/settings/NotificationsScreen.kt` | Profile → Notifiche |
| 2 | Settings · Crossfade | `ui/profile/settings/CrossfadeScreen.kt` | Profile → Crossfade |
| 3 | Settings · Download offline | `ui/profile/settings/DownloadOfflineScreen.kt` | Profile → Download offline |
| 4 | Settings · Lingua | `ui/profile/settings/LanguageScreen.kt` | Profile → Lingua |
| 5 | Settings · Tema | `ui/profile/settings/ThemeScreen.kt` | Profile → Tema |
| 6 | Settings · Informazioni | `ui/profile/settings/AboutScreen.kt` | Profile → Informazioni |
| 7 | Track action sheet | `ui/common/TrackActionSheet.kt` | Kebab on any `SongRow` |
| 8 | Playlist share dialog | `ui/playlists/PlaylistShareDialog.kt` | Share menu inside Playlist detail |
| 9 | App update banner | `ui/common/AppUpdateBanner.kt` | Home, when `AppUpdateChecker` finds a newer APK |

Plus two AA surfaces deferred (call out below) — total = **11 items**
covered. (Server / backend-host setting was removed in v0.10.4 —
will not be operator-configurable.)

All settings screens are **wired live** in v0.10.4: Crossfade applies
a fade-in volume ramp on auto track-transitions, Download offline
reads the real audio-cache size, Notifiche toggles persist, and the
Profile header counts (Brani / Playlist / Artisti) call the backend.

---

## Shared building blocks

All settings sub-pages share a scaffold (`ui/profile/settings/SettingsSubScreen.kt`):

```
SettingsSubScreen(title, onBack) {
  SettingsCard(eyebrow = "// SECTION") {
    SettingsToggleRow(label, detail, checked, onCheckedChange)
    SettingsRadioRow(label, selected, onClick)
    SettingsInfoRow(label, value)
  }
}
```

- Background: `MHGradient.screenBg()` (1F1F1F → 080808)
- Top bar: arrow back + Italian title (`headlineSmall`)
- Cards: `RoundedCornerShape(12.dp)` on `MHColors.Card` (#181818)
- Toggle thumb: `MHColors.Lime` track when on, `Card` when off
- Radio dot: 8dp black inner circle on lime fill, `Divider` when off

---

## Per-screen detail

### 1. Notifiche (Notifications)

Purpose: per-channel notification toggles.

Layout:
- Card "Musica" with three toggles:
  - **Nuove uscite** — "Avvisi quando un artista che segui pubblica un brano" (default on)
  - **Aggiornamenti playlist** — "Quando le playlist generate vengono aggiornate" (on)
  - **Riepilogo settimanale** — "Domenica · cosa hai ascoltato di più" (off)
- Card "Sistema":
  - **Solo durante Android Auto** — "Silenzia tutte le notifiche quando il telefono è in auto" (off)

Open question for Design: should the AA-only toggle live here or under Android Auto-specific settings?

### 2. Crossfade

Purpose: set 0–12 sec audio crossfade.

Layout:
- Card "Durata transizione" — big mono number (`56sp` lime) + "sec" caption, slider with 13 steps (0..12), `0` left / `12 sec` right rail.
- Card "Come funziona" — 1-paragraph explainer.

Open question: do you want a "preview" affordance (play a 15s sample) like the mockup hinted at?

### 3. Download offline

Purpose: storage gauge + management.

Layout:
- Card "Spazio occupato" — big mono number + "GB di N GB" + horizontal lime gauge (filled width = `used / cap`).
- Card "Opzioni" — toggles: **Solo Wi-Fi** (default on) and **Download automatico** ("Scarica i brani che ascolti più di 3 volte").
- Card "Manutenzione" — destructive **Cancella tutti i download** in `#FF4D2E`.

Numbers are placeholders — wired to download cache repository in a follow-up.

Open question: do you want a per-item list of downloaded songs/albums for granular removal? Currently all-or-nothing.

### 4. Lingua

Purpose: app language picker, separate from system locale.

Layout:
- Card "Lingua app" — radio rows: Italiano / English / Español / Français / Deutsch.
- Card "Nota" — explainer that song lyrics + metadata stay in original language.

### 5. Tema

Purpose: light/dark/system theme.

Layout:
- Card "Aspetto" — radio rows: Scuro / Chiaro / Sistema (default Scuro).
- Card "Nota" — explicit warning that "il tema chiaro è sperimentale" since the design language assumes dark.

Open question: do you want a Material You toggle here or as a fourth radio?

### 6. Informazioni (About)

Purpose: version + build metadata + credits.

Layout:
- Centered hero: lime monogram tile (M), "MusicHub" headline, mono "versione X.Y.Z".
- Card "Build" — info rows: Versione app / Backend / Debug build (Sì/No).
- Card "Crediti" — short paragraph.

Open question: should we link out to GitHub / open-source licenses page?

### 7. Track action sheet (kebab)

Purpose: unified per-track action sheet, replaces the ad-hoc menus
that used to live separately on each list. Reached from kebab on
every `SongRow`.

Layout:
- Modal bottom sheet on `#161616`, drag handle on `MHColors.TextLo2`.
- Sticky header: 48dp generative cover + title + artist (mono caption).
- Action rows (any subset, opt-in via callback args):
  - Riproduci dopo · Aggiungi alla coda · Aggiungi a playlist
  - Aggiungi/Rimuovi dai preferiti (lime when liked)
  - Scarica · Rimuovi download
  - Mostra testo · Apri video
  - Vai all'artista · Vai all'album
  - Condividi
  - Timer di spegnimento
  - Rimuovi dalla playlist (red `#FF4D2E`, only when in playlist context)

Each row: 22dp icon + 16dp gap + Italian label `bodyLarge`.

Open question: do you want an icon on every row (current) or only on
"primary" actions? Spotify-style is no-icon. Keeping icons gives
the sheet a more app-native feel.

### 8. Playlist share dialog

Purpose: generate `mediaplayer://share/<token>` and let user copy
or open system share-sheet.

Layout:
- Modal `Dialog`, `RoundedCornerShape(20.dp)` on `#1A1A1A`.
- Eyebrow "// CONDIVIDI PLAYLIST" + headline + 1-paragraph
  one-shot-copy explainer.
- Link card on `rgba(255,255,255,0.04)` — Link icon (lime) + the
  full URL (mono caption) + Copy icon button.
- Full-width lime CTA "Condividi" → `Intent.ACTION_SEND` chooser.
- Footer "Chiudi" text button.

Open question: do you want a "Show QR" affordance (so people can
scan the link from another phone)? Out of scope for v1.

### 9. App update banner

Purpose: surface available APK from the self-hosted update channel.

Layout:
- Lime-tinted card on Home (`background MHColors.Lime.copy(alpha = 0.10f)`,
  `border 1dp Lime alpha 0.35`).
- Lime circle + Download icon.
- Eyebrow "// AGGIORNAMENTO DISPONIBILE" + version transition
  "v0.10.2 → v0.10.3" (titleMedium bold) + "Tocca per installare".
- Trailing X icon to dismiss locally (re-shows next launch).

Tap: `AppUpdateInstaller.install()`. Dismiss: hides for the session.

Open question: where should it sit on Home — above the greeting
(currently planned) or as a floating snackbar pinned to the
player bar?

---

## Deferred / out of scope

### AA · Onboarding

Android Auto doesn't render Compose; surfaces a templated UI from
`MediaItem`s. Plan: when the user is anonymous, surface an info
`MediaItem` at the AA root with title "Accedi sul telefono" and a
description directing the user to sign in via the phone app.

### AA · Equalizer

Same constraint — needs a `SessionCommand` registered on
`MediaSession` so AA can render driver-safe preset chips. Not a
"screen" you can mock pixel-perfect; we'll request a layout-only
spec when ready.

---

## Open design questions for Claude Design

1. Settings sub-pages currently use chevron-less rows + tap to
   navigate. Want to add a chevron back on each row for
   discoverability?
2. The slider thumb on Crossfade is the default Material lime —
   should we draw a custom 18dp thumb with a lime ring like Now
   Playing's scrubber for consistency?
3. Track action sheet has icons on every row. Spotify-style would
   be no-icon. Which fits MusicHub better?
4. Playlist share dialog uses a `Dialog`, not a bottom sheet — feels
   more "ceremonial" for sharing. Confirm vs swap to sheet?
5. App update banner: where on Home should it sit?

Send corrections / mockups and we'll re-pass these screens.
