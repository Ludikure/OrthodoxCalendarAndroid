package com.orthodox.calendar.engine

/**
 * Pairs each fixed (non-moveable) feast of a day with the saint biography that
 * belongs to it by comparing the distinguishing words of the feast name with
 * the words of each bio title.
 *
 * Rank, place and liturgical words ("свети", "мученик", "прп.", "bishop",
 * "цариградски") carry no identity and are ignored. The remaining words are
 * compared with tolerance for inflection (Вартоломеј/Вартоломеја, Петар/Петра),
 * spelling variants (Јевстатије/Евстатије, Тедот/Теодот) and OCR typos. A bio
 * whose title *is* a feast's name is paired with it first; the rest are assigned
 * best-first across the whole day, and a weak match is accepted only when it is
 * the sole remaining candidate, because a wrong bio is worse than none.
 *
 * `scripts/shared/simulate_bio_matching.py` in the iOS repo is the reference
 * implementation and runs the same rules over a whole year of data; keep this,
 * it and iOS `BioMatcher.swift` in sync. `BioMatcherTest` checks this port
 * against assignments generated from the reference.
 */
object BioMatcher {

    /** Returns feast index -> bio index for every feast that gets a bio. */
    fun assign(feastNames: List<String>, moveable: List<Boolean>, bioTitles: List<String>): Map<Int, Int> {
        if (bioTitles.isEmpty() || feastNames.size != moveable.size) return emptyMap()
        val fixed = feastNames.indices.filter { !moveable[it] }
        val feastAll = feastNames.map { tokens(it).toSet() }
        val feastSig = feastNames.map { significant(it) }
        val bioAll = bioTitles.map { tokens(it).toSet() }
        val bioSig = bioTitles.map { significant(it) }

        fun pairScore(i: Int, j: Int): Int {
            if (feastSig[i].isNotEmpty()) {
                val s = score(feastSig[i], bioSig[j])
                if (s > 0) return s
                // Names that differ only in generic words ("Сабор светих дванаест апостола"
                // vs "Сабор светих славних апостола") still qualify as a weak candidate.
                return if (feastAll[i].intersect(bioAll[j]).size >= 3) 1 else 0
            }
            // No distinguishing word at all ("Сабор Пресвете Богородице"): the whole
            // name has to be contained in the title.
            return if (feastAll[i].isNotEmpty() && bioAll[j].containsAll(feastAll[i])) 2 else 0
        }

        val scores = HashMap<Int, List<Int>>()
        val pairs = ArrayList<Triple<Int, Int, Int>>()   // (score, feast, bio)
        for (i in fixed) {
            val row = bioTitles.indices.map { pairScore(i, it) }
            scores[i] = row
            for (j in bioTitles.indices) pairs.add(Triple(row[j], i, j))
        }
        pairs.sortWith(compareByDescending<Triple<Int, Int, Int>> { it.first }
            .thenBy { it.second }
            .thenBy { it.third })

        val result = HashMap<Int, Int>()
        val usedBios = HashSet<Int>()
        // A bio whose title *is* a feast's name names that feast and no other.
        // Pair those first: the greedy pass below breaks score ties by position,
        // so on a day with several similar names (three Macarii, "Constantine and
        // Helen" next to "Helen of Dechani") the bio would otherwise go to
        // whichever tying feast comes first. A named bio left over duplicates one
        // already placed — it stays unassigned rather than landing on a stranger.
        val feastWords = feastNames.map { tokens(it) }
        val bioWords = bioTitles.map { tokens(it) }
        val names = feastWords.filter { it.isNotEmpty() }.toSet()
        for ((j, title) in bioWords.withIndex()) {
            if (title !in names) continue
            val i = fixed.firstOrNull { result[it] == null && feastWords[it] == title }
            if (i != null) result[i] = j
            usedBios.add(j)
        }
        for ((s, feast, bio) in pairs) {
            if (s < 2) break
            if (result[feast] != null || bio in usedBios) continue
            result[feast] = bio
            usedBios.add(bio)
        }
        // Weak matches only when there is exactly one candidate left for the feast.
        for (i in fixed) {
            if (result[i] != null) continue
            val candidates = bioTitles.indices.filter { it !in usedBios && scores[i]!![it] >= 1 }
            if (candidates.size == 1) {
                result[i] = candidates[0]
                usedBios.add(candidates[0])
            }
        }
        // Legacy single-bio day (one combined text): show it on the first fixed feast.
        if (result.isEmpty() && bioTitles.size == 1 && 0 !in usedBios) {
            fixed.firstOrNull()?.let { result[it] = 0 }
        }
        return result
    }

    // MARK: - Words

    private val WORD = Regex("[\\p{L}\\p{N}_]+")

    /**
     * Lower-cased words of a name. Numbers are dropped; two-letter words count
     * only when capitalised (a name like "Ор" or a numeral like "II", not "св").
     */
    fun tokens(text: String): List<String> {
        val out = ArrayList<String>()
        for (match in WORD.findAll(text)) {
            val raw = match.value
            val first = raw[0]
            if (first.isDigit()) continue
            if (raw.length < 2) continue
            if (raw.length == 2 && !first.isUpperCase()) continue
            out.add(raw.lowercase())
        }
        return out
    }

    fun significant(text: String): List<String> = tokens(text).filter { it !in generic }

    /** Sum over the feast's words of the best match among the bio's words. */
    fun score(feastWords: List<String>, bioWords: List<String>): Int =
        feastWords.sumOf { f -> bioWords.maxOfOrNull { tokenMatch(f, it) } ?: 0 }

