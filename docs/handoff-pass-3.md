# Handoff — pass-3 fixes: done

> **Pass 4 (2026-09-11) supersedes parts of this file.** It found a launch crash this
> pass introduced and three claims that did not hold; see "Corrections" in
> `docs/code-review-pass-3.md`. The suite is now 92 tests.

Date: 2026-09-11. Supersedes the earlier version of this file, which said the
session ended because a build hung. It did not hang: one test made the test task
look hung. Read "The hang, and what it actually was" before touching the test
harness; everything else here is bookkeeping.

## The hang, and what it actually was

The compiler was never involved. `./gradlew compileDebugUnitTestKotlin --offline`
finished in 5 s at the commit where `testDebugUnitTest` had been spinning for
minutes. The spin was inside the test worker:

`CalendarViewModel.startTodayTicker` sleeps `millisUntilNextMidnight(now) + 1` and
re-arms when it wakes, so on the `TestCoroutineScheduler` that `runTest` shares with
`Dispatchers.Main` the queue never empties. `advanceUntilIdle()` therefore never
returns — jstack showed `TestCoroutineScheduler.advanceUntilIdle` burning CPU in one
thread, which is indistinguishable from a wedged daemon or a compiler stuck on an
inference variable. `runTest` also drains the scheduler on the way out, with the same
"until idle" rule, so a test that never touches `advanceUntilIdle` can still hang at
the *end* of the body.

The fix, all of it in `CalendarViewModelTest`:

* `calendarTest { vm, years -> … }` wraps `runTest` and cancels
  `androidx.lifecycle.viewModelScope` in a `finally` — what the framework does on
  `onCleared`. Every test in the file goes through it; a bare `runTest` there hangs.
* `pump()` advances a bounded slice (ten steps of 10 ms: past the fake month's 50 ms
  delay, a day short of the ticker's) instead of asking whether the scheduler is idle.
* Do **not** expose the ticker job from the ViewModel to make it cancellable. The
  scope is already cancellable and that is the production contract too.

`SettingsSourceFake`'s `flow { throw failing }` is typed by its `val` declaration, so
there is nothing to infer from the throwing branch. That was the earlier
misdiagnosis; it cost three killed builds and changed nothing.

## Verified

| Command | Result |
|---|---|
| `./gradlew testDebugUnitTest --offline --no-daemon` | **BUILD SUCCESSFUL**, 74 tests, 0 failures, 0 skipped, 13 classes, ~2 s of test time |
| `./gradlew :app:lintDebug --no-daemon` | BUILD SUCCESSFUL, zero errors |
| `./gradlew :app:assembleDebug --no-daemon` | BUILD SUCCESSFUL, `app/build/outputs/apk/debug/app-debug.apk` |

Run them through `/tmp/gb.sh <budget_seconds> <log> <gradle args…>` — it dumps every
Java thread dump and kills the build if it outlives the budget. Keep doing that: the
suite is fast now, but the failure mode when it is not is an unbounded spin.

Class counts: `CalendarViewModelTest` 13, `FastingPeriodSpansTest` 8,
`ReadingLabelsTest` 7, `BoundedCacheTest` 6, `MemoryTrimTest` 6,
`CalendarArchiveTest` 5, `BioMatcherTest` 5, `MidnightTest` 5, `ScriptFoldingTest` 5,
`GuardedReadTest` 4, `IsoDateTest` 4, `AppUpdateGateTest` 3, `FastingStyleTest` 3.
Robolectric 4.14.1 runs offline under `@Config(sdk = [34])`; the merged debug assets
(`localization/*.json`) are visible to it, which is why the ViewModel tests can load a
real bundle.

## Where the findings stand

`docs/code-review-pass-3.md` is annotated item by item. Everything in it is fixed
except two things, both marked there and both deliberate:

* the in-app splash is still timed rather than gated on data readiness;
* `day.feasts.drop(1)` in the share text — verified safe against the data, left alone.

Docs are folded: `docs/release-notes-1.5.1.md` has "**Also fixed (third review
pass)**" and "**Under the hood (third review pass)**"; `PARITY.md` records the
store-URL/`market://` divergence, the guarded `startActivity` behaviour, and the
five-bucket fasting legend (iOS still names four and tints the fifth — port it
there rather than removing it here).

The store text in the release notes was **not** extended: the English block is at 477
of Play's 500 characters, and the third-pass fixes refine claims already in it.

## Still worth doing, in order

1. `CalendarRepository.loadMonth` month filtering (`monthKeyPrefix`) and
   `poolName("en_nc") == "en"` have no test. `YearSource` makes the first easy; the
   second is a parity contract resting on a comment that the two pools were
   byte-identical.
2. `ScriptureReading.text(translation)` — the WEB→KJV chain, including OT readings
   where `textWeb` is null.
3. Play Console bookkeeping from the earlier passes: the `READ_CALENDAR`/
   `WRITE_CALENDAR` declaration in data safety if it is still there, and the release
   signing (`keystore.properties` is untracked by design, release builds stay unsigned
   without it).
4. On a device: the two things JVM tests cannot see — the night window background
   under a DARK app theme on a light system, and the share/reminder intents.

## Rules that still apply

* Never `advanceUntilIdle()` in this module. Bounded `pump()` and a cancelled scope.
* `TRIM_MEMORY_UI_HIDDEN` is not memory pressure; `MemoryTrimTest` fails if it becomes
  a releasing level. A reviewer will want to add it. Don't.
  `TRIM_MEMORY_BACKGROUND` is the opposite case and must stay a releasing level: on
  API 34+ it and UI_HIDDEN are the only levels delivered, so without it the release
  never runs. (Pass 4 — this file originally left it out.)
* `YearSourceFake` answers the archive bounds itself and never fetches an adjacent
  year, so no test reaches the network or the 62 MB of assets by accident. If the
  ViewModel ever starts fetching a neighbour, change the fake, not the ViewModel.
* Gradle: `--offline --no-daemon` and a timeout on the shell call. `--rerun-typed` is
  not an option (`Unknown command-line option`); it is `--rerun-tasks`.
