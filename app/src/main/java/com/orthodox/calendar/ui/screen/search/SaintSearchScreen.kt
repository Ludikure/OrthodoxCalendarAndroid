package com.orthodox.calendar.ui.screen.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.data.repository.CalendarRepository
import com.orthodox.calendar.ui.theme.AppColors
import kotlinx.coroutines.delay

data class SaintSearchResult(
    val matchedText: String,
    val gregorianMonth: Int,
    val gregorianDay: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaintSearchScreen(
    repository: CalendarRepository,
    localization: LocalizationBundle,
    language: AppLanguage,
    currentYear: Int,
    onNavigateToDate: (month: Int, day: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SaintSearchResult>>(emptyList()) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchTitle = when (language) {
        AppLanguage.SR -> "\u041F\u0440\u0435\u0442\u0440\u0430\u0433\u0430"
        AppLanguage.RU -> "\u041F\u043E\u0438\u0441\u043A"
        AppLanguage.EN, AppLanguage.EN_NC -> "Search"
    }
    val searchPrompt = when (language) {
        AppLanguage.SR -> "\u0418\u043C\u0435 \u0441\u0432\u0435\u0442\u0438\u0442\u0435\u0459\u0430 \u0438\u043B\u0438 \u043F\u0440\u0430\u0437\u043D\u0438\u043A\u0430"
        AppLanguage.RU -> "\u0418\u043C\u044F \u0441\u0432\u044F\u0442\u043E\u0433\u043E \u0438\u043B\u0438 \u043F\u0440\u0430\u0437\u0434\u043D\u0438\u043A\u0430"
        AppLanguage.EN, AppLanguage.EN_NC -> "Saint or feast name"
    }
    val noResultsText = when (language) {
        AppLanguage.SR -> "\u041D\u0435\u043C\u0430 \u0440\u0435\u0437\u0443\u043B\u0442\u0430\u0442\u0430"
        AppLanguage.RU -> "\u041D\u0438\u0447\u0435\u0433\u043E \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D\u043E"
        AppLanguage.EN, AppLanguage.EN_NC -> "No results"
    }
    val doneText = when (language) {
        AppLanguage.SR -> "\u0413\u043E\u0442\u043E\u0432\u043E"
        AppLanguage.RU -> "\u0413\u043E\u0442\u043E\u0432\u043E"
        AppLanguage.EN, AppLanguage.EN_NC -> "Done"
    }

    // Debounced search
    LaunchedEffect(query) {
        delay(200)
        val q = query.lowercase().trim()
        if (q.length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }

        val calFile = try {
            repository.load(language.code, currentYear)
        } catch (e: Exception) {
            null
        }
        if (calFile == null) {
            results = emptyList()
            return@LaunchedEffect
        }

        val found = mutableListOf<SaintSearchResult>()
        for ((_, day) in calFile.days) {
            for (feast in day.feasts) {
                if (feast.name.lowercase().contains(q)) {
                    found.add(
                        SaintSearchResult(
                            matchedText = feast.name,
                            gregorianMonth = day.gregorianMonth,
                            gregorianDay = day.gregorianDay
                        )
                    )
                }
            }
        }

        results = found
            .sortedBy { String.format("%02d-%02d", it.gregorianMonth, it.gregorianDay) }
            .take(50)
    }

    // Auto-focus search field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(searchTitle) },
                actions = {
                    Text(
                        text = doneText,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(horizontal = 16.dp)
                    )
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search field
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(searchPrompt) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            results = emptyList()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester)
            )

            // Results
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (results.isEmpty() && query.length >= 2) {
                    item {
                        Text(
                            text = noResultsText,
                            color = AppColors.mutedText,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(results) { result ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigateToDate(result.gregorianMonth, result.gregorianDay)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = result.matchedText,
                                fontSize = 14.sp,
                                color = AppColors.darkText,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${result.gregorianDay} ${localization.ui.months.getOrElse(result.gregorianMonth - 1) { "" }}",
                                fontSize = 12.sp,
                                color = AppColors.mutedText
                            )
                        }
                    }
                }
            }
        }
    }
}
