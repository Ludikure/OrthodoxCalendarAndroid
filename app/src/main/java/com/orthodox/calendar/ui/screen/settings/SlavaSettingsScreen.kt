package com.orthodox.calendar.ui.screen.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.data.repository.YearSource
import com.orthodox.calendar.data.slava.FriendSlava
import com.orthodox.calendar.data.slava.SlavaAnchor
import com.orthodox.calendar.data.slava.SlavaCatalog
import com.orthodox.calendar.data.slava.SlavaDay
import com.orthodox.calendar.data.slava.SlavaSettings
import com.orthodox.calendar.data.slava.SlavaStore
import com.orthodox.calendar.data.slava.SlavaText
import com.orthodox.calendar.engine.ChurchDates
import com.orthodox.calendar.ui.theme.AppColors
import com.orthodox.calendar.ui.util.Haptics
import com.orthodox.calendar.ui.util.foldForSearch
import com.orthodox.calendar.ui.util.rememberNotificationPermissionRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/*
 * Settings › Моја слава: the user's krsna slava, reminders, and friends'
 * slavas. Serbian only, so its text is Serbian throughout.
 *
 * Mirror of `OrthodoxCalendar/Views/SlavaSettingsView.swift`. iOS pushes the
 * picker, the custom date and the friend editor as navigation destinations;
 * here they are pages of one route, kept on a small stack so the system back
 * gesture walks them the same way.
 */

private sealed interface SlavaPage {
    data object Main : SlavaPage
    /** Chooses a slava for the user, or for the friend being added. */
    data class Picker(val title: String, val forFriend: Boolean) : SlavaPage
    data class Custom(val forFriend: Boolean) : SlavaPage
    data object Friend : SlavaPage
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlavaSettingsScreen(
    store: SlavaStore,
    years: YearSource,
    localization: LocalizationBundle,
    onBack: () -> Unit,
    /** Called once notifications were asked for; reschedules the reminders. */
    onNotificationsAsked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by store.settings.collectAsState()
    val stack = remember { mutableStateListOf<SlavaPage>(SlavaPage.Main) }
    val page = stack.last()
    val view = LocalView.current
    val askForNotifications = rememberNotificationPermissionRequest(onNotificationsAsked)

    // The friend being added, kept here so the picker can fill it in.
    var friendPerson by remember { mutableStateOf("") }
    var friendSlava by remember { mutableStateOf<SlavaDay?>(null) }
    // The custom slava being entered.
    var customName by remember { mutableStateOf("") }
    var customDate by remember { mutableStateOf(LocalDate.now()) }

    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) else onBack() }

    fun choose(slava: SlavaDay, forFriend: Boolean) {
        Haptics.light(view)
        if (forFriend) {
            friendSlava = slava
        } else {
            store.update { it.copy(mine = slava) }
            askForNotifications()
        }
        // Back to where the picker was opened from.
        while (stack.last() is SlavaPage.Picker || stack.last() is SlavaPage.Custom) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    val title = when (page) {
        SlavaPage.Main -> "Моја слава"
        is SlavaPage.Picker -> page.title
        is SlavaPage.Custom -> "Други датум"
        SlavaPage.Friend -> "Слава пријатеља"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    when (page) {
                        SlavaPage.Friend -> {
                            val slava = friendSlava
                            val person = friendPerson.trim()
                            TextButton(
                                enabled = slava != null && person.isNotEmpty(),
                                onClick = {
                                    if (slava == null) return@TextButton
                                    store.update { it.copy(friends = it.friends + FriendSlava(person = person, slava = slava)) }
                                    askForNotifications()
                                    pop()
                                }
                            ) { Text("Сачувај", color = AppColors.crimson) }
                        }
                        is SlavaPage.Custom -> {
                            val name = customName.trim()
                            TextButton(
                                enabled = name.isNotEmpty(),
                                onClick = {
                                    val (m, d) = ChurchDates.julian(customDate)
                                    choose(SlavaDay(name, name, SlavaAnchor.Julian(m, d)), page.forFriend)
                                }
                            ) { Text("Сачувај", color = AppColors.crimson) }
                        }
                        else -> Unit
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (page) {
                SlavaPage.Main -> MainPage(
                    settings = settings,
                    store = store,
                    localization = localization,
                    onPick = { t -> stack.add(SlavaPage.Picker(t, forFriend = false)) },
                    onAddFriend = {
                        friendPerson = ""
                        friendSlava = null
                        stack.add(SlavaPage.Friend)
                    }
                )
                is SlavaPage.Picker -> PickerPage(
                    years = years,
                    localization = localization,
                    onPick = { choose(it, page.forFriend) },
                    onCustom = {
                        customName = ""
                        customDate = LocalDate.now()
                        stack.add(SlavaPage.Custom(page.forFriend))
                    }
                )
                is SlavaPage.Custom -> CustomPage(
                    name = customName,
                    onName = { customName = it },
                    date = customDate,
                    onDate = { customDate = it },
                    localization = localization
                )
                SlavaPage.Friend -> FriendPage(
                    person = friendPerson,
                    onPerson = { friendPerson = it },
                    slava = friendSlava,
                    localization = localization,
                    onPick = { stack.add(SlavaPage.Picker("Њихова слава", forFriend = true)) }
                )
            }
        }
    }
}

