# Code review — third pass (Android, 1.5.1 / versionCode 15)

Findings from a full read of all 57 Kotlin sources plus manifest, res, gradle and
assets, taken after the first two review passes were folded into
`release-notes-1.5.1.md`. Each item names the file and function, the mechanism, and
the fix I would make.

**Status: fixed, except two things** — the in-app splash is still timed rather than
gated on data readiness, and `day.feasts.drop(1)` in the share text is untouched
(verified safe, left on purpose). Both are marked where they appear. The user-visible
fixes are folded into `release-notes-1.5.1.md` under "**Also fixed (third review
pass)**".

Verified after fixing: `./gradlew testDebugUnitTest` → **74 tests, 0 failures, 0
skipped** across 13 classes, about 2 s of test time (was five classes and 23 tests:
`AppUpdateGateTest`, `BioMatcherTest`, `FastingStyleTest`, `ReadingLabelsTest`,
`ScriptFoldingTest`); `./gradlew :app:lintDebug` → no errors; `./gradlew
:app:assembleDebug` → APK. The test class that used to make the build look wedged is
in that suite and no longer does — see "Also worth knowing" at the foot of this file.
The classic Android traps this code previously fell into — composition-scoped
repositories, `contains`-based fasting classification, dedup cleanup in the wrong
`finally`, `ActivityNotFoundException` on `ACTION_INSERT`, `AppColors` from
non-composables — are fixed and documented, and are not repeated here.

Line numbers are from HEAD at `be5972b`.

---


## Corrections (pass 4, 2026-09-11)

Four statements in this pass were wrong, and three of them changed code.

- **The app could not construct its ViewModel.** Not a finding here — a defect this
  pass introduced. `CalendarViewModel` gained defaulted constructor parameters (for
  `YearSource`/`SettingsSource`) without `@JvmOverloads`, so no `(Application)`
  constructor existed for `viewModel()`'s reflective factory. Every launch crashed;
  every test passed. Fixed; `ViewModelFactoryTest` builds it the app's way.
- **Trim levels** (P3, unbounded year cache). The constants are ordered — RUNNING
  5/10/15, UI_HIDDEN 20, BACKGROUND 40, MODERATE 60, COMPLETE 80 — and BACKGROUND
  does not share 20 with UI_HIDDEN. From API 34 an app is sent only UI_HIDDEN and
  BACKGROUND, so the release set below, which omitted BACKGROUND, never ran on
  Android 14+. BACKGROUND now releases; UI_HIDDEN still does not.
- **The midnight refresh** used a coroutine `delay`, which runs on uptime and stops in
  deep sleep. The date is now also re-read on every ON_START, and the tick re-armed
  from the wall clock.
- **Turkish/Azeri locale** (P3 i18n table). Not a bug. Kotlin's no-argument
  `lowercase()`/`uppercase()` are `Locale.ROOT` in the stdlib; only Java's
  no-argument forms follow the default locale. The `Locale.ROOT` edits are harmless
  and stay; the claims are withdrawn, from the release notes too.

Also found in pass 4: the disk trim ranked by year number rather than recency, and
the revision-change `inFlight.clear()` did not stop a stale load re-inserting its
year. Both fixed, with tests.

## P1 — Stale month rendered under the new header, with no progress feedback

**Fixed.** `CalendarUiState` carries `loadedLocale`/`loadedMonth`/`loadedYear` and a
`loadedContentKey`; `loadData` clears `daysInMonth` (and `fastingPeriods`) unless the
days on screen are exactly the ones it is about to replace, and stamps that identity
on both the success and the failure path. The header names the month asked for, the
body either agrees or the spinner covers it, and list and grid key their scroll and
reset effects on `loadedContentKey` instead of `days.size` — so the retained scroll
offset and the January → March "same length, no re-scroll" case both go away. Pinned
by `CalendarViewModelTest`: *a month change clears the days it is about to replace*,
*months of equal length are different content*, and *walking a year never leaves one
month's days under another's header* — 24 months, so it crosses the year boundary the
date picker and the header both have to agree about.

