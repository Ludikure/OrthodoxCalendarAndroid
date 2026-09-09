package com.orthodox.calendar.ui.util

import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.ScriptureReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingLabelsTest {

    /**
     * Every distinct `source` in the 2024-2099 archive, counted across all four
     * locales. The vocabulary is closed — the lectionary tables produce it — so
     * this is the whole input domain for [serviceLabel].
     */
    private val allSources = listOf(
        "Gospel", "fixed", "Epistle", "Vespers", "Matins Gospel", "6th Hour",
        "Great Blessing of Waters", "8th Matins Gospel", "1st Matins Gospel",
        "3rd Matins Gospel", "4th Matins Gospel", "7th Matins Gospel",
        "Cross Procession", "9th Matins Gospel", "10th Matins Gospel",
        "2nd Matins Gospel", "5th Matins Gospel", "6th Matins Gospel",
        "1st Hour", "11th Matins Gospel", "3rd Hour", "1st Hour, Gospel",
        "3rd Hour, Epistle", "3rd Hour, Gospel", "6th Hour, Gospel",
        "9th Hour, Epistle", "9th Hour, Gospel", "9th Hour", "6th Hour, Epistle",
        "1st Hour, Prophecy", "3rd Hour, Prophecy", "9th Hour, Prophecy",
        "1st Hour, Epistle", "6th Hour, Prophecy", "Sixth Hour",
        "1st Passion Gospel", "2nd Passion Gospel", "3rd Passion Gospel",
        "4th Passion Gospel", "5th Passion Gospel", "6th Passion Gospel",
        "7th Passion Gospel", "8th Passion Gospel", "9th Passion Gospel",
        "10th Passion Gospel", "11th Passion Gospel", "12th Passion Gospel",
        "Vespers Gospel", "Matins", "Matins Epistle"
    )

    private fun reading(source: String? = null, service: String? = null, type: String = "gospel") =
        ScriptureReading(type = type, source = source, service = service)

    /**
     * The point of translating `source` rather than printing it: no English may
     * reach a Serbian or Russian card. Digits and separators are fine ("6. час").
     */
    @Test
    fun `no latin letters leak into the cyrillic locales`() {
        for (language in listOf(AppLanguage.SR, AppLanguage.RU)) {
            for (source in allSources) {
                val label = serviceLabel(reading(source), language) ?: continue
                assertTrue(
                    "$source -> $label ($language)",
                    label.none { it in 'a'..'z' || it in 'A'..'Z' }
                )
            }
        }
    }

    /** Every value in the vocabulary is either translated or deliberately dropped. */
    @Test
    fun `every source value is handled`() {
        val dropped = setOf("Gospel", "Epistle", "fixed")
        for (source in allSources) {
            val label = serviceLabel(reading(source), AppLanguage.EN)
            if (source in dropped) {
                assertNull(source, label)
            } else {
                assertTrue(source, !label.isNullOrBlank())
            }
        }
    }

    /** A localized `service` from the data wins; the English `source` is the fallback. */
    @Test
    fun `service takes priority over source`() {
        assertEquals("Вечерња", serviceLabel(reading("Vespers", service = "Вечерња"), AppLanguage.SR))
        assertEquals("Вечерња", serviceLabel(reading("fixed", service = "Вечерња"), AppLanguage.SR))
        assertEquals("Vespers", serviceLabel(reading("Vespers"), AppLanguage.EN))
    }

    /** The parts that only restate the reading's own type are dropped, including
     *  the second half of a compound value. */
    @Test
    fun `type restatements are dropped`() {
        assertNull(serviceLabel(reading("Gospel"), AppLanguage.SR))
        assertNull(serviceLabel(reading("Epistle"), AppLanguage.RU))
        assertNull(serviceLabel(reading("fixed"), AppLanguage.EN))
        assertEquals("1. час", serviceLabel(reading("1st Hour, Gospel"), AppLanguage.SR))
        assertEquals("9th Hour", serviceLabel(reading("9th Hour, Prophecy"), AppLanguage.EN))
    }

    @Test
    fun `ordinals are rendered per language`() {
        assertEquals("8. јутрењско јеванђеље", serviceLabel(reading("8th Matins Gospel"), AppLanguage.SR))
        assertEquals("8-е утреннее Евангелие", serviceLabel(reading("8th Matins Gospel"), AppLanguage.RU))
        assertEquals("8th Matins Gospel", serviceLabel(reading("8th Matins Gospel"), AppLanguage.EN))
        assertEquals("11th Passion Gospel", serviceLabel(reading("11th Passion Gospel"), AppLanguage.EN_NC))
        // English ordinal suffixes are computed, not appended: 1st/2nd/3rd, and
        // 11th/12th/13th rather than 11st/12nd/13rd.
        assertEquals("1st Hour", serviceLabel(reading("1st Hour"), AppLanguage.EN))
        assertEquals("3rd Hour", serviceLabel(reading("3rd Hour"), AppLanguage.EN))
        assertEquals("2nd Passion Gospel", serviceLabel(reading("2nd Passion Gospel"), AppLanguage.EN))
        assertEquals("12th Passion Gospel", serviceLabel(reading("12th Passion Gospel"), AppLanguage.EN))
        // "Sixth Hour" and "6th Hour" are the same slot spelled two ways.
        assertEquals(
            serviceLabel(reading("6th Hour"), AppLanguage.SR),
            serviceLabel(reading("Sixth Hour"), AppLanguage.SR)
        )
    }

    /** An unknown value is dropped, never shown raw in a non-English UI. */
    @Test
    fun `unknown source is dropped`() {
        assertNull(serviceLabel(reading("Compline"), AppLanguage.SR))
        assertNull(serviceLabel(reading(""), AppLanguage.EN))
        assertNull(serviceLabel(reading(null), AppLanguage.EN))
    }

    /**
     * The Great Canon's odes arrive as type "other" and their title already
     * names them; the card printed the literal word OTHER above it.
     */
    @Test
    fun `unknown type has no label`() {
        assertNull(readingTypeLabel("other", AppLanguage.SR))
        assertNull(readingTypeLabel("", AppLanguage.EN))
        assertEquals("ЈЕВАНЂЕЉЕ", readingTypeLabel("gospel", AppLanguage.SR))
        assertEquals("EPISTLE", readingTypeLabel("apostol", AppLanguage.EN))
        assertEquals("ВЕТХИЙ ЗАВЕТ", readingTypeLabel("ot", AppLanguage.RU))
    }
}
