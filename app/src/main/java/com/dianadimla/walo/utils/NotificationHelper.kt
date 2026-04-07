package com.dianadimla.walo.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dianadimla.walo.R

// Helper class for displaying dropdown system notifications for AI nudges.
class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "walo_ai_nudges"
        private const val CHANNEL_NAME = "Walo AI Nudges"
        private const val CHANNEL_DESCRIPTION = "Notifications for spending behavior guidance"
        private var notificationId = 100 // Starting point for unique IDs
    }

    init {
        createNotificationChannel()
    }

    // Creates the notification channel for Android 8.0 (Oreo) and higher.
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Shows a high-priority dropdown notification.
    // message The nudge message to display.
    @SuppressLint("MissingPermission")
    fun showNotification(message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification) //Icon from Icons8
            .setContentTitle("Walo AI Nudge")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Dropdown behavior
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true) // Removes notification when clicked

        try {
            with(NotificationManagerCompat.from(context)) {
                // Generate a unique ID for each nudge
                notify(notificationId++, builder.build())
            }
        } catch (e: SecurityException) {
            // Permission check for Android 13+ is handled in MainActivity
        }
    }
}
