package com.orthodox.calendar.ui.screen.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.ui.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    language: AppLanguage,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(1f) }

    val splashTitle = when (language) {
        AppLanguage.SR -> "\u041F\u0440\u0430\u0432\u043E\u0441\u043B\u0430\u0432\u043D\u0438 \u041A\u0430\u043B\u0435\u043D\u0434\u0430\u0440"
        AppLanguage.RU -> "\u041F\u0440\u0430\u0432\u043E\u0441\u043B\u0430\u0432\u043D\u044B\u0439 \u041A\u0430\u043B\u0435\u043D\u0434\u0430\u0440\u044C"
        AppLanguage.EN, AppLanguage.EN_NC -> "Orthodox Calendar"
    }
    val splashSubtitle = when (language) {
        AppLanguage.SR -> "\u0426\u0440\u043A\u0432\u0435\u043D\u0438 \u041A\u0430\u043B\u0435\u043D\u0434\u0430\u0440"
        AppLanguage.RU -> "\u0426\u0435\u0440\u043A\u043E\u0432\u043D\u044B\u0439 \u041A\u0430\u043B\u0435\u043D\u0434\u0430\u0440\u044C"
        AppLanguage.EN, AppLanguage.EN_NC -> "Church Calendar"
    }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(600))
        delay(900)
        alpha.animateTo(0f, animationSpec = tween(400))
        onFinished()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.headerBg)
            .alpha(alpha.value),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "\u2626",
            fontSize = 80.sp,
            color = AppColors.goldAccent,
            modifier = Modifier.scale(scale.value)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = splashTitle,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = splashSubtitle,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = AppColors.goldAccent
        )
    }
}
