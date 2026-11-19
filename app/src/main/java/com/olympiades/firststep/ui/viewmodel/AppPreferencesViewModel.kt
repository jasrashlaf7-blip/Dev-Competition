package com.olympiades.firststep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olympiades.firststep.data.preferences.UserPreferencesState
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.AppThemeMode
import com.olympiades.firststep.domain.model.DateTimeHelper
import com.olympiades.firststep.domain.repository.FirstStepRepository
import com.olympiades.firststep.domain.security.PinSecurityHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppPreferencesViewModel(
    private val repository: FirstStepRepository
) : ViewModel() {

    private val _isSeedReady = MutableStateFlow(false)
    val isSeedReady: StateFlow<Boolean> = _isSeedReady.asStateFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    val preferences: StateFlow<UserPreferencesState> = repository.observePreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferencesState()
        )

    val favoriteCount: StateFlow<Int> = repository.observeFavoriteCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val unansweredOver7DaysCount: StateFlow<Int> = repository.observeApplications(null)
        .map { apps ->
            val today = DateTimeHelper.todayIso()
            apps.count { it.isUnansweredOver7Days(today) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    init {
        viewModelScope.launch {
            val initialPrefs = runCatching { repository.observePreferences().first() }.getOrNull()
            if (initialPrefs?.pinLockEnabled == true && initialPrefs.pinHash.isNotBlank()) {
                _isAppLocked.value = true
            }
            runCatching { repository.ensureInitialized() }
            _isSeedReady.value = true
        }
    }

    fun verifyAndUnlock(pin: String): Boolean {
        val current = preferences.value
        val valid = PinSecurityHelper.verifyPin(pin, current.pinSalt, current.pinHash)
        if (valid) {
            _isAppLocked.value = false
        }
        return valid
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setLanguage(language)
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(themeMode)
        }
    }
}
