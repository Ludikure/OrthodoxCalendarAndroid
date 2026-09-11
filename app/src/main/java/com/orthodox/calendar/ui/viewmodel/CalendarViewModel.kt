package com.orthodox.calendar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orthodox.calendar.data.localization.LocalizationManager
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.AppTheme
import com.orthodox.calendar.data.model.BibleTranslation
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.FastingPeriodInfo
import com.orthodox.calendar.data.model.FastingPeriods
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.data.preferences.AppPreferences
import com.orthodox.calendar.data.preferences.SettingsSource
import com.orthodox.calendar.data.preferences.firstOrDefault
import com.orthodox.calendar.data.repository.YearSource
import com.orthodox.calendar.OrthodoxCalendarApp
import com.orthodox.calendar.ui.util.millisUntilNextMidnight
import com.orthodox.calendar.ui.util.toIsoDate
import com.orthodox.calendar.ui.util.parseIsoDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

enum class ViewMode { LIST, GRID }

/**
 * The bounds of the archive and the one rule derived from them: the calendar
 * stops at January 2024 going back and December 2099 going forward.
 *
 * The rule used to be hand-written in four places — the header's chevrons, the
 * ViewModel's navigation guards, and both modes of the date picker — where each
 * restated `year <= MIN && month <= 1` in its own way. They agreed when the
 * review found them; that is not the same as being unable to drift.
 */
object CalendarArchive {
    /** Bounds of the v2 archive on the Worker (2024-2099; the pipeline's
     *  Julian+13 date math holds through 2099). Years inside the bundled
     *  window load offline; the rest download once and cache on device. */
    const val MIN_YEAR = 2024
    const val MAX_YEAR = 2099

    /** False at January [MIN_YEAR] — stepping before it loads nothing and
     *  strands the user on an empty month. */
    fun canGoPrevious(month: Int, year: Int): Boolean = year > MIN_YEAR || month > 1

    /** False at December [MAX_YEAR]. */
    fun canGoNext(month: Int, year: Int): Boolean = year < MAX_YEAR || month < 12
}

data class CalendarUiState(
    val currentMonth: Int = LocalDate.now().monthValue,
    val currentYear: Int = LocalDate.now().year,
    val daysInMonth: List<CalendarDay> = emptyList(),
    /** Fasting-season info per `gregorianDate`, computed across the loaded year. */
    val fastingPeriods: Map<String, FastingPeriodInfo> = emptyMap(),
    val viewMode: ViewMode = ViewMode.LIST,
    val isLoading: Boolean = false,
    /** The last load failed for a reason other than connectivity — the archive
     *  has no such year. The failure view composes its own localized message
     *  from this and [isOffline]; the state carries no user-facing text. */
    val loadFailed: Boolean = false,
    val isOffline: Boolean = false,
    /**
     * Which (locale, month, year) [daysInMonth] actually holds.
     *
     * `currentMonth`/`currentYear`/`language` say what the user asked for; these
     * say what arrived. Keeping them apart is what makes "the header reads
     * February while the list shows January" impossible instead of a race: a
     * load clears the days it is about to replace, so the list is either the
     * month named above it or empty and under a spinner.
     */
    val loadedLocale: String = "",
    val loadedMonth: Int = 0,
    val loadedYear: Int = 0,
    val language: AppLanguage = AppLanguage.SR,
    val theme: AppTheme = AppTheme.SYSTEM,
    val bibleTranslation: BibleTranslation = BibleTranslation.KJV,
    val localization: LocalizationBundle? = null,
    /**
     * Today as `yyyy-MM-dd`, owned by the ViewModel rather than read during
     * composition.
     *
     * `LocalDate.now()` in a composable is fixed at that composition, so the
     * "today" ring and the fasting banner's "is today in this month" branch went
     * stale across midnight until the month was reloaded — the banner in
     * particular kept showing a day count for the day before. Refreshed by a
     * timer that sleeps to the next midnight and on every load.
     */
    val today: String = LocalDate.now().toIsoDate(),
    val scrollToTodayTrigger: Boolean = false
) {
    /**
     * Identity of the day list on screen, for use as an effect key. It changes
     * exactly when a month's days arrive — `days.size`, the old key, did not:
     * January and March have the same length, so scrolling to today silently
     * skipped every third month.
     */
    val loadedContentKey: String get() = "$loadedLocale/$loadedYear/$loadedMonth"

    val canGoPrevious: Boolean get() = CalendarArchive.canGoPrevious(currentMonth, currentYear)
    val canGoNext: Boolean get() = CalendarArchive.canGoNext(currentMonth, currentYear)
}

/**
 * What a request for one specific day turned out to be.
 *
 * `dayFor` returned `CalendarDay?`, so "still loading" and "this year cannot be
 * loaded — the archive has no such year, or we are offline" were the same value
 * and the detail route rendered both as an endless spinner with no way out. The
 * third case gets its own screen now.
 */
