package com.example.wellnessguide.ui.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R
import com.example.wellnessguide.notifications.ReminderReceiver
import com.example.wellnessguide.recent.RecentActivityStore
class RemindersFragment : Fragment() {

    private val primaryColor = Color.rgb(26, 107, 114)
    private val backgroundColor = Color.rgb(238, 248, 247)
    private val surfaceColor = Color.WHITE
    private val textPrimary = Color.rgb(28, 43, 45)
    private val textSecondary = Color.rgb(93, 122, 126)
    private val borderColor = Color.rgb(221, 237, 234)

    private val prefsName = "wellness_reminders"
    private val channelId = "wellness_reminders_channel"

    private var pendingPermissionAction: (() -> Unit)? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingPermissionAction?.invoke()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Notification permission is needed to turn on reminders.",
                    Toast.LENGTH_LONG
                ).show()
            }

            pendingPermissionAction = null
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val scroll = ScrollView(requireContext()).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(42))
        }

        addTopBar(root)

        root.addView(bigTitle("Reminders"))

        root.addView(
            paragraph(
                "Turn on wellness reminders. When a reminder is on, a persistent notification stays visible until you turn it off."
            )
        )

        root.addView(
            noteCard(
                "Tip: Use Test Reminder first. It sends a notification after 10 seconds so you can confirm that notifications are working."
            )
        )

        root.addView(
            reminderCard(
                icon = "✅",
                title = "Test Reminder",
                description = "Sends a test notification after 10 seconds.",
                statusKey = "test_enabled",
                onEnable = { updateUi ->
                    requestPermissionThenRun {
                        scheduleOneTimeReminder(
                            requestCode = 4100,
                            notificationId = 4100,
                            delayMillis = 10 * 1000L,
                            title = "Test Reminder",
                            message = "Your Wellness Guide reminders are working.",
                            statusKey = "test_enabled",
                            successMessage = "Test reminder set. Wait 10 seconds."
                        )
                        updateUi()
                    }
                },
                onDisable = { updateUi ->
                    cancelReminder(
                        requestCode = 4100,
                        notificationId = 4100,
                        statusKey = "test_enabled",
                        successMessage = "Test reminder turned off."
                    )
                    updateUi()
                }
            )
        )

        root.addView(
            reminderCard(
                icon = "💧",
                title = "Water Reminder",
                description = "Reminds you to drink water every 2 hours.",
                statusKey = "water_enabled",
                onEnable = { updateUi ->
                    requestPermissionThenRun {
                        scheduleIntervalReminder(
                            requestCode = 4101,
                            notificationId = 4101,
                            title = "Water Reminder",
                            message = "Drink a glass of water. Staying hydrated supports energy and recovery.",
                            initialDelayMillis = 2 * 60 * 60 * 1000L,
                            intervalMillis = 2 * 60 * 60 * 1000L,
                            statusKey = "water_enabled",
                            successMessage = "Water reminder turned on."
                        )
                        updateUi()
                    }
                },
                onDisable = { updateUi ->
                    cancelReminder(
                        requestCode = 4101,
                        notificationId = 4101,
                        statusKey = "water_enabled",
                        successMessage = "Water reminder turned off."
                    )
                    updateUi()
                }
            )
        )

        root.addView(
            reminderCard(
                icon = "👁",
                title = "Rest Eyes Reminder",
                description = "Reminds you to rest your eyes every 20 minutes.",
                statusKey = "eyes_enabled",
                onEnable = { updateUi ->
                    requestPermissionThenRun {
                        scheduleIntervalReminder(
                            requestCode = 4102,
                            notificationId = 4102,
                            title = "Rest Your Eyes",
                            message = "Try the 20-20-20 rule: look 20 feet away for 20 seconds.",
                            initialDelayMillis = 20 * 60 * 1000L,
                            intervalMillis = 20 * 60 * 1000L,
                            statusKey = "eyes_enabled",
                            successMessage = "Rest eyes reminder turned on."
                        )
                        updateUi()
                    }
                },
                onDisable = { updateUi ->
                    cancelReminder(
                        requestCode = 4102,
                        notificationId = 4102,
                        statusKey = "eyes_enabled",
                        successMessage = "Rest eyes reminder turned off."
                    )
                    updateUi()
                }
            )
        )

        root.addView(
            reminderCard(
                icon = "📋",
                title = "Daily Check-In Reminder",
                description = "Reminds you every day at 8:00 PM to check your wellness.",
                statusKey = "daily_enabled",
                onEnable = { updateUi ->
                    requestPermissionThenRun {
                        scheduleDailyReminder(
                            requestCode = 4103,
                            notificationId = 4103,
                            hour = 20,
                            minute = 0,
                            title = "Daily Wellness Check-In",
                            message = "How are you feeling today? Log your mood, sleep, water, stress, and symptoms.",
                            statusKey = "daily_enabled",
                            successMessage = "Daily check-in reminder turned on for 8:00 PM."
                        )
                        updateUi()
                    }
                },
                onDisable = { updateUi ->
                    cancelReminder(
                        requestCode = 4103,
                        notificationId = 4103,
                        statusKey = "daily_enabled",
                        successMessage = "Daily check-in reminder turned off."
                    )
                    updateUi()
                }
            )
        )

        root.addView(
            reminderCard(
                icon = "📝",
                title = "Symptom Follow-Up",
                description = "Reminds you after 6 hours to check if your symptoms improved or worsened.",
                statusKey = "followup_enabled",
                onEnable = { updateUi ->
                    requestPermissionThenRun {
                        scheduleOneTimeReminder(
                            requestCode = 4104,
                            notificationId = 4104,
                            delayMillis = 6 * 60 * 60 * 1000L,
                            title = "Symptom Follow-Up",
                            message = "How are your symptoms now? Check if they improved, stayed the same, or worsened.",
                            statusKey = "followup_enabled",
                            successMessage = "Symptom follow-up reminder set for 6 hours from now."
                        )
                        updateUi()
                    }
                },
                onDisable = { updateUi ->
                    cancelReminder(
                        requestCode = 4104,
                        notificationId = 4104,
                        statusKey = "followup_enabled",
                        successMessage = "Symptom follow-up reminder turned off."
                    )
                    updateUi()
                }
            )
        )

        scroll.addView(root)
        return scroll
    }

    private fun addTopBar(root: LinearLayout) {
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            )
        }

        val menu = TextView(requireContext()).apply {
            text = "☰"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(textPrimary)
            background = roundedBg(surfaceColor, borderColor, 2, 40f)
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            setOnClickListener {
                (requireActivity() as MainActivity).openDrawer()
            }
        }

        val title = TextView(requireContext()).apply {
            text = "Reminders"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                leftMargin = dp(12)
            }
        }

        header.addView(menu)
        header.addView(title)
        root.addView(header)
    }

    private fun reminderCard(
        icon: String,
        title: String,
        description: String,
        statusKey: String,
        onEnable: (updateUi: () -> Unit) -> Unit,
        onDisable: (updateUi: () -> Unit) -> Unit
    ): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(18))
            background = roundedBg(surfaceColor, borderColor, 2, 26f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
            }
        }

        val topRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val iconView = TextView(requireContext()).apply {
            text = icon
            textSize = 24f
            gravity = Gravity.CENTER
            background = roundedBg(backgroundColor, Color.rgb(205, 232, 228), 1, 40f)
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
        }

        val titleColumn = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                leftMargin = dp(12)
            }
        }

        val titleView = TextView(requireContext()).apply {
            text = title
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
        }

        val descView = TextView(requireContext()).apply {
            text = description
            textSize = 13f
            setTextColor(textSecondary)
            setLineSpacing(4f, 1f)
            setPadding(0, dp(4), 0, 0)
        }

        titleColumn.addView(titleView)
        titleColumn.addView(descView)

        val statusBadge = TextView(requireContext())
        updateStatusBadge(statusBadge, statusKey)

        topRow.addView(iconView)
        topRow.addView(titleColumn)
        topRow.addView(statusBadge)

        val buttonRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(16), 0, 0)
        }

        val enableButton = smallButton("Turn On", true).apply {
            setOnClickListener {
                onEnable {
                    updateStatusBadge(statusBadge, statusKey)
                }
            }
        }

        val disableButton = smallButton("Turn Off", false).apply {
            setOnClickListener {
                onDisable {
                    updateStatusBadge(statusBadge, statusKey)
                }
            }
        }

        buttonRow.addView(enableButton)
        buttonRow.addView(disableButton)

        card.addView(topRow)
        card.addView(buttonRow)

        return card
    }

    private fun updateStatusBadge(
        badge: TextView,
        statusKey: String
    ) {
        val enabled = isReminderEnabled(statusKey)

        badge.text = if (enabled) "On" else "Off"
        badge.textSize = 11f
        badge.setTypeface(null, Typeface.BOLD)
        badge.setTextColor(
            if (enabled) Color.rgb(5, 150, 105) else textSecondary
        )
        badge.gravity = Gravity.CENTER
        badge.setPadding(dp(12), dp(6), dp(12), dp(6))
        badge.background = roundedBg(
            if (enabled) Color.rgb(236, 253, 245) else Color.rgb(243, 244, 246),
            if (enabled) Color.rgb(5, 150, 105) else Color.rgb(209, 213, 219),
            1,
            30f
        )
    }

    private fun smallButton(textValue: String, primary: Boolean): Button {
        return Button(requireContext()).apply {
            text = textValue
            textSize = 13f
            isAllCaps = false
            setTextColor(if (primary) Color.WHITE else textPrimary)
            background = if (primary) {
                roundedBg(primaryColor, primaryColor, 2, 20f)
            } else {
                roundedBg(Color.WHITE, borderColor, 2, 20f)
            }

            layoutParams = LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            ).apply {
                rightMargin = dp(6)
                leftMargin = dp(6)
            }
        }
    }

    private fun requestPermissionThenRun(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                action()
            } else {
                pendingPermissionAction = action
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            action()
        }
    }

    private fun scheduleIntervalReminder(
        requestCode: Int,
        notificationId: Int,
        title: String,
        message: String,
        initialDelayMillis: Long,
        intervalMillis: Long,
        statusKey: String,
        successMessage: String
    ) {
        val alarmManager =
            requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val pendingIntent = reminderPendingIntent(
            requestCode = requestCode,
            notificationId = notificationId,
            title = title,
            message = message,
            ongoing = true,
            oneTime = false,
            statusKey = statusKey
        )

        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + initialDelayMillis,
            intervalMillis,
            pendingIntent
        )

        setReminderEnabled(statusKey, true)
        RecentActivityStore.add(
            requireContext(),
            "$title turned on",
            successMessage,
            reminderTypeFromTitle(title)
        )
        showActiveReminderNotification(
            notificationId = notificationId,
            title = "$title is On",
            message = message
        )

        Toast.makeText(
            requireContext(),
            successMessage,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun scheduleDailyReminder(
        requestCode: Int,
        notificationId: Int,
        hour: Int,
        minute: Int,
        title: String,
        message: String,
        statusKey: String,
        successMessage: String
    ) {
        val alarmManager =
            requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        val pendingIntent = reminderPendingIntent(
            requestCode = requestCode,
            notificationId = notificationId,
            title = title,
            message = message,
            ongoing = true,
            oneTime = false,
            statusKey = statusKey
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        setReminderEnabled(statusKey, true)
        RecentActivityStore.add(
            requireContext(),
            "$title turned on",
            successMessage,
            reminderTypeFromTitle(title)
        )
        showActiveReminderNotification(
            notificationId = notificationId,
            title = "$title is On",
            message = message
        )

        Toast.makeText(
            requireContext(),
            successMessage,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun scheduleOneTimeReminder(
        requestCode: Int,
        notificationId: Int,
        delayMillis: Long,
        title: String,
        message: String,
        statusKey: String,
        successMessage: String
    ) {
        val alarmManager =
            requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val pendingIntent = reminderPendingIntent(
            requestCode = requestCode,
            notificationId = notificationId,
            title = title,
            message = message,
            ongoing = false,
            oneTime = true,
            statusKey = statusKey
        )

        val triggerAtMillis = SystemClock.elapsedRealtime() + delayMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }

        setReminderEnabled(statusKey, true)

        RecentActivityStore.add(
            requireContext(),
            "$title scheduled",
            successMessage,
            reminderTypeFromTitle(title)
        )

        Toast.makeText(
            requireContext(),
            successMessage,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun cancelReminder(
        requestCode: Int,
        notificationId: Int,
        statusKey: String,
        successMessage: String
    ) {
        val alarmManager =
            requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(requireContext(), ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.cancel(notificationId)

        setReminderEnabled(statusKey, false)
        RecentActivityStore.add(
            requireContext(),
            "Reminder turned off",
            successMessage,
            "general"
        )
        Toast.makeText(
            requireContext(),
            successMessage,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun reminderPendingIntent(
        requestCode: Int,
        notificationId: Int,
        title: String,
        message: String,
        ongoing: Boolean,
        oneTime: Boolean,
        statusKey: String
    ): PendingIntent {
        val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("notificationId", notificationId)
            putExtra("ongoing", ongoing)
            putExtra("oneTime", oneTime)
            putExtra("statusKey", statusKey)
            putExtra("type", reminderTypeFromTitle(title))
        }

        return PendingIntent.getBroadcast(
            requireContext(),
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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

    private fun showActiveReminderNotification(
        notificationId: Int,
        title: String,
        message: String
    ) {
        createNotificationChannel()

        val openAppIntent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.ic_activity)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

    private fun isReminderEnabled(key: String): Boolean {
        return requireContext()
            .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getBoolean(key, false)
    }

    private fun setReminderEnabled(key: String, enabled: Boolean) {
        requireContext()
            .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, enabled)
            .apply()
    }

    private fun bigTitle(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            setPadding(0, dp(18), 0, dp(4))
        }
    }

    private fun paragraph(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 15f
            setTextColor(textSecondary)
            setLineSpacing(5f, 1f)
            setPadding(0, dp(8), 0, dp(8))
        }
    }

    private fun noteCard(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.rgb(145, 75, 0))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBg(
                Color.rgb(255, 250, 220),
                Color.rgb(245, 185, 65),
                2,
                20f
            )
        }
    }

    private fun roundedBg(
        bgColor: Int,
        strokeColor: Int,
        strokeWidth: Int,
        radius: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(bgColor)
            setStroke(strokeWidth, strokeColor)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}