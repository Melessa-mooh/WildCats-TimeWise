package com.example.wildcatstimewise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log

class NotificationHistoryAdapter : ListAdapter<NotificationHistoryItem, NotificationHistoryAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    // Corrected pattern: Ensure no illegal characters like 't' are here.
    private val timestampFormatter = SimpleDateFormat("MMM d, yyyy hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_notification_history, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, timestampFormatter)
    }

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView
        private val messageTextView: TextView
        private val timestampTextView: TextView

        init {
            titleTextView = itemView.findViewById(R.id.notificationTitleTextView)
            messageTextView = itemView.findViewById(R.id.notificationMessageTextView)
            timestampTextView = itemView.findViewById(R.id.notificationTimestampTextView)
        }

        fun bind(item: NotificationHistoryItem, formatter: SimpleDateFormat) {
            titleTextView.text = item.title
            messageTextView.text = item.message
            try {
                timestampTextView.text = formatter.format(Date(item.timestamp))
            } catch (e: Exception) {
                Log.e("NotificationViewHolder", "Error formatting timestamp: ${item.timestamp}", e)
                try {
                    timestampTextView.text = itemView.context.getString(R.string.invalid_date_fallback)
                } catch (resEx: Exception) {
                    timestampTextView.text = "Invalid Date"
                }
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationHistoryItem>() {
        override fun areItemsTheSame(oldItem: NotificationHistoryItem, newItem: NotificationHistoryItem): Boolean {
            return oldItem.timestamp == newItem.timestamp && oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: NotificationHistoryItem, newItem: NotificationHistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}