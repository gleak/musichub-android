# Auth — mockup vs impl

Source mockup: `mockup/mh-auth.jsx` (exports `LoginScreen`, `OnboardingScreen`, `OnboardingSheet`, `AccountSwitchDialog`).

## Coverage
- mockup screens drawn:
  1. `LoginScreen` (idle / signing-in / error states)
  2. `OnboardingScreen` (genre tag picker)
  3. `OnboardingSheet` (welcome bottom sheet)
  4. `AccountSwitchDialog` (sign-out confirmation modal)
- impl screens found:
  - `app/src/main/kotlin/com/mediaplayer/android/ui/auth/LoginScreen.kt` + `AuthViewModel.kt`
  - `app/src/main/kotlin/com/mediaplayer/android/ui/onboarding/OnboardingScreen.kt` + `OnboardingViewModel.kt`
  - `app/src/main/kotlin/com/mediaplayer/android/ui/onboarding/OnboardingSheet.kt`
  - Sign-out `AlertDialog` lives inside `app/src/main/kotlin/com/mediaplayer/android/ui/profile/ProfileScreen.kt:95-119` (no dedicated file)

## Per-screen findings

### LoginScreen

- **mockup** (`mh-auth.jsx:5-80`):
  - Padding `88px 28px 40px`; radial gradient hero `#2A4615 → BG_TOP → BG_BOTTOM` (lime-tinted top).
  - 92x92 lime tile with **rounded equalizer-bars SVG monogram** (8 dark bars, not the letter "M"), `borderRadius: 22`, drop shadow `0 24px 48px -12px rgba(168,224,78,0.45)`, `marginBottom: 32`.
  - Eyebrow `// BENVENUTO` (mono lime, letterSpacing 1.5).
  - Title `MusicHub` 32px, weight 800, letterSpacing -0.8.
  - Subtitle: `"Per ascoltare la tua libreria, accedi con Google."` (14px, maxWidth 280).
  - Error block (when `state==='error'`): inline panel `rgba(225,72,72,0.12)` bg, 1px `rgba(225,72,72,0.35)` border, 10px radius, red dot bullet, two-line text "Accesso non riuscito" + "Verifica la connessione e riprova. Codice: `auth/network-error`" with code in mono.
  - CTA: pill `borderRadius: 999`, **white** background, dark text `#1F1F1F`, **multi-color Google G logo SVG** (Google brand colours), `box-shadow: 0 8px 24px rgba(0,0,0,0.4)`, padding `14px 16px`. Label flips to `"Accesso in corso…"` while `state==='signing-in'`.
  - Footer fine-print under CTA: `"Continuando accetti i Termini e l'Informativa privacy."` 11px, `T.TEXT_LO2`.

- **impl** (`LoginScreen.kt:33-115`):
  - `Box` filling screen with `MHGradient.screenBg()` (the global app gradient — not the lime-tinted radial from the mockup).
  - 72.dp lime square, `RoundedCornerShape(16.dp)`, **`Text("M")`** as monogram (not the equalizer bar SVG).
  - Eyebrow `EyebrowText(text = "Benvenuto")` → renders as `// BENVENUTO`.
  - Title `Text("MusicHub", style = MaterialTheme.typography.headlineLarge)`.
  - Subtitle: `"La tua libreria musicale personale."`.
  - 48.dp spacer then `Button(...)` lime container, dark content, `RoundedCornerShape(999.dp)`, height 48.dp, label `"Accedi con Google"`. **No leading G logo, no white surface, no shadow.**
  - Loading state (`State.Loading`) renders only `CircularProgressIndicator(color = MHColors.Lime)` centred — there is no inline "signing-in" label on the button.
  - Error state shows `state.message` (raw exception message, English-leaning) in `Color(0xFFFF4D2E)`, `bodySmall`, *below* the button — no panel, no eyebrow, no `auth/...` code, no italian copy.
  - No T&C / privacy footer.

