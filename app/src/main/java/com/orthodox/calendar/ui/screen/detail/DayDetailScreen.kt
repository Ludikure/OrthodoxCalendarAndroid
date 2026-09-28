package com.orthodox.calendar.ui.screen.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
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
import com.orthodox.calendar.ui.util.backLabel
import com.orthodox.calendar.ui.util.shareLabel
import com.orthodox.calendar.data.model.BibleTranslation
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.FastingPeriodInfo
import com.orthodox.calendar.data.model.FastingPeriods
import com.orthodox.calendar.data.model.Feast
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.data.model.Reflection
import com.orthodox.calendar.data.model.SaintBio
import com.orthodox.calendar.engine.BioMatcher
import com.orthodox.calendar.data.slava.SlavaCatalog
import com.orthodox.calendar.data.slava.SlavaDay
import com.orthodox.calendar.data.slava.SlavaMark
import com.orthodox.calendar.data.slava.SlavaStore
import com.orthodox.calendar.data.slava.SlavaText
import com.orthodox.calendar.ui.util.Haptics
import com.orthodox.calendar.ui.util.rememberNotificationPermissionRequest
import com.orthodox.calendar.ui.util.toIsoDate
import kotlinx.coroutines.delay
import java.time.LocalDate
import com.orthodox.calendar.ui.util.fastingVisuals
import com.orthodox.calendar.ui.theme.AppColors
import java.util.Locale

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
    modifier: Modifier = Modifier,
    /** The user's slava settings; marks and offers show only in Serbian. */
    slavaStore: SlavaStore? = null,
    /** Today as `yyyy-MM-dd`, from the ViewModel. */
    today: String = LocalDate.now().toIsoDate(),
    /** Called after a slava is set from a saint card, once notifications were asked for. */
    onSlavaSet: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isGreat = day.isGreatFeast
    val slavaSettings = slavaStore?.settings?.collectAsState()?.value
        ?.takeIf { language == AppLanguage.SR }
    /** The slava just set from a saint card, shown in the undo toast. */
    var justSetSlava by remember { mutableStateOf<SlavaDay?>(null) }
    LaunchedEffect(justSetSlava) {
        if (justSetSlava != null) {
            delay(5_000)
            justSetSlava = null
        }
    }
    val askForNotifications = rememberNotificationPermissionRequest(onSlavaSet)
    // A saint card offers "set as your slava" only on a slava feast, in Serbian,
    // and only until the user has one — after that it never shows.
    val slavaOffer: (Feast) -> SlavaDay? = { feast ->
        if (slavaSettings == null || slavaSettings.mine != null) null
        else SlavaCatalog.slava(feast, day)
    }
    val setSlava: (SlavaDay) -> Unit = { slava ->
        Haptics.medium(view)
        slavaStore?.update { it.copy(mine = slava) }
        justSetSlava = slava
        askForNotifications()
    }

    val formattedDate = localization.ui.dayAndMonth(day.gregorianDay, day.gregorianMonth)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = formattedDate, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backLabel(language))
                    }
                },
                actions = {
                    IconButton(onClick = { onAddReminder() }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = when (language) {
                                AppLanguage.SR -> "Додај подсетник"
                                AppLanguage.RU -> "Добавить напоминание"
                                AppLanguage.EN, AppLanguage.EN_NC -> "Add reminder"
                            }
                        )
                    }
                    IconButton(onClick = {
                        if (!shareDay(context, day, localization, language)) {
                            val noShareAppText = when (language) {
                                AppLanguage.SR -> "\u041D\u0435\u043C\u0430 \u0430\u043F\u043B\u0438\u043A\u0430\u0446\u0438\u0458\u0435 \u0437\u0430 \u0434\u0435\u0459\u045A\u0435."
                                AppLanguage.RU -> "\u041D\u0435\u0442 \u043F\u0440\u0438\u043B\u043E\u0436\u0435\u043D\u0438\u044F \u0434\u043B\u044F \u043E\u0442\u043F\u0440\u0430\u0432\u043A\u0438."
                                AppLanguage.EN, AppLanguage.EN_NC -> "No app available to share with."
                            }
                            Toast.makeText(context, noShareAppText, Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = shareLabel(language))
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
      Box(modifier = Modifier.fillMaxSize()) {
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
                // The user's or a friends' slava on this day (Serbian only)
                slavaSettings?.mark(day)?.let { mark ->
                    Spacer(modifier = Modifier.height(16.dp))
                    SlavaSection(
                        mark = mark,
                        mine = slavaSettings.mine,
                        isToday = day.gregorianDate == today,
                        fastingType = day.fasting.type
                    )
                }

                // Fasting section
                Spacer(modifier = Modifier.height(16.dp))
                FastingSection(day = day)
                SectionDivider()

                // Saints / Commemorations
                if (day.feasts.isNotEmpty()) {
                    SaintsSection(
                        day = day,
                        localization = localization,
                        language = language,
                        slavaOffer = slavaOffer,
                        onSetSlava = setSlava
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

        justSetSlava?.let { slava ->
            SlavaToast(
                slava = slava,
                remindsWeekBefore = slavaSettings?.remindWeekBefore == true,
                onUndo = {
                    slavaStore?.update { it.copy(mine = null) }
                    justSetSlava = null
                }
            )
        }
      }
    }
}

// MARK: - Slava

@Composable
private fun SlavaSection(mark: SlavaMark, mine: SlavaDay?, isToday: Boolean, fastingType: String) {
    Column {
        if (mark.isMine && mine != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.bannerBg, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AppColors.cardBg)
                        .border(2.dp, AppColors.gold, CircleShape)
                ) {
                    Text(text = "🕯", fontSize = 26.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ВАША СЛАВА",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = AppColors.slavaGold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isToday) "Срећна слава!" else mine.name,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = AppColors.bannerTitle,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = mine.saint,
                    fontFamily = FontFamily.Serif,
                    fontSize = 15.sp,
                    color = AppColors.bodyText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = SlavaText.table(fastingType),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.darkText,
                    textAlign = TextAlign.Center
                )
            }
        }
        mark.friendLines.forEachIndexed { index, line ->
            if (index > 0 || mark.isMine) Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(AppColors.slavaRowBg, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = "🕯")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Слава: $line",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.slavaGold
                )
            }
        }
    }
}

