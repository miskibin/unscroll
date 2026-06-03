package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SettingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SettingRepository

    val delaySeconds: StateFlow<Int>
    val blockedPackages: StateFlow<String>
    val cooldownEnabled: StateFlow<Boolean>
    val cooldownUsageMinutes: StateFlow<Int>
    val cooldownPeriodMinutes: StateFlow<Int>
    val intentionPlan: StateFlow<String>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SettingRepository(database.settingDao())
        
        delaySeconds = repository.delaySecondsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = SettingRepository.DEFAULT_DELAY_SECONDS
            )

        blockedPackages = repository.blockedPackagesFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = SettingRepository.DEFAULT_BLOCKED_PACKAGES
            )

        cooldownEnabled = repository.cooldownEnabledFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = false
            )

        cooldownUsageMinutes = repository.cooldownUsageMinutesFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = SettingRepository.DEFAULT_COOLDOWN_USAGE_MINUTES
            )

        cooldownPeriodMinutes = repository.cooldownPeriodMinutesFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = SettingRepository.DEFAULT_COOLDOWN_PERIOD_MINUTES
            )

        intentionPlan = repository.intentionPlanFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = SettingRepository.DEFAULT_INTENTION_PLAN
            )
    }

    fun updateDelaySeconds(seconds: Int) {
        viewModelScope.launch {
            repository.setDelaySeconds(seconds)
        }
    }

    fun updateBlockedPackages(packages: String) {
        viewModelScope.launch {
            repository.setBlockedPackages(packages)
        }
    }

    fun updateCooldownEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setCooldownEnabled(enabled)
        }
    }

    fun updateCooldownUsageMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setCooldownUsageMinutes(minutes)
        }
    }

    fun updateCooldownPeriodMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setCooldownPeriodMinutes(minutes)
        }
    }

    fun updateIntentionPlan(plan: String) {
        viewModelScope.launch {
            repository.setIntentionPlan(plan)
        }
    }
}