- **Differences**:
  - **[LAYOUT]** Mockup uses a vertical layout with hero centred in flex-1 region and CTA glued to the bottom (`padding: 88px 28px 40px`, button at bottom edge). Impl centres the entire column with a single `Box(contentAlignment = Center)` + 32dp padding — CTA floats with the column rather than being pinned bottom.
  - **[LAYOUT]** Tile is 92dp + 22.dp radius in mockup vs 72dp + 16.dp radius in impl, and uses a custom **bar-graph SVG (8 rounded rects)** as the brand monogram. Impl uses a plain `Text("M")` letter — the brand mark is wrong.
  - **[LAYOUT]** Mockup CTA is a **white pill with the Google multi-color "G" SVG**; impl CTA is a **lime pill with no icon**. This contradicts Google brand guidelines and breaks visual hierarchy with the lime tile.
  - **[LAYOUT]** Mockup gradient is a lime-tinted radial (`radial-gradient(120% 60% at 50% 0%, #2A4615 0%, BG_TOP 35%, BG_BOTTOM 100%)`) specific to LoginScreen. Impl reuses the generic `MHGradient.screenBg()`.
  - **[COPY]** Subtitle: mockup `"Per ascoltare la tua libreria, accedi con Google."` vs impl `"La tua libreria musicale personale."`.
  - **[COPY]** No "signing-in" CTA label — mockup has `"Accesso in corso…"`, impl just swaps the whole screen for a spinner.
  - **[COPY]** Error: mockup uses titled card `"Accesso non riuscito"` + body `"Verifica la connessione e riprova. Codice: auth/network-error"` (mono code). Impl shows the raw `e.message` (e.g. `"Sign-in failed"` from `AuthViewModel.kt:64`).
  - **[COPY]** Mockup has T&C / privacy footer text; impl has none.
  - **[STATE]** Three states in mockup: `idle | signing-in | error`. Impl has four: `Loading | NotSignedIn | SignedIn | Error`. The mockup's `signing-in` is a button-label swap; impl's `Loading` replaces the entire screen with a centred spinner.
  - **[STATE]** Impl `State.Loading` is also used for the **initial auto-sign-in probe** (`AuthViewModel.kt:30-47`) — this means the user sees a bare spinner before the LoginScreen ever renders. Mockup has no equivalent first-frame state.
  - **[STATE]** Impl `State.SignedIn` renders `Unit` inside `LoginScreen` (`LoginScreen.kt:112`); the gate is expected to swap composables outside. Mockup has no `SignedIn` rendering inside this file either, so behavior matches there.
  - **[BEHAVIOR]** Cancel-the-Google-picker path is special-cased in `AuthViewModel.kt:59-62` (returns to `NotSignedIn`); mockup doesn't show this state explicitly but it lines up.
  - **[BEHAVIOR]** Impl: silent token rejection (`AuthViewModel.kt:37-42`) clears the token and falls back to `NotSignedIn` — no toast, no error surface. No mockup counterpart.

### OnboardingScreen / OnboardingSheet

#### OnboardingScreen (genre picker)

- **mockup** (`mh-auth.jsx:88-139`):
  - Linear gradient `#1F1F1F → #080808` (dark-grey → near-black).
  - Header padding `64px 24px 12px`. Eyebrow `// PASSO 1 / 1`.
  - Title `"Cosa ascolti?"` 28px, weight 800, letterSpacing -0.6.
  - Subtitle `"Scegli almeno 3 generi. Useremo questo segnale per costruire il tuo Daily Mix."`.
  - Body is a **flex-wrap pill row** (chips inline, variable width per label), gap 10. Pill = `padding: 10px 16px`, `borderRadius: 999`. Selected: lime fill, dark text, leading 14px Check icon. Unselected: `rgba(255,255,255,0.06)` fill + 1px white-10% border.
  - 12 italian genres (`mh-auth.jsx:83-86`): `Indie, Elettronica, Hip-hop, Jazz, Classica, Ambient, Rock, Pop, R&B, Lo-fi, Punk, Cantautorato`. Pre-picked: `{Indie, Elettronica, Jazz}`.
  - Footer row (with 1px top border `rgba(255,255,255,0.06)`):
    - Left: text button `"Salta"` (transparent, `T.TEXT_LO`).
    - Center: mono `"{count} selezionati"` 11px, `T.TEXT_LO2`.
    - Right: lime pill `"Continua"` — opacity 0.4 if `picked.size < 3`, else 1.0 (always rendered, not disabled).

