package com.orthodox.calendar.widget

import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.CalendarFile
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.data.model.UILabels
import com.orthodox.calendar.data.slava.SlavaAnchor
import com.orthodox.calendar.data.slava.SlavaDay
import com.orthodox.calendar.data.slava.SlavaText
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The widgets' snapshot: consecutive days from the bundled data, the right
 * fields, and the slava countdown as the banner counts it. Port of
 * `OrthodoxCalendarTests/WidgetSnapshotTests.swift`.
 */
class WidgetSnapshotTest {
    @get:Rule val tmp = TemporaryFolder()

    private val json = Json { ignoreUnknownKeys = true }

    private fun assets(): File =
        listOf("src/main/assets/localization", "app/src/main/assets/localization")
            .map(::File).firstOrNull { it.isDirectory }
            ?: error("bundled calendars not found from ${File(".").absolutePath}")

    private fun year(locale: String, year: Int): Map<String, CalendarDay> =
        json.decodeFromString(CalendarFile.serializer(), File(assets(), "calendar_${locale}_$year.json").readText())
            .days.values.associateBy { it.gregorianDate }

    private fun labels(language: AppLanguage): UILabels =
        json.decodeFromString(
            LocalizationBundle.serializer(),
            File(assets(), "${language.localizationFile}.json").readText()
        ).ui

    private fun date(key: String) = LocalDate.parse(key)

    private val nikoljdan = SlavaDay("Никољдан", "Свети Николај", SlavaAnchor.Julian(12, 6))

    @Test
    fun `twenty-one consecutive days across the year boundary`() {
        val days = year("sr", 2026) + year("sr", 2027)
        val ui = labels(AppLanguage.SR)
        val snapshot = WidgetSnapshotBuilder.build(days, date("2026-12-20"), AppLanguage.SR, ui, mySlava = null)
        assertNotNull(snapshot)
        snapshot!!

        assertEquals("sr", snapshot.language)
        assertEquals(WidgetSnapshot.DAY_COUNT, snapshot.days.size)
        assertEquals("2026-12-20", snapshot.days.first().date)
        assertEquals("2027-01-09", snapshot.days.last().date)
        snapshot.days.zipWithNext().forEach { (a, b) ->
            assertEquals("${a.date} → ${b.date}", 1L, ChronoUnit.DAYS.between(date(a.date), date(b.date)))
        }

        // Fields come from the day as the calendar shows it.
        val christmas = snapshot.day("2027-01-07")!!
        val source = days.getValue("2027-01-07")
        assertTrue(christmas.isGreatFeast)
        assertEquals(source.primaryFeast?.name, christmas.primary)
        assertEquals(source.fasting.type, christmas.fastingType)
        assertEquals(source.fasting.label, christmas.fastingLabel)
        assertEquals(source.fasting.abbrev.orEmpty(), christmas.fastingAbbrev)
        assertEquals(ui.daysOfWeekFull[source.weekdayIndex], christmas.weekday)
        assertEquals(ui.dayAndMonth(7, 1), christmas.dateLabel)
        assertTrue(christmas.secondary.size <= WidgetSnapshotBuilder.MAX_SECONDARY)
        assertNull(christmas.slava)
        // A day that is not a great feast carries no ✦.
        assertFalse(snapshot.days.all { it.isGreatFeast })
    }

    @Test
    fun `secondary names are kept short and never repeat the primary`() {
        val days = year("ru", 2026)
        val snapshot = WidgetSnapshotBuilder.build(
            days, date("2026-01-01"), AppLanguage.RU, labels(AppLanguage.RU), null
        )!!
        for (day in snapshot.days) {
            assertTrue(day.secondary.size <= WidgetSnapshotBuilder.MAX_SECONDARY)
            assertTrue(day.secondary.none { it == day.primary })
            assertTrue(day.secondary.all { it.length <= WidgetSnapshotBuilder.MAX_SECONDARY_LENGTH })
        }
        val long = "а".repeat(200)
        assertEquals(WidgetSnapshotBuilder.MAX_SECONDARY_LENGTH, WidgetSnapshotBuilder.shorten(long).length)
        assertTrue(WidgetSnapshotBuilder.shorten(long).endsWith("…"))
    }

