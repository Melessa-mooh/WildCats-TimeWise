// File: NotificationReceiver.kt
package com.example.wildcatstimewise

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
        const val ACTION_SHOW_REMINDER_NOTIFICATION = "com.example.wildcatstimewise.action.SHOW_REMINDER_NOTIFICATION"
        const val ACTION_EVENT_START_NOTIFICATION = "com.example.wildcatstimewise.action.EVENT_START_NOTIFICATION"

        const val EXTRA_TITLE = "notification_title"
        const val EXTRA_MESSAGE = "notification_message"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val CHANNEL_ID = "EVENT_REMINDERS_CHANNEL"
        const val CHANNEL_NAME = "Event Reminders & Alerts"
        const val CHANNEL_DESC = "Notifications for scheduled event reminders and start times"

        private const val PREFS_NAME = "NotificationHistoryPrefs"
        private const val KEY_HISTORY_LIST = "notification_history"
        private const val MAX_HISTORY_ITEMS = 50
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent with action: ${intent.action}")

        val action = intent.action
        val titleFromIntent = intent.getStringExtra(EXTRA_TITLE) ?: "Event Alert"
        val messageFromIntent = intent.getStringExtra(EXTRA_MESSAGE) ?: "Check your event details."
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, System.currentTimeMillis().toInt())

        var finalNotificationTitle = titleFromIntent
        var finalNotificationMessage = messageFromIntent

        when (action) {
            ACTION_SHOW_REMINDER_NOTIFICATION -> {
                Log.d(TAG, "Processing REMINDER: $finalNotificationTitle")
            }
            ACTION_EVENT_START_NOTIFICATION -> {
                finalNotificationTitle = "$titleFromIntent is starting now!"
                finalNotificationMessage = messageFromIntent.ifEmpty { "Your event '$titleFromIntent' is beginning." }
                Log.d(TAG, "Processing EVENT START: $finalNotificationTitle")
            }
            else -> {
                Log.w(TAG, "Received intent with unknown or unhandled action: $action")
                return
            }
        }

        createNotificationChannel(context)

        val tapIntent = Intent(context, DashboardPage::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingTapIntentRequestCode = notificationId + (action?.hashCode() ?: 0)
        val pendingTapIntent: PendingIntent = PendingIntent.getActivity(
            context,
            pendingTapIntentRequestCode,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(finalNotificationTitle)
            .setContentText(finalNotificationMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingTapIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(NotificationCompat.BigTextStyle().bigText(finalNotificationMessage))
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show notification ID: $notificationId.")
                return
            }
        }

        try {
            notificationManager.notify(notificationId, builder.build())
            Log.i(TAG, "Notification shown successfully with ID: $notificationId, Action: $action")
            saveNotificationToHistory(context, finalNotificationTitle, finalNotificationMessage)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException showing notification (ID: $notificationId): ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification (ID: $notificationId): ${e.message}")
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManagerSys: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManagerSys.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel '$CHANNEL_ID' created or verified.")
        }
    }

    private fun saveNotificationToHistory(context: Context, title: String, message: String) {
        try {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = sharedPreferences.getString(KEY_HISTORY_LIST, null)
            val currentHistory: MutableList<NotificationHistoryItem> = if (jsonString != null) {
                try {
                    Json.decodeFromString(
                        ListSerializer(NotificationHistoryItem.serializer()),
                        jsonString
                    ).toMutableList()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deserializing history, starting fresh.", e)
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }

            val newItem = NotificationHistoryItem(
                title = title,
                message = message,
                timestamp = System.currentTimeMillis()
            )

            currentHistory.add(0, newItem)

            while (currentHistory.size > MAX_HISTORY_ITEMS) {
                currentHistory.removeAt(currentHistory.size - 1)
            }

            val updatedJsonString = Json.encodeToString(
                ListSerializer(NotificationHistoryItem.serializer()),
                currentHistory
            )

            val success = sharedPreferences.edit().putString(KEY_HISTORY_LIST, updatedJsonString).commit()
            if (!success) {
                Log.e(TAG, "Failed to commit notification history to SharedPreferences!")
            } else {
                Log.d(TAG, "Successfully committed notification to history. New size: ${currentHistory.size}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save notification history", e)
        }
    }
}