// MARK: - Main page

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainPage(
    settings: SlavaSettings,
    store: SlavaStore,
    localization: LocalizationBundle,
    onPick: (title: String) -> Unit,
    onAddFriend: () -> Unit
) {
    var showTime by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Крсна слава")
        val mine = settings.mine
        if (mine != null) {
            RowItem(onClick = { onPick("Промени славу") }) {
                SlavaLabel(mine, localization, Modifier.weight(1f))
                Chevron()
            }
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            RowItem(onClick = { store.update { it.copy(mine = null) } }) {
                Text("Уклони славу", fontSize = 16.sp, color = AppColors.crimson)
            }
        } else {
            RowItem(onClick = { onPick("Изаберите славу") }) {
                Text("Изаберите славу", fontSize = 16.sp, color = AppColors.crimson, modifier = Modifier.weight(1f))
                Chevron()
            }
        }
        Footer("Слава се памти по црквеном календару, па сваке године пада на прави дан.")

        HorizontalDivider()
        Header("Подсетници")
        ToggleRow("Недељу дана пре", settings.remindWeekBefore) { v -> store.update { it.copy(remindWeekBefore = v) } }
        ToggleRow("На дан славе", settings.remindOnDay) { v -> store.update { it.copy(remindOnDay = v) } }
        ToggleRow("Дан пре славе пријатеља", settings.remindFriends) { v -> store.update { it.copy(remindFriends = v) } }
        RowItem(onClick = { showTime = true }) {
            Text("Време", fontSize = 16.sp, color = AppColors.darkText, modifier = Modifier.weight(1f))
            Text(
                String.format(Locale.ROOT, "%02d:%02d", settings.reminderMinutes / 60, settings.reminderMinutes % 60),
                fontSize = 16.sp,
                color = AppColors.crimson
            )
        }

        HorizontalDivider()
        Header("Славе пријатеља")
        settings.friends.forEach { friend ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(friend.person, fontSize = 16.sp, color = AppColors.darkText)
                    Text(
                        "${friend.slava.name} · ${slavaWhen(friend.slava, localization)}",
                        fontSize = 12.sp,
                        color = AppColors.mutedText
                    )
                }
                IconButton(onClick = { store.update { s -> s.copy(friends = s.friends.filter { it.id != friend.id }) } }) {
                    Icon(Icons.Default.Delete, contentDescription = "Обриши", tint = AppColors.mutedText)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        }
        RowItem(onClick = onAddFriend) {
            Icon(Icons.Default.Add, contentDescription = null, tint = AppColors.crimson)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Додај славу", fontSize = 16.sp, color = AppColors.crimson)
        }
        Spacer(modifier = Modifier.padding(bottom = 32.dp))
    }

    if (showTime) {
        val state = rememberTimePickerState(
            initialHour = settings.reminderMinutes / 60,
            initialMinute = settings.reminderMinutes % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    store.update { it.copy(reminderMinutes = state.hour * 60 + state.minute) }
                    showTime = false
                }) { Text("У реду", color = AppColors.crimson) }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) { Text("Откажи", color = AppColors.mutedText) }
            },
            title = { Text("Време подсетника") },
            text = { TimePicker(state = state) }
        )
    }
}

