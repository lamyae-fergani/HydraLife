package com.example.hydralife

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.hydralife.data.HydrationDatabase
import com.example.hydralife.data.HydrationRepository
import com.example.hydralife.data.UserSessionManager
import com.example.hydralife.utils.NavigationDrawerManager
import com.example.hydralife.worker.HydrationReminderWorker
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

class MyAccountActivity : AppCompatActivity() {

    private lateinit var sessionManager: UserSessionManager
    private lateinit var repository: HydrationRepository

    private lateinit var ivMenuBtn: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var tvGoalSummary: TextView
    private lateinit var tvSettingLanguage: TextView
    private lateinit var tvSettingTheme: TextView
    private lateinit var tvSettingNotifications: TextView
    private lateinit var tvSettingAbout: TextView
    private lateinit var btnLogout: MaterialButton
    private lateinit var bottomNavigationView: BottomNavigationView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableNotifications()
            Toast.makeText(this, getString(R.string.reminders_enabled_toast), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.permission_denied_toast), Toast.LENGTH_LONG).show()
            sessionManager.setNotificationsEnabled(false)
            updateNotificationPreferenceText()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_account)

        sessionManager = UserSessionManager(this)
        val dao = HydrationDatabase.getDatabase(this).hydrationDao()
        repository = HydrationRepository(dao)

        ivMenuBtn = findViewById(R.id.ivMenuBtn)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        tvGoalSummary = findViewById(R.id.tvGoalSummary)
        tvSettingLanguage = findViewById(R.id.tvSettingLanguage)
        tvSettingTheme = findViewById(R.id.tvSettingTheme)
        tvSettingNotifications = findViewById(R.id.tvSettingNotifications)
        tvSettingAbout = findViewById(R.id.tvSettingAbout)
        btnLogout = findViewById(R.id.btnLogout)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        ivMenuBtn.setOnClickListener {
            NavigationDrawerManager.showDrawer(this, "account")
        }

        updateUserInfoUI()
        loadGoalSummary()
        updateLanguagePreferenceText()
        updateThemePreferenceText()
        updateNotificationPreferenceText()

        bottomNavigationView.selectedItemId = R.id.nav_account
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    navigateTo(MainActivity::class.java)
                    true
                }
                R.id.nav_objectives -> {
                    navigateTo(ObjectivesActivity::class.java)
                    true
                }
                R.id.nav_ai_assistant -> {
                    navigateTo(AiAssistantActivity::class.java)
                    true
                }
                R.id.nav_account -> true
                else -> false
            }
        }

        btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        tvSettingLanguage.setOnClickListener {
            showLanguageSelectionDialog()
        }

        tvSettingTheme.setOnClickListener {
            showThemeSelectionDialog()
        }

        tvSettingNotifications.setOnClickListener {
            showNotificationPreferencesDialog()
        }

        tvSettingAbout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.about_dialog_title))
                .setMessage(getString(R.string.about_dialog_msg))
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun updateUserInfoUI() {
        tvUserName.text = sessionManager.getUserName()
        tvUserEmail.text = sessionManager.getUserEmail()
    }

    private fun loadGoalSummary() {
        lifecycleScope.launch {
            val goalEntity = repository.getOrCreateGoalToday()
            val goalMl = goalEntity.goalMl
            val goalL = String.format(Locale.getDefault(), "%.1f L", goalMl / 1000.0)
            tvGoalSummary.text = "$goalMl ml ($goalL)"
        }
    }

    private fun updateLanguagePreferenceText() {
        val locales = AppCompatDelegate.getApplicationLocales()
        val langCode = if (!locales.isEmpty) locales[0]?.language else "en"
        val langName = when (langCode) {
            "fr" -> "Français"
            "ar" -> "العربية"
            else -> "English"
        }
        tvSettingLanguage.text = getString(R.string.language_pref, langName)
    }

    private fun showLanguageSelectionDialog() {
        val languages = arrayOf("English", "Français", "العربية")
        val codes = arrayOf("en", "fr", "ar")

        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentCode = if (!currentLocales.isEmpty) currentLocales[0]?.language else "en"
        val checkedItem = codes.indexOf(currentCode).takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_lang_title))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val selectedCode = codes[which]
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedCode))
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updateThemePw referenceText() {
        val mode = sessionManager.getThemeMode()
        val name = when (mode) {
            AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.theme_light)
            AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.theme_dark)
            else -> getString(R.string.theme_system)
        }
        tvSettingTheme.text = getString(R.string.theme_pref, name)
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf(getString(R.string.theme_light), getString(R.string.theme_dark), getString(R.string.theme_system))
        val modes = intArrayOf(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        val currentMode = sessionManager.getThemeMode()
        val checkedItem = modes.indexOf(currentMode).takeIf { it >= 0 } ?: 2

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_theme_title))
            .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                val selectedMode = modes[which]
                sessionManager.setThemeMode(selectedMode)
                updateThemePreferenceText()
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updateNotificationPreferenceText() {
        val enabled = sessionManager.isNotificationsEnabled()
        val statusStr = if (enabled) getString(R.string.enabled_status) else getString(R.string.disabled_status)
        tvSettingNotifications.text = getString(R.string.notification_pref_status, statusStr)
    }

    private fun showNotificationPreferencesDialog() {
        val currentEnabled = sessionManager.isNotificationsEnabled()
        val options = arrayOf(getString(R.string.enabled_status), getString(R.string.disabled_status))

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.notification_pref))
            .setSingleChoiceItems(options, if (currentEnabled) 0 else 1) { dialog, which ->
                if (which == 0) {
                    checkAndRequestNotificationPermission()
                } else {
                    disableNotifications()
                    Toast.makeText(this, getString(R.string.reminders_disabled_toast), Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    enableNotifications()
                    Toast.makeText(this, getString(R.string.reminders_enabled_toast), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            enableNotifications()
            Toast.makeText(this, getString(R.string.reminders_enabled_toast), Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableNotifications() {
        sessionManager.setNotificationsEnabled(true)
        updateNotificationPreferenceText()

        val workRequest = PeriodicWorkRequestBuilder<HydrationReminderWorker>(2, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "hydration_reminder_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun disableNotifications() {
        sessionManager.setNotificationsEnabled(false)
        updateNotificationPreferenceText()

        WorkManager.getInstance(applicationContext).cancelUniqueWork("hydration_reminder_work")
    }

    private fun showEditProfileDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.edit_profile_title))

        val input = EditText(this).apply {
            setText(sessionManager.getUserName())
            hint = getString(R.string.edit_profile_hint)
        }
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.save)) { _, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                sessionManager.saveUser(newName, sessionManager.getUserEmail())
                updateUserInfoUI()
                Toast.makeText(this, getString(R.string.profile_updated_toast), Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
        builder.show()
    }
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.logout_dialog_title))
            .setMessage(getString(R.string.logout_dialog_msg))
            .setPositiveButton(getString(R.string.logout_button)) { _, _ ->
                sessionManager.logout()
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun navigateTo(targetActivity: Class<*>) {
        val intent = Intent(this, targetActivity).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }
}
