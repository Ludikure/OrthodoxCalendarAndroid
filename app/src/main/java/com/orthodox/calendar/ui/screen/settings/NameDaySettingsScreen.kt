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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.nameday.FriendNameDay
import com.orthodox.calendar.data.nameday.NameDayCatalog
import com.orthodox.calendar.data.nameday.NameDayChoice
import com.orthodox.calendar.data.nameday.NameDaySettings
import com.orthodox.calendar.data.nameday.NameDayStore
import com.orthodox.calendar.data.nameday.NameDayText
import com.orthodox.calendar.ui.components.NameDayIcon
import com.orthodox.calendar.ui.theme.AppColors
import com.orthodox.calendar.ui.util.Haptics
import com.orthodox.calendar.ui.util.rememberNotificationPermissionRequest
import java.time.LocalDate
import java.util.Locale

/*
 * Settings › Мои именины: the user's name day, reminders, and friends' name
 * days. Russian only, so its text is Russian throughout.
 *
 * Mirror of `OrthodoxCalendar/Views/NameDaySettingsView.swift`. iOS pushes the
 * editor and the saint picker as navigation destinations; here they are pages of
 * one route on a small stack, as in SlavaSettingsScreen, so the system back
 * gesture walks them the same way and the editor keeps what was typed while the
 * picker is open.
 */

private sealed interface NameDayPage {
    data object Main : NameDayPage
    data object Editor : NameDayPage
    data object Picker : NameDayPage
}

