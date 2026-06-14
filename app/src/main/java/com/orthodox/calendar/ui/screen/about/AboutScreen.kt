package com.orthodox.calendar.ui.screen.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    language: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val aboutTitle = when (language) {
        AppLanguage.SR -> "\u041E \u0430\u043F\u043B\u0438\u043A\u0430\u0446\u0438\u0458\u0438"
        AppLanguage.RU -> "\u041E \u043F\u0440\u0438\u043B\u043E\u0436\u0435\u043D\u0438\u0438"
        AppLanguage.EN, AppLanguage.EN_NC -> "About"
    }
    val appTitle = when (language) {
        AppLanguage.SR -> "\u041F\u0440\u0430\u0432\u043E\u0441\u043B\u0430\u0432\u043D\u0438 \u041A\u0430\u043B\u0435\u043D\u0434\u0430\u0440"
        AppLanguage.RU -> "\u041F\u0440\u0430\u0432\u043E\u0441\u043B\u0430\u0432\u043D\u044B\u0439 \u041A\u0430\u043B\u0435\u043D\u0434\u0430\u0440\u044C"
        AppLanguage.EN, AppLanguage.EN_NC -> "Orthodox Calendar"
    }
    val dataSources = when (language) {
        AppLanguage.SR -> "\u0418\u0437\u0432\u043E\u0440\u0438 \u043F\u043E\u0434\u0430\u0442\u0430\u043A\u0430"
        AppLanguage.RU -> "\u0418\u0441\u0442\u043E\u0447\u043D\u0438\u043A\u0438 \u0434\u0430\u043D\u043D\u044B\u0445"
        AppLanguage.EN, AppLanguage.EN_NC -> "Data Sources"
    }
    val algorithms = when (language) {
        AppLanguage.SR -> "\u0410\u043B\u0433\u043E\u0440\u0438\u0442\u043C\u0438"
        AppLanguage.RU -> "\u0410\u043B\u0433\u043E\u0440\u0438\u0442\u043C\u044B"
        AppLanguage.EN, AppLanguage.EN_NC -> "Algorithms"
    }
    val contentLabel = when (language) {
        AppLanguage.SR -> "\u0421\u0430\u0434\u0440\u0436\u0430\u0458"
        AppLanguage.RU -> "\u0421\u043E\u0434\u0435\u0440\u0436\u0430\u043D\u0438\u0435"
        AppLanguage.EN, AppLanguage.EN_NC -> "Content"
    }
    val saintBiosSr = when (language) {
        AppLanguage.SR -> "\u0416\u0438\u0442\u0438\u0458\u0430 \u0441\u0432\u0435\u0442\u0438\u0445 \u2014 \u041E\u0445\u0440\u0438\u0434\u0441\u043A\u0438 \u041F\u0440\u043E\u043B\u043E\u0433"
        AppLanguage.RU -> "\u0416\u0438\u0442\u0438\u044F \u0441\u0432\u044F\u0442\u044B\u0445 (\u0441\u0435\u0440\u0431\u0441\u043A\u0438\u0435) \u2014 \u041E\u0445\u0440\u0438\u0434\u0441\u043A\u0438\u0439 \u041F\u0440\u043E\u043B\u043E\u0433"
        AppLanguage.EN, AppLanguage.EN_NC -> "Serbian saint biographies \u2014 Ohrid Prologue"
    }
    val readingsSr = when (language) {
        AppLanguage.SR -> "\u0421\u0432\u0435\u0442\u0438\u0442\u0435\u0459\u0438 \u0438 \u0447\u0438\u0442\u0430\u045A\u0430 (\u0441\u0440\u043F\u0441\u043A\u0438)"
        AppLanguage.RU -> "\u0421\u0432\u044F\u0442\u044B\u0435 \u0438 \u0447\u0442\u0435\u043D\u0438\u044F (\u0441\u0435\u0440\u0431\u0441\u043A\u0438\u0435)"
        AppLanguage.EN, AppLanguage.EN_NC -> "Saints and readings (Serbian)"
    }
    val saintBiosRu = when (language) {
        AppLanguage.SR -> "\u0416\u0438\u0442\u0438\u0458\u0430 \u0441\u0432\u0435\u0442\u0438\u0445 (\u0440\u0443\u0441\u043A\u0438)"
        AppLanguage.RU -> "\u0416\u0438\u0442\u0438\u044F \u0441\u0432\u044F\u0442\u044B\u0445 (\u0440\u0443\u0441\u0441\u043A\u0438\u0435)"
        AppLanguage.EN, AppLanguage.EN_NC -> "Saint biographies (Russian)"
    }
    val readingsEnBios = when (language) {
        AppLanguage.SR -> "\u0416\u0438\u0442\u0438\u0458\u0430 \u0441\u0432\u0435\u0442\u0438\u0445 \u0438 \u0447\u0438\u0442\u0430\u045A\u0430 (\u0435\u043D\u0433\u043B\u0435\u0441\u043A\u0438)"
        AppLanguage.RU -> "\u0416\u0438\u0442\u0438\u044F \u0441\u0432\u044F\u0442\u044B\u0445 \u0438 \u0447\u0442\u0435\u043D\u0438\u044F (\u0430\u043D\u0433\u043B\u0438\u0439\u0441\u043A\u0438\u0435)"
        AppLanguage.EN, AppLanguage.EN_NC -> "Saint biographies and readings (English)"
    }
    val readingsEn = when (language) {
        AppLanguage.SR -> "\u0411\u0438\u0431\u043B\u0438\u0458\u0441\u043A\u0430 \u0447\u0438\u0442\u0430\u045A\u0430 (\u0435\u043D\u0433\u043B\u0435\u0441\u043A\u0438)"
        AppLanguage.RU -> "\u0411\u0438\u0431\u043B\u0435\u0439\u0441\u043A\u0438\u0435 \u0447\u0442\u0435\u043D\u0438\u044F (\u0430\u043D\u0433\u043B\u0438\u0439\u0441\u043A\u0438\u0435)"
        AppLanguage.EN, AppLanguage.EN_NC -> "Bible readings (English)"
    }
    val paschalionTitle = when (language) {
        AppLanguage.SR -> "\u041F\u0430\u0441\u0445\u0430\u043B\u0438\u043E\u043D"
        AppLanguage.RU -> "\u041F\u0430\u0441\u0445\u0430\u043B\u0438\u044F"
        AppLanguage.EN, AppLanguage.EN_NC -> "Paschalion"
    }
    val paschalionDetail = when (language) {
        AppLanguage.SR -> "\u0410\u043B\u0433\u043E\u0440\u0438\u0442\u0430\u043C \u041C\u0435\u0435\u0443\u0441\u0430 \u0437\u0430 \u0438\u0437\u0440\u0430\u0447\u0443\u043D\u0430\u0432\u0430\u045A\u0435 \u0434\u0430\u0442\u0443\u043C\u0430 \u0412\u0430\u0441\u043A\u0440\u0441\u0430"
        AppLanguage.RU -> "\u0410\u043B\u0433\u043E\u0440\u0438\u0442\u043C \u041C\u0435\u0435\u0443\u0441\u0430 \u0434\u043B\u044F \u0432\u044B\u0447\u0438\u0441\u043B\u0435\u043D\u0438\u044F \u0434\u0430\u0442\u044B \u041F\u0430\u0441\u0445\u0438"
        AppLanguage.EN, AppLanguage.EN_NC -> "Meeus algorithm for computing the date of Pascha"
    }
    val lectionaryTitle = when (language) {
        AppLanguage.SR -> "\u0422\u0438\u043F\u0438\u043A\u043E\u043D \u043B\u0435\u043A\u0446\u0438\u043E\u043D\u0430\u0440"
        AppLanguage.RU -> "\u0422\u0438\u043F\u0438\u043A\u043E\u043D \u043B\u0435\u043A\u0446\u0438\u043E\u043D\u0430\u0440\u0438\u0439"
        AppLanguage.EN, AppLanguage.EN_NC -> "Typikon Lectionary"
    }
    val lectionaryDetail = when (language) {
        AppLanguage.SR -> "\u0410\u043B\u0433\u043E\u0440\u0438\u0442\u0430\u043C \u0437\u0430 \u0434\u043D\u0435\u0432\u043D\u0430 \u0447\u0438\u0442\u0430\u045A\u0430 \u043F\u043E \u0422\u0438\u043F\u0438\u043A\u043E\u043D\u0443"
        AppLanguage.RU -> "\u0410\u043B\u0433\u043E\u0440\u0438\u0442\u043C \u0434\u043D\u0435\u0432\u043D\u044B\u0445 \u0447\u0442\u0435\u043D\u0438\u0439 \u043F\u043E \u0422\u0438\u043F\u0438\u043A\u043E\u043D\u0443"
        AppLanguage.EN, AppLanguage.EN_NC -> "Algorithm for daily readings according to the Typikon"
    }
    val fastingTitle = when (language) {
        AppLanguage.SR -> "\u041F\u0440\u0430\u0432\u0438\u043B\u0430 \u043F\u043E\u0441\u0442\u0430"
        AppLanguage.RU -> "\u041F\u0440\u0430\u0432\u0438\u043B\u0430 \u043F\u043E\u0441\u0442\u0430"
        AppLanguage.EN, AppLanguage.EN_NC -> "Fasting Rules"
    }
    val fastingDetail = when (language) {
        AppLanguage.SR -> "7 \u043D\u0438\u0432\u043E\u0430 \u043F\u043E\u0441\u0442\u0430 \u043F\u043E \u0422\u0438\u043F\u0438\u043A\u043E\u043D\u0443 \u0441\u0430 \u043F\u0440\u0430\u0432\u0438\u043B\u0438\u043C\u0430 \u0421\u041F\u0426"
        AppLanguage.RU -> "7 \u0443\u0440\u043E\u0432\u043D\u0435\u0439 \u043F\u043E\u0441\u0442\u0430 \u043F\u043E \u0422\u0438\u043F\u0438\u043A\u043E\u043D\u0443 \u0441 \u043F\u0440\u0430\u0432\u0438\u043B\u0430\u043C\u0438 \u0420\u041F\u0426"
        AppLanguage.EN, AppLanguage.EN_NC -> "7-level fasting engine based on Typikon rules"
    }
    val prologTitle = when (language) {
        AppLanguage.SR -> "\u041E\u0445\u0440\u0438\u0434\u0441\u043A\u0438 \u041F\u0440\u043E\u043B\u043E\u0433"
        AppLanguage.RU -> "\u041E\u0445\u0440\u0438\u0434\u0441\u043A\u0438\u0439 \u041F\u0440\u043E\u043B\u043E\u0433"
        AppLanguage.EN, AppLanguage.EN_NC -> "Ohrid Prologue"
    }
    val prologDetail = when (language) {
        AppLanguage.SR -> "\u0421\u0432. \u041D\u0438\u043A\u043E\u043B\u0430\u0458 \u0412\u0435\u043B\u0438\u043C\u0438\u0440\u043E\u0432\u0438\u045B \u2014 \u0436\u0438\u0442\u0438\u0458\u0430, \u043F\u043E\u0443\u043A\u0435 \u0438 \u0445\u0438\u043C\u043D\u0435"
        AppLanguage.RU -> "\u0421\u0432\u0442. \u041D\u0438\u043A\u043E\u043B\u0430\u0439 \u0412\u0435\u043B\u0438\u043C\u0438\u0440\u043E\u0432\u0438\u0447 \u2014 \u0436\u0438\u0442\u0438\u044F, \u043F\u043E\u0443\u0447\u0435\u043D\u0438\u044F \u0438 \u0433\u0438\u043C\u043D\u044B"
        AppLanguage.EN, AppLanguage.EN_NC -> "St. Nikolai Velimirovich \u2014 lives, homilies, and hymns"
    }
    val bibleTitle = when (language) {
        AppLanguage.SR -> "\u0421\u0432\u0435\u0442\u043E \u041F\u0438\u0441\u043C\u043E"
        AppLanguage.RU -> "\u0421\u0432\u044F\u0449\u0435\u043D\u043D\u043E\u0435 \u041F\u0438\u0441\u0430\u043D\u0438\u0435"
        AppLanguage.EN, AppLanguage.EN_NC -> "Holy Scripture"
    }
    val bibleDetail = when (language) {
        AppLanguage.SR -> "\u0422\u0435\u043A\u0441\u0442\u043E\u0432\u0438 \u0438\u0437 \u0411\u0438\u0431\u043B\u0438\u0458\u0435 \u043D\u0430 \u0441\u0440\u043F\u0441\u043A\u043E\u043C, \u0440\u0443\u0441\u043A\u043E\u043C \u0438 \u0435\u043D\u0433\u043B\u0435\u0441\u043A\u043E\u043C"
        AppLanguage.RU -> "\u0422\u0435\u043A\u0441\u0442\u044B \u0438\u0437 \u0411\u0438\u0431\u043B\u0438\u0438 \u043D\u0430 \u0441\u0435\u0440\u0431\u0441\u043A\u043E\u043C, \u0440\u0443\u0441\u0441\u043A\u043E\u043C \u0438 \u0430\u043D\u0433\u043B\u0438\u0439\u0441\u043A\u043E\u043C"
        AppLanguage.EN, AppLanguage.EN_NC -> "Bible texts in Serbian, Russian, and English"
    }
    val privacyLabel = when (language) {
        AppLanguage.SR -> "\u041F\u043E\u043B\u0438\u0442\u0438\u043A\u0430 \u043F\u0440\u0438\u0432\u0430\u0442\u043D\u043E\u0441\u0442\u0438"
        AppLanguage.RU -> "\u041F\u043E\u043B\u0438\u0442\u0438\u043A\u0430 \u043A\u043E\u043D\u0444\u0438\u0434\u0435\u043D\u0446\u0438\u0430\u043B\u044C\u043D\u043E\u0441\u0442\u0438"
        AppLanguage.EN, AppLanguage.EN_NC -> "Privacy Policy"
    }
    val supportLabel = when (language) {
        AppLanguage.SR -> "\u041F\u043E\u0434\u0440\u0448\u043A\u0430"
        AppLanguage.RU -> "\u041F\u043E\u0434\u0434\u0435\u0440\u0436\u043A\u0430"
        AppLanguage.EN, AppLanguage.EN_NC -> "Support"
    }
    val disclaimer = when (language) {
        AppLanguage.SR -> "\u041E\u0432\u0430 \u0430\u043F\u043B\u0438\u043A\u0430\u0446\u0438\u0458\u0430 \u0458\u0435 \u043D\u0435\u0437\u0430\u0432\u0438\u0441\u043D\u0438 \u043F\u0440\u043E\u0458\u0435\u043A\u0430\u0442 \u0438 \u043D\u0438\u0458\u0435 \u0437\u0432\u0430\u043D\u0438\u0447\u043D\u0438 \u043F\u0440\u043E\u0438\u0437\u0432\u043E\u0434 \u043D\u0438\u0458\u0435\u0434\u043D\u0435 \u0446\u0440\u043A\u0432\u0435\u043D\u0435 \u043E\u0440\u0433\u0430\u043D\u0438\u0437\u0430\u0446\u0438\u0458\u0435. \u041F\u043E\u0434\u0430\u0446\u0438 \u0441\u0443 \u043F\u0440\u0438\u043A\u0443\u043F\u0459\u0435\u043D\u0438 \u0438\u0437 \u0458\u0430\u0432\u043D\u043E \u0434\u043E\u0441\u0442\u0443\u043F\u043D\u0438\u0445 \u0438\u0437\u0432\u043E\u0440\u0430 \u0440\u0430\u0434\u0438 \u0434\u0443\u0445\u043E\u0432\u043D\u0435 \u043A\u043E\u0440\u0438\u0441\u0442\u0438 \u0432\u0435\u0440\u043D\u0438\u043A\u0430."
        AppLanguage.RU -> "\u042D\u0442\u043E \u043F\u0440\u0438\u043B\u043E\u0436\u0435\u043D\u0438\u0435 \u044F\u0432\u043B\u044F\u0435\u0442\u0441\u044F \u043D\u0435\u0437\u0430\u0432\u0438\u0441\u0438\u043C\u044B\u043C \u043F\u0440\u043E\u0435\u043A\u0442\u043E\u043C \u0438 \u043D\u0435 \u044F\u0432\u043B\u044F\u0435\u0442\u0441\u044F \u043E\u0444\u0438\u0446\u0438\u0430\u043B\u044C\u043D\u044B\u043C \u043F\u0440\u043E\u0434\u0443\u043A\u0442\u043E\u043C \u043A\u0430\u043A\u043E\u0439-\u043B\u0438\u0431\u043E \u0446\u0435\u0440\u043A\u043E\u0432\u043D\u043E\u0439 \u043E\u0440\u0433\u0430\u043D\u0438\u0437\u0430\u0446\u0438\u0438. \u0414\u0430\u043D\u043D\u044B\u0435 \u0441\u043E\u0431\u0440\u0430\u043D\u044B \u0438\u0437 \u043E\u0431\u0449\u0435\u0434\u043E\u0441\u0442\u0443\u043F\u043D\u044B\u0445 \u0438\u0441\u0442\u043E\u0447\u043D\u0438\u043A\u043E\u0432 \u0434\u043B\u044F \u0434\u0443\u0445\u043E\u0432\u043D\u043E\u0439 \u043F\u043E\u043B\u044C\u0437\u044B \u0432\u0435\u0440\u0443\u044E\u0449\u0438\u0445."
        AppLanguage.EN, AppLanguage.EN_NC -> "This app is an independent project and is not an official product of any church organization. Data is gathered from publicly available sources for the spiritual benefit of the faithful."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(aboutTitle) },
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
        ) {
            // App header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\u2626",
                    fontSize = 48.sp,
                    color = AppColors.crimson
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = appTitle,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AppColors.darkText
                )
                Text(
                    text = "v${com.orthodox.calendar.BuildConfig.VERSION_NAME}",
                    fontSize = 12.sp,
                    color = AppColors.mutedText
                )
            }

            HorizontalDivider()

            // Data Sources section
            SectionTitle(dataSources)
            CreditRow("crkvenikalendar.com", saintBiosSr)
            CreditRow("pravoslavno.rs", readingsSr)
            CreditRow("azbyka.ru", saintBiosRu)
            CreditRow("orthocal.info", readingsEnBios)
            CreditRow("holytrinityorthodox.com", readingsEn)

            HorizontalDivider()

            // Algorithms section
            SectionTitle(algorithms)
            CreditRow(paschalionTitle, paschalionDetail)
            CreditRow(lectionaryTitle, lectionaryDetail)
            CreditRow(fastingTitle, fastingDetail)

            HorizontalDivider()

            // Content section
            SectionTitle(contentLabel)
            CreditRow(prologTitle, prologDetail)
            CreditRow(bibleTitle, bibleDetail)

            HorizontalDivider()

            // Links
            LinkRow(privacyLabel) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ludikure.github.io/OrthodoxCalendar/privacy"))
                context.startActivity(intent)
            }
            LinkRow(supportLabel) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ludikure.github.io/OrthodoxCalendar/"))
                context.startActivity(intent)
            }

            HorizontalDivider()

            // Disclaimer
            Text(
                text = disclaimer,
                fontSize = 12.sp,
                color = AppColors.mutedText,
                lineHeight = 18.sp,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = AppColors.mutedText,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun CreditRow(title: String, detail: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.darkText
        )
        Text(
            text = detail,
            fontSize = 12.sp,
            color = AppColors.mutedText
        )
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = AppColors.darkText
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "\u2197", fontSize = 14.sp, color = AppColors.mutedText)
    }
}