`CalendarViewModel.loadData` (`ui/viewmodel/CalendarViewModel.kt:226`) sets
`isLoading = true` but leaves `daysInMonth` untouched, and
`CalendarTabScreen.kt:161` only shows the spinner when the list is empty:

```kotlin
// CalendarViewModel.loadData
_uiState.update { it.copy(isLoading = true, loadFailed = false, isOffline = false) }

// CalendarTabScreen:161
if (uiState.isLoading && uiState.daysInMonth.isEmpty()) { …spinner… }
```

`goToNextMonth` / `goToMonth` update `currentMonth`/`currentYear` synchronously, so
for the duration of a load (fast for a bundled year, 1–3 s for a downloaded one):

1. `MonthHeaderBar` reads **"February"** while `MonthListScreen` still renders
   **January's days**.
2. No spinner appears, so the screen looks finished and the user taps a day from
   the wrong month.
3. `MonthListScreen`'s `listState` survives the data swap, so the new month opens
   at the **old scroll offset** (mid-month) rather than at the top. It only jumps
   when `days.size` happens to change *and* today is in the target month
   (`ui/components/MonthListScreen.kt:41`), which is why the behaviour reads as
   inconsistent rather than wrong.

Fix — make "which month is on screen" explicit state and clear on mismatch, then
let `isLoading` drive the indicator unconditionally:

```kotlin
// CalendarUiState: val loadedMonth: Int = …, val loadedYear: Int = …
_uiState.update {
    val same = it.loadedLocale == locale && it.loadedMonth == month && it.loadedYear == year
    it.copy(isLoading = true, loadFailed = false, isOffline = false,
            daysInMonth = if (same) it.daysInMonth else emptyList())
}
```

This fixes all three symptoms at once and lets `MonthListScreen`'s `days.size`
effect key collapse to a single honest `scrollToTodayTrigger`.

---

## P1 — A failed DataStore read leaves the app permanently blank

**Fixed** at the root rather than per call site: `firstOrDefault` in
`data/preferences/GuardedRead.kt` rethrows `CancellationException` and substitutes a
default for anything else, and all three preference reads go through it. Worth noting
the blast radius was worse than the blank screen — the collector's death cancelled
the whole `init` scope, so `loadMonth` died with it and no month ever loaded either.
Covered by `GuardedReadTest` (4 — *a corrupt preferences file yields the default*,
*cancellation is rethrown, not defaulted*, and the two that keep it honest about
reading real values), and from the ViewModel side by *an unreadable preferences file
still opens the calendar*.

`CalendarViewModel.kt:75-77`:

```kotlin
val lang = preferences.languageFlow.first()
val theme = preferences.themeFlow.first()
val bibleTranslation = preferences.bibleTranslationFlow.first()
```

`preferencesDataStore.data` throws `IOException` on a corrupt or locked preferences
file, or on a full disk — real field failures, and iOS `UserDefaults` has no
equivalent. The `init` coroutine dies before `_uiState.update`, so `localization`
stays `null`, `CalendarTabScreen.kt:68` (`val localization = uiState.localization ?: return`)
draws **nothing at all**, and `NavGraph` gates Search and Settings the same way.
No retry, no fallback, no log.

Guard each read (`runCatching { … }.getOrNull() ?: default`), or read all three
through one guarded `combine`. `LocalizationManager.loadBundle` already degrades to
SR and then to an emergency bundle; the preferences path is the only unguarded
startup input left.

---

## P2 — A day that cannot be resolved spins forever

**Fixed.** `resolveDay` returns `DayOutcome` — `Found(day, periodInfo)`, `Loading`, or
`Missing` (`NoData` / `Offline`) — and both routes render `DayDetailUnresolved`
(what happened, why, Retry) over the shared `dayOutcome` composable, so the
resolution logic the finding found duplicated twice is now written once and the two
routes differ only in what they do with a found day. `attempt`, a mutable key in the
composable, re-runs the resolve on retry even when the flow value does not change;
`CancellationException` still propagates, and the offline case recovers when the
connection does (*a dropped connection is reported as offline and recovers on retry*,
*an unresolvable day says so instead of spinning*, *a malformed date is no data, not a
crash and not a spinner*).

