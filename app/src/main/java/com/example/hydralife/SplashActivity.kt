package com.example.hydralife

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.hydralife.data.UserSessionManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = UserSessionManager(this)
        AppCompatDelegate.setDefaultNightMode(sessionManager.getThemeMode())

        setContentView(R.layout.activity_splash)

        // Delay for 2.5 seconds before navigating
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = when {
                !sessionManager.isOnboardingCompleted() -> {
                    Intent(this, OnboardingActivity::class.java)
                }
                sessionManager.isLoggedIn() -> {
                    Intent(this, MainActivity::class.java)
                }
                else -> {
                    Intent(this, LoginActivity::class.java)
                }
            }
            startActivity(intent)
            finish()
        }, 2500)
    }
}
