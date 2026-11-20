package com.example.hydralife.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.hydralife.*
import com.example.hydralife.data.UserSessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog

object NavigationDrawerManager {

    fun showDrawer(activity: Activity, currentScreen: String) {
        val dialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_navigation_drawer, null)
        dialog.setContentView(view)

        val items = listOf(
            Triple(R.id.menuDashboard, "dashboard", MainActivity::class.java),
            Triple(R.id.menuObjectives, "objectives", ObjectivesActivity::class.java),
            Triple(R.id.menuAi, "ai", AiAssistantActivity::class.java),
            Triple(R.id.menuConversations, "conversations", ConversationsActivity::class.java),
            Triple(R.id.menuAccount, "account", MyAccountActivity::class.java),
            Triple(R.id.menuSettings, "settings", null),
            Triple(R.id.menuAbout, "about", null),
            Triple(R.id.menuLogout, "logout", null)
        )

        for ((id, tag, targetClass) in items) {
            val itemView = view.findViewById<LinearLayout>(id) ?: continue
            val labelView = itemView.getChildAt(1) as? TextView
            val iconView = itemView.getChildAt(0) as? ImageView

            if (tag == currentScreen) {
                labelView?.setTextColor(android.graphics.Color.parseColor("#18D6B2"))
                iconView?.setColorFilter(android.graphics.Color.parseColor("#18D6B2"))
            }

            itemView.setOnClickListener {
                dialog.dismiss()
                when {
                    targetClass != null && targetClass != activity::class.java -> {
                        val intent = Intent(activity, targetClass).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        activity.startActivity(intent)
                        activity.overridePendingTransition(0, 0)
                        activity.finish()
                    }
                    tag == "settings" -> {
                        if (activity !is MyAccountActivity) {
                            val intent = Intent(activity, MyAccountActivity::class.java)
                            activity.startActivity(intent)
                        }
                    }
                    tag == "about" -> {
                        AlertDialog.Builder(activity)
                            .setTitle(activity.getString(R.string.about_dialog_title))
                            .setMessage(activity.getString(R.string.about_dialog_msg))
                            .setPositiveButton("OK") { d, _ -> d.dismiss() }
                            .show()
                    }
                    tag == "logout" -> {
                        AlertDialog.Builder(activity)
                            .setTitle(activity.getString(R.string.logout_dialog_title))
                            .setMessage(activity.getString(R.string.logout_dialog_msg))
                            .setPositiveButton(activity.getString(R.string.logout_button)) { _, _ ->
                                UserSessionManager(activity).logout()
                                val intent = Intent(activity, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                activity.startActivity(intent)
                                activity.finish()
                            }
                            .setNegativeButton(activity.getString(R.string.cancel)) { d, _ -> d.dismiss() }
                            .show()
                    }
                }
            }
        }

        dialog.show()
    }
}