`ui/navigation/NavGraph.kt:98-108`, and the identical block at `135-145` for the
reminder route:

```kotlin
var day by remember(gregorianDate) { mutableStateOf(uiState.daysInMonth.firstOrNull { … }) }
LaunchedEffect(gregorianDate) { if (day == null) day = viewModel.dayFor(gregorianDate) }
if (resolved == null || localization == null) DayDetailPlaceholder(…)
```

`dayFor` (`CalendarViewModel.kt:161-163`) swallows `NotFound` and `Offline` into
`null`, so "still loading" and "this year cannot be loaded" render identically: an
**indefinite spinner** with no retry. Reachable from the date picker or search on a
year the archive lacks. `DayDetailPlaceholder`'s own doc comment ("*or when it
cannot be*") shows the two cases were meant to be separated.

Fix: `dayFor` returns a sealed result (or sets a `dayLoadFailed` flag); render
`defaultNoDataMessage` + Retry in the placeholder. While in here, the 20-line block
duplicated across the two routes should become one composable taking a content lambda.

---

## P2 — `shareDay` is the one unguarded `startActivity` left in the app

**Fixed.** `shareDay` returns `Boolean`; the caller toasts "No app available to share
with." in the UI language, matching `AddReminderScreen`. `MonthHeaderBar.onShare` is
still the `{}` placeholder at `CalendarTabScreen.kt:188` — a dead callback rather than
a crash, and the only share path left unwired.

`ui/screen/detail/DayDetailScreen.kt:557`:

```kotlin
context.startActivity(Intent.createChooser(sendIntent, null))
```

Compare `MainActivity.kt:58` (guarded, with a comment on why), `AboutScreen.kt:263`
(guarded), `AddReminderScreen.kt:196-201` (`ActivityNotFoundException` caught). The
share path — the one that runs on a device with no share target, or while a
profile/clone has resolution temporarily broken — is unguarded and kills the
process from the screen with the richest content in the app. Wrap it and toast on
failure, matching `AddReminderScreen`.

---

## P2 — The fasting-season badge disappears on exactly the days that need it

**Fixed.** `periodInfoFor` runs the same `computeSpans` the month list uses, for the
day being opened, so the badge no longer depends on which month happened to be loaded
when the detail screen opened. The span computation is covered by
`FastingPeriodSpansTest` (7); the detail-path wiring is not, because the fake year
source carries no seasons by design.

`NavGraph.kt:114` supplies `periodInfo = uiState.fastingPeriods[gregorianDate]`,
but `fastingPeriods` is computed only for the currently loaded month/year
(`CalendarViewModel.kt:238`). `CalendarTabScreen` handles neighbour days by loading
them; the detail screen does not. Opening **Jan 1 or Dec 31** — the very days
`seasonDays` exists for — during the Nativity Fast yields no season badge, while the
month row for the same day shows it.

Not hypothetical. Verified against the bundled data (`calendar_{ru,sr}_{2025,2026}.json`):
in every year, `12-31` and `01-01` are both tagged `nativity_fast` — 31 of 31
December days and the first 6 January days are tagged, both locales — so the run
straddles the year boundary every single year.

Fix: compute the span for the resolved day (the repository already has that year
loaded), or widen the lookup to `seasonDays` for the day's own year.

---

## P2 — Language switch renders old-locale content under new-locale chrome

**Fixed** by the same clear — the locale is part of the loaded identity, so switching
language empties the list until the new locale's month lands. Pinned by *switching language clears the month it is
about to replace*, which asserts the invariant at every step of the switch rather
than at one snapshot: where the bundle load suspends depends on the dispatcher, and
the bug was never "the screen at time T is stale", it was "there existed a moment when
the screen mixed two locales".

