package com.orthodox.calendar.data.nameday

import android.content.Context
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.engine.ChurchDates
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.util.Locale

/**
 * The Russian name days bundled as `assets/localization/imeniny_ru.json`
 * (generated in the iOS repo by `scripts/russian/bundle_imeniny.py` from the
 * azbyka.ru calendar, and a byte-for-byte copy of its bundle): which names each
 * day keeps, every commemoration of each name, and the civil → church name
 * correspondences ("Иван" → "Иоанн").
 *
 * Mirror of `NameDayCatalog` in `OrthodoxCalendar/Models/NameDay.swift`.
 */
class NameDayCatalog private constructor(
    /** Church date → names, main first; "02-29" holds St John Cassian's. */
    val days: Map<String, List<DayName>>,
    val pascha: Map<Int, List<DayName>>,
    val rules: List<Pair<NameDayAnchor, List<String>>>,
    val names: Map<String, List<Commemoration>>,
    /** Folded civil or diminutive form → church forms, usual first. */
    val civil: Map<String, List<String>>
) {
    data class Commemoration(
        val name: String,
        val anchor: NameDayAnchor,
        val title: String,
        val isNewMartyr: Boolean
    ) {
        val id: String get() = "$name|$anchor|$title"
    }

    /** Folded church name → church name. */
    private val churchByFold: Map<String, String> = names.keys.associateBy { fold(it) }

    // MARK: - A day's names

    /** Everyone whose name day [day] is, main saints first, each name once. */
    fun names(day: CalendarDay): List<DayName> {
        var fixed = days[day.julianDate].orEmpty()
        // Julian February 29 exists every fourth year; otherwise St John
        // Cassian is kept with the 28th.
        val year = day.gregorianDate.take(4).toIntOrNull()
        if (day.julianDate == "02-28" && year != null && !ChurchDates.isJulianLeap(year)) {
            fixed = fixed + days["02-29"].orEmpty()
        }
        val moveable = pascha[day.paschaDistance].orEmpty()
        val ruled = rules.filter { it.first.matches(day) }
            .flatMap { r -> r.second.map { DayName(it, isMain = false) } }
        val all = moveable.filter { it.isMain } + fixed.filter { it.isMain } +
            moveable.filter { !it.isMain } + fixed.filter { !it.isMain } + ruled
        val seen = HashSet<String>()
        return all.filter { seen.add(it.name) }
    }

    // MARK: - Names

    /**
     * The church forms a name the user types stands for: "Иван" → ["Иоанн"],
     * "Юрий" → ["Георгий", "Юрий"], a church name itself → [it]. Empty when no
     * saint by that name is in the calendar.
     */
    fun churchForms(input: String): List<String> {
        val key = fold(input)
        if (key.isEmpty()) return emptyList()
        civil[key]?.let { forms -> return forms.filter { names[it] != null } }
        churchByFold[key]?.let { return listOf(it) }
        return emptyList()
    }

    /** One autocomplete row: a known name and the church form it stands for. */
    data class Suggestion(val name: String, val church: String)

    /** Autocomplete: known civil and church names starting with [prefix]. */
    fun suggestions(prefix: String, limit: Int = 8): List<Suggestion> {
        val key = fold(prefix)
        if (key.isEmpty()) return emptyList()
        val out = mutableListOf<Suggestion>()
        val seen = HashSet<String>()
        for (civ in civil.keys.sorted()) {
            if (!civ.startsWith(key)) continue
            val church = churchForms(civ).firstOrNull() ?: continue
            val display = civ.take(1).uppercase(RU) + civ.drop(1)
            if (seen.add(display)) out += Suggestion(display, church)
        }
        for ((folded, church) in churchByFold.entries.sortedBy { it.key }) {
            if (folded.startsWith(key) && seen.add(church)) out += Suggestion(church, church)
        }
        // Exact and shorter names first: "Иван" before "Иванна".
        return out.sortedWith(compareBy<Suggestion> { it.name.length }.thenBy { it.name }).take(limit)
    }

    /** Every commemoration of a church name. */
    fun commemorations(churchName: String): List<Commemoration> = names[churchName].orEmpty()

    /** Church names containing [query], for the manual saint picker. */
    fun churchNames(query: String, limit: Int = 30): List<String> {
        val key = fold(query)
        if (key.isEmpty()) return emptyList()
        return churchByFold.entries.filter { it.key.contains(key) }
            .sortedWith(compareBy<Map.Entry<String, String>> { if (it.key.startsWith(key)) 0 else 1 }.thenBy { it.value })
            .take(limit)
            .map { it.value }
    }

    /** A name day found from a birthday, and its date. */
    data class Found(val commemoration: Commemoration, val date: LocalDate)

    /**
     * The name day by the Church's custom: the first commemoration of the name
     * on or after the birthday (in [year]), wrapping into the next year. The
     * first church form with any commemoration decides ("Юрий" → Георгий). New
     * martyrs count only when asked for, as in azbyka.ru's own name-day finder
     * ("учитывать новомучеников") — or when a name has no other saint.
     */
    fun firstNameDay(
        churchForms: List<String>,
        birthMonth: Int,
        birthDay: Int,
        year: Int,
        includeNewMartyrs: Boolean = false
    ): Found? {
        if (birthMonth !in 1..12 || birthDay < 1) return null
        // February 29 in a common year counts from March 1.
        val birthday = LocalDate.of(year, birthMonth, 1).plusDays((birthDay - 1).toLong())
        for (form in churchForms) {
            val all = commemorations(form)
            val older = all.filter { !it.isNewMartyr }
            val candidates = if (includeNewMartyrs || older.isEmpty()) all else older
            val best = candidates
                .mapNotNull { c -> c.anchor.nextOccurrence(birthday)?.let { Found(c, it) } }
                .minByOrNull { it.date }
            if (best != null) return best
        }
        return null
    }

    companion object {
        /** How many names a day shows before "и другие". */
        const val DAY_LIST_LIMIT = 12

        private val RU = Locale.forLanguageTag("ru")

        val empty = NameDayCatalog(emptyMap(), emptyMap(), emptyList(), emptyMap(), emptyMap())

        @Volatile private var shared: NameDayCatalog? = null

        /** The bundled catalog, read once per process; empty if it cannot be read. */
        fun shared(context: Context): NameDayCatalog {
            shared?.let { return it }
            synchronized(this) {
                shared?.let { return it }
                val catalog = runCatching {
                    context.assets.open("localization/imeniny_ru.json").use { it.readBytes().decodeToString() }
                }.getOrNull()?.let(::parse) ?: empty
                shared = catalog
                return catalog
            }
        }

        fun fold(s: String): String = s.trim().lowercase(RU).replace('ё', 'е')

        /** The first [limit] names to show and how many "и другие" hides. */
        fun capped(list: List<DayName>, limit: Int = DAY_LIST_LIMIT): Pair<List<DayName>, Int> {
            // A cap that would hide just one name hides nothing.
            if (list.size <= limit + 1) return list to 0
            return list.take(limit) to (list.size - limit)
        }

        /** Parses the bundled JSON; null when it is not a catalog. */
        fun parse(text: String): NameDayCatalog? = runCatching {
            val root = Json.parseToJsonElement(text).jsonObject
            fun str(o: JsonObject, k: String) = o[k]?.jsonObject.orEmpty()
                .mapValues { it.value.jsonPrimitive.content }

            fun dayNames(s: String): List<DayName> {
                val halves = s.split(";")
                val main = halves.first().split(",").filter { it.isNotEmpty() }.map { DayName(it, true) }
                val other = if (halves.size > 1) {
                    halves[1].split(",").filter { it.isNotEmpty() }.map { DayName(it, false) }
                } else emptyList()
                return main + other
            }

            val rules = root["rules"]!!.jsonArray.map { r ->
                val o = r.jsonObject
                val windows = o["w"]!!.jsonArray.mapNotNull { w ->
                    val a = w.jsonArray
                    val wd = (a.getOrNull(0) as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
                    val first = (a.getOrNull(1) as? JsonPrimitive)?.takeIf { it.isString }?.content
                    val last = (a.getOrNull(2) as? JsonPrimitive)?.takeIf { it.isString }?.content
                    if (a.size == 3 && wd != null && first != null && last != null) NameDayWindow(wd, first, last) else null
                }
                val anchor: NameDayAnchor = NameDayAnchor.Weekday(windows)
                anchor to o["n"]!!.jsonPrimitive.content.split(",").filter { it.isNotEmpty() }
            }
            val titles = root["titles"]!!.jsonArray.map { it.jsonPrimitive.content }

            fun anchor(s: String): NameDayAnchor? = when {
                s.startsWith("P") -> s.drop(1).toIntOrNull()?.let { NameDayAnchor.Pascha(it) }
                s.startsWith("R") -> s.drop(1).toIntOrNull()?.let { rules.getOrNull(it)?.first }
                else -> NameDayAnchor.monthDay(s).let { (m, d) -> NameDayAnchor.Julian(m, d) }
            }

            val names = root["names"]!!.jsonObject.mapValues { (name, rows) ->
                rows.jsonArray.mapNotNull { row ->
                    val a = row as? JsonArray ?: return@mapNotNull null
                    if (a.size != 3) return@mapNotNull null
                    val key = (a[0] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return@mapNotNull null
                    val t = (a[1] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull ?: return@mapNotNull null
                    val title = titles.getOrNull(t) ?: return@mapNotNull null
                    val anchor = anchor(key) ?: return@mapNotNull null
                    val newMartyr = (a[2] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull == 1
                    Commemoration(name, anchor, title, newMartyr)
                }
            }
            val civil = root["civil"]!!.jsonObject.mapValues { (_, v) -> v.jsonArray.map { it.jsonPrimitive.content } }
            NameDayCatalog(
                days = str(root, "days").mapValues { dayNames(it.value) },
                pascha = str(root, "pascha").mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to dayNames(v) } }.toMap(),
                rules = rules,
                names = names,
                civil = civil
            )
        }.getOrNull()
    }
}
