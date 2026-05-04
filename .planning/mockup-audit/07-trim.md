# Ringtone trim — mockup vs impl

## Mockup summary

`mockup/mh-trim.jsx` proposes a full-screen "Modalità · Taglio" (Trim Mode)
editor for cutting a track into a shorter clip — primarily for ringtone /
alarm use. It is a dual-timeline interactive editor:

Top bar (`mh-trim.jsx:64-69`):
- Close button (`I.X`).
- Mono caption `// MODALITÀ · TAGLIO` in accent green.
- Right-side accent-pill `Salva` button.

Track header (`mh-trim.jsx:71-81`): 60×60 cover blob, title (`Undertow`),
`artist · album` line, plus a mono row showing the original duration
(`fmt(total) · originale` → `04:08.6 · originale`).

Section 1 — Preview / scrub bar (`mh-trim.jsx:83-119`):
- Section label `// 01 · ASCOLTO · scrub libero`.
- Full-width 96-bar pseudo-random waveform; bars outside the trim region
  rendered dim, bars inside in `T.TEXT_HI`. A subtle accent-tinted overlay
  (`rgba(168,224,78,0.06)`) with dashed borders highlights the IN→OUT slice.
- A bright yellow (`#FFC857`) playhead with shadowed flag head, free to
  scrub anywhere across the full track (not just the trim window).
- Time row: `00:00.0` left, current playhead chip (with pulsing dot) center,
  `fmt(total)` right.
- Transport row of 5 buttons:
  - `−5` (jump back 5 s, mono small button).
  - `Vai a IN` (jump to IN handle, custom svg arrow icon).
  - Big play/pause centerpiece (`56×56`, accent → goldenrod when playing).
  - `Vai a OUT` (jump to OUT handle).
  - `+5` (jump forward 5 s).

Section 2 — Trim bar (`mh-trim.jsx:121-156`):
- Section label `// 02 · TAGLIO · sposta i punti IN / OUT`.
- A thinner card with a single-line track. Inactive parts are 2 px hairline
  white. The active region is an 8 px accent-green bar between IN and OUT.
- Mini waveform inside the active region (40 % accent over very dim
  background) — visual flair only.
- Two draggable accent-green handles (`Handle`, `mh-trim.jsx:224-261`) with:
  - Vertical glow line.
  - Asymmetric rounded body (`4px 2px 2px 4px` for IN, mirrored for OUT).
  - A small floating label tag below each handle: `IN · 00:18.4`,
    `OUT · 02:42.0`, in mono with accent border.
- Below the track, two `NudgeBox` panels (`mh-trim.jsx:263-275`) labelled
  `// IN` / `// OUT`. Each shows the time in 22 px bold mono and a 4-button
  fine-nudge grid: `−1s`, `−.1`, `+.1`, `+1s`.
- Three quick-action pills (`mh-trim.jsx:150-154`):
  - `Aggancia al silenzio` ("snap to silence").
  - `Fade in/out` (rendered active in the mockup).
  - `Anteprima A/B` ("A/B preview").

Result summary card (`mh-trim.jsx:158-168`):
- Accent-tinted rounded card. Mono label `// RISULTATO`.
- Big mono duration of the cut clip (`fmt(outT - inT)`) followed by a
  subdued caption `· tagliato {fmt(total - (outT - inT))} dall'originale`.

Saved-toast state (`mh-trim.jsx:170-181`, only when `state==='savedToast'`):
- Bottom toast pinned 36 px from the bottom edge with accent border.
- Round accent badge with check icon.
- Heading `Salvato come · Undertow (cut)`.
- Mono follow-up: `Versione locale · sostituirà l'originale nelle
  playlist? Sì / No` — the mockup is asking the user to decide whether
  the trimmed copy should replace the original everywhere.

Bottom hint (`mh-trim.jsx:185-188`): mono caps tip
`TIENI PREMUTO UN MARCATORE PER ZOOM ×8` (long-press a handle for
8× horizontal zoom).

Three documented states (`mh-trim.jsx:52`):
`editing | playingPreview | savedToast`. The play button changes color and
the playhead position differs between `editing` and `playingPreview`.

Helpers worth flagging:
- `genWaveform()` (`mh-trim.jsx:5-20`) — Mulberry32-ish 96-bar synthesised
  envelope (build / plateau / soft fade) so the waveform reads as plausible
  audio; the mockup is purely visual, no audio decode.
- `fmt()` (`mh-trim.jsx:44-49`) — `mm:ss.s` time format, e.g. `02:25.4`.
- `Handle`, `NudgeBox`, `Pill`, `JumpInIcon`, `JumpOutIcon` — local
  components keyed off `T.ACCENT` / `T.MONO` / `T.CARD`.

## Implementation status

**The trim UI does not exist in the Android codebase.**

Searches across `app/src/main/kotlin` for `Trim`, `TrimScreen`, `TrimSheet`,
`TrimDialog`, `Suoneria`, and any composable handling waveform editing
returned **zero matches** for trim functionality. All `trim()` hits are
`String.trim()` on user input.

What does exist for the ringtone/alarm use case is a one-shot, no-UI
exporter that saves the **entire** track:

