package com.orthodox.calendar.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.orthodox.calendar.ui.theme.AppColors
import java.util.Locale

/** Icon, ink colour and background tint for one fasting type. */
data class FastingVisuals(
    val icon: String,
    val color: Color,
    val background: Color
)

/**
 * Fasting types are the pipeline's ASCII codes, compared in ASCII.
 *
 * `Locale.ROOT` is stated for the reader, not needed for the device: Kotlin's
 * no-argument `lowercase()` is already locale-invariant — the stdlib calls
 * `toLowerCase(Locale.ROOT)` — unlike Java's `toLowerCase()`. The codes contain
 * no capital I either, so no device language ever changed how they fold.
 */
fun normalizeFastingType(type: String): String = type.lowercase(Locale.ROOT)

/**
 * The one place a fasting type becomes a colour.
 *
 * There were four private copies of this mapping — the month row's badge, the
 * detail screen's badge, the grid cell's tint, and the selected-day dot — with
 * slightly different fall-throughs (`else -> Transparent` against
 * `null -> Transparent`). The classifier they all call, [fastingStyle], is
 * tested; the colours it decides were not, and are exactly the part that drifts.
 */
@Composable
fun fastingVisuals(type: String): FastingVisuals {
    val t = normalizeFastingType(type)
    return when (fastingStyle(t)) {
        FastingStyle.STRICT -> FastingVisuals(
            icon = if (t == "dryeating") "\uD83C\uDF5E" else "\uD83D\uDEAB",
            color = AppColors.fastStrict,
            background = AppColors.fastStrictBg
        )
        FastingStyle.WATER -> FastingVisuals(
            icon = "\uD83D\uDCA7",
            color = AppColors.fastWater,
            background = AppColors.fastWaterBg
        )
        FastingStyle.OIL -> FastingVisuals(
            icon = "\uD83E\uDED2",
            color = AppColors.fastOil,
            background = AppColors.fastOilBg
        )
        FastingStyle.FISH -> FastingVisuals(
            icon = "\uD83D\uDC1F",
            color = AppColors.fastFish,
            background = AppColors.fastFishBg
        )
        FastingStyle.FREE -> FastingVisuals(
            icon = "\u2713",
            color = AppColors.fastFree,
            background = AppColors.fastFreeBg
        )
    }
}

/**
 * The bucket's ink colour, or null when the day does not fast — those days are
 * left uncoloured rather than coloured with a "free" colour, in the grid especially,
 * where a tint on every non-fasting day would say nothing.
 */
@Composable
fun fastingInkOrNull(type: String): Color? =
    when (fastingStyle(normalizeFastingType(type))) {
        FastingStyle.STRICT -> AppColors.fastStrict
        FastingStyle.WATER -> AppColors.fastWater
        FastingStyle.OIL -> AppColors.fastOil
        FastingStyle.FISH -> AppColors.fastFish
        FastingStyle.FREE -> null
    }

/** As [fastingInkOrNull], for a call site that draws something either way. */
@Composable
fun fastingInk(type: String): Color = fastingInkOrNull(type) ?: Color.Transparent
