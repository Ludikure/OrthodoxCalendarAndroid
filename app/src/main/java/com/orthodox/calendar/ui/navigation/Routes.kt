package com.orthodox.calendar.ui.navigation

sealed class Routes(val route: String) {
    data object Splash : Routes("splash")
    data object Calendar : Routes("calendar")
    data object DayDetail : Routes("day_detail/{gregorianDate}") {
        fun createRoute(gregorianDate: String) = "day_detail/$gregorianDate"
    }
    data object Search : Routes("search")
    data object Settings : Routes("settings")
    data object About : Routes("about")
    data object DatePicker : Routes("date_picker")
}