sealed interface DayOutcome {
    data class Found(val day: CalendarDay, val periodInfo: FastingPeriodInfo?) : DayOutcome
    /** Not resolved *yet*; the spinner is honest and a result is coming. */
    data object Loading : DayOutcome
    /** Not resolvable, and no retry will change that unless the reason says so. */
    sealed interface Missing : DayOutcome {
        /** The archive has no such year. */
        data object NoData : Missing
        /** Could not be fetched — retrying once there is a connection works. */
        data object Offline : Missing
    }
}

/**
 * `@JvmOverloads` is load-bearing. MainActivity obtains this through
 * `viewModel()`, whose default factory looks up an `(Application)` constructor by
 * reflection, and Kotlin emits no such overload for a constructor with default
 * arguments unless asked to. Without it every launch threw "Cannot create an
 * instance of class CalendarViewModel" while every test passed — the tests call
 * this constructor directly. `ViewModelFactoryTest` builds it the way the app does.
 */
class CalendarViewModel @JvmOverloads constructor(
    application: Application,
    private val years: YearSource = (application as OrthodoxCalendarApp).repository,
    private val settings: SettingsSource = AppPreferences(application),
    private val localizationManager: LocalizationManager = LocalizationManager(application),
    /** What time it is on the wall; "today" comes from here. A function rather
     *  than a fixed `Clock` so a time-zone change is picked up the way
     *  `LocalDateTime.now()` picks it up; injectable so tests can move it. */
    private val wallClock: () -> LocalDateTime = { LocalDateTime.now() },
    /** Where the fasting-season spans are computed: they walk a year of days. */
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default
) : AndroidViewModel(application) {

    /** In-flight month/year load; cancelled when a newer navigation starts so
     *  rapid year-switching can't stack concurrent ~50 MB streamed-year loads. */
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(
        wallClock().toLocalDate().let { date ->
            CalendarUiState(
                currentMonth = date.monthValue,
                currentYear = date.year,
                today = date.toIsoDate()
            )
        }
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    /** The midnight tick; [startTodayTicker] replaces it rather than stacking a
     *  second. Declared above `init`, which starts it: Kotlin runs initializers
     *  and init blocks in source order, so declared below, this `= null` would
     *  run after init and orphan the tick already running. */
    private var tickerJob: Job? = null

    init {
        viewModelScope.launch {
            // Guarded reads: a corrupt or unreadable preferences file used to
            // kill this coroutine at its first `first()`, so `localization`
            // stayed null — and every screen stops while it is null, which is a
            // blank app with no retry and no way back. Defaults are the correct
            // answer to an unreadable file; it is what a first launch gets.
            val lang = settings.languageFlow.firstOrDefault(AppLanguage.SR)
            val theme = settings.themeFlow.firstOrDefault(AppTheme.SYSTEM)
            val bibleTranslation =
                settings.bibleTranslationFlow.firstOrDefault(BibleTranslation.KJV)
            val bundle = localizationManager.loadBundle(lang)
            _uiState.update {
                it.copy(
                    language = lang,
                    theme = theme,
                    bibleTranslation = bibleTranslation,
                    localization = bundle
                )
            }
            loadMonth()
        }
        startTodayTicker()
    }

    fun loadMonth() {
        val state = _uiState.value
        loadData(state.language.code, state.currentMonth, state.currentYear)
    }

    fun forceReload(language: AppLanguage) {
        viewModelScope.launch {
            settings.setLanguage(language)
            val bundle = localizationManager.loadBundle(language)
            _uiState.update { it.copy(language = language, localization = bundle) }
            // `loadData` sees that the locale it is loading is not the one on
            // screen and clears the list: saint names, fasting labels and
            // reading references are baked into the JSON per locale, so the
            // previous locale's days are not "still good while the new one
            // arrives", they are Serbian saints under Russian headings.
            loadData(language.code, _uiState.value.currentMonth, _uiState.value.currentYear)
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settings.setTheme(theme)
            _uiState.update { it.copy(theme = theme) }
        }
    }

    fun setBibleTranslation(translation: BibleTranslation) {
        viewModelScope.launch {
            settings.setBibleTranslation(translation)
            _uiState.update { it.copy(bibleTranslation = translation) }
        }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    /** At the first/last month the archive covers — the chevrons stop here.
     *  Stepping outside loads nothing and strands the user on an empty month. */
    fun atFirstMonth(state: CalendarUiState = _uiState.value) =
        !CalendarArchive.canGoPrevious(state.currentMonth, state.currentYear)

    fun atLastMonth(state: CalendarUiState = _uiState.value) =
        !CalendarArchive.canGoNext(state.currentMonth, state.currentYear)

    fun goToPreviousMonth() {
        if (atFirstMonth()) return
        _uiState.update {
            if (it.currentMonth == 1) {
                it.copy(currentMonth = 12, currentYear = it.currentYear - 1)
            } else {
                it.copy(currentMonth = it.currentMonth - 1)
            }
        }
        loadMonth()
    }

    fun goToNextMonth() {
        if (atLastMonth()) return
        _uiState.update {
            if (it.currentMonth == 12) {
                it.copy(currentMonth = 1, currentYear = it.currentYear + 1)
            } else {
                it.copy(currentMonth = it.currentMonth + 1)
            }
        }
        loadMonth()
    }

    /**
     * The day for a date, loading its year if it is not the one on screen, plus
     * the fasting season it belongs to.
     *
     * Search and the date picker navigate to a day while its month is still
     * loading, so resolving from `daysInMonth` alone found nothing. Returning a
     * nullable day conflated "not yet" with "impossible"; the routes need the
     * difference because one of them is a spinner and the other is an apology.
     */
    suspend fun resolveDay(gregorianDate: String): DayOutcome {
        _uiState.value.daysInMonth
            .firstOrNull { it.gregorianDate == gregorianDate }
            ?.let { return DayOutcome.Found(it, _uiState.value.fastingPeriods[gregorianDate]) }

        val date = parseIsoDate(gregorianDate) ?: return DayOutcome.Missing.NoData
        val locale = _uiState.value.language.code
        return try {
            val day = years.loadMonth(locale, date.year, date.monthValue)
                .firstOrNull { it.gregorianDate == gregorianDate }
                ?: return DayOutcome.Missing.NoData
            DayOutcome.Found(day, periodInfoFor(locale, date, gregorianDate))
        } catch (c: CancellationException) {
            throw c
        } catch (e: YearSource.LoadError.NotFound) {
            DayOutcome.Missing.NoData
        } catch (e: Exception) {
            // Everything that is not a definite "the archive has no such year"
            // gets the retry affordance, because retrying is the right response
            // to a dropped connection and to a hiccup in the data alike. What
            // none of them get is the endless spinner the old nullable return
            // produced.
            DayOutcome.Missing.Offline
        }
    }

    fun goToMonth(month: Int, year: Int) {
        _uiState.update { it.copy(currentMonth = month, currentYear = year) }
        loadMonth()
    }

    fun goToToday() {
        val now = wallClock().toLocalDate()
        _uiState.update {
            it.copy(
                currentMonth = now.monthValue,
                currentYear = now.year,
                today = now.toIsoDate(),
                scrollToTodayTrigger = !it.scrollToTodayTrigger
            )
        }
        loadMonth()
    }

    /** Re-run the current month's load (used by the offline/error retry UI). */
    fun retry() {
        loadMonth()
    }

    /**
     * Days used to compute fasting-season spans. Normally just the cached year,
     * but when a boundary month (Nov/Dec/Jan) ends/starts inside a season we also
     * merge the adjacent year so a season straddling the year boundary (the
     * Nativity Fast, Nov 28 – Jan 6) resolves to its true dates. The neighbour is
     * pulled only in those months — never on a normal launch — and failure to
     * fetch it (offline / out of range) falls back to the single year.
     *
     * November pulls a year the user is not looking at, one month earlier than it
     * is strictly needed; the fast runs into it either way and the year is on disk
     * by then, so the alternative — a second condition per month — buys nothing.
     */
    private suspend fun seasonDays(locale: String, year: Int, month: Int): List<CalendarDay> {
        val current = years.load(locale, year).days.values.toList()
        val sorted = current.sortedBy { it.gregorianDate }
        val merged = ArrayList(current)
        if (month >= 11 && sorted.lastOrNull()?.fastingPeriod != null) {
            merged += neighbourYear(locale, year + 1)
        }
        if (month == 1 && sorted.firstOrNull()?.fastingPeriod != null) {
            merged += neighbourYear(locale, year - 1)
        }
        return merged
    }

    /**
     * The adjacent year, read from cache only; empty when it is not there.
     *
     * Not `runCatching`: that catches `Throwable`, cancellation included, so a
     * load cancelled with the ViewModel would be swallowed here and this
     * coroutine would run on over half a season. An absent neighbour really is
     * harmless — the season then runs to the edge of the data — cancellation is
     * not.
     */
    private suspend fun neighbourYear(locale: String, year: Int): List<CalendarDay> =
        try {
            years.load(locale, year, allowNetwork = false).days.values.toList()
        } catch (c: CancellationException) {
            throw c
        } catch (e: Exception) {
            emptyList()
        }

    /**
     * The fasting season of one day when the month on screen is a different
     * month (or year). `fastingPeriods` covers the loaded year only, so opening
     * Jan 1 or Dec 31 — the days the Nativity Fast is *about* — from a date
     * picker or a search result left the detail screen with no season badge
     * while the grid cell for the same day had one, from the day's own data.
     *
     * Cheap in practice: the day resolved, so its year is in memory, and
     * `allowNetwork` is false for the neighbour anyway.
     */
    private suspend fun periodInfoFor(
        locale: String,
        date: LocalDate,
        gregorianDate: String
    ): FastingPeriodInfo? {
        _uiState.value.fastingPeriods[gregorianDate]?.let { return it }
        return try {
            val names = _uiState.value.localization?.fastingPeriodNames ?: emptyMap()
            withContext(computeDispatcher) {
                FastingPeriods.computeSpans(
                    seasonDays(locale, date.year, date.monthValue), names
                )[gregorianDate]
            }
        } catch (c: CancellationException) {
            throw c
        } catch (e: Exception) {
            // A missing season badge must never cost the user the day itself.
            null
        }
    }

    /**
     * Sleeps to the next local midnight and re-publishes [CalendarUiState.today].
     * One coroutine sleeping for a day, not a polling timer; cancelled with the
     * ViewModel, and replaced — never stacked — when [refreshToday] re-arms it.
     *
     * Not enough on its own. `delay` on the main dispatcher is a
     * `Handler.postDelayed`, which runs on `SystemClock.uptimeMillis`, and uptime
     * stops in deep sleep. A phone left overnight with the app open woke this
     * tick hours late, and nothing else moved "today" until a month reloaded.
     */
    private fun startTodayTicker() {
        tickerJob?.cancel()
        // The first wait is measured here, at the call, not when the coroutine is
        // first dispatched. On the real main dispatcher (`Main.immediate`) those
        // are the same instant; on any other they are not, and a tick re-armed
        // "from the wall clock" has to mean the clock at the refresh that armed it.
        var wait = millisUntilNextMidnight(wallClock())
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(wait)
                publishToday()
                wait = millisUntilNextMidnight(wallClock())
            }
        }
    }

    private fun publishToday() {
        val today = wallClock().toLocalDate().toIsoDate()
        _uiState.update { if (it.today == today) it else it.copy(today = today) }
    }

    /**
     * Re-reads the date and re-arms the midnight tick from the wall clock.
     * MainActivity calls this on every ON_START — the moment a phone that slept
     * through midnight comes back. Re-arming matters as much as re-reading: the
     * tick armed before the sleep is still most of a day of *uptime* from firing,
     * so without a fresh one the next midnight would pass unnoticed with the app
     * open in the user's hand.
     */
    fun refreshToday() {
        publishToday()
        startTodayTicker()
    }

    private fun loadData(locale: String, month: Int, year: Int) {
        _uiState.update {
            // What is on screen belongs to the month we are about to load, or
            // nothing at all. Keeping it was the "February header over January's
            // days" bug: the header updates synchronously, the days do not, no
            // spinner appears over stale content, and the list keeps the old
            // scroll offset — so the month opens mid-month and a tap lands on a
            // day the user did not mean.
            val keepsDays = it.loadedLocale == locale &&
                it.loadedMonth == month &&
                it.loadedYear == year &&
                it.daysInMonth.isNotEmpty()
            it.copy(
                isLoading = true,
                loadFailed = false,
                isOffline = false,
                daysInMonth = if (keepsDays) it.daysInMonth else emptyList(),
                fastingPeriods = if (keepsDays) it.fastingPeriods else emptyMap()
            )
        }

        // Cancel any in-flight load so quick navigation doesn't pile up
        // concurrent large network fetches/decodes (which could OOM-crash).
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val days = years.loadMonth(locale, year, month)
                // The full year is already cached by loadMonth; compute season runs
                // (start/end + day index) across it so spans crossing months resolve.
                // Off the Main dispatcher: this walks ~1100 days and parses two
                // dates per adjacency test, and viewModelScope is Main-bound.
                val names = _uiState.value.localization?.fastingPeriodNames ?: emptyMap()
                val spans = withContext(computeDispatcher) {
                    FastingPeriods.computeSpans(seasonDays(locale, year, month), names)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        daysInMonth = days,
                        fastingPeriods = spans,
                        loadedLocale = locale,
                        loadedMonth = month,
                        loadedYear = year,
                        today = wallClock().toLocalDate().toIsoDate(),
                        loadFailed = false,
                        isOffline = false
                    )
                }
            } catch (c: CancellationException) {
                throw c // superseded by a newer load — don't surface as an error
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isOffline = e is YearSource.LoadError.Offline,
                        loadFailed = true,
                        daysInMonth = emptyList(),
                        fastingPeriods = emptyMap(),
                        loadedLocale = locale,
                        loadedMonth = month,
                        loadedYear = year
                    )
                }
            }
        }
    }
}
