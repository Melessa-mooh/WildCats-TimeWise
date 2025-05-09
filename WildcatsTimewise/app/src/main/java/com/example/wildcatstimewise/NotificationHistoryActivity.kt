package com.example.wildcatstimewise

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NotificationHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationHistoryAdapter
    private lateinit var emptyTextView: TextView
    private lateinit var clearAllHistoryButton: ImageView
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val TAG = "NotificationHistoryAct"
        const val PREFS_NAME = "NotificationHistoryPrefs"
        const val KEY_HISTORY_LIST = "notification_history"
        private const val MAX_HISTORY_ITEMS = 50
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_history)

        val toolbar: Toolbar = findViewById(R.id.toolbar_notification_history)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        recyclerView = findViewById(R.id.notificationHistoryRecyclerView)
        emptyTextView = findViewById(R.id.emptyHistoryTextView)
        clearAllHistoryButton = findViewById(R.id.clearAllHistoryButton)
        adapter = NotificationHistoryAdapter()
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setupRecyclerView()
        setupListeners()
        loadNotificationHistory()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val swipeHandler = SwipeToDeleteCallback(this, adapter) { notificationItem, position ->
            showDeleteConfirmationDialog(notificationItem, position)
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun setupListeners() {
        clearAllHistoryButton.setOnClickListener {

            val jsonString = sharedPreferences.getString(KEY_HISTORY_LIST, null)
            val isEmpty = if (jsonString != null) {
                try {
                    Json.decodeFromString(ListSerializer(NotificationHistoryItem.serializer()), jsonString).isEmpty()
                } catch (e: Exception) {
                    true
                }
            } else {
                true
            }

            if (isEmpty) {
                Toast.makeText(this, "Notification history is already empty.", Toast.LENGTH_SHORT).show()
            } else {
                showClearAllConfirmationDialog()
            }
        }
    }

    private fun showClearAllConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear History?")
            .setMessage("Are you sure you want to delete all notification history? This cannot be undone.")
            .setPositiveButton("Clear All") { dialog, _ ->
                clearNotificationHistory()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun clearNotificationHistory() {
        try {
            sharedPreferences.edit().remove(KEY_HISTORY_LIST).apply()
            Toast.makeText(this, "Notification history cleared.", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Notification history cleared from SharedPreferences.")
            loadNotificationHistory()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear notification history", e)
            Toast.makeText(this, "Error clearing history.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadNotificationHistory() {
        Log.d(TAG, "Loading notification history...")
        val jsonString = sharedPreferences.getString(KEY_HISTORY_LIST, null)

        val historyList: List<NotificationHistoryItem> = if (jsonString != null) {
            try {
                Json.decodeFromString(
                    ListSerializer(NotificationHistoryItem.serializer()),
                    jsonString
                )
                    .also { Log.d(TAG, "Loaded ${it.size} items from SharedPreferences.") }
            } catch (e: SerializationException) {
                Log.e(TAG, "Error deserializing notification history", e)
                emptyList<NotificationHistoryItem>()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error loading notification history", e)
                emptyList<NotificationHistoryItem>()
            }
        } else {
            Log.d(TAG, "No notification history found in SharedPreferences.")
            emptyList<NotificationHistoryItem>()
        }

        val sortedList = historyList.sortedByDescending { it.timestamp }

        if (sortedList.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyTextView.visibility = View.VISIBLE
            Log.d(TAG, "Displaying empty history message.")
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyTextView.visibility = View.GONE
            Log.d(TAG, "Submitted ${sortedList.size} items to adapter.")
        }
        adapter.submitList(sortedList)
    }

    private fun showDeleteConfirmationDialog(itemToDelete: NotificationHistoryItem, position: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Notification")
            .setMessage("Are you sure you want to delete this notification?\n\"${itemToDelete.title}\"")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteNotificationItem(itemToDelete)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                try {
                    if (position >= 0 && position < adapter.itemCount) {
                        adapter.notifyItemChanged(position)
                    }
                } catch (e: Exception) { Log.e(TAG, "Error notifying item change on cancel", e) }
                dialog.dismiss()
            }
            .setOnDismissListener {
                if (::adapter.isInitialized) {
                    try {
                        if (position >= 0 && position < adapter.itemCount) {
                            adapter.notifyItemChanged(position)
                        }
                    } catch (e: Exception) { Log.e(TAG, "Error notifying item change on dismiss", e) }
                }
            }
            .setOnCancelListener {
                if (::adapter.isInitialized) {
                    try {
                        if (position >= 0 && position < adapter.itemCount) {
                            adapter.notifyItemChanged(position)
                        }
                    } catch (e: Exception) { Log.e(TAG, "Error notifying item change on cancel listener", e) }
                }
            }
            .show()
    }


    private fun deleteNotificationItem(itemToDelete: NotificationHistoryItem) {
        try {
            val jsonString = sharedPreferences.getString(KEY_HISTORY_LIST, null)
            val currentHistory: MutableList<NotificationHistoryItem> = if (jsonString != null) {
                try {
                    Json.decodeFromString(
                        ListSerializer(NotificationHistoryItem.serializer()),
                        jsonString
                    ).toMutableList()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deserializing history for delete, starting fresh.", e)
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }

            val removed = currentHistory.removeIf { it.timestamp == itemToDelete.timestamp && it.title == itemToDelete.title && it.message == itemToDelete.message }

            if (removed) {
                val updatedJsonString = Json.encodeToString(
                    ListSerializer(NotificationHistoryItem.serializer()),
                    currentHistory
                )
                sharedPreferences.edit().putString(KEY_HISTORY_LIST, updatedJsonString).apply()
                Log.d(TAG, "Deleted item. New history size: ${currentHistory.size}")
                Toast.makeText(this, "Notification removed.", Toast.LENGTH_SHORT).show()

                loadNotificationHistory()

            } else {
                Log.w(TAG, "Item to delete not found in current history list.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete notification history item", e)
            Toast.makeText(this, "Error removing notification.", Toast.LENGTH_SHORT).show()
        }
    }


    private inner class SwipeToDeleteCallback(
        val context: Context,
        val adapter: NotificationHistoryAdapter,
        val confirmAction: (NotificationHistoryItem, Int) -> Unit
    ) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

        private val deleteIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.delete_white)
        private val paint: Paint = Paint()
        private val startColor = Color.WHITE
        private val endColor = Color.parseColor("#DC143C")


        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                try {
                    val item = adapter.currentList[position]
                    confirmAction(item, position)
                } catch (e: IndexOutOfBoundsException) {
                    Log.e("SwipeToDeleteCallback", "Error getting item at position $position", e)
                    try { adapter.notifyItemChanged(position) } catch (notifyEx: Exception) { Log.e(TAG, "Error notifying item change after swipe error", notifyEx)}
                } catch (e: Exception) {
                    Log.e("SwipeToDeleteCallback", "Generic error during swipe action at $position", e)
                    try { adapter.notifyItemChanged(position) } catch (notifyEx: Exception) { Log.e(TAG, "Error notifying item change after generic swipe error", notifyEx)}
                }
            }
        }

        override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
            val itemViewWidth = viewHolder.itemView.width.toFloat()
            if (itemViewWidth <= 0) {
                return 0.5f
            }
            val density = context.resources.displayMetrics.density
            val swipeThresholdPx = 40 * density
            val thresholdFraction = swipeThresholdPx / itemViewWidth
            return thresholdFraction.coerceIn(0.1f, 1.0f)
        }

        override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return super.getSwipeDirs(recyclerView, viewHolder)
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val itemView = viewHolder.itemView
            val iconMargin = (itemView.height - (deleteIcon?.intrinsicHeight ?: 0)) / 2
            val iconTop = itemView.top + iconMargin
            val iconBottom = iconTop + (deleteIcon?.intrinsicHeight ?: 0)
            val iconLeft: Int
            val iconRight: Int

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                val density = context.resources.displayMetrics.density
                val swipeActionDistancePx = 40 * density
                val progress = (abs(dX) / swipeActionDistancePx).coerceIn(0f, 1f)

                val startA = Color.alpha(startColor)
                val startR = Color.red(startColor)
                val startG = Color.green(startColor)
                val startB = Color.blue(startColor)

                val endA = Color.alpha(endColor)
                val endR = Color.red(endColor)
                val endG = Color.green(endColor)
                val endB = Color.blue(endColor)

                val currentA = (startA + (endA - startA) * progress).toInt()
                val currentR = (startR + (endR - startR) * progress).toInt()
                val currentG = (startG + (endG - startG) * progress).toInt()
                val currentB = (startB + (endB - startB) * progress).toInt()

                paint.color = Color.argb(currentA, currentR, currentG, currentB)

                c.drawRect(
                    itemView.right + dX,
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat(),
                    paint
                )

                iconLeft = itemView.right - iconMargin - (deleteIcon?.intrinsicWidth ?: 0)
                iconRight = itemView.right - iconMargin
                deleteIcon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                deleteIcon?.draw(c)

            } else {
                deleteIcon?.setBounds(0, 0, 0, 0)
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }
}