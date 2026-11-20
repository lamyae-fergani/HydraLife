package com.example.hydralife.utils

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.hydralife.MyAccountActivity
import com.example.hydralife.R
import com.example.hydralife.data.UserSessionManager
import com.example.hydralife.worker.HydrationReminderWorker
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.concurrent.TimeUnit

object SettingsDrawerManager {

    fun showDrawer(activity: Activity) {
        val dialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_settings_drawer, null)
        dialog.setContentView(view)

        val sessionManager = UserSessionManager(activity)

        // 1. Profile Section
        val tvDrawerName = view.findViewById<TextView>(R.id.tvDrawerName)
        val tvDrawerEmail = view.findViewById<TextView>(R.id.tvDrawerEmail)
        val layoutDrawerProfile = view.findViewById<LinearLayout>(R.id.layoutDrawerProfile)

        tvDrawerName.text = sessionManager.getUserName()
        tvDrawerEmail.text = sessionManager.getUserEmail()

        layoutDrawerProfile.setOnClickListener {
            dialog.dismiss()
            if (activity !is MyAccountActivity) {
                val intent = Intent(activity, MyAccountActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                activity.startActivity(intent)
            }
        }

        val turquoise = Color.parseColor("#18D6B2")

        // Dropdown Headers and Contents
        val headerLanguage = view.findViewById<LinearLayout>(R.id.headerLanguage)
        val contentLanguage = view.findViewById<LinearLayout>(R.id.contentLanguage)
        val ivLangChevron = view.findViewById<ImageView>(R.id.ivLangChevron)
        val tvLangValue = view.findViewById<TextView>(R.id.tvLangValue)

        val headerAppearance = view.findViewById<LinearLayout>(R.id.headerAppearance)
        val contentAppearance = view.findViewById<LinearLayout>(R.id.contentAppearance)
        val ivAppearanceChevron = view.findViewById<ImageView>(R.id.ivAppearanceChevron)
        val tvAppearanceValue = view.findViewById<TextView>(R.id.tvAppearanceValue)

        val headerNotifications = view.findViewById<LinearLayout>(R.id.headerNotifications)
        val contentNotifications = view.findViewById<LinearLayout>(R.id.contentNotifications)
        val ivNotificationChevron = view.findViewById<ImageView>(R.id.ivNotificationChevron)
        val tvNotificationValue = view.findViewById<TextView>(R.id.tvNotificationValue)

        val headerAbout = view.findViewById<LinearLayout>(R.id.headerAbout)
        val contentAbout = view.findViewById<LinearLayout>(R.id.contentAbout)
        val ivAboutChevron = view.findViewById<ImageView>(R.id.ivAboutChevron)

        val allContents = listOf(contentLanguage, contentAppearance, contentNotifications, contentAbout)
        val allChevrons = listOf(ivLangChevron, ivAppearanceChevron, ivNotificationChevron, ivAboutChevron)

        fun toggleSection(content: LinearLayout, chevron: ImageView) {
            val isExpanded = content.visibility == View.VISIBLE
            for (c in allContents) c.visibility = View.GONE
            for (ch in allChevrons) ch.animate().rotation(0f).setDuration(200).start()

            if (!isExpanded) {
                content.visibility = View.VISIBLE
                chevron.animate().rotation(180f).setDuration(200).start()
            }
        }

        headerLanguage.setOnClickListener { toggleSection(contentLanguage, ivLangChevron) }
        headerAppearance.setOnClickListener { toggleSection(contentAppearance, ivAppearanceChevron) }
        headerNotifications.setOnClickListener { toggleSection(contentNotifications, ivNotificationChevron) }
        headerAbout.setOnClickListener { toggleSection(contentAbout, ivAboutChevron) }

        // Language Options
        val locales = AppCompatDelegate.getApplicationLocales()
        val currentLang = if (!locales.isEmpty) locales[0]?.language else "en"
        tvLangValue.text = when (currentLang) {
            "fr" -> "Français"
            "ar" -> "العربية"
            else -> "English"
        }

        val langEnglish = view.findViewById<TextView>(R.id.langEnglish)
        val langFrench = view.findViewById<TextView>(R.id.langFrench)
        val langArabic = view.findViewById<TextView>(R.id.langArabic)

        when (currentLang) {
            "fr" -> langFrench.setTextColor(turquoise)
            "ar" -> langArabic.setTextColor(turquoise)
            else -> langEnglish.setTextColor(turquoise)
        }

        langEnglish.setOnClickListener {
            dialog.dismiss()
            if (currentLang != "en") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                activity.recreate()
            }
        }
        langFrench.setOnClickListener {
            dialog.dismiss()
            if (currentLang != "fr") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("fr"))
                activity.recreate()
            }
        }
        langArabic.setOnClickListener {
            dialog.dismiss()
            if (currentLang != "ar") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar"))
                activity.recreate()
            }
        }

        // Appearance Options
        val currentTheme = sessionManager.getThemeMode()
        tvAppearanceValue.text = when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> activity.getString(R.string.theme_light)
            AppCompatDelegate.MODE_NIGHT_YES -> activity.getString(R.string.theme_dark)
            else -> activity.getString(R.string.theme_system)
        }

        val themeLight = view.findViewById<TextView>(R.id.themeLight)
        val themeDark = view.findViewById<TextView>(R.id.themeDark)
        val themeSystem = view.findViewById<TextView>(R.id.themeSystem)

        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> themeLight.setTextColor(turquoise)
            AppCompatDelegate.MODE_NIGHT_YES -> themeDark.setTextColor(turquoise)
            else -> themeSystem.setTextColor(turquoise)
        }

        themeLight.setOnClickListener {
            dialog.dismiss()
            if (currentTheme != AppCompatDelegate.MODE_NIGHT_NO) {
                sessionManager.setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
                activity.recreate()
            }
        }
        themeDark.setOnClickListener {
            dialog.dismiss()
            if (currentTheme != AppCompatDelegate.MODE_NIGHT_YES) {
                sessionManager.setThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
                activity.recreate()
            }
        }
        themeSystem.setOnClickListener {
            dialog.dismiss()
            if (currentTheme != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                sessionManager.setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                activity.recreate()
            }
        }

        // Notifications Options
        val isNotifEnabled = sessionManager.isNotificationsEnabled()
        tvNotificationValue.text = if (isNotifEnabled) activity.getString(R.string.enabled_status) else activity.getString(R.string.disabled_status)

        val notifEnable = view.findViewById<TextView>(R.id.notifEnable)
        val notifDisable = view.findViewById<TextView>(R.id.notifDisable)

        if (isNotifEnabled) notifEnable.setTextColor(turquoise) else notifDisable.setTextColor(turquoise)

        notifEnable.setOnClickListener {
            dialog.dismiss()
            sessionManager.setNotificationsEnabled(true)
            val workRequest = PeriodicWorkRequestBuilder<HydrationReminderWorker>(2, TimeUnit.HOURS).build()
            WorkManager.getInstance(activity.applicationContext).enqueueUniquePeriodicWork(
                "hydration_reminder_work",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Toast.makeText(activity, activity.getString(R.string.reminders_enabled_toast), Toast.LENGTH_SHORT).show()
        }

        notifDisable.setOnClickListener {
            dialog.dismiss()
            sessionManager.setNotificationsEnabled(false)
            WorkManager.getInstance(activity.applicationContext).cancelUniqueWork("hydration_reminder_work")
            Toast.makeText(activity, activity.getString(R.string.reminders_disabled_toast), Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }
}
