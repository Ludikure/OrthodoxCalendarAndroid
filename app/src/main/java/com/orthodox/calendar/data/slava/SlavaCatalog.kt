package com.orthodox.calendar.data.slava

import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.Feast
import com.orthodox.calendar.ui.util.foldForSearch

/**
 * The slavas Serbian families commonly keep, offered first in the picker.
 *
 * Built from the pipeline's `isSlava` flags (pravoslavno.rs) merged with
 * crkvenikalendar.rs's list of Serbian slavas, which adds the moveable ones,
 * Павловдан, and the second commemoration on days both keep (Лучиндан beside
 * St Peter of Cetinje, Ћириловдан beside St Auxentius). Dates are Julian — the
 * civil date is 13 days later — or a distance from Pascha. Any other
 * commemoration can still be chosen by searching the whole calendar, and a date
 * the calendar doesn't name through "Други датум".
 *
 * Mirror of `OrthodoxCalendar/Models/SlavaCatalog.swift`; the lists must agree.
 */
object SlavaCatalog {
    /**
     * The slava [feast] is, on [day], or null when it isn't one.
     *
     * A day can hold two slavas (Лучиндан beside St Peter of Cetinje), so the
     * common list is matched by name, not just date. A feast the data flags as a
     * slava but the list doesn't name still counts, kept by its church date.
     */
    fun slava(feast: Feast, day: CalendarDay): SlavaDay? {
        val name = foldForSearch(feast.name)
        common.filter { it.matches(day) }.firstOrNull {
            name.contains(foldForSearch(it.name)) || name.contains(foldForSearch(it.saint))
        }?.let { return it }
        if (!feast.isSlava || feast.moveable) return null
        val parts = day.julianDate.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 2) return null
        return SlavaDay(feast.name, feast.name, SlavaAnchor.Julian(parts[0], parts[1]))
    }

    val moveable: List<SlavaDay> = listOf(
        SlavaDay("Лазарева субота", "Васкрсење праведног Лазара", SlavaAnchor.Pascha(-8)),
        SlavaDay("Цвети", "Улазак Господа Исуса Христа у Јерусалим", SlavaAnchor.Pascha(-7)),
        SlavaDay("Спасовдан", "Вазнесење Господње", SlavaAnchor.Pascha(39)),
        SlavaDay("Тројичиндан", "Педесетница – Силазак Светог Духа", SlavaAnchor.Pascha(49)),
        SlavaDay("Духовдан", "Духовски понедељак – Дан Светог Духа", SlavaAnchor.Pascha(50)),
    )

    /** In civil-calendar order through the year (January 2 is Julian 20 December). */
    val fixed: List<SlavaDay> = listOf(
        SlavaDay("Игњатијевдан", "Свети Игњатије Богоносац", SlavaAnchor.Julian(12, 20)),
        SlavaDay("Божић", "Рождество Христово", SlavaAnchor.Julian(12, 25)),
        SlavaDay("Стевањдан", "Свети првомученик и архиђакон Стефан", SlavaAnchor.Julian(12, 27)),
        SlavaDay("Василијевдан", "Свети Василије Велики", SlavaAnchor.Julian(1, 1)),
        SlavaDay("Богојављење", "Крштење Господње", SlavaAnchor.Julian(1, 6)),
        SlavaDay("Јовањдан", "Сабор светог Јована Крститеља", SlavaAnchor.Julian(1, 7)),
        SlavaDay("Савиндан", "Свети Сава, први архиепископ српски", SlavaAnchor.Julian(1, 14)),
        SlavaDay("Часне вериге", "Часне вериге светог апостола Петра", SlavaAnchor.Julian(1, 16)),
        SlavaDay("Трифундан", "Свети мученик Трифун", SlavaAnchor.Julian(2, 1)),
        SlavaDay("Сретење", "Сретење Господње", SlavaAnchor.Julian(2, 2)),
        SlavaDay("Симеоновдан", "Свети Симеон Богопримац и Ана пророчица", SlavaAnchor.Julian(2, 3)),
        SlavaDay("Ћириловдан", "Свети Кирило Словенски", SlavaAnchor.Julian(2, 14)),
        SlavaDay("Свети Авксентије", "Преподобни Авксентије", SlavaAnchor.Julian(2, 14)),
        SlavaDay("Младенци", "Светих четрдесет мученика Севастијских", SlavaAnchor.Julian(3, 9)),
        SlavaDay("Благовести", "Благовештење Пресвете Богородице", SlavaAnchor.Julian(3, 25)),
        SlavaDay("Ђурђевдан", "Свети великомученик Георгије", SlavaAnchor.Julian(4, 23)),
        SlavaDay("Марковдан", "Свети апостол и јеванђелист Марко", SlavaAnchor.Julian(4, 25)),
        SlavaDay("Свети Василије Острошки", "Свети Василије Острошки Чудотворац", SlavaAnchor.Julian(4, 29)),
        SlavaDay("Јеремијевдан", "Свети пророк Јеремија", SlavaAnchor.Julian(5, 1)),
        SlavaDay("Јовањдан пролетњи", "Свети апостол и јеванђелист Јован Богослов", SlavaAnchor.Julian(5, 8)),
        SlavaDay("Летњи Никола", "Пренос моштију светог Николаја", SlavaAnchor.Julian(5, 9)),
        SlavaDay("Ћирило и Методије", "Свети Кирило и Методије", SlavaAnchor.Julian(5, 11)),
        SlavaDay("Цар Константин и царица Јелена", "Свети цар Константин и царица Јелена", SlavaAnchor.Julian(5, 21)),
        SlavaDay("Видовдан", "Свети кнез Лазар и свети српски мученици", SlavaAnchor.Julian(6, 15)),
        SlavaDay("Ивањдан", "Рођење светог Јована Претече", SlavaAnchor.Julian(6, 24)),
        SlavaDay("Петровдан", "Свети апостоли Петар и Павле", SlavaAnchor.Julian(6, 29)),
        SlavaDay("Павловдан", "Сабор светих дванаест апостола", SlavaAnchor.Julian(6, 30)),
        SlavaDay("Прокопијевдан", "Свети великомученик Прокопије", SlavaAnchor.Julian(7, 8)),
        SlavaDay("Свети арханђел Гаврило", "Сабор светог арханђела Гаврила", SlavaAnchor.Julian(7, 13)),
        SlavaDay("Огњена Марија", "Света великомученица Марина", SlavaAnchor.Julian(7, 17)),
        SlavaDay("Илиндан", "Свети пророк Илија", SlavaAnchor.Julian(7, 20)),
        SlavaDay("Блага Марија", "Света Марија Магдалина", SlavaAnchor.Julian(7, 22)),
        SlavaDay("Света Петка Римљанка", "Преподобномученица Параскева Римљанка", SlavaAnchor.Julian(7, 26)),
        SlavaDay("Пантелијевдан", "Свети великомученик Пантелејмон", SlavaAnchor.Julian(7, 27)),
        SlavaDay("Преображење", "Преображење Господње", SlavaAnchor.Julian(8, 6)),
        SlavaDay("Велика Госпојина", "Успеније Пресвете Богородице", SlavaAnchor.Julian(8, 15)),
        SlavaDay("Усековање", "Усековање главе светог Јована Крститеља", SlavaAnchor.Julian(8, 29)),
        SlavaDay("Мала Госпојина", "Рођење Пресвете Богородице", SlavaAnchor.Julian(9, 8)),
        SlavaDay("Свети Јоаким и Ана", "Свети праведници Јоаким и Ана", SlavaAnchor.Julian(9, 9)),
        SlavaDay("Крстовдан", "Воздвижење часног Крста", SlavaAnchor.Julian(9, 14)),
        SlavaDay("Зачеће светог Јована", "Зачеће светог Јована Претече и Крститеља", SlavaAnchor.Julian(9, 23)),
        SlavaDay("Јовањдан јесењи", "Свети апостол и јеванђелист Јован Богослов", SlavaAnchor.Julian(9, 26)),
        SlavaDay("Михољдан", "Преподобни Киријак Отшелник", SlavaAnchor.Julian(9, 29)),
        SlavaDay("Покров Пресвете Богородице", "Покров Пресвете Богородице", SlavaAnchor.Julian(10, 1)),
        SlavaDay("Томиндан", "Свети апостол Тома", SlavaAnchor.Julian(10, 6)),
        SlavaDay("Срђевдан", "Свети мученици Сергије и Вакх", SlavaAnchor.Julian(10, 7)),
        SlavaDay("Петковица", "Преподобна мати Параскева Трнова", SlavaAnchor.Julian(10, 14)),
        SlavaDay("Лучиндан", "Свети апостол и јеванђелист Лука", SlavaAnchor.Julian(10, 18)),
        SlavaDay("Свети Петар Цетињски", "Свети Петар Цетињски", SlavaAnchor.Julian(10, 18)),
        SlavaDay("Свети Прохор Пчињски", "Преподобни Прохор Пчињски", SlavaAnchor.Julian(10, 19)),
        SlavaDay("Митровдан", "Свети великомученик Димитрије", SlavaAnchor.Julian(10, 26)),
        SlavaDay("Свети Аврамије", "Свети Аврамије Затворник", SlavaAnchor.Julian(10, 29)),
        SlavaDay("Врачеви", "Свети Козма и Дамјан", SlavaAnchor.Julian(11, 1)),
        SlavaDay("Ђурђиц", "Обновљење храма светог великомученика Георгија", SlavaAnchor.Julian(11, 3)),
        SlavaDay("Аранђеловдан", "Сабор светог арханђела Михаила", SlavaAnchor.Julian(11, 8)),
        SlavaDay("Мратиндан", "Свети краљ Стефан Дечански", SlavaAnchor.Julian(11, 11)),
        SlavaDay("Свети Јован Милостиви", "Свети Јован Милостиви", SlavaAnchor.Julian(11, 12)),
        SlavaDay("Свети Јован Златоусти", "Свети Јован Златоусти", SlavaAnchor.Julian(11, 13)),
        SlavaDay("Матејевдан", "Свети апостол и јеванђелист Матеј", SlavaAnchor.Julian(11, 16)),
        SlavaDay("Ваведење", "Ваведење Пресвете Богородице", SlavaAnchor.Julian(11, 21)),
        SlavaDay("Свети Алимпије", "Преподобни Алимпије Столпник", SlavaAnchor.Julian(11, 26)),
        SlavaDay("Андрејевдан", "Свети апостол Андреј Првозвани", SlavaAnchor.Julian(11, 30)),
        SlavaDay("Никољдан", "Свети Николај Чудотворац", SlavaAnchor.Julian(12, 6)),
    )

    val common: List<SlavaDay> = moveable + fixed
}
