package com.example.wildcatstimewise

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

object NotificationHelper {

    const val CHANNEL_ID = "event_notifications_channel"
    const val CHANNEL_NAME = "Event Reminders"
    const val CHANNEL_DESCRIPTION = "Notifications for upcoming events"
    private const val TAG = "NotificationHelper"
    private const val MAX_HISTORY_ITEMS = 50


    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }

    fun saveNotificationToHistory(context: Context, title: String, message: String) {
        val sharedPreferences = context.getSharedPreferences(
            NotificationHistoryActivity.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        val newItem = NotificationHistoryItem(title, message, System.currentTimeMillis())
        val jsonString = sharedPreferences.getString(NotificationHistoryActivity.KEY_HISTORY_LIST, null)
        val currentHistory: MutableList<NotificationHistoryItem> = if (jsonString != null) {
            try {
                Json.decodeFromString(
                    ListSerializer(NotificationHistoryItem.serializer()),
                    jsonString
                ).toMutableList()
            } catch (e: Exception) {
                Log.e(TAG, "Error deserializing history for saving, starting fresh.", e)
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        currentHistory.add(0, newItem)

        val historyToSave = if (currentHistory.size > MAX_HISTORY_ITEMS) {
            currentHistory.subList(0, MAX_HISTORY_ITEMS)
        } else {
            currentHistory
        }

        try {
            val updatedJsonString = Json.encodeToString(
                ListSerializer(NotificationHistoryItem.serializer()),
                historyToSave
            )
            sharedPreferences.edit().putString(NotificationHistoryActivity.KEY_HISTORY_LIST, updatedJsonString).apply()
            Log.d(TAG, "Notification saved to history. Title: $title, New history size: ${historyToSave.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize and save notification history", e)
        }
    }
}