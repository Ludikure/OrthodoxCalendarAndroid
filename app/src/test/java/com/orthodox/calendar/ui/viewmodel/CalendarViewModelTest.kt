package com.orthodox.calendar.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.orthodox.calendar.OrthodoxCalendarApp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.AppTheme
import com.orthodox.calendar.data.model.BibleTranslation
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.CalendarFile
import com.orthodox.calendar.data.model.Feast
import com.orthodox.calendar.data.model.FastingInfo
import com.orthodox.calendar.data.preferences.SettingsSource
import com.orthodox.calendar.data.repository.YearSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year

/**
 * The load state machine, against a fake year source.
 *
 * [YearSource] and [SettingsSource] exist so this file can be written at all:
 * every user-visible bug the calendar has had — February's header over January's
 * days, the blank app behind an unreadable preferences file, the detail screen
 * that spun forever for a year that cannot load — lives in this state machine,
 * and none of it was reachable without a `Context` and real assets.
 *
 * Two traps in the harness, both from the ViewModel owning a coroutine that
 * outlives every assertion it makes — read these before reaching for
 * `advanceUntilIdle()`, which is not used here on purpose:
 *
 *  1. [CalendarViewModel] runs a ticker that sleeps to the next midnight and
 *     re-arms itself when it wakes. On a scheduler this test shares with
 *     `Dispatchers.Main` the queue therefore never empties, so "run until idle"
 *     never returns — it spun at 100 % of one core for minutes and looked like a
 *     hung compiler. [pump] advances a bounded slice instead, one that passes a
 *     fake month's delay and stays a day short of the ticker's.
 *  2. `runTest` drains the scheduler after the test body, with the same "until
 *     idle" rule, so it would hang at the end of every test even with [pump].
 *     [calendarTest] therefore cancels each ViewModel's scope when its test ends,
 *     the way the framework does when a ViewModel is cleared. Write a new test
 *     through [calendarTest]; a bare `runTest` here hangs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalendarViewModelTest {

    private lateinit var mainDispatcher: CoroutineDispatcher

    @Before
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- The state machine -------------------------------------------------

    /**
     * The bug this pins: the header moved synchronously while the days arrived
     * later, so for the duration of a load the screen read "February" over
     * January's list, kept the old scroll offset, and a tap landed on a day the
     * user had not aimed at.
     */
    @Test
    fun `a month change clears the days it is about to replace`() = calendarTest { vm, _ ->
        goTo(vm, 1, 2024)

        vm.goToMonth(2, 2024)
        // The request is queued behind a delay; nothing has arrived yet.
        runCurrent()
        val during = vm.uiState.value
        assertTrue("a new month must show a spinner", during.isLoading)
        assertEquals("the header names the month the user asked for", 2, during.currentMonth)
        assertEquals("...and the loaded month is still the one on screen", 1, during.loadedMonth)
        assertTrue("January's days must not sit under a February header", during.daysInMonth.isEmpty())

        awaitLoaded(vm)
        assertEquals(2, vm.uiState.value.loadedMonth)
        assertFalse(vm.uiState.value.daysInMonth.isEmpty())
    }

    /**
     * The effect key the list uses to re-scroll. It used to be the day count, so
     * January (31) → March (31) produced the same key and scrolling to today
     * silently skipped every 31-day month.
     */
    @Test
    fun `months of equal length are different content`() = calendarTest { vm, _ ->
        goTo(vm, 1, 2024)
        val january = vm.uiState.value
        goTo(vm, 3, 2024)
        awaitLoaded(vm)
        val march = vm.uiState.value

        assertEquals(january.daysInMonth.size, march.daysInMonth.size)
        assertEquals("sr/2024/1", january.loadedContentKey)
        assertEquals("sr/2024/3", march.loadedContentKey)
    }

    /**
     * A month is either the one the header names or empty and under a spinner —
     * never the previous month's days. Asserted for every step of a walk across
     * a year, including across the year boundary.
     */
    @Test
    fun `walking a year never leaves one month's days under another's header`() =
        calendarTest { vm, _ ->
            vm.goToMonth(1, 2024)
            var month = 1
            var year = 2024
            // Two years' worth, so the walk crosses the year boundary the date
            // picker and the header both have to agree about.
            repeat(24) {
                awaitLoaded(vm)
                val settled = vm.uiState.value
                assertEquals("header month", month, settled.currentMonth)
                assertEquals("header year", year, settled.currentYear)
                assertEquals("the days must match the header", settled.currentMonth, settled.loadedMonth)
                assertEquals("the days must match the header", settled.currentYear, settled.loadedYear)
                assertTrue("month ${settled.currentMonth}-${settled.currentYear} loaded no days",
                    settled.daysInMonth.isNotEmpty())
                vm.goToNextMonth()
                month++; if (month > 12) { month = 1; year++ }
            }
        }

    // ---- Startup ----------------------------------------------------------

    /**
     * `DataStore.data` throws on a corrupt or unreadable preferences file. The
     * startup coroutine used to read it with a bare `first()`, so the exception
     * killed it before `localization` was set — and every screen stops while that
     * is null, which is a blank app with no retry and no way back.
     */
    @Test
    fun `an unreadable preferences file still opens the calendar`() = calendarTest(
        settings = SettingsSourceFake(failing = IOException("corrupt prefs"))
    ) { vm, _ ->
        awaitLoaded(vm)

        val state = vm.uiState.value
        assertNotNull("a blank app is what a null bundle produces", state.localization)
        assertEquals("a default is what a first launch gets", AppLanguage.SR, state.language)
        assertEquals(AppTheme.SYSTEM, state.theme)
        assertEquals(BibleTranslation.KJV, state.bibleTranslation)
        assertFalse(state.loadFailed)
        assertTrue("startup must still have loaded a month", state.daysInMonth.isNotEmpty())
    }

    /** The other half: a readable file wins over the defaults. */
    @Test
    fun `stored settings are applied at startup`() = calendarTest(
        settings = SettingsSourceFake(
            language = flowOf(AppLanguage.RU),
            theme = flowOf(AppTheme.DARK),
            bibleTranslation = flowOf(BibleTranslation.WEB)
        )
    ) { vm, _ ->
        awaitLoaded(vm)
        assertEquals(AppLanguage.RU, vm.uiState.value.language)
        assertEquals(AppTheme.DARK, vm.uiState.value.theme)
    }

    /**
     * A language switch used to leave the previous locale's saints, fasting labels
     * and reading references on screen under the new language's section headers for
     * the whole of the next load — the day JSON is per locale, so what was on screen
     * was not "still fine until the new month lands", it was Serbian saints under
     * Russian headings. Same clear as a month change; the locale is part of what
     * "which month is on screen" means.
     *
     * Asserted as an invariant over every step rather than as one snapshot: where
     * the bundle load suspends depends on the dispatcher, and the bug is not "a
     * snapshot at time T is stale", it is "there existed a moment when the screen
     * mixed two locales".
     */
    @Test
    fun `switching language clears the month it is about to replace`() = calendarTest { vm, _ ->
        goTo(vm, 5, 2024)
        assertEquals("sr", vm.uiState.value.loadedLocale)

        vm.forceReload(AppLanguage.RU)
        repeat(20) {
            runCurrent()
            val during = vm.uiState.value
            assertTrue(
                "never old-locale days under new-locale chrome: loaded=${during.loadedLocale}, asked=${during.language.code}",
                during.daysInMonth.isEmpty() || during.loadedLocale == during.language.code
            )
            pump()
        }
        awaitLoaded(vm)

        val after = vm.uiState.value
        assertEquals(AppLanguage.RU, after.language)
        assertEquals("ru", after.loadedLocale)
        assertEquals(5, after.loadedMonth)
        assertTrue("the new locale's month must still arrive", after.daysInMonth.isNotEmpty())
    }

    // ---- The archive bounds -------------------------------------------------

    /**
     * Stepping back from January 2024 used to move the header to December 2023,
     * load nothing — the archive starts in 2024 — and strand the user on an empty
     * month with no way out but the date picker.
     */
    @Test
    fun `the month before the archive is not offered`() = calendarTest { vm, years ->
        vm.goToMonth(1, CalendarArchive.MIN_YEAR)
        awaitLoaded(vm)

        vm.goToPreviousMonth()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(1, state.currentMonth)
        assertEquals(CalendarArchive.MIN_YEAR, state.currentYear)
        assertFalse("the first month of the archive has no previous", state.canGoPrevious)
        assertTrue("nothing may be fetched for a year that does not exist",
            years.requests.none { it.second < CalendarArchive.MIN_YEAR })
    }

    @Test
    fun `the month after the archive is not offered`() = calendarTest { vm, years ->
        vm.goToMonth(12, CalendarArchive.MAX_YEAR)
        awaitLoaded(vm)

        vm.goToNextMonth()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(12, state.currentMonth)
        assertEquals(CalendarArchive.MAX_YEAR, state.currentYear)
        assertFalse(state.canGoNext)
        assertTrue(years.requests.none { it.second > CalendarArchive.MAX_YEAR })
    }

    // ---- Failure states -----------------------------------------------------

    /**
     * "The archive has no such year" and "we are offline" used to be the same
     * nullable return, so both rendered as a spinner. They need different screens
     * — only one of them gets better on its own.
     */
    @Test
    fun `a year with no data fails as a failure, not as a connectivity problem`() =
        calendarTest { vm, years ->
            goTo(vm, 1, 2024)

            years.failure = YearSource.LoadError.NotFound
            vm.goToMonth(6, 2024)
            awaitLoaded(vm)

            val state = vm.uiState.value
            assertTrue(state.loadFailed)
            assertFalse("a missing year is not a dropped connection", state.isOffline)
            assertTrue(state.daysInMonth.isEmpty())
        }

    @Test
    fun `a dropped connection is reported as offline and recovers on retry`() =
        calendarTest { vm, years ->
            goTo(vm, 1, 2024)

            years.failure = YearSource.LoadError.Offline
            vm.goToMonth(6, 2024)
            awaitLoaded(vm)

            assertTrue(vm.uiState.value.isOffline)
            assertTrue(vm.uiState.value.daysInMonth.isEmpty())

            years.failure = null
            vm.retry()
            awaitLoaded(vm)

            val state = vm.uiState.value
            assertFalse(state.isOffline)
            assertFalse(state.loadFailed)
            assertEquals(6, state.loadedMonth)
        }

    // ---- Resolving one day --------------------------------------------------

    @Test
    fun `a day outside the month on screen is still found`() = calendarTest { vm, _ ->
        goTo(vm, 1, 2024)

        val outcome = vm.resolveDay("2024-07-15")
        assertTrue("a reachable day must resolve, its month or not: $outcome",
            outcome is DayOutcome.Found)
        assertEquals("2024-07-15", (outcome as DayOutcome.Found).day.gregorianDate)
    }

    /** The route argument comes from a deep link or a restored back stack. */
    @Test
    fun `a malformed date is no data, not a crash and not a spinner`() = calendarTest { vm, _ ->
        goTo(vm, 1, 2024)
        assertTrue(vm.resolveDay("07-07-2024") is DayOutcome.Missing.NoData)
        assertTrue(vm.resolveDay("") is DayOutcome.Missing.NoData)
    }

    /**
     * The bug this pins: `dayFor` returned null for a year that cannot load, the
     * detail screen read null as "still loading", and the user watched a spinner
     * that no amount of waiting would end.
     */
    @Test
    fun `an unresolvable day says so instead of spinning`() = calendarTest { vm, years ->
        goTo(vm, 1, 2024)

        years.failure = YearSource.LoadError.Offline
        assertTrue("offline must be reported as retryable",
            vm.resolveDay("2030-03-03") is DayOutcome.Missing.Offline)

        years.failure = YearSource.LoadError.NotFound
        assertTrue(vm.resolveDay("2030-03-03") is DayOutcome.Missing.NoData)
    }

    // ---- Fixtures -----------------------------------------------------------

    // ---- Today ---------------------------------------------------------------

    /** ON_START re-reads the date; see MainActivity. */
    @Test
    fun `a refresh publishes the date the wall clock says`() {
        var now = LocalDateTime.of(2026, 3, 10, 10, 0)
        calendarTest(wallClock = { now }) { vm, _ ->
            assertEquals("2026-03-10", vm.uiState.value.today)
            now = LocalDateTime.of(2026, 3, 11, 8, 0)
            vm.refreshToday()
            assertEquals("2026-03-11", vm.uiState.value.today)
        }
    }

    /**
     * Deep sleep, modelled: the wall clock moves and the scheduler does not —
     * `delay` on the main dispatcher runs on `uptimeMillis`, which stops in deep
     * sleep. The tick armed at launch is still most of a day of uptime from
     * firing when the phone wakes, so unless a refresh re-arms it from the wall
     * clock, the next midnight passes unnoticed with the app open in the hand.
     */
    @Test
    fun `a refresh re-arms the midnight tick from the wall clock`() {
        var now = LocalDateTime.of(2026, 3, 10, 0, 0, 5)
        calendarTest(wallClock = { now }) { vm, _ ->
            awaitLoaded(vm) // launch load done; the launch tick is ~24 h of scheduler time away

            now = LocalDateTime.of(2026, 3, 11, 23, 59, 0) // asleep all day: no scheduler time passed
            vm.refreshToday()
            assertEquals("2026-03-11", vm.uiState.value.today)

            now = LocalDateTime.of(2026, 3, 12, 0, 0, 2) // midnight, with the app open and awake
            advanceTimeBy(61_000)
            runCurrent()
            assertEquals("2026-03-12", vm.uiState.value.today)
        }
    }

    /** The tick still does its own job while the phone stays awake. */
    @Test
    fun `the midnight tick moves today while the app stays open`() {
        var now = LocalDateTime.of(2026, 3, 10, 23, 59, 0)
        calendarTest(wallClock = { now }) { vm, _ ->
            awaitLoaded(vm)
            assertEquals("2026-03-10", vm.uiState.value.today)
            now = LocalDateTime.of(2026, 3, 11, 0, 0, 2)
            advanceTimeBy(61_000)
            runCurrent()
            assertEquals("2026-03-11", vm.uiState.value.today)
        }
    }

    /**
     * A ViewModel under test, disposed before the test ends.
     *
     * The disposal is the point — see trap 2 in the class comment. Every test in
     * this file goes through here rather than calling [runTest] itself.
     */
    private fun calendarTest(
        settings: SettingsSource = SettingsSourceFake(),
        years: YearSourceFake = YearSourceFake(),
        wallClock: () -> LocalDateTime = { LocalDateTime.now() },
        block: suspend TestScope.(CalendarViewModel, YearSourceFake) -> Unit
    ) = runTest {
        val vm = viewModel(years, settings, wallClock)
        try {
            block(vm, years)
        } finally {
            // What the framework does when a ViewModel is cleared, and what the
            // alternative is: the midnight ticker stays queued on this scheduler
            // for the next 24 hours of virtual time, and `runTest`, which drains
            // the scheduler on the way out, waits for it.
            vm.viewModelScope.cancel()
        }
    }

    private fun viewModel(
        years: YearSource,
        settings: SettingsSource,
        wallClock: () -> LocalDateTime
    ): CalendarViewModel {
        val app = RuntimeEnvironment.getApplication() as OrthodoxCalendarApp
        return CalendarViewModel(
            app,
            years = years,
            settings = settings,
            wallClock = wallClock,
            // The season spans run on the test scheduler instead of a real
            // Dispatchers.Default thread, so nothing here waits on a real thread.
            computeDispatcher = mainDispatcher
        )
    }

    /** Loads a month and waits for it to settle. */
    private fun TestScope.goTo(vm: CalendarViewModel, month: Int, year: Int) {
        vm.goToMonth(month, year)
        awaitLoaded(vm)
    }

    /**
     * Moves virtual time along far enough for one month's load without ever
     * asking whether the scheduler is idle — trap 1 in the class comment. Ten
     * steps of 10 ms pass the fake's 50 ms month delay and leave the ticker's
     * 24 h well ahead of the clock.
     */
    private fun TestScope.pump() {
        repeat(10) { advanceTimeBy(10) }
    }

    /**
     * Pumps until the load settles. Everything the load touches — the fake's
     * delay, the settings flows, the season spans (the injected compute
     * dispatcher) — runs on the test scheduler, so a bounded number of pumps
     * either settles it or proves it never will. There is no real thread to wait
     * for, so no wall-clock deadline and no sleeping: this used to poll a real
     * Dispatchers.Default with Thread.sleep, which works until a slow CI machine
     * makes it flake.
     */
    private fun TestScope.awaitLoaded(vm: CalendarViewModel) {
        repeat(50) {
            pump()
            val state = vm.uiState.value
            // `loadedMonth` is 0 until something has arrived, so this is not the
            // initial state pretending to be a finished load.
            val settled = !state.isLoading &&
                (state.loadedMonth != 0 || state.loadFailed || state.isOffline)
            if (settled) return
        }
        fail("month never finished loading")
    }

    /**
     * Deterministic years with no gaps: [YearSource.loadMonth] and [load] agree,
     * every day has a primary feast named after its date, and nothing carries a
     * fasting season so no adjacent year is fetched behind the tests' backs.
     *
     * Bounded like the real archive rather than generating any year asked for:
     * "2030-03-03" is meant to be unreachable here, exactly as it is on a phone
     * after the archive ends, so the failure paths test the failure and not a
     * fixture that quietly handed over data.
     */
    private class YearSourceFake : YearSource {

        /** (locale, year, month) in the order asked for. */
        val requests = mutableListOf<Triple<String, Int, Int>>()

        /** Thrown by every read while set — the failure-path tests flip this. */
        var failure: Throwable? = null

        /** Keeps a load suspended so the state *during* it can be observed. */
        var monthDelayMillis: Long = 50

        private val files = mutableMapOf<Pair<String, Int>, CalendarFile>()

        override suspend fun loadMonth(locale: String, year: Int, month: Int): List<CalendarDay> {
            requests += Triple(locale, year, month)
            if (monthDelayMillis > 0) delay(monthDelayMillis)
            failure?.let { throw it }
            val file = file(locale, year) ?: throw YearSource.LoadError.NotFound
            return file.days.values.filter { day ->
                day.gregorianDate.substring(5, 7).toInt() == month
            }
        }

        override suspend fun load(locale: String, year: Int, allowNetwork: Boolean): CalendarFile {
            failure?.let { throw it }
            return file(locale, year) ?: throw YearSource.LoadError.NotFound
        }

        /** The archive's years only; [CalendarArchive.MIN_YEAR]–[MAX_YEAR]. */
        private fun file(locale: String, year: Int): CalendarFile? {
            if (year < CalendarArchive.MIN_YEAR || year > CalendarArchive.MAX_YEAR) return null
            return files.getOrPut(locale to year) {
                val days = (0 until Year.of(year).length()).map { offset ->
                    val date = LocalDate.of(year, 1, 1).plusDays(offset.toLong())
                    day(date, locale)
                }
                CalendarFile(
                    year = year,
                    locale = locale,
                    generatedBy = "test",
                    days = days.associateBy { it.gregorianDate }
                )
            }
        }

        private fun day(date: LocalDate, locale: String) = CalendarDay(
            gregorianDate = date.toString(),
            julianDate = date.toString().substring(5),
            dayOfWeek = date.dayOfWeek.value - 1,
            paschaDistance = 0,
            feasts = listOf(
                Feast(
                    name = "${locale.uppercase()} saint of ${date}",
                    importance = "normal",
                    displayRole = "primary",
                    type = "saint"
                )
            ),
            fasting = FastingInfo(type = "free", label = "", explanation = ""),
            fastingPeriod = null
        )
    }

    private class SettingsSourceFake(
        language: Flow<AppLanguage> = flowOf(AppLanguage.SR),
        theme: Flow<AppTheme> = flowOf(AppTheme.SYSTEM),
        bibleTranslation: Flow<BibleTranslation> = flowOf(BibleTranslation.KJV),
        /** Every read fails, the way a corrupt preferences file fails. */
        private val failing: Throwable? = null
    ) : SettingsSource {

        // Typed by the `val`, never inferred from the failing branch: that branch
        // throws instead of emitting, so there is nothing there to infer from.
        override val languageFlow: Flow<AppLanguage> =
            if (failing == null) language else flow { throw failing }
        override val themeFlow: Flow<AppTheme> =
            if (failing == null) theme else flow { throw failing }
        override val bibleTranslationFlow: Flow<BibleTranslation> =
            if (failing == null) bibleTranslation else flow { throw failing }

        val written = mutableListOf<String>()

        override suspend fun setLanguage(language: AppLanguage) {
            written += "language=${language.code}"
        }

        override suspend fun setTheme(theme: AppTheme) {
            written += "theme=$theme"
        }

        override suspend fun setBibleTranslation(translation: BibleTranslation) {
            written += "translation=${translation.code}"
        }
    }
}
