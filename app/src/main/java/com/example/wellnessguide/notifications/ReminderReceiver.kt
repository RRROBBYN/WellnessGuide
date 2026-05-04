package com.example.wellnessguide.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R

class ReminderReceiver : BroadcastReceiver() {

    private val channelId = "wellness_reminders_channel"
    private val prefsName = "wellness_reminders"

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Wellness Reminder"
        val message = intent.getStringExtra("message") ?: "You have a wellness reminder."
        val notificationId = intent.getIntExtra("notificationId", System.currentTimeMillis().toInt())
        val ongoing = intent.getBooleanExtra("ongoing", false)
        val oneTime = intent.getBooleanExtra("oneTime", false)
        val statusKey = intent.getStringExtra("statusKey") ?: ""
        val type = intent.getStringExtra("type") ?: reminderTypeFromTitle(title)

        AppNotificationStore.add(
            context = context.applicationContext,
            title = title,
            message = message,
            type = type
        )

        showSystemNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            message = message,
            ongoing = ongoing
        )

        if (oneTime && statusKey.isNotBlank()) {
            context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(statusKey, false)
                .apply()
        }
    }

    private fun showSystemNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        ongoing: Boolean
    ) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_activity)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                channelId,
                "Wellness Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for water, rest, sleep, check-ins, and symptom follow-ups."
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun reminderTypeFromTitle(title: String): String {
        val lower = title.lowercase()

        return when {
            lower.contains("water") -> "water"
            lower.contains("sleep") -> "sleep"
            lower.contains("eye") || lower.contains("eyes") -> "eyes"
            lower.contains("check") -> "status"
            lower.contains("symptom") -> "warning"
            lower.contains("test") -> "status"
            else -> "general"
        }
    }
}