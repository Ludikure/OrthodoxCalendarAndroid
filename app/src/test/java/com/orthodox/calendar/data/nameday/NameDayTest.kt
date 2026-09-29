package com.orthodox.calendar.data.nameday

import com.orthodox.calendar.app.NameDayReminders
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.CalendarFile
import com.orthodox.calendar.ui.screens.nameDayCountdown
import com.orthodox.calendar.ui.util.toIsoDate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Russian name days: the bundled catalog lands each name on the day the
 * calendar shows, the birthday rule picks the first commemoration on or after
 * it, and civil names map to church ones only where the mapping is trusted.
 *
 * Mirror of `OrthodoxCalendarTests/NameDayTests.swift`.
 */
class NameDayTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val assets: File by lazy {
        listOf("src/main/assets/localization", "app/src/main/assets/localization")
            .map(::File).firstOrNull { it.isDirectory }
            ?: error("assets not found from ${File(".").absolutePath}")
    }

    private val catalog: NameDayCatalog by lazy {
        NameDayCatalog.parse(File(assets, "imeniny_ru.json").readText()) ?: error("imeniny_ru.json did not parse")
    }

    private val years = HashMap<Int, CalendarFile>()

    private fun russianYear(year: Int): CalendarFile = years.getOrPut(year) {
        json.decodeFromString(CalendarFile.serializer(), File(assets, "calendar_ru_$year.json").readText())
    }

    private fun day(file: CalendarFile, monthDay: String): CalendarDay =
        file.days[monthDay] ?: error("${file.year}-$monthDay")

    private fun ymd(d: LocalDate?): String = d?.toIsoDate() ?: "nil"

    private fun firstNameDay(name: String, month: Int, day: Int, year: Int = 2026, newMartyrs: Boolean = false): String =
        ymd(catalog.firstNameDay(catalog.churchForms(name), month, day, year, newMartyrs)?.date)

    private fun names(year: Int, monthDay: String) = catalog.names(day(russianYear(year), monthDay)).map { it.name }

    @Test
    fun `catalog is bundled`() {
        assertTrue(catalog.names.size > 1000)
        assertTrue(catalog.days.size > 350)
        assertFalse(catalog.civil.isEmpty())
    }

    @Test
    fun `asset is byte-identical to the iOS bundle when the iOS repo is beside this one`() {
        val ios = listOf("../../OrthodoxCalendar", "../OrthodoxCalendar")
            .map { File(it, "OrthodoxCalendar/Localization/imeniny_ru.json") }
            .firstOrNull { it.isFile } ?: return
        assertTrue(ios.readBytes().contentEquals(File(assets, "imeniny_ru.json").readBytes()))
    }

    // MARK: - A day's names

    @Test
    fun `Tatiana on January 25`() {
        assertEquals(DayName("Татиана", true), catalog.names(day(russianYear(2026), "01-25")).first())
    }

    @Test
    fun `Faith Hope Love Sophia on September 30`() {
        val list = catalog.names(day(russianYear(2026), "09-30"))
        assertEquals(listOf("Вера", "Надежда", "Любовь", "София"), list.take(4).map { it.name })
        assertTrue(list.take(4).all { it.isMain })
    }

    @Test
    fun `Myrrh-bearers follow Pascha`() {
        // Pascha 2026 is 12 April; the Myrrh-bearers' Sunday two weeks later.
        val list = names(2026, "04-26")
        assertTrue(list.toString(), "Мария" in list && "Марфа" in list && "Иосиф" in list)
        assertEquals("2026-04-26", ymd(NameDayAnchor.Pascha(14).occurrence(2026)))
        assertEquals("2027-05-16", ymd(NameDayAnchor.Pascha(14).occurrence(2027)))
    }

    @Test
    fun `Sunday after the Nativity rule`() {
        val joseph = catalog.commemorations("Иосиф").first { it.anchor is NameDayAnchor.Weekday }
        // Julian 26 December 2026 is Friday 8 January 2027: the Sunday is the 10th.
        assertEquals("2027-01-10", ymd(joseph.anchor.occurrence(2026)))
        assertEquals("2026-01-11", ymd(joseph.anchor.occurrence(2025)))
        // The Nativity on a Sunday (7 January 2024): kept on Monday the 8th.
        assertEquals("2024-01-08", ymd(joseph.anchor.occurrence(2023)))
        // And the calendar day it lands on lists Joseph, David and James.
        val list = names(2027, "01-10")
        assertTrue(list.toString(), "Иосиф" in list && "Давид" in list && "Иаков" in list)
        assertFalse("Иаков" in names(2027, "01-11"))
    }

    @Test
    fun `Cassian keeps February 29 only in a leap year`() {
        // 2026: Julian 28 February (13 March) holds him.
        assertTrue("Кассиан" in names(2026, "03-13"))
        // 2028: Julian 29 February is 13 March; the 28th (12 March) does not.
        assertFalse("Кассиан" in names(2028, "03-12"))
        assertTrue("Кассиан" in names(2028, "03-13"))

        val cassian = catalog.commemorations("Кассиан").first { it.anchor == NameDayAnchor.Julian(2, 29) }
        assertEquals("2026-03-13", ymd(cassian.anchor.occurrence(2026)))
        assertEquals("2028-03-13", ymd(cassian.anchor.occurrence(2028)))
        assertTrue(cassian.anchor.matches(day(russianYear(2026), "03-13")))
        assertTrue(cassian.anchor.matches(day(russianYear(2028), "03-13")))
        assertFalse(cassian.anchor.matches(day(russianYear(2028), "03-12")))
    }

    @Test
    fun `every day lists main saints first and caps at twelve`() {
        for ((key, d) in russianYear(2026).days) {
            val list = catalog.names(d)
            val firstOther = list.indexOfFirst { !it.isMain }
            if (firstOther >= 0) assertFalse(key, list.drop(firstOther).any { it.isMain })
            assertEquals("duplicate name on $key", list.size, list.map { it.name }.toSet().size)
            val (shown, hidden) = NameDayCatalog.capped(list)
            assertEquals(key, list.size, shown.size + hidden)
            assertEquals(key, list.take(shown.size), shown)
            assertTrue(key, hidden == 0 || shown.size == NameDayCatalog.DAY_LIST_LIMIT)
        }
    }

    @Test
    fun `capping order`() {
        val list = (1..20).map { DayName("n$it", it <= 3) }
        val (shown, hidden) = NameDayCatalog.capped(list)
        assertEquals((1..12).map { "n$it" }, shown.map { it.name })
        assertEquals(8, hidden)
        // Hiding a single name would save nothing: all thirteen show.
        assertEquals(0, NameDayCatalog.capped(list.take(13)).second)
        assertEquals(5, NameDayCatalog.capped(list.take(5)).first.size)
    }

    // MARK: - Birthday rule

    @Test
    fun `first commemoration on or after the birthday`() {
        assertEquals("2026-01-25", firstNameDay("Татьяна", 1, 1))
        assertEquals("2026-01-25", firstNameDay("Таня", 1, 25))       // the day itself counts
        // Next: the royal passion-bearers (Julian 4 July), who are not new martyrs.
        assertEquals("2026-07-17", firstNameDay("Татиана", 1, 26))
        assertEquals("2026-07-17", firstNameDay("Татиана", 1, 26, newMartyrs = true))
        // Past the year's last Tatiana (Julian 10 December = 23 December) it wraps.
        assertEquals("2027-01-25", firstNameDay("Татьяна", 12, 24))
        // A name kept once a year wraps too: Або, Julian 8 January = 21 January.
        assertEquals("2026-01-21", firstNameDay("Або", 1, 21))
        assertEquals("2027-01-21", firstNameDay("Або", 1, 22))
    }

    @Test
    fun `new martyrs count only when asked for`() {
        // After 17 July every Tatiana of the year is a new martyr: azbyka.ru's
        // finder skips them by default, so the name day is next 25 January.
        assertEquals("2027-01-25", firstNameDay("Татьяна", 7, 18))
        // "Учитывать новомучеников": St Tatiana Gribkova, Julian 1 September.
        assertEquals("2026-09-14", firstNameDay("Татьяна", 7, 18, newMartyrs = true))
    }

    @Test
    fun `a name with only new martyrs still gets a day`() {
        val (name, list) = catalog.names.entries.first { it.value.isNotEmpty() && it.value.all { c -> c.isNewMartyr } }
        val hit = catalog.firstNameDay(listOf(name), 1, 1, 2026)
        assertNotNull(name, hit)
        assertTrue(name, hit!!.commemoration in list)
    }

    @Test
    fun `birthday rule reaches moveable dates`() {
        // Born 20 April: the Myrrh-bearers' Sunday (26 April 2026) is the first Salome after it.
        val hit = catalog.firstNameDay(listOf("Саломия"), 4, 20, 2026)
        assertEquals("2026-04-26", ymd(hit?.date))
        assertEquals(NameDayAnchor.Pascha(14), hit?.commemoration?.anchor)
    }

    @Test
    fun `usual church form decides`() {
        // Юрий → Георгий first; the new martyr Юрий is only the second form.
        assertEquals("Георгий", catalog.firstNameDay(catalog.churchForms("Юрий"), 5, 1, 2026)?.commemoration?.name)
    }

    @Test
    fun `February 29 birthday counts from March 1 in a common year`() {
        assertEquals(
            catalog.firstNameDay(catalog.churchForms("Иван"), 3, 1, 2026)?.date,
            catalog.firstNameDay(catalog.churchForms("Иван"), 2, 29, 2026)?.date
        )
    }

    // MARK: - Civil names

    @Test
    fun `civil mapping`() {
        assertEquals(listOf("Иоанн"), catalog.churchForms("Иван"))
        assertEquals(listOf("Иоанн"), catalog.churchForms("  иван "))
        assertEquals(listOf("Иоанн"), catalog.churchForms("Ваня"))
        assertEquals(listOf("Георгий", "Юрий"), catalog.churchForms("Юрий"))
        assertEquals(listOf("Петр"), catalog.churchForms("Пётр"))
        assertEquals(listOf("Татиана"), catalog.churchForms("Татьяна"))
        // A church name maps to itself.
        assertEquals(listOf("Иоанн"), catalog.churchForms("Иоанн"))
    }

    @Test
    fun `unsure mappings left out unless azbyka gives them`() {
        assertEquals(emptyList<String>(), catalog.churchForms("Егор"))
        assertEquals(emptyList<String>(), catalog.churchForms("Полина"))
        assertEquals(emptyList<String>(), catalog.churchForms("Кристина"))
        // Маргарита is not mapped to Марина, but is a church name of its own.
        assertEquals(listOf("Маргарита"), catalog.churchForms("Маргарита"))
        // azbyka.ru's own mappings stay.
        assertEquals(listOf("Феодот"), catalog.churchForms("Богдан"))
        assertEquals(listOf("Иоанна"), catalog.churchForms("Жанна"))
    }

    @Test
    fun `unknown name has no saint`() {
        assertEquals(emptyList<String>(), catalog.churchForms("Руслан"))
        assertEquals(emptyList<String>(), catalog.churchForms(""))
    }

    @Test
    fun suggestions() {
        val list = catalog.suggestions("Тат").map { it.name }
        assertTrue(list.toString(), "Татьяна" in list && "Татиана" in list)
        assertEquals("Иоанн", catalog.suggestions("Ваня").first().church)
    }

    // MARK: - Store and wording

    private val tatiana = NameDayChoice(
        name = "Таня", churchName = "Татиана", title = "Мц. Татианы Римской",
        anchor = NameDayAnchor.Julian(1, 12), birthMonth = 1, birthDay = 1
    )

    @Test
    fun `mark and settings round trip`() {
        val file = russianYear(2026)
        assertTrue(tatiana.matches(day(file, "01-25")))
        assertFalse(tatiana.matches(day(file, "01-24")))

        val settings = NameDaySettings(mine = tatiana, friends = listOf(FriendNameDay(person = "мама", nameDay = tatiana)))
        assertEquals(settings, NameDayStore.decode(NameDayStore.encode(settings)))
        // A weekday anchor survives the round trip too.
        val joseph = catalog.commemorations("Иосиф").first { it.anchor is NameDayAnchor.Weekday }
        val withRule = settings.copy(mine = tatiana.copy(anchor = joseph.anchor))
        assertEquals(withRule, NameDayStore.decode(NameDayStore.encode(withRule)))

        val mark = settings.mark(day(file, "01-25"))
        assertEquals(NameDayMark(isMine = true, friendLines = listOf("мама · Татиана")), mark)
        assertNull(settings.mark(day(file, "01-24")))
        // A friend without a label is shown by their name.
        assertEquals("Таня", FriendNameDay(person = "", nameDay = tatiana).displayName)
    }

    @Test
    fun `countdown row is Russian's slava row`() {
        val settings = NameDaySettings(mine = tatiana)
        assertEquals(10, nameDayCountdown(settings, "2026-01-15", 1, 2026)?.days)
        assertEquals("Татиана", nameDayCountdown(settings, "2026-01-15", 1, 2026)?.name)
        assertNull(nameDayCountdown(settings, "2026-01-15", 3, 2026))       // another month on screen
        assertNull(nameDayCountdown(settings, "2025-12-20", 12, 2025))      // 36 days out
        assertEquals(0, nameDayCountdown(settings, "2026-01-25", 1, 2026)?.days)
        assertNull(nameDayCountdown(null, "2026-01-25", 1, 2026))
    }

    @Test
    fun `countdown plurals`() {
        assertEquals("С днём ангела!", NameDayText.countdown(0))
        assertEquals("завтра", NameDayText.countdown(1))
        assertEquals("через 2 дня", NameDayText.countdown(2))
        assertEquals("через 5 дней", NameDayText.countdown(5))
        assertEquals("через 11 дней", NameDayText.countdown(11))
        assertEquals("через 12 дней", NameDayText.countdown(12))
        assertEquals("через 21 день", NameDayText.countdown(21))
        assertEquals("через 22 дня", NameDayText.countdown(22))
        assertEquals("через 30 дней", NameDayText.countdown(30))
    }

    @Test
    fun `when text`() {
        val today = LocalDate.of(2026, 1, 1)
        assertEquals("25 января (12 января по ст. ст.)", NameDayText.whenText(NameDayAnchor.Julian(1, 12), today))
        assertEquals("переходящая, ближайшая 26 апреля", NameDayText.whenText(NameDayAnchor.Pascha(14), today))
        assertEquals("воскресенье, 25 января", NameDayText.longDate(LocalDate.of(2026, 1, 25)))
    }

    @Test
    fun `reminder wording and timing`() {
        val friend = FriendNameDay(id = "f", person = "мама", nameDay = tatiana)
        val settings = NameDaySettings(mine = tatiana, friends = listOf(friend), remindFriendsDayBefore = true)
        val plan = NameDayReminders.plan(settings, LocalDateTime.of(2026, 1, 1, 12, 0))
        val first = plan.filter { it.fireAt.toLocalDate() <= LocalDate.of(2026, 1, 25) }
        assertEquals(
            setOf(
                Triple("2026-01-24T09:00", "Завтра именины: мама", "Татиана, воскресенье, 25 января."),
                Triple("2026-01-25T09:00", "Сегодня именины: мама", "Татиана — не забудьте поздравить."),
                Triple("2026-01-25T09:00", "С днём ангела!", "Сегодня ваши именины — память: Мц. Татианы Римской.")
            ),
            first.map { Triple(it.fireAt.toString(), it.title, it.body) }.toSet()
        )
        // Two occurrences each: this January and next.
        assertEquals(6, plan.size)
        // Switched off, nothing for friends; the eve is off by default.
        assertEquals(2, NameDayReminders.plan(settings.copy(remindFriends = false, remindFriendsDayBefore = false),
            LocalDateTime.of(2026, 1, 1, 12, 0)).size)
        // The day's reminder already past is not scheduled again.
        assertTrue(NameDayReminders.plan(settings, LocalDateTime.of(2026, 1, 25, 10, 0))
            .none { it.fireAt.toLocalDate() == LocalDate.of(2026, 1, 25) })
    }
}