- **impl** (`OnboardingScreen.kt:57-145`):
  - Background `MHGradient.screenBg()` (generic app gradient).
  - 24.dp top spacer, eyebrow `EyebrowText("Benvenuto")` → `// BENVENUTO` (mockup uses `// PASSO 1 / 1`).
  - Title `"Cosa ascolti?"` (same). Subtitle `"Scegli almeno $MIN_PICKS generi per ricevere suggerimenti adatti."` (different copy).
  - Body is a **`LazyVerticalGrid` of 3 fixed columns** with 72.dp-tall `GenreTile` rectangles (10.dp gap) — not pills/chips.
  - 20 lowercase english-tagged genres (`OnboardingScreen.kt:196-201`): `rock, pop, electronic, jazz, hip-hop, classical, metal, indie, r&b, country, blues, folk, punk, reggae, soul, techno, house, ambient, latin, world`. **No pre-selection** (`setOf<String>()`).
  - Tile renders `genre.replaceFirstChar { it.uppercase() }` — so they appear as `Rock`, `Hip-hop` etc. Selected uses `MaterialTheme.colorScheme.primary` (Lime) + `onPrimary`; unselected uses `surfaceContainerHigh` + `outlineVariant` border via `CoverShapes.Card`.
  - Footer is a single column under the grid:
    - Error text (in `colorScheme.error`, `bodySmall`) when `error != null`.
    - Primary `Button(...)` 52.dp full-width, `enabled = canContinue` (disabled state, not opacity). Label flips between `"Scegli ancora ${MIN_PICKS - picked.size}"`, spinner (`CircularProgressIndicator`), or `"Continua"`. **No mockup equivalent for the dynamic countdown label** — mockup keeps `"Continua"` always and shows a separate `"{n} selezionati"` mono counter.
    - `TextButton(onClick = onSkip)` `"Salta per ora"` full-width *below* primary CTA.