/** What the editor holds while it is open, the picker's choice included. */
private class EditorState(
    val title: String,
    val isFriend: Boolean,
    initial: NameDayChoice? = null
) {
    var person by mutableStateOf("")
    var name by mutableStateOf(initial?.name.orEmpty())
    var knowsBirthday by mutableStateOf(initial == null || initial.birthMonth != null)
    var birthMonth by mutableStateOf(initial?.birthMonth ?: 1)
    var birthDay by mutableStateOf(initial?.birthDay ?: 1)
    /** A saint chosen by hand; cleared when the name or the birthday changes. */
    var manual by mutableStateOf(
        initial?.takeIf { it.birthMonth == null || it.birthDay == null }?.let {
            NameDayCatalog.Commemoration(it.churchName, it.anchor, it.title, isNewMartyr = false)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameDaySettingsScreen(
    store: NameDayStore,
    onBack: () -> Unit,
    /** Called once notifications were asked for; reschedules the reminders. */
    onNotificationsAsked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by store.settings.collectAsState()
    val context = LocalContext.current
    val catalog = remember { NameDayCatalog.shared(context.applicationContext) }
    val stack = remember { mutableStateListOf<NameDayPage>(NameDayPage.Main) }
    val page = stack.last()
    val view = LocalView.current
    // Asked for only when there is something to remind about, never at launch.
    val askForNotifications = rememberNotificationPermissionRequest(onNotificationsAsked)
    var editor by remember { mutableStateOf<EditorState?>(null) }

    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) else onBack() }

    BackHandler(enabled = stack.size > 1) { pop() }

    val current = editor
    val chosen = current?.let { chosen(it, catalog, settings.includeNewMartyrs) }

    val title = when (page) {
        NameDayPage.Main -> "Мои именины"
        NameDayPage.Editor -> current?.title.orEmpty()
        NameDayPage.Picker -> "Выбор святого"
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
                    if (page == NameDayPage.Editor && current != null) {
                        val name = current.name.trim()
                        TextButton(
                            enabled = chosen != null && name.isNotEmpty(),
                            onClick = {
                                val c = chosen ?: return@TextButton
                                val auto = current.manual == null && current.knowsBirthday
                                val choice = NameDayChoice(
                                    name = name, churchName = c.name, title = c.title, anchor = c.anchor,
                                    birthMonth = if (auto) current.birthMonth else null,
                                    birthDay = if (auto) current.birthDay else null
                                )
                                Haptics.light(view)
                                if (current.isFriend) {
                                    val person = current.person.trim()
                                    store.update { it.copy(friends = it.friends + FriendNameDay(person = person, nameDay = choice)) }
                                } else {
                                    store.update { it.copy(mine = choice) }
                                }
                                askForNotifications()
                                pop()
                            }
                        ) { Text("Сохранить", color = AppColors.crimson) }
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (page) {
                NameDayPage.Main -> MainPage(
                    settings = settings,
                    store = store,
                    onEditMine = {
                        editor = EditorState("Мои именины", isFriend = false, initial = settings.mine)
                        stack.add(NameDayPage.Editor)
                    },
                    onAddFriend = {
                        editor = EditorState("Именины друга", isFriend = true)
                        stack.add(NameDayPage.Editor)
                    }
                )
                NameDayPage.Editor -> current?.let {
                    EditorPage(
                        state = it,
                        catalog = catalog,
                        chosen = chosen,
                        onPickSaint = { stack.add(NameDayPage.Picker) }
                    )
                }
                NameDayPage.Picker -> current?.let { state ->
                    PickerPage(
                        churchNames = catalog.churchForms(state.name),
                        catalog = catalog,
                        onPick = { c ->
                            Haptics.light(view)
                            state.manual = c
                            pop()
                        }
                    )
                }
            }
        }
    }
}

/** A saint chosen by hand, else the one the birthday finds. */
private fun chosen(state: EditorState, catalog: NameDayCatalog, includeNewMartyrs: Boolean): NameDayCatalog.Commemoration? {
    state.manual?.let { return it }
    if (!state.knowsBirthday) return null
    return catalog.firstNameDay(
        churchForms = catalog.churchForms(state.name),
        birthMonth = state.birthMonth,
        birthDay = state.birthDay,
        year = LocalDate.now().year,
        includeNewMartyrs = includeNewMartyrs
    )?.commemoration
}

// MARK: - Main page

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainPage(
    settings: NameDaySettings,
    store: NameDayStore,
    onEditMine: () -> Unit,
    onAddFriend: () -> Unit
) {
    var showTime by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Именины")
        val mine = settings.mine
        if (mine != null) {
            RowItem(onClick = onEditMine) {
                NameDayLabel(mine, Modifier.weight(1f))
                Chevron()
            }
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            RowItem(onClick = { store.update { it.copy(mine = null) } }) {
                Text("Удалить именины", fontSize = 16.sp, color = AppColors.crimson)
            }
        } else {
            RowItem(onClick = onEditMine) {
                Text("Указать имя и день рождения", fontSize = 16.sp, color = AppColors.crimson, modifier = Modifier.weight(1f))
                Chevron()
            }
        }
        Footer("По обычаю Церкви именины — первый после дня рождения день памяти святого, чьё имя вы носите. Если вы знаете своего святого, выберите его сами.")

        HorizontalDivider()
        ToggleRow("Учитывать новомучеников", settings.includeNewMartyrs) { v -> store.update { it.copy(includeNewMartyrs = v) } }
        Footer("Когда именины ищутся по дню рождения. Без этого новомученики учитываются, только если других святых с таким именем нет.")

        HorizontalDivider()
        Header("Напоминания")
        ToggleRow("В день именин", settings.remindOnDay) { v -> store.update { it.copy(remindOnDay = v) } }
        ToggleRow("Именины друзей — в тот же день", settings.remindFriends) { v -> store.update { it.copy(remindFriends = v) } }
        ToggleRow("Именины друзей — накануне", settings.remindFriendsDayBefore) { v -> store.update { it.copy(remindFriendsDayBefore = v) } }
        RowItem(onClick = { showTime = true }) {
            Text("Время", fontSize = 16.sp, color = AppColors.darkText, modifier = Modifier.weight(1f))
            Text(
                String.format(Locale.ROOT, "%02d:%02d", settings.reminderMinutes / 60, settings.reminderMinutes % 60),
                fontSize = 16.sp,
                color = AppColors.crimson
            )
        }

        HorizontalDivider()
        Header("Именины друзей")
        settings.friends.forEach { friend ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(friend.displayName, fontSize = 16.sp, color = AppColors.darkText)
                    Text(
                        "${friend.nameDay.churchName} · ${NameDayText.whenText(friend.nameDay.anchor)}",
                        fontSize = 12.sp,
                        color = AppColors.mutedText
                    )
                }
                IconButton(onClick = { store.update { s -> s.copy(friends = s.friends.filter { it.id != friend.id }) } }) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = AppColors.mutedText)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        }
        RowItem(onClick = onAddFriend) {
            Icon(Icons.Default.Add, contentDescription = null, tint = AppColors.crimson)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить именины", fontSize = 16.sp, color = AppColors.crimson)
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
                }) { Text("Готово", color = AppColors.crimson) }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) { Text("Отмена", color = AppColors.mutedText) }
            },
            title = { Text("Время напоминаний") },
            text = { TimePicker(state = state) }
        )
    }
}

/** A name day: the church name over the saint and when it falls. */
@Composable
private fun NameDayLabel(choice: NameDayChoice, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NameDayIcon(Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (choice.name == choice.churchName) choice.name else "${choice.name} (${choice.churchName})",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = AppColors.darkText
            )
        }
        Text(
            NameDayText.whenText(choice.anchor),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.slavaGold
        )
        Text(choice.title, fontSize = 12.sp, color = AppColors.mutedText, maxLines = 3)
    }
}

// MARK: - Editor

/**
 * Finds a name day from a first name and a birthday — the first commemoration
 * of the name on or after it — or lets the user choose the saint.
 */
