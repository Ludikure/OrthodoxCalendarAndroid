# Cross-Platform Parity

This Android app and the iOS app (`OrthodoxCalendar`, SwiftUI) are kept at **functional
and visual parity** as two native codebases that mirror each other by convention. iOS is
the lead platform / source of truth.

## Conventions

- **1:1 file & symbol naming.** A concept lives under the same name on both sides, e.g.
  `engine/BioMatcher.kt` ↔ `Engine/BioMatcher.swift`,
  `app/AppUpdateGate.kt` ↔ `App/AppUpdateGate.swift`.
- **Same architecture.** MVVM; a single `CalendarViewModel` exposing immutable UI state.
- **Versions are deliberately independent.** iOS ships what the App Store has and
  Android what Play has, and the two diverged when iOS released the v2 archive inside
  1.4.3 while Android released it as 1.5.0. Current: Android **1.5.1** (versionCode 15),
  iOS **1.4.4** (build 17). One `minVersion` in `config.json` gates *both* apps, so a
  value between the two hits them differently — pick one at or below the lower of the
  two released versions unless you mean to gate only one platform.

## Shared contracts — must stay byte-identical across platforms

Any change here is applied to **both** apps in the same change-set.

1. **Calendar JSON schema** — `CalendarFile` / `CalendarDay` / `Feast` / `FastingInfo` /
   `ScriptureReading` / `Reflection` / `SaintBio`. Produced by the shared
   `build_database.py` pipeline.
2. **Localization JSON schema** — `LocalizationBundle` / `UILabels` (incl. optional
   `loadingLabel`, `offlineMessage`, `updateRequired*`, Bible-translation labels). The
   `sr/ru/en.json` files are byte-identical copies across the two repos, so a new key is
   a change to both models; screen-local strings stay inline per-language instead.
6. **Saint-bio matching** — `engine/BioMatcher.kt` ↔ `Engine/BioMatcher.swift`, both
   checked against `scripts/shared/simulate_bio_matching.py` in the iOS repo.
   `app/src/test/resources/bio_assignments_2026.tsv` is generated from it; regenerate
   the fixture when the rules or the bundled data change.
3. **Worker API** (`https://orthodox-calendar-api.ludikure.workers.dev`):
   - `GET /api/v2/{locale}/{year}` → a deduplicated `CalendarFile` (2024–2099, ~1.4 MB).
     Its `ref`/`textRef`/`textWebRef` values resolve against the **bundled**
     `texts_<locale>.json`, so the bundled pool must be a superset of the archive's.
   - `GET /api/v2/texts/{locale}` → the full pool for a locale (escape hatch). `en_nc`
     shares `en`'s pool on both platforms and on the Worker.
   - `GET /api/config` → `{ minVersion, latestVersion, appStoreUrl, playStoreUrl?,
     dataRevision }`. A change in `dataRevision` invalidates the on-device year cache;
     both apps must drop disk *and* memory caches when it moves.
   - `GET /api/{locale}/{year}` → the legacy fat `CalendarFile` for pre-1.4 clients.
     Never overwritten by the v2 publish.
4. **Version-compare rule** — dotted numeric; missing components count as 0, and a
   version that does not parse is *not* older (`AppUpdateGate.isOlder`). The gate is
   fail-open: counting an unparseable component as 0 made every such version older
   than any real minimum, which would have walled a working app behind the update
   screen. Identical on both platforms, and covered by a test on each.
5. **No calendar arithmetic in either app.** The Paschalion, the Julian↔Gregorian
   offset and the fasting rules live in `scripts/shared/` and are baked into the JSON
   (`julianDate`, `paschaDistance`, `fasting`), so neither app recomputes them and the
   two cannot drift. Android carried `PaschaCalculator`/`JulianConverter` ports with no
   production caller — and a hardcoded `OFFSET = 13` valid only to 2099 while the app
   offers years to 2099 — so they were deleted rather than left as a trap for the first
   caller. `BioMatcher` is the one exception below: it must run on device because the
   pairing depends on the rendered feast list.

## Platform notes

- Persistence keys differ by platform idiom (iOS `UserDefaults` camelCase vs Android
  `DataStore` snake_case); only the stored **values** must match.
- Store URLs differ: iOS uses `appStoreUrl`; Android prefers `playStoreUrl`, falling back
  to `market://details?id=com.orthodox.calendar`.
