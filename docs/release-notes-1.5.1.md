# Release notes — 1.5.1 (versionCode 15)

Supersedes `release-notes-1.5.0.md`, which described version 1.5.0 (vc 14). That
artifact was never uploaded — versionCode 15 replaces it, and the content below
covers considerably more.

**Production is on 1.4.2 (vc 11).** 1.4.3 (vc 12) went to internal/closed testing
and 1.5.0 (vc 13) to open testing, so everything since 1.4.2 is new to production
users. The versionName moves rather than only the versionCode this time: the
bundled calendar data was regenerated and the saint-biography matching rewritten,
so this is not the same build open testing has.

**Release name (Play Console, internal only, ≤50 chars):**
```
1.5.1 (15) — Correct saints and calendars
```

Play's "What's new" limit is **500 characters per language**. All three below are
within it.

---

## English (en-US)

```
• Fasting follows each church's own calendar, day by day (SPC, Russian Church, ROCOR, OCA).
• Saint lives appear under the right saint; saints and moving commemorations fall on their proper days every year, leap years included.
• English (New Calendar): feasts and fasts on their proper dates.
• All years 2024–2099; 2025–2030 work offline.
• Fixed fasting colours, the "Great Feast" label and a blank screen from search.
• The date picker opens the day you tap.
• No calendar permission needed.
```

## Serbian (sr) — Cyrillic

```
• Пост прати календар СПЦ дан по дан.
• Житија стоје уз правог свеца; свеци, Задушнице, Детињци, Материце и Оци падају на прави дан сваке године, и у преступним.
• Исправљен енглески нови календар: празници и постови на тачним датумима.
• Све године 2024–2099; 2025–2030 раде офлајн.
• Исправљене боје поста и празан екран при отварању дана из претраге.
• Бирач датума отвара дан који додирнете.
• Апликација више не тражи приступ календару.
```

## Russian (ru)

```
• Пост следует календарю Русской Церкви день за днём.
• Жития стоят у своего святого; святые и переходящие памяти приходятся на верный день каждого года, в том числе високосного.
• Исправлен английский новый календарь: праздники и посты на верных датах.
• Все годы 2024–2099; 2025–2030 работают офлайн.
• Исправлены цвета поста и пустой экран при открытии дня из поиска.
• Выбор даты открывает нужный день.
• Приложение не запрашивает доступ к календарю.
```

---

## Everything in this release (not for the store listing)

Since 1.4.2, the last version production users have:

**Calendar data**
- Saint biographies rebuilt from their sources for all three languages and matched
  to the right feast. Around 166 English entries a year were showing another
  saint's life; the Serbian pool had titles and text mixed together.
- English (New Calendar) carried the Old Calendar's saints with the Revised great
  feasts pasted on top, so every fixed great feast appeared twice, thirteen days
  apart, and Christmas fell inside the Nativity Fast. Its saints, fasts and
  readings now all sit on Revised Julian dates.
- Every year 2024–2099 available; 2025–2030 bundled offline, the rest downloaded
  once and kept.

**Fixed**
- Fasting colours in the month grid painted fast days as oil days.
- The "Great Feast" label was always Serbian, in every language.
- Opening a day from search or the date picker could show a blank screen with no
  way back.
- The date picker's day grid ignored which day you tapped.
- Reminders could be filed a day early in time zones east of UTC, and could crash
  the app on a device with no calendar app.
- The month arrows walked past 2024 and 2099 into empty months.
- A year with no data blamed your internet connection.
- The fasting banner could show a day count from a season split across a year
  boundary.
- Search no longer stutters while typing.
- Russian dates used the heading form of the month after a day number — "19 Декабрь",
  "2 Май". They read "19 декабря", "2 мая" now: the day header, search results,
  reminders, shared text, the month grid's day card and the fasting banner
  ("15 мар – 1 мая"). The genitive names are a new optional `monthsGenitive` key in
  `ru.json`, shared with iOS.

**Also fixed (second review pass)**
- Scripture readings now name the service they belong to — Vespers, Matins, the
  Hours, the numbered Matins and Passion gospels. The data always carried it, in
  English; it is translated per language now. A quarter of Serbian readings and a
  third of English ones gain a label that was previously blank.
