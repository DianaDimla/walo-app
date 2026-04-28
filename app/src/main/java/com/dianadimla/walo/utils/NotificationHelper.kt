/**
 * Helper class for managing and displaying system notifications for AI nudges.
 * Configures the required notification channels and manages high-importance delivery.
 */
package com.dianadimla.walo.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dianadimla.walo.R

class NotificationHelper(private val context: Context) {

    companion object {
        // Unique channel ID to ensure distinct delivery settings for AI guidance
        private const val CHANNEL_ID = "walo_ai_nudges_v3"
        private const val CHANNEL_NAME = "Walo AI Nudges"
        private const val CHANNEL_DESCRIPTION = "Notifications for spending behaviour guidance"
        private var notificationId = 100 
    }

    init {
        createNotificationChannel()
    }

    /**
     * Initialises the notification channel required for Android Oreo (8.0) and above.
     * Sets high importance to enable heads-up visual alerts.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Constructs and displays a system notification with the provided message.
     * Configures priority and defaults to ensure consistent visibility across devices.
     */
    @SuppressLint("MissingPermission")
    fun showNotification(message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle("Walo AI Nudge")
            .setContentText(message)
            // Priority and defaults are critical for heads-up alert visibility
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                // Increments ID to prevent previous notifications from being overwritten
                notify(notificationId++, builder.build())
            }
        } catch (e: SecurityException) {
            // Permission handling is deferred to the main activity entry point
        }
    }
}
