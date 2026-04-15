package com.orthodox.calendar.ui.screen.reminder

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.ui.theme.AppColors
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    day: CalendarDay,
    localization: LocalizationBundle,
    language: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val defaultTitle = day.primaryFeast?.name ?: day.feasts.firstOrNull()?.name ?: ""
    var title by remember { mutableStateOf(defaultTitle) }
    var notes by remember { mutableStateOf("") }

    val monthName = localization.ui.months.getOrElse(day.gregorianMonth - 1) { "" }
    val formattedDate = "${day.gregorianDay} $monthName ${day.gregorianDate.take(4)}"

    val addReminderTitle = when (language) {
        AppLanguage.SR -> "\u041F\u043E\u0434\u0441\u0435\u0442\u043D\u0438\u043A"
        AppLanguage.RU -> "\u041D\u0430\u043F\u043E\u043C\u0438\u043D\u0430\u043D\u0438\u0435"
        AppLanguage.EN, AppLanguage.EN_NC -> "Reminder"
    }
    val titlePlaceholder = when (language) {
        AppLanguage.SR -> "\u041D\u0430\u0437\u0438\u0432"
        AppLanguage.RU -> "\u041D\u0430\u0437\u0432\u0430\u043D\u0438\u0435"
        AppLanguage.EN, AppLanguage.EN_NC -> "Title"
    }
    val dateLabel = when (language) {
        AppLanguage.SR -> "\u0414\u0430\u0442\u0443\u043C"
        AppLanguage.RU -> "\u0414\u0430\u0442\u0430"
        AppLanguage.EN, AppLanguage.EN_NC -> "Date"
    }
    val notesLabel = when (language) {
        AppLanguage.SR -> "\u0411\u0435\u043B\u0435\u0448\u043A\u0435"
        AppLanguage.RU -> "\u0417\u0430\u043C\u0435\u0442\u043A\u0438"
        AppLanguage.EN, AppLanguage.EN_NC -> "Notes"
    }
    val saveText = when (language) {
        AppLanguage.SR -> "\u0421\u0430\u0447\u0443\u0432\u0430\u0458"
        AppLanguage.RU -> "\u0421\u043E\u0445\u0440\u0430\u043D\u0438\u0442\u044C"
        AppLanguage.EN, AppLanguage.EN_NC -> "Save"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(addReminderTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(titlePlaceholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = dateLabel,
                    fontSize = 14.sp,
                    color = AppColors.mutedText
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formattedDate,
                    fontSize = 14.sp,
                    color = AppColors.darkText
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(notesLabel) },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    addCalendarEvent(context, day, title, notes)
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.crimson),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = saveText,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

private fun addCalendarEvent(context: Context, day: CalendarDay, title: String, notes: String) {
    val date = day.date ?: LocalDate.parse(day.gregorianDate)
    val startMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.ALL_DAY, true)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 86400000)
        if (notes.isNotEmpty()) {
            putExtra(CalendarContract.Events.DESCRIPTION, notes)
        }
    }
    context.startActivity(intent)
}