`CalendarViewModel.forceReload` (`:96-102`) swaps `localization` immediately but
leaves `daysInMonth` holding the previous locale's `CalendarDay` objects — saint
names, fasting labels and reading references are all baked into the JSON per locale
— until the new load lands. Because the loading branch requires an empty list, the
user reads Serbian saint names under Russian section headers for the whole load,
worst on a downloaded year. Same fix as the first item: clear `daysInMonth`, or gate
rendering on `loadedLocale == language.code`.

---

## P3 — `inFlight` dedup key omits `allowNetwork`

`data/repository/CalendarRepository.kt:90-100` keys the in-flight map
`calendar_{locale}_{year}`, but the job body closes over `allowNetwork`. Two
concurrent requests for the same year with different flags share one job, so a
network-allowed caller can inherit `NotFound` from an `allowNetwork = false` caller
— surfacing as "No calendar data for 2031" (`CalendarTabScreen.kt:200`) with a retry
button that looks broken.

**Fixed.** `inFlightKey(locale, year, allowNetwork)`, and `checkRevisionOnce` clears
`inFlight` alongside `monthCache`, since a revision move invalidates both — otherwise
a load started under the old revision re-inserts a year the app has just been told is
stale, for the life of the process.

Low impact today: `seasonDays` issues its no-network load only after `loadMonth`
returns, in the same coroutine, so the window is narrow. Still worth one line,
because the failure mode is a heisenbug:

```kotlin
private fun inFlightKey(locale: String, year: Int, allowNetwork: Boolean) =
    "${fileKey(locale, year)}:${if (allowNetwork) "net" else "local"}"
```

Related, same file: `checkRevisionOnce` clears `cache` and `textsCache`
(`:193-196`) but not `inFlight`, so a load started under the old revision can
re-insert itself into `cache` after the clear and be served for the life of the
process.

---

## P3 — Unbounded in-memory year cache; `largeHeap` is doing the work

`CalendarRepository.cache` is a `ConcurrentHashMap<String, CalendarFile>` with no
eviction, and there is no `onTrimMemory` anywhere in the app (grep
`onTrimMemory|LruCache|removeEldest` → zero hits). A resolved year is 365 days of
feasts, readings and bios; browsing 2024–2099 across four locales retains every one.
The manifest comment on `largeHeap` explains the 17 MB Russian pool but not the
unbounded map. Bound it to ~4–6 keys (locale×year) and clear pools for inactive
locales on trim.

**Fixed.** `BoundedCache` (LRU by insertion order, `MAX_CACHED_YEARS` across locales,
`MAX_YEARS_PER_LOCALE_ON_DISK` per locale on disk), `releaseMemory()` on the
repository, and `OrthodoxCalendarApp.onTrimMemory` deciding what each level means in
**[Pass 4: the release set described here omitted BACKGROUND and never ran on API 34+ — see Corrections.]** `app/MemoryTrim.kt`. `TRIM_MEMORY_UI_HIDDEN` is deliberately *not* a releasing level:
it only means the user left the app, and dropping 17 MB there would cost a re-decode
on return. `BoundedCacheTest` (6) and `MemoryTrimTest` (6, Robolectric — the per-level decision,
with one test guarding the premise that the `ComponentCallbacks2` constants are
distinct and non-zero, because a file asserting against all-zeros would pass however
the function behaved).

---

## P3 — i18n and polish