- **Differences**:
  - **[LAYOUT]** Mockup uses a flex-wrap **pill chip cloud**; impl uses a fixed 3-column **LazyVerticalGrid** with 72.dp tall tiles. Visually completely different selectors.
  - **[LAYOUT]** Mockup footer is a horizontal row `[Salta | counter | Continua]` with a thin top divider; impl stacks `[Continua]` over `[Salta per ora]` vertically with no divider and no counter.
  - **[LAYOUT]** Selected pill in mockup has a **leading Check icon inside the pill**; impl `GenreTile` does too (`OnboardingScreen.kt:174-179`) — this part matches.
  - **[LAYOUT]** Background gradient mismatch: mockup `linear(#1F1F1F → #080808)` vs impl `MHGradient.screenBg()` (radial brand gradient).
  - **[COPY]** Eyebrow: mockup `// PASSO 1 / 1` vs impl `// BENVENUTO`. The mockup eyebrow signals progress in a multi-step (even though there's only 1); impl loses that.
  - **[COPY]** Subtitle: mockup `"Scegli almeno 3 generi. Useremo questo segnale per costruire il tuo Daily Mix."` vs impl `"Scegli almeno 3 generi per ricevere suggerimenti adatti."` — the Daily Mix payoff is dropped.
  - **[COPY]** Genre list: mockup has 12 italian-cased entries (`Elettronica, Classica, Lo-fi, Cantautorato`); impl has 20 lowercase english/keyword entries (`electronic, classical, metal, country, blues, folk, reggae, soul, techno, house, latin, world`). Set, capitalisation, language and count all diverge.
  - **[COPY]** Skip button: mockup `"Salta"` vs impl `"Salta per ora"`.
  - **[COPY]** Counter `"{n} selezionati"` is missing in impl.
  - **[COPY]** Primary CTA dynamic label `"Scegli ancora N"` exists only in impl.
  - **[STATE]** Mockup has no `saving` / `error` state — Continua is just a styled button. Impl wires `saving: Boolean` (spinner inside button) and `error: String?` (text above button). Impl is richer here; mockup needs an in-progress treatment.
  - **[BEHAVIOR]** Mockup pre-selects `{Indie, Elettronica, Jazz}` for visual demo only. Impl starts with empty selection (`OnboardingScreen.kt:63`). Pre-seeding is a presentation choice, not a real spec — leaving impl as-is is fine.
  - **[BEHAVIOR]** Impl persists picks via `rememberSaveable` — survives rotation. Mockup state isn't persisted (React local state only).
  - **[BEHAVIOR]** Impl `OnboardingViewModel.skip()` writes `OnboardingPreferences.markDismissed()` and refreshes auth state; submit posts `seedGenres(...)`. No mockup equivalent — mockup is decorative.

#### OnboardingSheet

- **mockup** (`mh-auth.jsx:142-183`):
  - Bottom sheet (`alignItems: 'flex-end'`) with backdrop `rgba(0,0,0,0.7)`, sheet `borderRadius: 24px 24px 0 0`, gradient `#2A4615 → #181818` (lime-tinted top), padding `24px 24px 40px`.
  - 36x4 grab handle `rgba(255,255,255,0.18)` centred top.
  - 76x76 lime tile with the **same equalizer-bars monogram** as `LoginScreen`, `borderRadius: 18`.
  - Centred eyebrow `// BENVENUTO IN MUSICHUB`.
  - Title `"La tua libreria,\nil tuo ritmo."` 24px weight 800 (two lines, hard `<br/>`).
  - Body `"Importa playlist da Spotify, scarica per l'offline e lascia che Daily Mix impari cosa ti piace."` (italian, 13.5px).
  - Single CTA: lime pill `"Inizia"`, full-width, `borderRadius: 999`. **No feature rows/icons at all.**

- **impl** (`OnboardingSheet.kt:40-92`):
  - `ModalBottomSheet` (Material3 default styling), no custom gradient or grab handle override.
  - **No monogram**.
  - Title `"Welcome to MediaPlayer"` (English, headlineMedium).
  - Body `"Your own music library, streamed from your own server. Sign in or play as a guest — your library follows you to the car."` (English, two sentences).
  - **Three feature rows** using vector icons (`Icons.Filled.LibraryMusic` / `MusicNote` / `PlayCircle`), each with title + subtitle:
    - `"Build your library"` / `"Import a Spotify playlist, or find new music with the Find tab."`
    - `"Like and organize"` / `"Heart what you love; the auto-playlists do the rest."`
    - `"Connect to your car"` / `"Android Auto picks up your library, voice search, sleep timer, and more."`
  - CTA `Button(...)` full-width labelled `"Get started"`.

- **Differences**:
  - **[LAYOUT]** Mockup is a **single-message hero sheet** (logo tile + tagline + CTA). Impl is a **3-feature explainer sheet**. Two different IA approaches.
  - **[LAYOUT]** Mockup has the lime monogram tile (76dp); impl has no logo/mark.
  - **[LAYOUT]** Mockup has lime-tinted gradient on the sheet itself; impl uses `ModalBottomSheet` default surface.
  - **[LAYOUT]** Mockup centres all text; impl left-aligns everything inside `Column(spacedBy = M)`.
  - **[COPY]** Eyebrow `// BENVENUTO IN MUSICHUB` — missing in impl (no eyebrow at all).
  - **[COPY]** Title `"La tua libreria, il tuo ritmo."` (italian, two-line) vs `"Welcome to MediaPlayer"` (english, one-line, references *MediaPlayer* not *MusicHub*).
  - **[COPY]** Body completely different and English vs Italian.
  - **[COPY]** Feature rows are not in the mockup at all.
  - **[COPY]** CTA `"Inizia"` (italian) vs `"Get started"` (english).
  - **[STATE]** Both are stateless display sheets — no divergence here.
  - **[BEHAVIOR]** Impl gating note in `OnboardingSheet.kt:30-32` says it shows when `ChangelogPreferences.lastSeenVersion() == null`. Mockup doesn't specify trigger. Wiring isn't broken — just the visual contract is.
  - **[BEHAVIOR]** Impl is named `MediaPlayer` in the body string (`OnboardingSheet.kt:53`) — leftover pre-rebrand copy. Per `CLAUDE.md` the brand is now MusicHub.

### Account switch dialog

- **mockup** (`mh-auth.jsx:186-219`):
  - Centred modal (`justifyContent: center`), backdrop `rgba(0,0,0,0.65)`, padding 24, max-width 320.
  - Surface `#1A1A1A`, `border: 1px solid rgba(255,255,255,0.08)`, `borderRadius: 16`, padding 22.
  - Eyebrow `// CAMBIA ACCOUNT` (mono lime).
  - Title `"Disconnettersi da MusicHub?"` 18px weight 700.
  - Body `"La libreria scaricata e i preferiti restano sul dispositivo. La sincronizzazione con il cloud si interrompe finché non accedi di nuovo."` (13px, two sentences).
  - **Account preview row** (custom): 32x32 gradient avatar with initial `L`, email `luca.r@gmail.com`, mono caption `"Account corrente"` — bg `rgba(255,255,255,0.04)`, radius 10, padding 10/12.
  - Two pill buttons in a row: `"Annulla"` (subtle white-6% bg) flex-1, `"Disconnetti"` (red `#E14848` bg, white text) flex-1, `borderRadius: 999`, gap 10.

- **impl** (`ProfileScreen.kt:95-119`):
  - Standard Material3 `AlertDialog`. No eyebrow.
  - Title `"Disconnettersi?"` (shorter — drops `"da MusicHub"`).
  - Body `"Verrai riportato alla schermata di accesso. I download e le playlist locali resteranno sul dispositivo."` (different framing — leads with where the user goes, mockup leads with what stays).
  - **No account preview row** — the dialog never shows whose account is signing out.
  - `confirmButton = TextButton("Disconnetti", color = 0xFFFF4D2E)` (red text, transparent button — not a red filled pill).
  - `dismissButton = TextButton("Annulla")`.

- **Differences**:
  - **[LAYOUT]** Different surface: `AlertDialog` (Material3) vs custom 16-radius card with mono eyebrow. Impl has no eyebrow, no card border, no rounded radius override.
  - **[LAYOUT]** Buttons are `TextButton`s (label-only) in impl; mockup uses two **pill `Button`s** of equal width (Annulla left, Disconnetti right red-filled). Visual weight is very different.
  - **[LAYOUT]** Account preview row is absent in impl — no avatar/initials/email row inside the dialog body. The user has to remember which account they're killing.
  - **[COPY]** Eyebrow `// CAMBIA ACCOUNT` missing in impl.
  - **[COPY]** Title: `"Disconnettersi da MusicHub?"` vs `"Disconnettersi?"`.
  - **[COPY]** Body fundamentally different (cloud-sync angle missing in impl, "playlist locali" wording vs "preferiti" vs "libreria scaricata").
  - **[COPY]** "Account corrente" caption is missing in impl.
  - **[STATE]** No state divergence (dialog is purely modal both sides).
  - **[BEHAVIOR]** Impl funnels both `Disconnetti` (`ProfileScreen.kt:289-293`) and `Cambia account` (`ProfileScreen.kt:206-210`) through the same `showSignOutConfirm` AlertDialog — mockup is named `AccountSwitchDialog` but its copy ("Disconnettersi da MusicHub?") makes it the same shared confirm. Wiring intent matches, only the chrome diverges.
  - **[BEHAVIOR]** Impl dialog does not visually distinguish "switch account" vs "sign out" — same surface for both entry points, same destructive label `Disconnetti`. Mockup also conflates them under a single `AccountSwitchDialog`. So this is consistent, but the eyebrow copy `// CAMBIA ACCOUNT` is lost.

## Missing in impl
1. **LoginScreen brand monogram** — equalizer-bars SVG (8 rounded rects) is the design's brand mark and it appears on both `LoginScreen` and `OnboardingSheet`. Impl uses a literal `Text("M")` letter — wrong icon entirely.
2. **Google CTA visual treatment** — multi-color G logo, white pill, dark text, drop shadow. Impl uses a lime pill with no icon.
3. **Login lime-tinted radial gradient** — distinct from the generic app gradient.
4. **Login error panel** — boxed inline panel with red bullet, two-line title+code copy. Impl uses a single line of `e.message`.
5. **Login "signing-in" CTA label** (`"Accesso in corso…"`) — impl wipes the whole screen for a spinner instead.
6. **Login T&C/privacy fine-print footer** (`"Continuando accetti i Termini e l'Informativa privacy."`).
7. **Onboarding pill chip-cloud layout + counter footer + thin divider** — impl is a 3-col grid with stacked CTAs.
8. **Onboarding eyebrow `// PASSO 1 / 1`** — impl shows `// BENVENUTO`.
9. **Onboarding genre set + italian capitalisation** — impl has 20 lowercase english slugs vs 12 italian display names. The user-facing labels in italian (`Elettronica`, `Classica`, `Lo-fi`, `Cantautorato`) are missing.
10. **OnboardingSheet welcome design entirely** — lime monogram tile, italian eyebrow + tagline, single-message layout. Impl is a 3-feature english explainer with no logo.
11. **Account dialog eyebrow `// CAMBIA ACCOUNT`**.
12. **Account dialog account-preview row** (avatar + email + mono "Account corrente").
13. **Account dialog filled red destructive pill** + filled neutral cancel pill (impl uses `TextButton`s).
14. **Account dialog cloud-sync warning copy** (`"La sincronizzazione con il cloud si interrompe…"`).

## Missing in mockup
1. **`State.Loading` initial-probe spinner** (auto-sign-in attempt before any UI). Impl needs this to cover token refresh; mockup never specifies it.
2. **OnboardingScreen `error` state** (`OnboardingScreen.kt:107-114`) — surfaced under the grid above the CTA. Mockup doesn't draw a network-failure variant for the picker.
3. **OnboardingScreen `saving` state** — spinner inside the primary CTA while `seedGenres` is in flight. Mockup has only an opacity fade.
4. **Dynamic CTA label `"Scegli ancora N"`** — impl-only ergonomics; mockup keeps a static `Continua`.
5. **`State.SignedIn` rendering branch** in `LoginScreen.kt:112` (no-op, gate handles routing) — not in mockup but also not visible.
6. **OnboardingSheet 3-feature explainer** + `LibraryMusic / MusicNote / PlayCircle` icons — fully impl-only, replaced the mockup's hero treatment.
7. **`AuthViewModel` dual rejection paths** — silent token-rejection clear (`AuthViewModel.kt:37-42`) and Google picker cancel branch (`AuthViewModel.kt:59-62`). Mockup doesn't model these.
8. **`refreshMe()` re-fetch hook** (`AuthViewModel.kt:83-89`) used after onboarding flips server-side `onboardingComplete`. Pure plumbing, no UI counterpart needed.