@Composable
private fun BoxScope.SlavaToast(slava: SlavaDay, remindsWeekBefore: Boolean, onUndo: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .background(AppColors.slavaInk, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Text(text = "🕯")
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${slava.name} је ваша слава",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.toastText
            )
            Text(
                text = if (remindsWeekBefore) "Подсетник стиже недељу дана пре" else "Видећете је у календару",
                fontSize = 12.sp,
                color = AppColors.toastText.copy(alpha = 0.75f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Поништи",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.toastText,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .clickable(onClick = onUndo)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        )
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
                    text = period.displayName.uppercase(Locale.ROOT),
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
    val (icon, color, bg) = fastingVisuals(day.fasting.type)

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
    language: AppLanguage,
    slavaOffer: (Feast) -> SlavaDay? = { null },
    onSetSlava: (SlavaDay) -> Unit = {}
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

        // Bios are paired with feasts once for the whole day: which feast a bio
        // belongs to depends on what the other feasts claim, so it cannot be
        // decided one card at a time.
        val bios = day.saintBios.orEmpty()
        val assigned = remember(day) {
            BioMatcher.assign(
                feastNames = day.feasts.map { it.name },
                moveable = day.feasts.map { it.moveable },
                bioTitles = bios.map { it.title }
            )
        }

        day.feasts.forEachIndexed { index, feast ->
            SaintCard(
                feast = feast,
                bio = assigned[index]?.let { bios.getOrNull(it) },
                localizedType = localizedSaintType(feast.type, language),
                slavaAction = slavaOffer(feast)?.let { slava -> { onSetSlava(slava) } }
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

/**
 * Builds the day's share text and opens the system sheet.
 *
 * Returns false when nothing on the device accepted the intent. The share sheet
 * is one of the few things every Android is expected to have, but a work profile
 * or a cloned app whose components are momentarily disabled resolves nothing, and
 * this was the last unguarded `startActivity` in the app: it took the process down
 * with the richest screen in it open. Handled the same way as the reminder
 * screen's `ACTION_INSERT`.
 */
private fun shareDay(
    context: Context,
    day: CalendarDay,
    localization: LocalizationBundle,
    language: AppLanguage
): Boolean {
    val lines = mutableListOf<String>()

    lines.add("\u2626 ${localization.ui.dayAndMonth(day.gregorianDay, day.gregorianMonth)}")
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
    return try {
        context.startActivity(Intent.createChooser(sendIntent, null))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