    @Test
    fun `stops where the data stops`() {
        val days = year("sr", 2026)
        val ui = labels(AppLanguage.SR)
        val snapshot = WidgetSnapshotBuilder.build(days, date("2026-12-25"), AppLanguage.SR, ui, null)!!
        assertEquals("2026-12-31", snapshot.days.last().date)
        assertNull(WidgetSnapshotBuilder.build(days, date("2027-01-02"), AppLanguage.SR, ui, null))
    }

    @Test
    fun `slava countdown within thirty days`() {
        val days = year("sr", 2026)
        val ui = labels(AppLanguage.SR)
        val snapshot = WidgetSnapshotBuilder.build(days, date("2026-12-07"), AppLanguage.SR, ui, nikoljdan)!!
        assertEquals(WidgetSnapshot.Slava("Никољдан", 12), snapshot.days[0].slava)
        assertEquals(1, snapshot.day("2026-12-18")?.slava?.daysUntil)
        assertEquals(0, snapshot.day("2026-12-19")?.slava?.daysUntil)
        // The day after, the next one is a year away: no countdown.
        assertNull(snapshot.day("2026-12-20")?.slava)
        assertEquals("за 12 дана", SlavaText.countdownLabel(12))
        assertEquals("за 21 дан", SlavaText.countdownLabel(21))
        assertEquals("Срећна слава!", SlavaText.countdownLabel(0))

        // More than 30 days out: nothing yet.
        val early = WidgetSnapshotBuilder.build(days, date("2026-11-01"), AppLanguage.SR, ui, nikoljdan)!!
        assertNull(early.days[0].slava)
        assertEquals(30, early.day("2026-11-19")?.slava?.daysUntil)
        assertNull(early.day("2026-11-18")?.slava)
    }

    @Test
    fun `no slava outside Serbian`() {
        for ((language, locale) in listOf(AppLanguage.EN to "en", AppLanguage.RU to "ru", AppLanguage.EN_NC to "en_nc")) {
            val snapshot = WidgetSnapshotBuilder.build(
                year(locale, 2026), date("2026-12-07"), language, labels(language), nikoljdan
            )!!
            assertEquals(locale, snapshot.language)
            assertTrue(snapshot.days.all { it.slava == null })
        }
    }

    @Test
    fun `round-trips through a file`() {
        val snapshot = WidgetSnapshotBuilder.build(
            year("ru", 2026), date("2026-09-27"), AppLanguage.RU, labels(AppLanguage.RU), null,
            now = Instant.parse("2026-09-27T08:00:00Z")
        )!!
        val file = File(tmp.root, WidgetSnapshot.FILE_NAME)
        WidgetSnapshot.write(snapshot, file)
        assertEquals(snapshot, WidgetSnapshot.read(file))
        assertFalse(File(tmp.root, WidgetSnapshot.FILE_NAME + ".tmp").exists())
    }

    @Test
    fun `an unreadable or other-version file reads as none`() {
        val file = File(tmp.root, WidgetSnapshot.FILE_NAME)
        assertNull(WidgetSnapshot.read(file))
        file.writeText("{not json")
        assertNull(WidgetSnapshot.read(file))
        val future = WidgetSnapshot(version = 99, language = "sr", generatedAt = "", days = emptyList())
        file.writeText(WidgetSnapshot.encode(future))
        assertNull(WidgetSnapshot.read(file))
    }

    @Test
    fun `reads the iOS snapshot format`() {
        // What `WidgetSnapshot.write()` in the iOS app produces (JSONEncoder,
        // nil slava omitted): the two apps agree on the file's shape.
        val ios = """{"version":1,"language":"sr","generatedAt":"2026-09-27T08:00:00Z","days":[
            {"date":"2026-12-19","weekday":"Субота","dateLabel":"19 Децембар",
             "primary":"Свети Николај","secondary":[],"isGreatFeast":false,
             "fastingType":"fish","fastingLabel":"Риба дозвољена","fastingAbbrev":"риба",
             "slava":{"name":"Никољдан","daysUntil":0}},
            {"date":"2026-12-20","weekday":"Недеља","dateLabel":"20 Децембар",
             "primary":"Игњатије","secondary":["a","b"],"isGreatFeast":false,
             "fastingType":"fish","fastingLabel":"Риба дозвољена","fastingAbbrev":"риба"}]}"""
        val snapshot = WidgetSnapshot.decode(ios)!!
        assertEquals(0, snapshot.day("2026-12-19")?.slava?.daysUntil)
        assertNull(snapshot.day("2026-12-20")?.slava)
        assertNull(snapshot.day("2026-12-21"))
    }
}
