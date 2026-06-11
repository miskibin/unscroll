package com.example

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import com.example.data.BudgetSnapshot
import com.example.data.GuardSettings
import com.example.data.GuardState
import com.example.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore.get(application)

    val settings: StateFlow<GuardSettings> = store.settings
    val budget: StateFlow<BudgetSnapshot> = GuardState.budget
    val serviceRunning: StateFlow<Boolean> = GuardState.serviceRunning

    private val _serviceEnabledInSystem = MutableStateFlow(false)
    val serviceEnabledInSystem: StateFlow<Boolean> = _serviceEnabledInSystem

    fun refreshServiceState() {
        _serviceEnabledInSystem.value = isAccessibilityServiceEnabled(getApplication())
    }

    fun setEnabled(value: Boolean) = store.setEnabled(value)
    fun setAllowanceSeconds(value: Int) = store.setAllowanceSeconds(value)
    fun setWindowMinutes(value: Int) = store.setWindowMinutes(value)
    fun setPlatformEnabled(key: String, enabled: Boolean) = store.setPlatformEnabled(key, enabled)

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val component = "${context.packageName}/${ScrollGuardService::class.java.name}"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { it.equals(component, ignoreCase = true) }
    }
}
