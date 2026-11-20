package com.example.hydralife.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class UserSessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hydralife_user_prefs", Context.MODE_PRIVATE)

    fun saveUser(name: String, email: String) {
        prefs.edit().apply {
            putString("KEY_NAME", name)
            putString("KEY_EMAIL", email)
            putBoolean("KEY_LOGGED_IN", true)
            apply()
        }
    }

    fun getUserName(): String {
        return prefs.getString("KEY_NAME", "Alex") ?: "Alex"
    }

    fun getUserEmail(): String {
        return prefs.getString("KEY_EMAIL", "alex@example.com") ?: "alex@example.com"
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("KEY_LOGGED_IN", false)
    }

    fun logout() {
        prefs.edit().apply {
            putBoolean("KEY_LOGGED_IN", false)
            apply()
        }
    }

    fun isNotificationsEnabled(): Boolean {
        return prefs.getBoolean("KEY_NOTIFICATIONS_ENABLED", false)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("KEY_NOTIFICATIONS_ENABLED", enabled).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("KEY_ONBOARDING_COMPLETED", false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("KEY_ONBOARDING_COMPLETED", completed).apply()
    }

    fun getThemeMode(): Int {
        return prefs.getInt("KEY_THEME_MODE", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt("KEY_THEME_MODE", mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getSelectedCity(): String {
        return prefs.getString("KEY_SELECTED_CITY", "Rabat") ?: "Rabat"
    }

    fun setSelectedCity(cityName: String) {
        prefs.edit().putString("KEY_SELECTED_CITY", cityName).apply()
    }

    fun getLastHotAlertTime(): Long {
        return prefs.getLong("KEY_LAST_HOT_ALERT_TIME", 0L)
    }

    fun setLastHotAlertTime(time: Long) {
        prefs.edit().putLong("KEY_LAST_HOT_ALERT_TIME", time).apply()
    }
}
