package com.orthodox.calendar.data.slava

import com.orthodox.calendar.app.SlavaReminders
import com.orthodox.calendar.data.model.CalendarFile
import com.orthodox.calendar.engine.Paschalion
import com.orthodox.calendar.ui.screens.slavaCountdown
import com.orthodox.calendar.ui.util.toIsoDate
import kotlinx.coroutines.test.runTest
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
 * A slava lands on the day the calendar shows for it, in every year, and the
 * reminder wording follows Serbian grammar and the day's fast.
 *
 * Mirror of `OrthodoxCalendarTests/SlavaTests.swift`.
 */
class SlavaTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun date(key: String): LocalDate = LocalDate.parse(key)

    private fun ymd(d: LocalDate?): String = d?.toIsoDate() ?: "nil"

    /** The bundled Serbian years, as the app ships them. */
    private val bundledYears: List<CalendarFile> by lazy {
        val dir = listOf("src/main/assets/localization", "app/src/main/assets/localization")
            .map(::File).firstOrNull { it.isDirectory }
            ?: error("bundled calendars not found from ${File(".").absolutePath}")
        val files = dir.listFiles { f -> f.name.startsWith("calendar_sr_") && f.name.endsWith(".json") }
            .orEmpty().sortedBy { it.name }
            .map { json.decodeFromString(CalendarFile.serializer(), it.readText()) }
        assertTrue("no bundled Serbian years found", files.isNotEmpty())
        files
    }

    @Test
    fun `paschalion agrees with every bundled year`() {
        for (file in bundledYears) {
            val pascha = file.days.values.first { it.paschaDistance == 0 }
            assertEquals("year ${file.year}", pascha.gregorianDate, ymd(Paschalion.pascha(file.year)))
        }
        // Outside the bundle too, against the published Paschalion.
        assertEquals("2024-05-05", ymd(Paschalion.pascha(2024)))
        assertEquals("2034-04-09", ymd(Paschalion.pascha(2034)))
        assertEquals("2099-04-12", ymd(Paschalion.pascha(2099)))
    }

    @Test
    fun `fixed slava falls thirteen days after its church date`() {
        val nikoljdan = SlavaDay("Никољдан", "Свети Николај", SlavaAnchor.Julian(12, 6))
        assertEquals("2026-12-19", ymd(nikoljdan.nextOccurrence(date("2026-12-07"))))
        assertEquals("2026-12-19", ymd(nikoljdan.nextOccurrence(date("2026-12-19"))))
        assertEquals("2027-12-19", ymd(nikoljdan.nextOccurrence(date("2026-12-20"))))

        // Julian 25 December of 2026 is 7 January 2027.
        val bozic = SlavaDay("Божић", "Рождество Христово", SlavaAnchor.Julian(12, 25))
        assertEquals("2027-01-07", ymd(bozic.nextOccurrence(date("2026-12-20"))))
        assertEquals("2027-01-07", ymd(bozic.nextOccurrence(date("2027-01-05"))))
    }

    @Test
    fun `moveable slava follows pascha`() {
        val lazareva = SlavaDay("Лазарева субота", "", SlavaAnchor.Pascha(-8))
        assertEquals("2026-04-04", ymd(lazareva.nextOccurrence(date("2026-01-01"))))
        assertEquals("2027-04-24", ymd(lazareva.nextOccurrence(date("2026-04-05"))))
        val spasovdan = SlavaDay("Спасовдан", "", SlavaAnchor.Pascha(39))
        assertEquals("2026-05-21", ymd(spasovdan.nextOccurrence(date("2026-01-01"))))
    }

    @Test
    fun `every common slava matches exactly one day of each bundled year`() {
        assertEquals(68, SlavaCatalog.common.size)
        for (file in bundledYears) {
            for (slava in SlavaCatalog.common) {
                val hits = file.days.values.filter { slava.matches(it) }.map { it.gregorianDate }
                assertEquals("${slava.name} in ${file.year}: $hits", 1, hits.size)
                // And the day it matches is the day it is scheduled for.
                if (slava.anchor is SlavaAnchor.Pascha) {
                    assertEquals(slava.name, hits.first(), ymd(slava.occurrence(file.year)))
                }
            }
        }
    }

    @Test
    fun `matched day carries the slava`() {
        // The data's own slava flags agree with the catalog where both name the day.
        val file = bundledYears.first { it.year == 2026 }
        val nikoljdan = SlavaCatalog.fixed.first { it.name == "Никољдан" }
        val day = file.days.getValue("12-19")
        assertTrue(nikoljdan.matches(day))
        assertTrue(day.feasts.any { it.isSlava && it.name.contains("Никољдан") })
    }

    @Test
    fun `countdown plurals`() {
        assertEquals("Срећна слава!", SlavaText.countdownLabel(0))
        assertEquals("сутра", SlavaText.countdownLabel(1))
        assertEquals("за 2 дана", SlavaText.countdownLabel(2))
        assertEquals("за 11 дана", SlavaText.countdownLabel(11))
        assertEquals("за 21 дан", SlavaText.countdownLabel(21))
        assertEquals("за 30 дана", SlavaText.countdownLabel(30))
    }

    @Test
    fun `table follows the fast`() {
        assertEquals("Трпеза је мрсна.", SlavaText.table("free"))
        assertEquals("Трпеза је посна — риба је дозвољена.", SlavaText.table("fish"))
        assertEquals("Трпеза је посна — на води, без уља.", SlavaText.table("hotNoOil"))
        assertEquals("Трпеза је посна — строги пост.", SlavaText.table("dryEating"))
    }

    @Test
    fun `settings round trip`() {
        val settings = SlavaSettings(
            mine = SlavaCatalog.moveable[0],
            friends = listOf(FriendSlava(person = "Петровићи", slava = SlavaCatalog.fixed[0])),
            remindOnDay = false,
            reminderMinutes = 7 * 60 + 30
        )
        assertEquals(settings, SlavaStore.decode(SlavaStore.encode(settings)))
        // A value this version cannot read starts fresh rather than crashing.
        assertNull(SlavaStore.decode("{not json"))
    }

    @Test
    fun `every flagged slava feast can be set from its card`() {
        for (file in bundledYears) {
            for (day in file.days.values) {
                for (feast in day.feasts.filter { it.isSlava }) {
                    val slava = SlavaCatalog.slava(feast, day)
                    assertNotNull("${feast.name} ${day.gregorianDate}", slava)
                    assertTrue("${slava!!.name} ${day.gregorianDate}", slava.matches(day))
                }
            }
        }
    }

    @Test
    fun `two slavas on one day stay apart`() {
        // 31 October: St Peter of Cetinje (flagged) and Лучиндан (not flagged).
        val file = bundledYears.first { it.year == 2026 }
        val day = file.days.getValue("10-31")
        val names = day.feasts.mapNotNull { SlavaCatalog.slava(it, day)?.name }.toSet()
        assertEquals(setOf("Свети Петар Цетињски", "Лучиндан"), names)
        // And an ordinary saint is not offered.
        val minaDay = file.days.getValue("11-24")
        val mina = minaDay.feasts.first { it.name.contains("Мина") }
        assertNull(SlavaCatalog.slava(mina, minaDay))
    }

    @Test
    fun `row marks and banner countdown`() {
        val nikoljdan = SlavaCatalog.fixed.first { it.name == "Никољдан" }
        val andrej = SlavaCatalog.fixed.first { it.name == "Андрејевдан" }
        val settings = SlavaSettings(mine = nikoljdan, friends = listOf(FriendSlava(person = "Петровићи", slava = andrej)))
        val file = bundledYears.first { it.year == 2026 }
        assertEquals(SlavaMark(isMine = true), settings.mark(file.days.getValue("12-19")))
        assertEquals(SlavaMark(friendLines = listOf("Андрејевдан · Петровићи")), settings.mark(file.days.getValue("12-13")))
        assertNull(settings.mark(file.days.getValue("12-20")))

        // Within 30 days, shown while the month holds today or the slava.
        val c = slavaCountdown(settings, "2026-11-27", 11, 2026)
        assertEquals(22, c?.days)
        assertNotNull(slavaCountdown(settings, "2026-11-27", 12, 2026))
        assertNull(slavaCountdown(settings, "2026-11-27", 3, 2027))
        assertNull(slavaCountdown(settings, "2026-11-18", 11, 2026)) // 31 days out
        assertEquals(0, slavaCountdown(settings, "2026-12-19", 12, 2026)?.days)
        assertNull(slavaCountdown(null, "2026-12-19", 12, 2026))
    }

    @Test
    fun `reminder wording and timing`() = runTest {
        val nikoljdan = SlavaCatalog.fixed.first { it.name == "Никољдан" }
        val andrej = SlavaCatalog.fixed.first { it.name == "Андрејевдан" }
        val settings = SlavaSettings(
            mine = nikoljdan,
            friends = listOf(FriendSlava(person = "Петровићи", slava = andrej))
        )
        val plan = SlavaReminders.plan(settings, LocalDateTime.of(2026, 9, 27, 12, 0)) { d ->
            if (d.year == 2026) SlavaText.table("fish") else null
        }
        val week = plan.first { it.key == "mine.week.2026-12-19" }
        assertEquals(LocalDateTime.of(2026, 12, 12, 9, 0), week.fireAt)
        assertEquals("Никољдан за 7 дана", week.title)
        assertEquals(
            "Жито, колач и свећа. Позовите свештеника за освећење водице. Трпеза је посна — риба је дозвољена.",
            week.body
        )
        val onDay = plan.first { it.key == "mine.day.2026-12-19" }
        assertEquals("Срећна слава!", onDay.title)
        assertEquals("Данас је Никољдан. Трпеза је посна — риба је дозвољена.", onDay.body)
        // A year not on the device goes without the table line.
        assertEquals("Данас је Никољдан.", plan.first { it.key == "mine.day.2027-12-19" }.body)

        val friend = plan.first { it.key.startsWith("friend.") && it.key.endsWith("2026-12-13") }
        assertEquals(LocalDateTime.of(2026, 12, 12, 9, 0), friend.fireAt)
        assertEquals("Сутра: слава — Петровићи", friend.title)
        assertEquals("Андрејевдан, недеља 13. децембар.", friend.body)
        assertEquals(6, plan.size)

        // Switched off, nothing of that kind is planned.
        val quiet = SlavaReminders.plan(
            settings.copy(remindWeekBefore = false, remindFriends = false),
            LocalDateTime.of(2026, 9, 27, 12, 0)
        ) { null }
        assertEquals(listOf("mine.day.2026-12-19", "mine.day.2027-12-19"), quiet.map { it.key })
        assertFalse(quiet.any { it.key.startsWith("friend.") })
    }
}
