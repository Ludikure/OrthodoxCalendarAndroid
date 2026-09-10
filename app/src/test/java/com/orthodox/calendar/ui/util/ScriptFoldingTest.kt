package com.orthodox.calendar.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Serbian is read in both alphabets but the `sr` data is Cyrillic-only, so
 * search folds both sides to one form. The target is Serbian Latin: a Russian
 * romanization (ч→"ch", ш→"sh", ц→"ts") leaves a Serb typing the spelling they
 * actually use matching nothing.
 *
 * Mirror of `OrthodoxCalendarTests/SaintSearchFoldingTests.swift`.
 */
class ScriptFoldingTest {

    @Test
    fun `serbian cyrillic folds to serbian latin`() {
        val pairs = listOf(
            "Ђорђе" to "djordje",
            "Љубомир" to "ljubomir",
            "Његош" to "njegos",
            "Ћирило" to "cirilo",
            "Чудотворац" to "cudotvorac",
            "Шишатовачки" to "sisatovacki",
            "Џаџић" to "dzadzic",
            "Жарко" to "zarko",
            "Царица" to "carica",
        )
        for ((cyrillic, latin) in pairs) {
            assertEquals(cyrillic, latin, foldForSearch(cyrillic))
        }
    }

    /** Easy letters to omit, and omitting them makes every name containing
     *  them unreachable from a Latin query. */
    @Test
    fun `lj and nj are mapped`() {
        assertEquals("lj", foldForSearch("љ"))
        assertEquals("nj", foldForSearch("њ"))
        assertTrue(foldForSearch("Краљ Миљутин").contains("kralj"))
    }

    @Test
    fun `latin diacritics fold the same way`() {
        assertEquals(foldForSearch("Cirilo"), foldForSearch("Ćirilo"))
        assertEquals(foldForSearch("Sisatovacki"), foldForSearch("Šišatovački"))
        assertEquals(foldForSearch("Djordje"), foldForSearch("Đorđe"))
        assertEquals(foldForSearch("Zarko"), foldForSearch("Žarko"))
        assertEquals(foldForSearch("Ћирило"), foldForSearch("Ćirilo"))
    }

    /** Folding is applied to both the query and the name, so a same-script
     *  search must be unaffected by any of the above. */
    @Test
    fun `same script search is unaffected`() {
        assertTrue(foldForSearch("Свети Никола").contains(foldForSearch("Никола")))
        assertTrue(foldForSearch("Преподобни Сава Псковски").contains(foldForSearch("Сава")))
        assertTrue(foldForSearch("Saint Nicholas").contains(foldForSearch("Nicholas")))
    }

    @Test
    fun `folding is case insensitive`() {
        assertEquals(foldForSearch("никола"), foldForSearch("НИКОЛА"))
        assertEquals(foldForSearch("nikola"), foldForSearch("NIKOLA"))
        assertEquals(foldForSearch("Nikola"), foldForSearch("НИКОЛА"))
    }
}
