package com.orthodox.calendar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orthodox.calendar.data.localization.LocalizationManager
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.AppTheme
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.data.preferences.AppPreferences
import com.orthodox.calendar.data.repository.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ViewMode { LIST, GRID }

data class CalendarUiState(
    val currentMonth: Int = LocalDate.now().monthValue,
    val currentYear: Int = LocalDate.now().year,
    val daysInMonth: List<CalendarDay> = emptyList(),
    val viewMode: ViewMode = ViewMode.LIST,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loadedLocale: String = "",
    val language: AppLanguage = AppLanguage.SR,
    val theme: AppTheme = AppTheme.SYSTEM,
    val localization: LocalizationBundle? = null,
    val scrollToTodayTrigger: Boolean = false
) {
    companion object {
        const val MIN_YEAR = 2024
        const val MAX_YEAR = 2030
    }
}

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalendarRepository(application)
    private val localizationManager = LocalizationManager(application)
    private val preferences = AppPreferences(application)

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val lang = preferences.languageFlow.first()
            val theme = preferences.themeFlow.first()
            val bundle = localizationManager.loadBundle(lang)
            _uiState.update { it.copy(language = lang, theme = theme, localization = bundle) }
            loadMonth()
        }
    }

    fun loadMonth() {
        val state = _uiState.value
        loadData(state.language.code, state.currentMonth, state.currentYear)
    }

    fun forceReload(language: AppLanguage) {
        viewModelScope.launch {
            preferences.setLanguage(language)
            val bundle = localizationManager.loadBundle(language)
            _uiState.update { it.copy(language = language, localization = bundle) }
            loadData(language.code, _uiState.value.currentMonth, _uiState.value.currentYear)
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferences.setTheme(theme)
            _uiState.update { it.copy(theme = theme) }
        }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun goToPreviousMonth() {
        _uiState.update {
            if (it.currentMonth == 1) {
                it.copy(currentMonth = 12, currentYear = it.currentYear - 1)
            } else {
                it.copy(currentMonth = it.currentMonth - 1)
            }
        }
        loadMonth()
    }

    fun goToNextMonth() {
        _uiState.update {
            if (it.currentMonth == 12) {
                it.copy(currentMonth = 1, currentYear = it.currentYear + 1)
            } else {
                it.copy(currentMonth = it.currentMonth + 1)
            }
        }
        loadMonth()
    }

    fun goToMonth(month: Int, year: Int) {
        _uiState.update { it.copy(currentMonth = month, currentYear = year) }
        loadMonth()
    }

    fun goToToday() {
        val now = LocalDate.now()
        _uiState.update {
            it.copy(
                currentMonth = now.monthValue,
                currentYear = now.year,
                scrollToTodayTrigger = !it.scrollToTodayTrigger
            )
        }
        loadMonth()
    }

    private fun loadData(locale: String, month: Int, year: Int) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val days = repository.loadMonth(locale, year, month)
        if (days.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "No data for $locale $year",
                    daysInMonth = emptyList()
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    daysInMonth = days,
                    loadedLocale = locale
                )
            }
        }
    }
}
