package com.example.wildcatstimewise

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class EventAdapter(
    private val eventClickListener: OnEventClickListener
) : ListAdapter<EventInfo, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    interface OnEventClickListener {
        fun onEventLongClick(eventInfo: EventInfo)
        fun onEventDeleteClick(eventInfo: EventInfo)
        fun onEventItemClick(eventInfo: EventInfo, position: Int)
    }

    private var highlightedItemPosition = RecyclerView.NO_POSITION
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    private var currentlySelectedDateForHighlighting: LocalDate? = null

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView as MaterialCardView
        val eventNameTextView: TextView = itemView.findViewById(R.id.eventNameTextView)
        val eventTypeTextView: TextView = itemView.findViewById(R.id.eventTypeTextView)
        val eventDateTextView: TextView = itemView.findViewById(R.id.eventDateTextView)
        val eventTimeTextView: TextView = itemView.findViewById(R.id.eventTimeTextView)

        companion object {
            fun create(parent: ViewGroup): EventViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_event, parent, false)
                return EventViewHolder(view)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        return EventViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val currentEvent = getItem(position)
        val context = holder.itemView.context

        holder.eventNameTextView.text = currentEvent.name
        holder.eventTypeTextView.text = currentEvent.type
        try {
            holder.eventDateTextView.text = currentEvent.date.format(dateFormatter); holder.eventDateTextView.visibility = View.VISIBLE
        } catch (e: Exception) { Log.e("EventAdapter", "Date format error", e); holder.eventDateTextView.text = currentEvent.date.toString(); holder.eventDateTextView.visibility = View.VISIBLE }

        if (currentEvent.time != null) {
            try { holder.eventTimeTextView.text = currentEvent.time.format(timeFormatter); holder.eventTimeTextView.visibility = View.VISIBLE }
            catch (e: Exception) { Log.e("EventAdapter", "Time format error", e); holder.eventTimeTextView.text = "--:--"; holder.eventTimeTextView.visibility = View.VISIBLE }
        } else { holder.eventTimeTextView.visibility = View.GONE }

        val defaultCardColor = ContextCompat.getColor(context, R.color.fade_white)
        val highlightColor = ContextCompat.getColor(context, R.color.light_yellow_highlight)

        if (position == highlightedItemPosition) {
            holder.cardView.setCardBackgroundColor(highlightColor)
        } else {
            holder.cardView.setCardBackgroundColor(defaultCardColor)
        }

        if (currentEvent.isUserEvent) {
            holder.itemView.setOnLongClickListener {
                eventClickListener.onEventLongClick(currentEvent)
                true
            }
        } else {
            holder.itemView.setOnLongClickListener(null)
        }

        holder.itemView.setOnClickListener {
            eventClickListener.onEventItemClick(currentEvent, position)
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<EventInfo>() {
        override fun areItemsTheSame(oldItem: EventInfo, newItem: EventInfo): Boolean {
            return if (oldItem.isUserEvent && newItem.isUserEvent) oldItem.id == newItem.id
            else oldItem.name == newItem.name && oldItem.date == newItem.date && oldItem.type == newItem.type
        }
        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: EventInfo, newItem: EventInfo): Boolean {
            return oldItem == newItem
        }
    }

    fun setHighlightForDate(selectedDate: LocalDate?) {
        val oldPos = highlightedItemPosition
        currentlySelectedDateForHighlighting = selectedDate
        highlightedItemPosition = if (selectedDate != null) {
            currentList.indexOfFirst { it.date == selectedDate }
        } else {
            RecyclerView.NO_POSITION
        }

        if (oldPos != highlightedItemPosition) {
            if (oldPos != RecyclerView.NO_POSITION && oldPos < itemCount) {
                try { notifyItemChanged(oldPos) } catch (e: Exception) { Log.e("EventAdapter","Err notify old highlight", e) }
            }
            if (highlightedItemPosition != RecyclerView.NO_POSITION && highlightedItemPosition < itemCount) {
                try { notifyItemChanged(highlightedItemPosition) } catch (e: Exception) { Log.e("EventAdapter","Err notify new highlight", e) }
            }
        }
    }

    fun toggleHighlight(position: Int) {
        val oldPos = highlightedItemPosition
        if (highlightedItemPosition == position) {
            highlightedItemPosition = RecyclerView.NO_POSITION
        } else {
            highlightedItemPosition = position
        }

        if (oldPos != RecyclerView.NO_POSITION && oldPos < itemCount) {
            try { notifyItemChanged(oldPos) } catch (e: Exception) { Log.e("EventAdapter","Err notify old toggle", e) }
        }
        if (highlightedItemPosition != RecyclerView.NO_POSITION && highlightedItemPosition < itemCount) {
            try { notifyItemChanged(highlightedItemPosition) } catch (e: Exception) { Log.e("EventAdapter","Err notify new toggle", e) }
        }
    }

    fun clearHighlight() {
        val oldPos = highlightedItemPosition
        highlightedItemPosition = RecyclerView.NO_POSITION
        if (oldPos != RecyclerView.NO_POSITION && oldPos < itemCount) {
            try { notifyItemChanged(oldPos) } catch (e: Exception) { Log.e("EventAdapter","Err clearing highlight", e) }
        }
    }
}