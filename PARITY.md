# Cross-Platform Parity

This Android app and the iOS app (`OrthodoxCalendar`, SwiftUI) are kept at **functional
and visual parity** as two native codebases that mirror each other by convention. iOS is
the lead platform / source of truth.

## Conventions

- **1:1 file & symbol naming.** A concept lives under the same name on both sides, e.g.
  `engine/PaschaCalculator.kt` ↔ `Engine/PaschaCalculator.swift`,
  `app/AppUpdateGate.kt` ↔ `App/AppUpdateGate.swift`.
- **Same architecture.** MVVM; a single `CalendarViewModel` exposing immutable UI state.
- **Version lockstep.** Bump `versionName` (Android `app/build.gradle.kts`) together with
  iOS `MARKETING_VERSION` (`project.yml`). Current: **1.3.2**.

## Shared contracts — must stay byte-identical across platforms

Any change here is applied to **both** apps in the same change-set.

1. **Calendar JSON schema** — `CalendarFile` / `CalendarDay` / `Feast` / `FastingInfo` /
   `ScriptureReading` / `Reflection` / `SaintBio`. Produced by the shared
   `build_database.py` pipeline.
2. **Localization JSON schema** — `LocalizationBundle` / `UILabels` (incl. optional
   `loadingLabel`, `offlineMessage`, `updateRequired*`, Bible-translation labels).
3. **Worker API** (`https://orthodox-calendar-api.ludikure.workers.dev`):
   - `GET /api/{locale}/{year}` → a `CalendarFile`.
   - `GET /api/config` → `{ minVersion, appStoreUrl, playStoreUrl? }`.
4. **Version-compare rule** — dotted numeric, missing/non-numeric components count as 0
   (`AppUpdateGate.isOlder`). Identical on both platforms.
5. **Engine constants** — Julian↔Gregorian `OFFSET = 13` (years 1900–2099); Meeus Julian
   Pascha algorithm.

## Platform notes

- Persistence keys differ by platform idiom (iOS `UserDefaults` camelCase vs Android
  `DataStore` snake_case); only the stored **values** must match.
- Store URLs differ: iOS uses `appStoreUrl`; Android prefers `playStoreUrl`, falling back
  to `market://details?id=com.orthodox.calendar`.