    /**
     * 3 = same word, 2 = same word inflected or misspelt, 1 = weak (needs a
     * unique candidate), 0 = different words.
     */
    fun tokenMatch(a: String, b: String): Int {
        if (a == b) return 3
        val n = minOf(a.length, b.length)
        val m = maxOf(a.length, b.length)
        if (n >= 5 && a.take(5) == b.take(5) && m - n <= 3) return 2
        if (n >= 5 && m - n <= 2) {
            val dist = osaDistance(a, b)
            if (dist <= 1) {
                // A single substitution inside a short name (Матија/Марија) is a
                // different name; a dropped, added or swapped letter, or a change in
                // the last two letters, is inflection or a typo (Тедот/Теодот,
                // Петар/Петра, Патрокло/Патрокле).
                if (m != n || a.dropLast(2) == b.dropLast(2) || n >= 8) return 2
                return 0
            }
            if (dist <= 2 && n >= 8) return 2
        }
        if (n >= 4 && a.take(4) == b.take(4) && m - n <= 4) return 1
        if (n in 3..4 && m - n <= 1 && a.take(2) == b.take(2) && osaDistance(a, b) <= 1) return 1
        return 0
    }

    /** Optimal string alignment distance: Levenshtein plus adjacent transposition. */
    fun osaDistance(a: String, b: String): Int {
        val la = a.length
        val lb = b.length
        if (la == 0 || lb == 0) return maxOf(la, lb)
        val d = Array(la + 1) { IntArray(lb + 1) }
        for (i in 0..la) d[i][0] = i
        for (j in 0..lb) d[0][j] = j
        for (i in 1..la) {
            for (j in 1..lb) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
                }
            }
        }
        return d[la][lb]
    }

    /**
     * Words that name a rank, place, feast type or number rather than a person,
     * for Serbian, Russian and English titles together.
     */
    val generic: Set<String> = """
abbess abbot africa after afterfeast alexandria all and antioch apostle apostles archbishop
armenia asia athos before bishop bishops blessed britain brother brothers bulgaria caesarea
caves child children christ commemoration companions conception confessor confessors
constantinople cyprus daughter deacon deacons disciple disciples dormition egypt elder
emperor empress england father feast fool for forefeast gaul georgia god great greece hands
head her hermit hermits hieromartyr hieromartyrs him his holy honorable hundred icon icons
ii iii ireland italy jerusalem kiev king leavetaking martyr martyred martyrs metropolitan
monastery monk monks moscow most mother nativity new nicomedia novgorod nun nuns of others
our palestine patriarch persia placing priest priests prince princess prophet prophets queen
relics righteous robe romania rome russia saint saints scotland serbia seventy sinai sister
sisters soldiers son spain st sts sunday synaxis syria the them theotokos thessalonica those
thousand translation twelve uncovering venerable venerables virgin wales who wife with
wonderworker александрийского александријски анахорет антиохийского антиохијски ап апостол
апостола апостоли апостолов апп архидиакона архиепископ архиепископа архимандрит
архимандрита атонски афонского блажена блажени блгв блгвв блж ближних богородица богородице
богородицы божией брат брата велика велики великого великомученик великомученица вериге вмц
вмч воинов војник војника војници всех всея второго главе главы господа господня господње
господњег грузијски дальних два дев девица девице девојака девојке девы день десет дете деца
деце диакона дня друга други другим других египатски египетского его епископ епископа
епископи епископов жен жена затворника зачатие зачеће игуман игумана игуманија игумена
игумении иеродиакона иеромонаха иерусалимского иже икона иконе иконы инока иных исп
исповедник исповедника исповедников исповедници киевского кипарски кн кнез кнеза княгини
князя константинопольского које који краљ краља кћи матери мати мајка митрополит митрополита
монах монаха монахини монахиња московски московского мошти моштију мощей мощи мужей мученик
мученика мучеников мучениц мученица мученице мученици мученицима мц мцц мч мчч ним ними нова
новгородски новгородского нови нового новомученик новомученика новомучеников новомученици
новомч новомчч обретение обретење остале остали отац отаца отдание отца отцов оци
палестински память патриарха патријарх патријарха первого перенесение персидских пет
печерски печерских печерского пещерах подвижник подвижника положение попразднство послушника
прав праведна праведни предпразднство презвитер презвитера пренос преноса преподобна
преподобне преподобни преподобних преподобног преподобномученик преподобномученица
преподобномученици преподобных прес пресв пресвете пресвитера пресвитеров пресвятой прмц
прмцц прмч прмчч прор пророк пророка пророков прочих прп прписп прпп пустињак равноап ради
ризе ризы римски римского рождество рођење руке руси сабор св света свете свети светитељ
светитеља светих свето светог светога светогорски светом свештеномученик свештеномученика
свештеномученици свих свт свтт святая святителя святой святых священника седам седамдесет
седамдесеторице сестра сестре син сина синајски собор солунски спомен српски старац старца
столпник студенички схимник схимонах сщ сщисп сщмч сщмчч трећи три успение успеније успења
ученик ученика ученици хиландарски христа христов христових цар цара цариградски царица
царице царицы царя часне часних честных четири чудотворац чудотворца чудотворцев шест
юродивого ђакон ђакона јерусалимски јуродиви људи њим њима
""".trim().split(Regex("\\s+")).toSet()
}
