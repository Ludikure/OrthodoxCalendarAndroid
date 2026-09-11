package com.orthodox.calendar.ui.util

import java.util.Locale
/**
 * Folds Cyrillic and Latin to one form so saint search matches across scripts.
 *
 * Serbian is read in both alphabets but the `sr` data is Cyrillic-only, so a
 * query typed as "Nikola" or "Djordje" used to match nothing at all. Both the
 * query and the name being searched go through this, so same-script search is
 * unaffected either way.
 *
 * The target is Serbian Latin, because that is the bi-script case this exists
 * for: ч/ћ/ц fold to `c` and ш to `s`. A Russian romanization ("ch", "sh",
 * "ts") would leave a Serb typing the spelling they actually use matching
 * nothing. Latin diacritics fold the same way, so "Ćirilo" and "Cirilo" land on
 * the same string. Lossy by design — it decides what to show, never what the
 * data says.
 *
 * Mirror of `SaintSearchView.fold` in the iOS repo; the tables must agree.
 */
fun foldForSearch(text: String): String =
    text.lowercase(Locale.ROOT).map { folding[it] ?: it.toString() }.joinToString("")

private val folding: Map<Char, String> = mapOf(
    // Serbian Cyrillic, in Serbian Latin. љ and њ are easy to leave out and
    // doing so makes every name containing them unreachable from Latin.
    'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'ђ' to "dj",
    'е' to "e", 'ж' to "z", 'з' to "z", 'и' to "i", 'ј' to "j", 'к' to "k",
    'л' to "l", 'љ' to "lj", 'м' to "m", 'н' to "n", 'њ' to "nj", 'о' to "o",
    'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'ћ' to "c", 'у' to "u",
    'ф' to "f", 'х' to "h", 'ц' to "c", 'ч' to "c", 'џ' to "dz", 'ш' to "s",
    // Russian-only letters, folded consistently with the above.
    'ё' to "e", 'й' to "i", 'щ' to "sc", 'ъ' to "", 'ы' to "i", 'ь' to "",
    'э' to "e", 'ю' to "ju", 'я' to "ja",
    // Other Cyrillic that turns up in transliterated sources.
    'є' to "je", 'ї' to "ji", 'і' to "i", 'ў' to "u", 'ґ' to "g", 'ѣ' to "e",
    'ѳ' to "th", 'ѵ' to "i",
    // Latin diacritics, so a correctly typed Serbian query folds identically.
    'đ' to "dj", 'ć' to "c", 'č' to "c", 'š' to "s", 'ž' to "z",
)
