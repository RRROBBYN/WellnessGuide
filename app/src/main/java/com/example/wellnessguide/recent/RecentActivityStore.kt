package com.example.wellnessguide.recent

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class RecentActivityItem(
    val id: Long,
    val title: String,
    val message: String,
    val type: String,
    val createdAt: Long
)

object RecentActivityStore {

    private const val PREF_NAME = "recent_activity_store"
    private const val KEY_ACTIVITIES = "activities"

    fun add(
        context: Context,
        title: String,
        message: String,
        type: String = "general"
    ) {
        val current = getAll(context).toMutableList()

        val item = RecentActivityItem(
            id = System.currentTimeMillis(),
            title = title,
            message = message,
            type = type,
            createdAt = System.currentTimeMillis()
        )

        current.add(0, item)

        saveAll(context, current.take(40))
    }

    fun getAll(context: Context): List<RecentActivityItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ACTIVITIES, "[]") ?: "[]"

        return try {
            val array = JSONArray(raw)
            val items = mutableListOf<RecentActivityItem>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                items.add(
                    RecentActivityItem(
                        id = obj.optLong("id"),
                        title = obj.optString("title"),
                        message = obj.optString("message"),
                        type = obj.optString("type"),
                        createdAt = obj.optLong("createdAt")
                    )
                )
            }

            items.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        prefs.edit()
            .putString(KEY_ACTIVITIES, "[]")
            .apply()
    }

    private fun saveAll(
        context: Context,
        items: List<RecentActivityItem>
    ) {
        val array = JSONArray()

        items.forEach { item ->
            val obj = JSONObject()

            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("message", item.message)
            obj.put("type", item.type)
            obj.put("createdAt", item.createdAt)

            array.put(obj)
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        prefs.edit()
            .putString(KEY_ACTIVITIES, array.toString())
            .apply()
    }
}