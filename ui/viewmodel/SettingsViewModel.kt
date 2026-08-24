package com.example.nestworth.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nestworth.Repository.settings.AppSettings
import com.example.nestworth.Repository.settings.Currency
import com.example.nestworth.Repository.settings.SettingsRepository
import com.example.nestworth.Repository.settings.ThemeMode
import com.example.nestworth.Repository.settings.TimeRange
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings()
        )

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch {
            repository.setTheme(theme)
        }
    }

    fun setCurrency(currency: Currency) {
        viewModelScope.launch {
            repository.setCurrency(currency)
        }
    }

    fun setTimeRange(timeRange: TimeRange) {
        viewModelScope.launch {
            repository.setTimeRange(timeRange)
        }
    }
}