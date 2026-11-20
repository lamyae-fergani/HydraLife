package com.example.hydralife.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hydralife.data.UserSessionManager
import com.example.hydralife.utils.NotificationHelper
import com.example.hydralife.utils.WeatherApiService

class HydrationReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sessionManager = UserSessionManager(applicationContext)
        // Ensure reminder preference is still enabled before showing notification
        if (sessionManager.isNotificationsEnabled()) {
            val weather = WeatherApiService.getWeatherData(sessionManager.getSelectedCity())
            val temp = weather?.temperature ?: 20.0
            val isHot = temp > 28.0 // 28°C threshold for hot weather hydration alerts

            val currentTime = System.currentTimeMillis()
            val lastHotAlert = sessionManager.getLastHotAlertTime()
            // Duplicate prevention: 4 hours cooldown between hot weather alerts
            val shouldAlertHot = isHot && (currentTime - lastHotAlert > 4 * 60 * 60 * 1000L)
            if (shouldAlertHot) {
                sessionManager.setLastHotAlertTime(currentTime)
            }

            NotificationHelper.showHydrationReminder(applicationContext, shouldAlertHot)
        }
        return Result.success()
    }
}