| Location | Issue |
|---|---|
| `ui/util/ScriptFolding.kt:21` | `text.lowercase()` uses the **default locale**. On a Turkish/Azeri device a Latin query "Ivan" folds to `ıvan` while Cyrillic `Иван` folds to `ivan` → zero results. Use `lowercase(Locale.ROOT)`; consider NFD + dropping combining marks. Same latent shape at `FastingStyle.kt:17` and `ReadingLabels.kt:37,124` — benign only because the seven fasting codes contain no `I`. **Fixed** — `Locale.ROOT` in all four, `FastingVisuals` included. **Pass 4: not a bug — see Corrections.** |
| `SettingsScreen.kt:206`, `AboutScreen.kt:269`, `DayDetailScreen.kt:211` | `uppercase()` on English literals under a Turkish default locale gives `BİBLE TRANSLATİON`-style headers. **Fixed** — `uppercase(Locale.ROOT)` at all three. **Pass 4: not a bug — see Corrections.** |
| `ui/screen/grid/FastingLegend.kt` | Legend shows Oil / Fish / Strict / Feast and **omits Water**, while `GridDayCell.kt:146` tints `hotNoOil` days with `fastWater`. One day per year today; the legend is the only explanation of the tint. **Fixed** — five buckets, in a `FlowRow`: five labels in Serbian or Russian do not fit one line on a narrow phone. |
| `GridDayCell.kt:154` vs `SelectedDayCard.kt:136` | Two private `fastingColor` implementations over `fastingStyle`, one `else -> Transparent`, one `null -> Transparent`. **Fixed** — `ui/util/FastingVisuals.kt` is now the one colour+text mapping, with the tested classifier beside it; four call sites route through it. |
| `CalendarTabScreen.kt:140`, `MonthListScreen.kt:37`, `CalendarGridScreen.kt:52` | `LocalDate.now()` read during composition → the today ring and the banner's `todayInView` branch go stale across midnight until the month reloads. **Fixed** — `CalendarUiState.today`, re-published by `startTodayTicker` at midnight (`MidnightTest`, `IsoDateTest`). `DatePickerSheet`'s own `now` is left: it is read when the sheet opens, and the sheet does not live to midnight. |
| `CalendarGridScreen.kt:49-55` | `selectedDay` is plain `remember` and `LaunchedEffect(days, loadedLocale)` resets it to today, so a rotation silently discards the user's selected cell. **Fixed** — `rememberSaveable(loadedContentKey)` holding the ISO date: a rotation keeps the selected cell, a month change does not. |
| `res/values/themes.xml` | `Theme.OrthodoxCalendar` is `android:Theme.Material.Light.NoActionBar` with no night variant, so the window background is white under `AppTheme.DARK`: white flash beneath the splash, light launch preview in Recents. **Fixed** — `values-night/{colors,themes}.xml`, plus `MainActivity.syncWindowBackground`, which repaints the window when the *app's* theme is DARK while the system stays light (a resource qualifier cannot follow a setting). The two colours now exist in three definitions — window, Compose theme, resource bundle — which cannot share one; they are documented against each other and verified equal, `#F5F3EE` light / `#1C1A17` dark. |
| `MonthHeaderBar.kt:67-68`, `CalendarViewModel.kt:126-129`, `DatePickerSheet.kt:128-142,228,256` | The archive-boundary rule (`MIN_YEAR`/`MAX_YEAR` + month clamp) is hand-written in four places. **Fixed** — `CalendarArchive` (declared beside the state it governs, in `ui/viewmodel/CalendarViewModel.kt`) owns `MIN_YEAR`/`MAX_YEAR`, and `canGoPrevious`/`canGoNext` decide the chevrons; `CalendarUiState.canGoPrevious`/`canGoNext`, the ViewModel's `atFirstMonth`/`atLastMonth` guards and both modes of the date picker all read those two functions. The repository no longer restates the bounds at all. `CalendarArchiveTest` (5) pins the bounds, both edges and the year-boundary step. Follow-up, not a defect: this is data-layer knowledge living in the UI package — a mechanical move to `data/repository/`, no behaviour change. |
| `app/AppUpdateGate.kt:72` | Fallback is `https://play.google.com/store/apps/details?id=…`; `PARITY.md` (Platform notes) documents `market://details?id=com.orthodox.calendar`. `isStoreUrl` even accepts `market.android.com`. Code and doc disagree — pick one. **Reconciled in `PARITY.md`**, in the code's favour: `isStoreUrl` accepts `https` on `play.google.com`/`market.android.com` only, so a `market://` value from `/api/config` is rejected and the https fallback is used. https also survives a browser-only device, which `market://` does not. |
| `data/model/FastingPeriod.kt:81`, `engine/BioMatcher.kt:82` | The only `!!` in the app. Both provably safe (`sorted` filtered non-null; `scores` indexed by construction), but `filterIsInstance`/`getValue` would keep the invariant local. **Removed** — `getValue` where the key is provably present, with the reason written down beside it. No `!!` remains anywhere in `app/src/main`. |
| `ui/screen/splash/SplashScreen.kt:44-48` | `installSplashScreen()` (system) plus a 1.9 s in-app splash run in series on every cold start, and the in-app one is timed rather than gated on data readiness — the bundled year is usually ready well before the fade. **Not done, deliberately**: the system splash already covers process start, and re-gating the in-app one is a cold-start behaviour change that nothing on a JVM test machine can validate. |
| `ui/screen/search/SaintSearchScreen.kt:186` | `focusRequester.requestFocus()` in `LaunchedEffect(Unit)` with no placement guarantee; the documented `IllegalStateException("FocusRequester is not initialized")` is possible on some vendors. **Fixed** — guarded on `hasFocus`, so the keyboard is asked for once on entry and the request cannot precede placement. |
| `CalendarViewModel.kt:212,215` | `runCatching` around `repository.load(...)` swallows `CancellationException` — harmless here (the job is cancelled anyway, so the state update is skipped at the next suspension point), but it is the only `runCatching` in the app wrapping a suspend call; the `catch (c: CancellationException) { throw c }` idiom used elsewhere would be consistent. **Fixed** — the loads are explicit `try`/`catch` with `CancellationException` rethrown (superseded loads do not surface as errors), and the same discipline holds in `resolveDay` and `neighbourYear`. |
| `DayDetailScreen.kt:541` | `day.feasts.drop(1)` in the share text loses `feasts[0]` if a day ever has no `displayRole == "primary"`. Verified safe today: 365/365 days in `calendar_sr_2026.json` have exactly one primary and it is always index 0. Worth `distinctBy { it.name }` if the pipeline changes. **Unchanged on purpose** — the finding is a data-contract note, not a live defect; only the return value of `shareDay` moved. |