@Composable
private fun EditorPage(
    state: EditorState,
    catalog: NameDayCatalog,
    chosen: NameDayCatalog.Commemoration?,
    onPickSaint: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var nameFocused by remember { mutableStateOf(false) }
    val trimmed = state.name.trim()
    val forms = catalog.churchForms(state.name)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header(if (state.isFriend) "Кто и как зовут" else "Ваше имя")
        if (state.isFriend) {
            OutlinedTextField(
                value = state.person,
                onValueChange = { state.person = it },
                placeholder = { Text("Кто (например, мама, кум Сергей)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        OutlinedTextField(
            value = state.name,
            onValueChange = {
                state.name = it
                state.manual = null
            },
            placeholder = { Text("Имя") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, autoCorrectEnabled = false),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .onFocusChanged { nameFocused = it.isFocused }
        )
        if (nameFocused) {
            val suggestions = catalog.suggestions(trimmed).let { s ->
                if (s.size == 1 && s[0].name == trimmed) emptyList() else s
            }
            suggestions.forEach { s ->
                RowItem(onClick = {
                    state.name = s.name
                    state.manual = null
                    focusManager.clearFocus()
                }) {
                    Text(s.name, fontSize = 16.sp, color = AppColors.darkText, modifier = Modifier.weight(1f))
                    if (s.church != s.name) {
                        Text(s.church, fontSize = 12.sp, color = AppColors.mutedText)
                    }
                }
            }
        }
        when {
            trimmed.isEmpty() -> Footer("Можно обычное имя (Иван, Таня) или церковное (Иоанн, Татиана).")
            forms.isEmpty() -> Footer("Святого с именем «$trimmed» в календаре нет. Обычно при крещении дают созвучное или близкое по смыслу имя — выберите святого вручную.")
            forms != listOf(trimmed) -> Footer("Церковное имя: ${forms.joinToString(" или ")}")
        }

        HorizontalDivider()
        Header("День рождения")
        ToggleRow("Знаю день рождения", state.knowsBirthday) { state.knowsBirthday = it }
        if (state.knowsBirthday) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Месяц", fontSize = 16.sp, color = AppColors.darkText, modifier = Modifier.weight(1f))
                Choice(NameDayText.monthsNominative[state.birthMonth - 1], NameDayText.monthsNominative) { i ->
                    state.birthMonth = i + 1
                    state.birthDay = minOf(state.birthDay, NameDayText.daysIn(i + 1))
                    state.manual = null
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("День", fontSize = 16.sp, color = AppColors.darkText, modifier = Modifier.weight(1f))
                Choice("${state.birthDay}", (1..NameDayText.daysIn(state.birthMonth)).map { "$it" }) { i ->
                    state.birthDay = i + 1
                    state.manual = null
                }
            }
        }

        if (chosen != null) {
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            Header(if (state.manual == null) "Именины" else "Выбранный святой")
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    NameDayText.whenText(chosen.anchor),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AppColors.slavaGold
                )
                Text(chosen.title, fontSize = 15.sp, color = AppColors.bodyText)
            }
            if (state.manual == null) {
                Footer("Первый день памяти святого с именем ${chosen.name} после дня рождения.")
            }
        }

        if (trimmed.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            RowItem(onClick = onPickSaint) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = AppColors.crimson)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (forms.isEmpty()) "Выбрать святого" else "Выбрать другого святого или день",
                    fontSize = 16.sp,
                    color = AppColors.crimson,
                    modifier = Modifier.weight(1f)
                )
                Chevron()
            }
        }
        Spacer(modifier = Modifier.padding(bottom = 32.dp))
    }
}

// MARK: - Saint picker

/**
 * Every commemoration of the given church names, in calendar order, and a
 * search over all names for a saint the name doesn't lead to.
 */
@Composable
private fun PickerPage(
    churchNames: List<String>,
    catalog: NameDayCatalog,
    onPick: (NameDayCatalog.Commemoration) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val shown = if (query.isBlank()) churchNames else catalog.churchNames(query)
    val jan1 = LocalDate.now().withDayOfYear(1)

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Имя святого") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (shown.isEmpty()) {
                item {
                    Text(
                        if (query.isEmpty()) "Найдите святого по имени" else "Ничего не найдено",
                        color = AppColors.mutedText,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            for (name in shown) {
                item(key = "header:$name") { Header(name) }
                // Calendar order from 1 January, by this year's (or the next) date.
                val sorted = catalog.commemorations(name)
                    .sortedBy { it.anchor.nextOccurrence(jan1) ?: LocalDate.MAX }
                items(sorted, key = { "$name:${it.id}" }) { c ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(c) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            NameDayText.whenText(c.anchor),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.darkText
                        )
                        Text(
                            if (c.isNewMartyr) "${c.title} · новомученики" else c.title,
                            fontSize = 12.sp,
                            color = AppColors.mutedText,
                            maxLines = 3
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.padding(bottom = 32.dp)) }
        }
    }
}

// MARK: - Shared pieces

@Composable
private fun Header(title: String) {
    Text(
        text = title.uppercase(Locale.forLanguageTag("ru")),
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
