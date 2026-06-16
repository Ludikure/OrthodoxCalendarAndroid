package com.orthodox.calendar.ui.screen.detail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.BibleTranslation
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.FastingPeriodInfo
import com.orthodox.calendar.data.model.FastingPeriods
import com.orthodox.calendar.data.model.Feast
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.data.model.Reflection
import com.orthodox.calendar.data.model.SaintBio
import com.orthodox.calendar.ui.theme.AppColors

// Common saint title prefixes -- too generic for matching
private val commonWords: Set<String> = setOf(
    "свети", "света", "светог", "светих", "светом",
    "преподобни", "преподобна", "преподобног",
    "мученик", "мученица", "мученици",
    "свештеномученик", "великомученик",
    "святой", "святая", "святых", "святителя",
    "преподобный", "преподобная", "мученик",
    "saint", "holy", "venerable", "martyr",
    "blessed", "righteous"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    day: CalendarDay,
    localization: LocalizationBundle,
    language: AppLanguage,
    bibleTranslation: BibleTranslation,
    periodInfo: FastingPeriodInfo?,
    onBack: () -> Unit,
    onAddReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isGreat = day.isGreatFeast

    val formattedDate = "${day.gregorianDay} ${localization.ui.months.getOrElse(day.gregorianMonth - 1) { "" }}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = formattedDate, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onAddReminder() }) {
                        Text(text = "\u2795", fontSize = 18.sp)
                    }
                    IconButton(onClick = { shareDay(context, day, localization, language) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isGreat) AppColors.headerBg else AppColors.cardBg,
                    titleContentColor = AppColors.darkText
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.warmBg)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            // Hero section
            HeroSection(day = day, localization = localization, language = language, periodInfo = periodInfo)

            // Content sections
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // Fasting section
                Spacer(modifier = Modifier.height(16.dp))
                FastingSection(day = day)
                SectionDivider()

                // Saints / Commemorations
                if (day.feasts.isNotEmpty()) {
                    SaintsSection(
                        day = day,
                        localization = localization,
                        language = language
                    )
                    SectionDivider()
                }

                // Readings
                if (day.readings.isNotEmpty()) {
                    ReadingsSection(
                        day = day,
                        localization = localization,
                        language = language,
                        bibleTranslation = bibleTranslation
                    )
                }

                // Reflection
                day.reflection?.let { reflection ->
                    if (reflection.text.isNotEmpty()) {
                        SectionDivider()
                        ReflectionSection(reflection = reflection)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun HeroSection(
    day: CalendarDay,
    localization: LocalizationBundle,
    language: AppLanguage,
    periodInfo: FastingPeriodInfo?
) {
    val isGreat = day.isGreatFeast

    val greatFeastLabel = when (language) {
        AppLanguage.SR -> "\u0412\u0435\u043B\u0438\u043A\u0438 \u043F\u0440\u0430\u0437\u043D\u0438\u043A"
        AppLanguage.RU -> "\u0412\u0435\u043B\u0438\u043A\u0438\u0439 \u043F\u0440\u0430\u0437\u0434\u043D\u0438\u043A"
        AppLanguage.EN, AppLanguage.EN_NC -> "Great Feast"
    }

    val heroBg = if (isGreat) {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    AppColors.headerBg,
                    AppColors.darkText.copy(alpha = 0.15f),
                    AppColors.warmBg
                )
            )
        )
    } else {
        Modifier.background(AppColors.cardBg)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(heroBg)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Fasting season badge (e.g. Great Lent) \u2014 driven by CalendarDay.fastingPeriod
        periodInfo?.let { period ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        AppColors.bannerBg,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(text = "\u26EA", fontSize = 11.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = period.displayName.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = AppColors.bannerTitle
                )
                if (period.complete) {
                    Text(
                        text = "  \u00B7  ${FastingPeriods.dayLabel(language, period.dayIndex, period.total)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.bannerSubtext
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Title
        day.primaryFeast?.let { primary ->
            Text(
                text = primary.name,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = if (isGreat) Color.White else AppColors.darkText
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Great feast subtitle
        if (isGreat) {
            Text(
                text = greatFeastLabel,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = AppColors.goldAccent
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Meta row: weekday + julian date
        Row(verticalAlignment = Alignment.CenterVertically) {
            val idx = day.weekdayIndex
            val fullDayName = localization.ui.daysOfWeekFull.getOrElse(idx) { "" }
            Text(
                text = fullDayName,
                fontSize = 12.sp,
                color = if (isGreat) AppColors.lightMuted else AppColors.mutedText
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "\u2022",
                fontSize = 12.sp,
                color = (if (isGreat) AppColors.lightMuted else AppColors.mutedText).copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${localization.ui.julianLabel} ${day.julianDate}",
                fontSize = 12.sp,
                color = if (isGreat) AppColors.lightMuted else AppColors.mutedText
            )
        }
    }
}

@Composable
private fun FastingSection(day: CalendarDay) {
    val (icon, color, bg) = fastingStyle(day.fasting.type)

    Column {
        // Large fasting badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(bg, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = day.fasting.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        if (day.fasting.explanation.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = day.fasting.explanation,
                fontSize = 14.sp,
                color = AppColors.bodyText,
                lineHeight = 20.sp,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

@Composable
private fun SaintsSection(
    day: CalendarDay,
    localization: LocalizationBundle,
    language: AppLanguage
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "\u2626", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = localization.ui.commemorationsLabel,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AppColors.darkText
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        day.feasts.forEachIndexed { index, feast ->
            SaintCard(
                feast = feast,
                bio = findBio(day, feast, index),
                localizedType = localizedSaintType(feast.type, language)
            )
            if (index < day.feasts.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ReadingsSection(
    day: CalendarDay,
    localization: LocalizationBundle,
    language: AppLanguage,
    bibleTranslation: BibleTranslation
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "\uD83D\uDCD6", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = localization.ui.readingsLabel,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AppColors.darkText
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        day.readings.forEachIndexed { index, reading ->
            ReadingCard(
                reading = reading,
                language = language,
                bibleTranslation = bibleTranslation
            )
            if (index < day.readings.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ReflectionSection(reflection: Reflection) {
    val goldAccent = AppColors.goldAccent

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "\uD83D\uDCAD", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = reflection.source,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AppColors.darkText
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(AppColors.cardBg, AppColors.warmBg)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .drawBehind {
                    drawRect(
                        color = goldAccent,
                        topLeft = Offset.Zero,
                        size = Size(3.dp.toPx(), size.height)
                    )
                }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = reflection.text,
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = AppColors.bodyText,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun SectionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
            .height(1.dp)
            .background(AppColors.warmBorder)
    )
}

// Bio matching logic ported from iOS
private fun findBio(day: CalendarDay, feast: Feast, index: Int): SaintBio? {
    val bios = day.saintBios
    if (bios.isNullOrEmpty()) return null
    if (feast.moveable) return null

    // Single bio per day (Serbian Ohridski Prolog): show on first non-moveable feast only
    if (bios.size == 1) {
        // Find first non-moveable feast index
        val firstNonMoveableIndex = day.feasts.indexOfFirst { !it.moveable }
        return if (index == firstNonMoveableIndex) bios[0] else null
    }

    // Multiple bios (EN/RU): match by significant keywords, each bio used once
    val usedBioTitles = mutableSetOf<String>()
    for (i in 0 until index) {
        val f = day.feasts[i]
        if (f.moveable) continue
        val words = f.name.lowercase().split(" ")
            .filter { it.length > 3 && it !in commonWords }
        for (bio in bios) {
            if (bio.title in usedBioTitles) continue
            val bioLower = bio.title.lowercase()
            if (words.any { bioLower.contains(it) }) {
                usedBioTitles.add(bio.title)
                break
            }
        }
    }

    // Match this feast to an unclaimed bio
    val feastWords = feast.name.lowercase().split(" ")
        .filter { it.length > 3 && it !in commonWords }

    return bios.firstOrNull { bio ->
        bio.title !in usedBioTitles &&
            feastWords.any { bio.title.lowercase().contains(it) }
    }
}

private fun localizedSaintType(type: String, language: AppLanguage): String {
    val typesSr = mapOf(
        "feast" to "\u041F\u0440\u0430\u0437\u043D\u0438\u043A",
        "saint" to "\u0421\u0432\u0435\u0442\u0438",
        "apostle" to "\u0410\u043F\u043E\u0441\u0442\u043E\u043B",
        "great_martyr" to "\u0412\u0435\u043B\u0438\u043A\u043E\u043C\u0443\u0447\u0435\u043D\u0438\u043A",
        "hierarch" to "\u0421\u0432\u0435\u0442\u0438\u0442\u0435\u0459",
        "equal_to_apostles" to "\u0420\u0430\u0432\u043D\u043E\u0430\u043F\u043E\u0441\u0442\u043E\u043B\u043D\u0438",
        "venerable" to "\u041F\u0440\u0435\u043F\u043E\u0434\u043E\u0431\u043D\u0438",
        "hieromartyr" to "\u0421\u0432\u0435\u0448\u0442\u0435\u043D\u043E\u043C\u0443\u0447\u0435\u043D\u0438\u043A",
        "venerable_martyr" to "\u041F\u0440\u0435\u043F\u043E\u0434\u043E\u0431\u043D\u043E\u043C\u0443\u0447\u0435\u043D\u0438\u043A",
        "martyr" to "\u041C\u0443\u0447\u0435\u043D\u0438\u043A",
        "righteous" to "\u041F\u0440\u0430\u0432\u0435\u0434\u043D\u0438",
        "blessed" to "\u0411\u043B\u0430\u0436\u0435\u043D\u0438",
        "confessor" to "\u0418\u0441\u043F\u043E\u0432\u0435\u0434\u043D\u0438\u043A",
        "noble" to "\u0411\u043B\u0430\u0433\u043E\u0432\u0435\u0440\u043D\u0438",
        "prophet" to "\u041F\u0440\u043E\u0440\u043E\u043A",
        "synaxis" to "\u0421\u0430\u0431\u043E\u0440"
    )
    val typesRu = mapOf(
        "feast" to "\u041F\u0440\u0430\u0437\u0434\u043D\u0438\u043A",
        "saint" to "\u0421\u0432\u044F\u0442\u043E\u0439",
        "apostle" to "\u0410\u043F\u043E\u0441\u0442\u043E\u043B",
        "great_martyr" to "\u0412\u0435\u043B\u0438\u043A\u043E\u043C\u0443\u0447\u0435\u043D\u0438\u043A",
        "hierarch" to "\u0421\u0432\u044F\u0442\u0438\u0442\u0435\u043B\u044C",
        "equal_to_apostles" to "\u0420\u0430\u0432\u043D\u043E\u0430\u043F\u043E\u0441\u0442\u043E\u043B\u044C\u043D\u044B\u0439",
        "venerable" to "\u041F\u0440\u0435\u043F\u043E\u0434\u043E\u0431\u043D\u044B\u0439",
        "hieromartyr" to "\u0421\u0432\u044F\u0449\u0435\u043D\u043D\u043E\u043C\u0443\u0447\u0435\u043D\u0438\u043A",
        "venerable_martyr" to "\u041F\u0440\u0435\u043F\u043E\u0434\u043E\u0431\u043D\u043E\u043C\u0443\u0447\u0435\u043D\u0438\u043A",
        "martyr" to "\u041C\u0443\u0447\u0435\u043D\u0438\u043A",
        "righteous" to "\u041F\u0440\u0430\u0432\u0435\u0434\u043D\u044B\u0439",
        "blessed" to "\u0411\u043B\u0430\u0436\u0435\u043D\u043D\u044B\u0439",
        "confessor" to "\u0418\u0441\u043F\u043E\u0432\u0435\u0434\u043D\u0438\u043A",
        "noble" to "\u0411\u043B\u0430\u0433\u043E\u0432\u0435\u0440\u043D\u044B\u0439",
        "prophet" to "\u041F\u0440\u043E\u0440\u043E\u043A",
        "synaxis" to "\u0421\u043E\u0431\u043E\u0440"
    )
    val typesEn = mapOf(
        "feast" to "Feast",
        "saint" to "Saint",
        "apostle" to "Apostle",
        "great_martyr" to "Great Martyr",
        "hierarch" to "Hierarch",
        "equal_to_apostles" to "Equal-to-the-Apostles",
        "venerable" to "Venerable",
        "hieromartyr" to "Hieromartyr",
        "venerable_martyr" to "Venerable Martyr",
        "martyr" to "Martyr",
        "righteous" to "Righteous",
        "blessed" to "Blessed",
        "confessor" to "Confessor",
        "noble" to "Right-believing",
        "prophet" to "Prophet",
        "synaxis" to "Synaxis"
    )
    return when (language) {
        AppLanguage.SR -> typesSr[type] ?: type
        AppLanguage.RU -> typesRu[type] ?: type
        AppLanguage.EN, AppLanguage.EN_NC -> typesEn[type] ?: type
    }
}

@Composable
private fun fastingStyle(type: String): Triple<String, Color, Color> {
    val t = type.lowercase()
    return when {
        t == "totalabstinence" || t == "strict" -> Triple("\uD83D\uDEAB", AppColors.fastStrict, AppColors.fastStrictBg)
        t == "dryeating" -> Triple("\uD83C\uDF5E", AppColors.fastStrict, AppColors.fastStrictBg)
        t == "hotnooil" || t == "hotwithoutoil" || t == "water" -> Triple("\uD83D\uDCA7", AppColors.fastWater, AppColors.fastWaterBg)
        t.contains("nooil") -> Triple("\uD83D\uDCA7", AppColors.fastWater, AppColors.fastWaterBg)
        t.contains("oil") -> Triple("\uD83E\uDED2", AppColors.fastOil, AppColors.fastOilBg)
        t.contains("fish") || t.contains("roe") -> Triple("\uD83D\uDC1F", AppColors.fastFish, AppColors.fastFishBg)
        else -> Triple("\u2713", AppColors.fastFree, AppColors.fastFreeBg)
    }
}

private fun shareDay(
    context: Context,
    day: CalendarDay,
    localization: LocalizationBundle,
    language: AppLanguage
) {
    val monthName = localization.ui.months.getOrElse(day.gregorianMonth - 1) { "" }
    val lines = mutableListOf<String>()

    lines.add("\u2626 ${day.gregorianDay} $monthName")
    lines.add("")

    day.primaryFeast?.let { lines.add(it.name) }
    day.feasts.drop(1).forEach { lines.add("\u2022 ${it.name}") }

    lines.add("")
    lines.add(day.fasting.label)

    if (day.readings.isNotEmpty()) {
        lines.add("")
        day.readings.forEach { lines.add(it.displayReference) }
    }

    val shareText = lines.joinToString("\n")
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, shareText)
        this.type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}