---

## Test coverage gaps

Was: five classes, 23 tests, pure string and enum logic only — exactly the parts that
were previously broken, which is good discipline but not coverage of what is about to
change. Now 13 classes, 74 tests, about 2 s of test time, still JVM-only
(Robolectric 4.14.1 runs offline, so nothing here needs a device).

Covered by this pass (51 tests): **`FastingPeriods.computeSpans`**
(`FastingPeriodSpansTest`, 8 — run splitting, the `complete` edge flag, the 1-based
index, and a season crossing the year boundary staying one run), the whole load state
machine and its agreement with the header (`CalendarViewModelTest`, 13), the midnight
refresh (`MidnightTest`, `IsoDateTest`), the archive bounds (`CalendarArchiveTest`),
the guarded preference read (`GuardedReadTest`), the bounded cache and the trim levels
(`BoundedCacheTest`, `MemoryTrimTest`).

Still open, in the order I would take them:

- **`CalendarRepository.loadMonth`** month filtering (`monthKeyPrefix`) and
  `poolName("en_nc") == "en"` — the latter is an explicit parity contract and is still
  guaranteed only by a comment saying the two pools were byte-identical. No new seam
  needed: the repository reads through `context.assets`, and Robolectric already serves
  the merged debug assets, so a `CalendarRepository` over an application `Context` is
  the whole test.
- **`ScriptureReading.text(translation)`** — the WEB→KJV fallback chain, including OT
  readings where `textWeb` is null.
- No instrumentation or `uiAutomator` pass: the Compose screens themselves are checked
  by compilation and lint, not by running. `CalendarViewModelTest` drives the state
  the screens render, which is where this release's bugs actually were.

---

## Suggested order of work — and how it went

1. `loadData` clears stale days + `isLoading` drives the indicator — fixes three
   visible defects (wrong month under header, no feedback, retained scroll).
2. Guard the three `preferences.*.first()` reads.
3. `dayFor` tri-state → real "not found" UI; deduplicate the DayDetail/reminder
   block in `NavGraph`.
4. `runCatching` around the share `startActivity`.
5. Season badge for the resolved day; clear days on language switch.
6. Cheap correctness sweep: `Locale.ROOT` casing, `inFlightKey`, night
   `windowBackground`, `market://` vs `PARITY.md`, unify `fastingColor`, extract the
   year-boundary helper, add Water to the legend.
