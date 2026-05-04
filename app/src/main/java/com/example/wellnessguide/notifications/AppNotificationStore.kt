package com.example.wellnessguide.notifications

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AppNotificationItem(
    val id: Long,
    val title: String,
    val message: String,
    val type: String,
    val createdAt: Long,
    val isRead: Boolean
)

object AppNotificationStore {

    private const val PREF_NAME = "app_notification_store"
    private const val KEY_NOTIFICATIONS = "notifications"

    fun add(
        context: Context,
        title: String,
        message: String,
        type: String = "general"
    ) {
        val current = getAll(context).toMutableList()

        val item = AppNotificationItem(
            id = System.currentTimeMillis(),
            title = title,
            message = message,
            type = type,
            createdAt = System.currentTimeMillis(),
            isRead = false
        )

        current.add(0, item)

        saveAll(context, current.take(30))
    }

    fun getAll(context: Context): List<AppNotificationItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_NOTIFICATIONS, "[]") ?: "[]"

        return try {
            val array = JSONArray(raw)
            val items = mutableListOf<AppNotificationItem>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                items.add(
                    AppNotificationItem(
                        id = obj.optLong("id"),
                        title = obj.optString("title"),
                        message = obj.optString("message"),
                        type = obj.optString("type"),
                        createdAt = obj.optLong("createdAt"),
                        isRead = obj.optBoolean("isRead")
                    )
                )
            }

            items.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun markAllRead(context: Context) {
        val updated = getAll(context).map {
            it.copy(isRead = true)
        }

        saveAll(context, updated)
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        prefs.edit()
            .putString(KEY_NOTIFICATIONS, "[]")
            .apply()
    }

    private fun saveAll(
        context: Context,
        items: List<AppNotificationItem>
    ) {
        val array = JSONArray()

        items.forEach { item ->
            val obj = JSONObject()

            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("message", item.message)
            obj.put("type", item.type)
            obj.put("createdAt", item.createdAt)
            obj.put("isRead", item.isRead)

            array.put(obj)
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        prefs.edit()
            .putString(KEY_NOTIFICATIONS, array.toString())
            .apply()
    }
}