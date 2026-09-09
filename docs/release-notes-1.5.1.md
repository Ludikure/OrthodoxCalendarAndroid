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
• Saint lives now appear under the saint they belong to — many were paired with the wrong commemoration.
• English (New Calendar) is corrected: feasts and fasts fall on their proper dates.
• All years 2024–2099; 2025–2030 work fully offline.
• Fixed fasting colours in the month grid, the "Great Feast" label appearing in Serbian, and a blank screen when opening a day from search.
• The date picker now opens the day you tap.
• The app no longer asks for calendar permissions.
```

## Serbian (sr) — Cyrillic

```
• Житија светих сада стоје уз правог свеца — многа су била уз погрешан спомен.
• Исправљен енглески нови календар: празници и постови падају на тачне датуме.
• Све године 2024–2099; године 2025–2030 раде потпуно офлајн.
• Исправљене боје поста у мрежном приказу и празан екран при отварању дана из претраге.
• Бирач датума сада отвара дан који додирнете.
• Апликација више не тражи приступ календару.
```

## Russian (ru)

```
• Жития святых теперь стоят рядом со своим святым — многие были привязаны к чужой памяти.
• Исправлен английский новый календарь: праздники и посты приходятся на верные даты.
• Все годы с 2024 по 2099; годы 2025–2030 работают полностью офлайн.
• Исправлены цвета поста в сетке месяца и пустой экран при открытии дня из поиска.
• Выбор даты теперь открывает выбранный день.
• Приложение больше не запрашивает доступ к календарю.
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