7. Bounded year cache + `onTrimMemory`; tests for `computeSpans` and
   `text(translation)`.

Items 1–5 are user-visible and belong in the release; 6–7 are hygiene. All of it is
done — in this order, roughly — except the splash row, the `drop(1)` row, and the
`text(translation)` test named in item 7, all three recorded as still open above. The
user-visible fixes are in `release-notes-1.5.1.md` under "**Also fixed (third review
pass)**". Item 3's deduplication landed as one shared `dayOutcome` plus shared
placeholder/unresolved composables rather than one host composable; the two routes now
differ only in what they do with a resolved day.

---

## Also worth knowing before the next pass

- **A test that calls `advanceUntilIdle()` makes the build look wedged.**
  `CalendarViewModel.startTodayTicker` sleeps to the next midnight and re-arms, so the
  shared test scheduler never idles and `advanceUntilIdle` spins in `T1` for minutes —
  jstack shows `TestCoroutineScheduler.advanceUntilIdle` burning CPU in the test
  worker, which reads exactly like a stuck daemon. `calendarTest` in
  `CalendarViewModelTest` cancels the ViewModel's scope before `runTest` drains, and
  `pump()` advances a bounded number of steps so a ticker that only ever waits 50 ms
  under fake time cannot run away. Do not "fix" this by exposing the ticker job from
  the ViewModel: `calendarTest` cancels `androidx.lifecycle.viewModelScope` in a
  `finally`, which is what the framework does on `onCleared`, and the test file says
  so at both ends.
- **`YearSourceFake` is deliberately not a pass-through.** It answers the archive
  bounds itself and never fetches an adjacent year, so no test can reach the network
  or the 62 MB of bundled assets by accident. If a change makes the ViewModel fetch a
  neighbour, the fake is what should change, not the ViewModel.
- **`TRIM_MEMORY_UI_HIDDEN` must stay a non-releasing level.** *(Still true — but `TRIM_MEMORY_BACKGROUND` must be a releasing one; pass 4.)* It is the one level that
  is not memory pressure; `MemoryTrimTest` asserts it releases nothing, so adding it
  to the releasing set now fails a test instead of silently costing a 17 MB re-decode.

---

## Checked and found correct (do not re-litigate)

- `AddReminderScreen.addCalendarEvent` — all-day event in UTC, `ActivityNotFoundException`
  handled. Correct as written.
- `AppUpdateGate.isOlder` — fail-open on unparseable versions, matches `PARITY.md`
  and the iOS implementation, and is tested.
- `CalendarRepository` `inFlight` cleanup belongs to the job, not the caller; the
  `LAZY` start plus `invokeOnCompletion` ordering is right.
- Text-pool failures are deliberately not cached (`textsPool`), so a transient
  failure retries rather than blanking the locale for the process.
- `FastingStyle.fastingStyle` ordering (water before oil) is correct for all seven
  pipeline values and tested; all four visual call sites now route through it.
- `MonthHeaderBar`/`CalendarGridScreen` weekday maths (`dayOfWeek` 0=Mon, Mon-first
  grid, `weekdayIndex = (dayOfWeek + 1) % 7`) is consistent with the Python-side data.
- `calendar_cache` is excluded from both `backup_rules.xml` and
  `data_extraction_rules.xml` (cloud and device-transfer), mirroring iOS.
- `keystore.properties` is gitignored and untracked; only
  `keystore.properties.example` is committed, and release builds stay unsigned
  rather than failing when it is absent.
- Material colour scheme is overridden in `Theme.kt` (no default-Material purple
  leaking into `TextField`/`TopAppBar`), and `LocalIsDarkTheme` correctly drives
  `AppColors` so the app theme setting is honoured rather than the system one.
- Bundled asset window is 2025–2030 for four locales (62 MB); `MIN_YEAR = 2024` /
  `MAX_YEAR = 2099` intentionally reach past the bundle into the v2 archive.
