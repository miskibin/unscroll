package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingRepository(private val settingDao: SettingDao) {

    companion object {
        const val KEY_DELAY_SECONDS = "delay_seconds"
        const val DEFAULT_DELAY_SECONDS = 5

        const val KEY_BLOCKED_PACKAGES = "blocked_packages"
        const val DEFAULT_BLOCKED_PACKAGES = "com.instagram.android,com.zhiliaoapp.musically,com.ss.android.ugc.trill,com.ss.android.ugc.aweme"

        const val KEY_COOLDOWN_ENABLED = "cooldown_enabled"
        const val DEFAULT_COOLDOWN_ENABLED = "false"

        const val KEY_COOLDOWN_USAGE_MINUTES = "cooldown_usage_minutes"
        const val DEFAULT_COOLDOWN_USAGE_MINUTES = 5

        const val KEY_COOLDOWN_PERIOD_MINUTES = "cooldown_period_minutes"
        const val DEFAULT_COOLDOWN_PERIOD_MINUTES = 10

        const val KEY_INTENTION_PLAN = "intention_plan"
        const val DEFAULT_INTENTION_PLAN = ""
    }

    val delaySecondsFlow: Flow<Int> = settingDao.getSettingFlow(KEY_DELAY_SECONDS)
        .map { setting ->
            setting?.value?.toIntOrNull() ?: DEFAULT_DELAY_SECONDS
        }

    val blockedPackagesFlow: Flow<String> = settingDao.getSettingFlow(KEY_BLOCKED_PACKAGES)
        .map { setting ->
            setting?.value ?: DEFAULT_BLOCKED_PACKAGES
        }

    val cooldownEnabledFlow: Flow<Boolean> = settingDao.getSettingFlow(KEY_COOLDOWN_ENABLED)
        .map { setting ->
            setting?.value?.lowercase() == "true"
        }

    val cooldownUsageMinutesFlow: Flow<Int> = settingDao.getSettingFlow(KEY_COOLDOWN_USAGE_MINUTES)
        .map { setting ->
            setting?.value?.toIntOrNull() ?: DEFAULT_COOLDOWN_USAGE_MINUTES
        }

    val cooldownPeriodMinutesFlow: Flow<Int> = settingDao.getSettingFlow(KEY_COOLDOWN_PERIOD_MINUTES)
        .map { setting ->
            setting?.value?.toIntOrNull() ?: DEFAULT_COOLDOWN_PERIOD_MINUTES
        }

    val intentionPlanFlow: Flow<String> = settingDao.getSettingFlow(KEY_INTENTION_PLAN)
        .map { setting ->
            setting?.value ?: DEFAULT_INTENTION_PLAN
        }

    suspend fun getDelaySeconds(): Int {
        return settingDao.getSetting(KEY_DELAY_SECONDS)?.value?.toIntOrNull() ?: DEFAULT_DELAY_SECONDS
    }

    suspend fun setDelaySeconds(seconds: Int) {
        settingDao.insertSetting(Setting(KEY_DELAY_SECONDS, seconds.toString()))
    }

    suspend fun getBlockedPackages(): String {
        return settingDao.getSetting(KEY_BLOCKED_PACKAGES)?.value ?: DEFAULT_BLOCKED_PACKAGES
    }

    suspend fun setBlockedPackages(packages: String) {
        settingDao.insertSetting(Setting(KEY_BLOCKED_PACKAGES, packages))
    }

    suspend fun isCooldownEnabled(): Boolean {
        return settingDao.getSetting(KEY_COOLDOWN_ENABLED)?.value?.lowercase() == "true"
    }

    suspend fun setCooldownEnabled(enabled: Boolean) {
        settingDao.insertSetting(Setting(KEY_COOLDOWN_ENABLED, enabled.toString()))
    }

    suspend fun getCooldownUsageMinutes(): Int {
        return settingDao.getSetting(KEY_COOLDOWN_USAGE_MINUTES)?.value?.toIntOrNull() ?: DEFAULT_COOLDOWN_USAGE_MINUTES
    }

    suspend fun setCooldownUsageMinutes(minutes: Int) {
        settingDao.insertSetting(Setting(KEY_COOLDOWN_USAGE_MINUTES, minutes.toString()))
    }

    suspend fun getCooldownPeriodMinutes(): Int {
        return settingDao.getSetting(KEY_COOLDOWN_PERIOD_MINUTES)?.value?.toIntOrNull() ?: DEFAULT_COOLDOWN_PERIOD_MINUTES
    }

    suspend fun setCooldownPeriodMinutes(minutes: Int) {
        settingDao.insertSetting(Setting(KEY_COOLDOWN_PERIOD_MINUTES, minutes.toString()))
    }

    suspend fun getIntentionPlan(): String {
        return settingDao.getSetting(KEY_INTENTION_PLAN)?.value ?: DEFAULT_INTENTION_PLAN
    }

    suspend fun setIntentionPlan(plan: String) {
        settingDao.insertSetting(Setting(KEY_INTENTION_PLAN, plan))
    }
}