- Readings outside the three usual kinds no longer print the word "OTHER" above a
  title that already names them (the odes of the Great Canon).
- The last day of the month and the fasting legend no longer sit under the
  gesture bar.
- Rotating the phone no longer clears a forced-update screen, and a malformed
  version number can no longer trigger one.
- The About screen no longer crashes on a device with no browser.
- A failed load of a language's text pool is retried instead of leaving every
  saint life and reading blank until the app restarts.
- A cancelled month swipe no longer causes the same year to be downloaded twice.

**Also fixed (third review pass)**
- Navigating to a month no longer shows the previous month's days under the new
  heading. The header used to change immediately while the days arrived a moment
  later: the screen read "February" over January, opened at the old scroll
  position, and a tap landed on a day you had not aimed at. Switching language had
  the same problem — Serbian saints under Russian headings until the new month
  landed — and now clears too.
- Scrolling to today works in every month again. The list keyed that re-scroll on
  the number of days in the month, so January → March, 31 days each, skipped it.
- A settings file the app cannot read no longer opens a blank app. The defaults are
  used instead, which is what a first launch gets.
- A day that cannot be loaded says so instead of spinning for ever. "The archive
  has no such year" and "you are offline" were the same blank, and both looked
  like patience; only one of them gets better on its own, and that one now offers
  a retry that works.
- Opening Jan 1 or Dec 31 from search or the date picker keeps the fasting-season
  badge the month list shows for the same day. The Nativity Fast runs across the
  year boundary every single year.
- Sharing a day on a device with nothing that accepts shared text says so instead
  of closing the app.
- The month-grid legend names all five fasting colours now. Water days were tinted
  with a colour nothing explained.
- The "today" ring and the fasting banner's day count move to the new day at
  midnight, and when you come back to the app after the phone slept through it —
  they used to point at yesterday until the month was reloaded.
- Rotating the phone in month-grid view keeps the day you had selected instead of
  dropping back to today.
- No white flash under the splash, and no pale launch preview in Recents, when the
  app theme is DARK while the system stays light.
- Browsing many years no longer keeps every year visited decoded in memory.

  *Not folded into the store text above: the English block is at 477 of the 500
  allowed characters, and these are refinements of what it already claims — the
  "blank screen when opening a day" line now holds for the whole app, not just the
  route it was written about.*

**Under the hood (third review pass)**
- The archive's bounds and the rule for when the month chevrons stop live in one
  `CalendarArchive` object instead of four hand-written comparisons that agreed only
  by care.
- Fasting colours are mapped in one place instead of four private copies that had
  already begun to differ.
- The in-memory year cache is a bounded LRU, the disk cache keeps the most
  recently used downloaded years, and decoded years are released at
  `TRIM_MEMORY_BACKGROUND` — on Android 14 and later the only memory signal an app
  is still sent. Not on `TRIM_MEMORY_UI_HIDDEN`, which only means the user left
  the app and would cost them a re-decode on return.
- `YearSource` and `SettingsSource` interfaces, so the load state machine — where
  this app's user-visible bugs have all been — can be driven from a test instead of
  from a real device and real assets.
- Test suite: 74 JVM tests, about 2 s of test time, no device involved. New this
  pass — 51 of them — are the load state machine (13, Robolectric), the archive
  bounds, the midnight refresh, the bounded cache, the guarded preferences read, the
  fasting-season spans and the `onTrimMemory` levels. `AppUpdateGateTest`,
  `BioMatcherTest`, `FastingStyleTest`, `ReadingLabelsTest` and `ScriptFoldingTest`
  were already here.

**Fasting and saints (bundled data regenerated)**
- Serbian fasting now follows the SPC's own calendar (pravoslavno.rs, "Календар
  поста") day by day. It used to relax almost every strict day to oil — a "bold"
  saint upgraded the day, and 341 of 365 days carry one — so Great Lent weekdays
  showed oil; ordinary Wednesdays and Fridays too. About 99 days a year change. The
  Beheading of St John, which showed fish (or no fast at all on most weekdays), is
  dry eating now.
