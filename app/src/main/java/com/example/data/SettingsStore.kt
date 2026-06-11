package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.detection.Platforms
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GuardSettings(
    val enabled: Boolean = true,
    val allowanceSeconds: Int = 120,
    val windowMinutes: Int = 10,
    val enabledPlatforms: Set<String> = Platforms.all.map { it.key }.toSet(),
) {
    val allowanceMs: Long get() = allowanceSeconds * 1000L
    val windowMs: Long get() = windowMinutes * 60_000L
}

/**
 * SharedPreferences-backed settings, exposed as a StateFlow so both the
 * dashboard UI and the accessibility service always see the current values.
 */
class SettingsStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("scroll_guard", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<GuardSettings> = _settings

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _settings.value = read()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    fun setAllowanceSeconds(value: Int) =
        prefs.edit().putInt(KEY_ALLOWANCE_SECONDS, value.coerceIn(30, 600)).apply()

    fun setWindowMinutes(value: Int) =
        prefs.edit().putInt(KEY_WINDOW_MINUTES, value.coerceIn(5, 120)).apply()

    fun setPlatformEnabled(key: String, enabled: Boolean) {
        val current = _settings.value.enabledPlatforms.toMutableSet()
        if (enabled) current.add(key) else current.remove(key)
        prefs.edit().putStringSet(KEY_PLATFORMS, current).apply()
    }

    private fun read(): GuardSettings {
        val defaults = GuardSettings()
        return GuardSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, defaults.enabled),
            allowanceSeconds = prefs.getInt(KEY_ALLOWANCE_SECONDS, defaults.allowanceSeconds),
            windowMinutes = prefs.getInt(KEY_WINDOW_MINUTES, defaults.windowMinutes),
            enabledPlatforms = prefs.getStringSet(KEY_PLATFORMS, defaults.enabledPlatforms)
                ?.toSet() ?: defaults.enabledPlatforms,
        )
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_ALLOWANCE_SECONDS = "allowance_seconds"
        private const val KEY_WINDOW_MINUTES = "window_minutes"
        private const val KEY_PLATFORMS = "enabled_platforms"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context.applicationContext).also { instance = it }
            }
    }
}
