package com.orthodox.calendar.ui.util

/**
 * Which visual bucket a fasting type falls in.
 *
 * The pipeline emits exactly seven values (`fasting_engine.py`): free,
 * hotWithOil, hotNoOil, fish, fishRoe, dryEating, totalAbstinence. Classifying
 * them with `contains` is order-sensitive — `"hotNoOil".contains("oil")` is
 * true — so the water case has to be tested before the oil one. That ordering
 * used to be hand-written at four call sites and was wrong at one of them,
 * which painted a fasting day as an oil day. It lives here now, with a test
 * over all seven values.
 */
enum class FastingStyle { STRICT, WATER, OIL, FISH, FREE }

fun fastingStyle(type: String): FastingStyle {
    val t = type.lowercase()
    return when {
        t == "totalabstinence" || t == "dryeating" -> FastingStyle.STRICT
        t.contains("nooil") -> FastingStyle.WATER
        t.contains("oil") -> FastingStyle.OIL
        t.contains("fish") || t.contains("roe") -> FastingStyle.FISH
        else -> FastingStyle.FREE
    }
}