- Russian and English fasting follow the calendar each locale's saints come from:
  Russian days.pravoslavie.ru, English holytrinityorthodox.com (ROCOR), English (New
  Calendar) orthocal.info (the OCA). One shared set of rules served all three and
  disagreed with each on about a fifth of its days — oil on every ordinary Wednesday
  and Friday, where the Russian calendars allow fish in winter and the Paschal season
  and the OCA keeps a strict fast; fish on Tuesdays and Thursdays late in the Nativity
  Fast; one Great Lent for all three. The rules are now derived from each calendar
  (iOS repo: `scripts/shared/derive_fasting.py`, checked in CI). A year left out of the
  derivation is predicted at 95.9–98.6% (Russian), 96.4–99.7% (English) and 99.45–100%
  (New Calendar). About 65–100 days a year change per locale.
- The sentence under the Russian fasting level was days.pravoslavie.ru's 2026 text for
  the same date, repeated in every year: it contradicted the level beside it on 239
  days of 2027. It is the engine's own now.
- Leap years: from February 29 to March 12 every day showed the previous church day's
  saints and February 29 was blank (English New Calendar: February 16–28), and the
  lives went with them. Saints are read by church date now, and St John Cassian has
  his own February 29.
- About a hundred commemorations the sources print on their 2026 date — Задушнице,
  Детињци, Материце, Оци, Parents' Saturdays, the Sundays and Saturdays around the
  great feasts, the Paschal leave-takings, regional synaxes kept on a Sunday — stayed
  on that date in every year. They are placed by rules checked against the sources'
  own other years; five that no source pins down are dropped rather than guessed.
- Serbian month headings glued to the first saint of each month ("ФЕБРУАР – …") and
  English service rubrics listed as saints are gone.
- Dry eating was abbreviated "water" — the same as hot food without oil — in every
  language. It reads "суво" / "сухо" / "dry" now.
- Readings and great feasts are unchanged, and the text pools hold the same entries
  (now written in sorted order).

**Fourth review pass — corrections before release** *(internal: none of this
reached users, so none of it belongs in the store text)*
- The third pass left `CalendarViewModel` without the `(Application)` constructor
  that `viewModel()` builds it through, so every launch crashed ("Cannot create an
  instance of class CalendarViewModel", reproduced on an API 37 emulator) while all
  74 tests passed — each of them built the ViewModel directly. Fixed with
  `@JvmOverloads`; `ViewModelFactoryTest` now builds it through the activity's own
  factory. It never shipped.
- The memory release it added never ran on Android 14+: `TRIM_MEMORY_BACKGROUND` was
  left out as "the same value as UI_HIDDEN" — it is 40, not 20 — and from API 34 the
  other levels are no longer delivered. The note above is now true.
- Its midnight refresh was a coroutine delay, which runs on uptime and stops in deep
  sleep. The date is now also re-read, and the tick re-armed, on every ON_START.
  The note above is now true.
- The disk trim kept the highest year numbers rather than the most recently used:
  2024 was deleted straight after every download once twelve later years were
  cached. A revision change could also still re-insert a superseded year into
  memory; the in-flight clear meant to prevent it did not. A generation check does.
- Withdrew a note that Turkish/Azerbaijani search had been fixed: it was never
  broken. Kotlin's no-argument `lowercase()` is locale-invariant.
- Test suite: 92 JVM tests. The 18 new ones cover the ViewModel factory, the disk
  cache and its invalidation, which trim levels a current device delivers, the
  midnight tick across deep sleep, and the locale premise. The ViewModel tests no
  longer poll a real thread.

**Under the hood**
- `READ_CALENDAR`/`WRITE_CALENDAR` removed — the reminder flow hands off to the
  calendar app by intent and never needed them. Worth a note in the Play data
  safety form if the previous declaration is still there.
- One repository per process instead of two, each of which held the 17 MB Russian
  text pool.
- Dates formatted with a fixed locale, so the "today" highlight no longer
  disappears under a non-Gregorian device locale.
- Search, settings and add-reminder are real buttons with accessibility labels;
  so are the month arrows and the list/grid toggle, and "Back"/"Share" are no
  longer announced in English inside a Serbian or Russian UI.
- CI builds the release APK (R8) as well as debug.
- `PaschaCalculator`/`JulianConverter` deleted — no callers, and a Julian offset
  hardcoded for 1900–2099 in an app that offers years to 2099.