// MARK: - Picker

/**
 * Chooses a slava: the common ones first, then any commemoration in the
 * calendar by search, then a date the calendar doesn't name.
 */
@Composable
private fun PickerPage(
    years: YearSource,
    localization: LocalizationBundle,
    onPick: (SlavaDay) -> Unit,
    onCustom: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    // Every fixed commemoration in the Serbian calendar, one per church date and name.
    var calendarSaints by remember { mutableStateOf<List<SlavaDay>>(emptyList()) }
    LaunchedEffect(Unit) { calendarSaints = loadCalendarSaints(years) }

    val folded = foldForSearch(query.trim())
    fun matches(s: SlavaDay) = foldForSearch(s.name).contains(folded) || foldForSearch(s.saint).contains(folded)

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Слава или светитељ") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (folded.isEmpty()) {
                section("Покретне славе", SlavaCatalog.moveable, localization, onPick)
                section("Честе славе", SlavaCatalog.fixed, localization, onPick)
            } else {
                val common = SlavaCatalog.common.filter(::matches)
                section("Честе славе", common, localization, onPick)
                // Calendar commemorations matching the query, minus what the
                // common list already shows for the same church date.
                val others = if (folded.length < 2) emptyList() else {
                    val commonDates = SlavaCatalog.fixed.filter(::matches).map { it.anchor }.toSet()
                    calendarSaints.asSequence()
                        .filter { matches(it) && it.anchor !in commonDates }
                        .take(50)
                        .toList()
                }
                section("Из календара", others, localization, onPick)
                if (common.isEmpty() && others.isEmpty()) {
                    item {
                        Text(
                            "Нема резултата",
                            color = AppColors.mutedText,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            item {
                HorizontalDivider()
                RowItem(onClick = onCustom) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = AppColors.crimson)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Други датум", fontSize = 16.sp, color = AppColors.crimson)
                }
                Footer("Ако ваше славе нема у календару, унесите њен назив и датум.")
                Spacer(modifier = Modifier.padding(bottom = 32.dp))
            }
        }
    }
}

private fun LazyListScope.section(
    title: String,
    slavas: List<SlavaDay>,
    localization: LocalizationBundle,
    onPick: (SlavaDay) -> Unit
) {
    if (slavas.isEmpty()) return
    item(key = "header:$title") { Header(title) }
    items(slavas, key = { "$title:${it.id}" }) { slava ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPick(slava) }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                slava.name,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = AppColors.darkText
            )
            val whenText = slavaWhen(slava, localization)
            Text(
                if (slava.name == slava.saint) whenText else "${slava.saint} · $whenText",
                fontSize = 12.sp,
                color = AppColors.mutedText
            )
        }
    }
}

/**
 * Reads one Serbian year for its fixed commemorations: they sit on the same
 * church date every year, so any year serves. Moveable feasts are left to the
 * common list, which anchors them to Pascha.
 */
private suspend fun loadCalendarSaints(years: YearSource): List<SlavaDay> = try {
    val file = years.load("sr", LocalDate.now().year)
    withContext(Dispatchers.Default) {
        val seen = HashSet<String>()
        val out = mutableListOf<SlavaDay>()
        for (day in file.days.values.sortedBy { it.gregorianDate }) {
            val parts = day.julianDate.split("-").mapNotNull { it.toIntOrNull() }
            if (parts.size != 2) continue
            for (feast in day.feasts) {
                if (feast.moveable || feast.name.isEmpty()) continue
                val slava = SlavaDay(feast.name, feast.name, SlavaAnchor.Julian(parts[0], parts[1]))
                if (seen.add(slava.id)) out += slava
            }
        }
        out
    }
} catch (c: CancellationException) {
    throw c
} catch (e: Exception) {
    emptyList()
}

