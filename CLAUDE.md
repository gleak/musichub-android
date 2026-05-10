# MediaPlayer Android — project rules

## Changelog + version bump (MANDATORY)

Every user-visible feature added to this app **must**:

1. Bump `AppVersion.VERSION` in
   `app/src/main/kotlin/com/mediaplayer/android/data/AppVersion.kt`.
2. Bump `versionName` in `app/build.gradle.kts` to the same value
   (also bump `versionCode` by +1).
3. Prepend a new `ChangelogEntry` at the top of `Changelog.entries`
   in `AppVersion.kt`, listing the user-visible highlights of the
   release.

Versioning scheme: semver `MAJOR.MINOR.PATCH`.
- `PATCH` — small features, polish.
- `MINOR` — meaningful feature drops or milestone completions.
- `MAJOR` — breaking UX overhauls.

The `ChangelogSheet` auto-opens when stored `last_seen_version`
differs from `AppVersion.VERSION`. If the version is not bumped,
returning users will not see the new entry.

**Bug-fix-only commits do not require a bump.** Only user-visible
behaviour changes do.

Manual entry point: Home → Settings icon → "What's new".

## Changelog tone (MANDATORY)

Highlights in `Changelog.entries` must stay short and user-facing:

- One short paragraph per highlight (1–3 sentences). No essays.
- Describe the user-visible feature or bug fix and where it lives in
  the UI. That's it.
- **Never include**: file paths, class/method names, DTO field names,
  JSON keys, cron expressions, package names, root-cause analysis,
  before/after code explanations, ContentProvider / MediaSession /
  Media3 internals, backend redeploy instructions, or any other
  engineering implementation detail.
- Italian, conversational, tight. Match the tone of `0.20.0`'s
  "Shuffle che mescola di nuovo a ogni ciclo".

Save the engineering context for the git commit message, not the
changelog. The `ChangelogSheet` is read by users, not engineers.