- `app/src/main/kotlin/com/mediaplayer/android/playback/RingtoneExporter.kt`
  — `object RingtoneExporter` with `suspend fun exportAsAlarm(context, song)`
  (`RingtoneExporter.kt:26-28`). It downloads the full MP3 from
  `Network.streamUrl(song.id)` (`RingtoneExporter.kt:68`) and writes it to
  `MediaStore.Audio.Media` under `Alarms/MediaPlayer/<artist> - <title>.mp3`
  with `IS_ALARM=1` (`RingtoneExporter.kt:47-55`). Re-export deletes the
  prior row to avoid `(1)`, `(2)` duplicates (`RingtoneExporter.kt:39`,
  `94-113`).
- `PlaybackViewModel.kt:117-118` exposes `alarmExportState`, with sealed
  states `Idle | Exporting | Success(title) | Failure(message)`
  (`PlaybackViewModel.kt:148-153`).
- `PlaybackViewModel.kt:807-825` `saveCurrentAsAlarmSound()` triggers the
  export for the currently-playing song; `consumeAlarmExportState()` at
  `PlaybackViewModel.kt:827`.
- The trigger UI is a single dropdown row in the Now Playing overflow
  menu — `NowPlayingSheet.kt:595-608`, label `"Save as alarm sound"` /
  `"Saving as alarm…"`. Result is surfaced via two AlertDialogs:
  `Success` (`NowPlayingSheet.kt:742-753`) and `Failure`
  (`NowPlayingSheet.kt:755-761`). Both have a single OK button.

Changelog reference for the alarm export feature:
`AppVersion.kt:180` — `"… esporta come suoneria, …"`.

## Differences / gaps

The mockup is essentially **a feature that does not exist**. Every visual
element on the screen would be net-new work. Concretely:

1. **No editor screen, period.** There is no Compose surface that opens a
   trim editor. The exporter is fire-and-forget from a dropdown item.
2. **No waveform rendering.** The mockup's `Bars` + `genWaveform` would
   need to be replaced by real waveform extraction (FFmpeg /
   `MediaExtractor` + decoded PCM peaks) or, at minimum, a fake/animated
   waveform on Compose Canvas.
3. **No scrub-preview transport.** Current playback is whole-track; there
   is no concept of an in-editor preview cursor decoupled from the main
   ExoPlayer instance, no `−5 / +5` nudge, no `Vai a IN` / `Vai a OUT`
   jumps.
4. **No IN / OUT model.** The data layer has no notion of a trim region
   per song. `RingtoneExporter.exportAsAlarm` reads the **entire** stream
   body in one `copyTo(out)` (`RingtoneExporter.kt:74-76`). To honour a
   trim window we would need to either (a) stream-decode and re-encode a
   slice (FFmpeg or `MediaCodec` + `MediaMuxer`), or (b) ask the backend
   for a server-side cut.
5. **No nudge controls.** No `−1s / −.1 / +.1 / +1s` UI; no time
   formatting at decisecond precision like `fmt()`'s `02:25.4`. The app
   currently only displays `mm:ss`.
6. **No silence-snap, fade in/out, A/B preview.** All three quick-action
   pills are unimplemented features.
7. **No long-press zoom.** The bottom hint
   `TIENI PREMUTO UN MARCATORE PER ZOOM ×8` describes an 8× zoom mode on
   the trim bar that has no implementation analog.
8. **No "save as cut" flow.** The mockup's `Salva` button + saved-toast
   flow implies (a) exporting the trimmed slice to MediaStore as an
   alarm/ringtone with a `(cut)` suffix, **and** (b) optionally promoting
   that cut copy to replace the original in playlists ("sostituirà
   l'originale nelle playlist? Sì / No"). The latter is a much larger
   data-model change — playlists currently reference the original song
   id and there is no per-user "edited version" concept.
9. **Toast vs dialog mismatch.** Even the "saved" feedback differs: the
   mockup shows a bottom toast with inline Yes/No CTA; the existing alarm
   export shows a centered single-OK AlertDialog
   (`NowPlayingSheet.kt:742-753`).
10. **Italian copy.** Mockup uses Italian throughout
    (`MODALITÀ · TAGLIO`, `ASCOLTO · scrub libero`,
    `TAGLIO · sposta i punti IN / OUT`, `Aggancia al silenzio`,
    `Fade in/out`, `Anteprima A/B`, `RISULTATO`,
    `Salvato come · Undertow (cut)`,
    `Versione locale · sostituirà l'originale nelle playlist? Sì / No`,
    `TIENI PREMUTO UN MARCATORE PER ZOOM ×8`). The current app's
    alarm-export strings (`NowPlayingSheet.kt:580-608`) are English
    (`Save as alarm sound`, `Saving as alarm…`). Even if the trim
    feature is built, a localisation decision is required.
11. **Entry point.** The mockup is a dedicated full-screen mode; the
    current alarm export lives in the Now Playing overflow. If trim is
    built, the entry point would presumably be a new overflow item like
    `Trim and save…` that pushes the editor screen, leaving the existing
    `Save as alarm sound` as a one-tap shortcut for the whole track.

## Recommendation

This entire mockup is a **proposal for a not-yet-implemented feature**.
There is no UI to update — only a small backend exporter that handles
the full-track case. Implementing the mockup would be a meaningful new
phase: waveform decode, dual-timeline Compose UI, slice-export
(MediaCodec/MediaMuxer or FFmpeg), and a data-model decision about
whether trimmed copies are MediaStore-only or first-class library
entities. None of this should be treated as a polish/audit item — it is
a from-scratch feature comparable in scope to one of the larger M-series
milestones.
