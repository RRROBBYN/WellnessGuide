package com.example.wellnessguide.notifications

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReminderNotificationStore {

    data class AppNotification(
        val title: String,
        val message: String,
        val timestamp: Long,
        val type: String
    )

    private const val PREFS_NAME = "wellness_app_notifications"
    private const val KEY_NOTIFICATIONS = "notifications"

    fun add(
        context: Context,
        title: String,
        message: String,
        type: String = "reminder"
    ) {
        val current = getAll(context).toMutableList()

        current.add(
            0,
            AppNotification(
                title = title,
                message = message,
                timestamp = System.currentTimeMillis(),
                type = type
            )
        )

        val limited = current.take(30)

        val array = JSONArray()

        limited.forEach { item ->
            val obj = JSONObject()
            obj.put("title", item.title)
            obj.put("message", item.message)
            obj.put("timestamp", item.timestamp)
            obj.put("type", item.type)
            array.put(obj)
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NOTIFICATIONS, array.toString())
            .apply()
    }

    fun getAll(context: Context): List<AppNotification> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NOTIFICATIONS, "[]") ?: "[]"

        val result = mutableListOf<AppNotification>()

        return try {
            val array = JSONArray(raw)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                result.add(
                    AppNotification(
                        title = obj.optString("title", "Wellness Reminder"),
                        message = obj.optString("message", "Take a moment to check your wellness."),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        type = obj.optString("type", "reminder")
                    )
                )
            }

            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_NOTIFICATIONS)
            .apply()
    }
}