// MARK: - Custom date

/** A slava the calendar doesn't name: a name and a date, kept by church date. */
@Composable
private fun CustomPage(
    name: String,
    onName: (String) -> Unit,
    date: LocalDate,
    onDate: (LocalDate) -> Unit,
    localization: LocalizationBundle
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            label = { Text("Назив славе") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(top = 16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Датум", fontSize = 16.sp, color = AppColors.darkText, modifier = Modifier.weight(1f))
            // Day and month only: a slava falls on the same church date every
            // year, so the year is this one.
            val lastDay = YearMonth.of(date.year, date.monthValue).lengthOfMonth()
            Choice("${date.dayOfMonth}.", (1..lastDay).map { "$it." }) { i -> onDate(date.withDayOfMonth(i + 1)) }
            Spacer(modifier = Modifier.width(8.dp))
            Choice(SlavaText.months[date.monthValue - 1],
                SlavaText.months) { i ->
                val ym = YearMonth.of(date.year, i + 1)
                onDate(ym.atDay(minOf(date.dayOfMonth, ym.lengthOfMonth())))
            }
        }
        val (jm, jd) = ChurchDates.julian(date)
        Footer("По старом календару: ${localization.ui.dayAndMonth(jd, jm)}. Слава ће сваке године падати на тај дан.")
    }
}

@Composable
private fun Choice(label: String, options: List<String>, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text(label, color = AppColors.darkText) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { open = false; onSelect(i) })
            }
        }
    }
}

// MARK: - Friend editor

/** Adds a friend's slava: who, and which slava. */
@Composable
private fun FriendPage(
    person: String,
    onPerson: (String) -> Unit,
    slava: SlavaDay?,
    localization: LocalizationBundle,
    onPick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = person,
            onValueChange = onPerson,
            placeholder = { Text("Име (нпр. Петровићи, кум Марко)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        RowItem(onClick = onPick) {
            if (slava != null) {
                SlavaLabel(slava, localization, Modifier.weight(1f))
            } else {
                Text("Изаберите славу", fontSize = 16.sp, color = AppColors.crimson, modifier = Modifier.weight(1f))
            }
            Chevron()
        }
    }
}

// MARK: - Shared pieces

/** A slava's name over when it falls this year. */
@Composable
private fun SlavaLabel(slava: SlavaDay, localization: LocalizationBundle, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            "🕯 ${slava.name}",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = AppColors.darkText
        )
        Text(
            "${slava.saint} · ${slavaWhen(slava, localization)}",
            fontSize = 12.sp,
            color = AppColors.mutedText
        )
    }
}

/**
 * "19 децембар (6 децембар по старом)", or for a moveable slava
 * "покретна, следећа 21 мај". Mirror of iOS `SlavaLabel.when`.
 */
internal fun slavaWhen(slava: SlavaDay, localization: LocalizationBundle, today: LocalDate = LocalDate.now()): String {
    val civil = slava.nextOccurrence(today)?.let { localization.ui.dayAndMonth(it.dayOfMonth, it.monthValue) }.orEmpty()
    return when (val a = slava.anchor) {
        is SlavaAnchor.Pascha -> "покретна, следећа $civil"
        is SlavaAnchor.Julian -> "$civil (${localization.ui.dayAndMonth(a.day, a.month)} по старом)"
    }
}

@Composable
private fun Header(title: String) {
    Text(
        text = title.uppercase(Locale.ROOT),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = AppColors.mutedText,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun Footer(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = AppColors.mutedText,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun RowItem(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content
    )
}

@Composable
private fun Chevron() {
    Text(text = "›", fontSize = 20.sp, color = AppColors.mutedText)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 16.sp, color = AppColors.darkText, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AppColors.crimson)
        )
    }
}
