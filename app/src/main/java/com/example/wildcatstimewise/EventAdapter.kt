package com.example.wildcatstimewise

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.BaseAdapter

class EventAdapter(private val context: Context, private val events: List<Event>) : BaseAdapter() {

    override fun getCount(): Int = events.size
    override fun getItem(position: Int): Any = events[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.event_item, parent, false)

        val tvDate = view.findViewById<TextView>(R.id.tv_event_date)
        val tvName = view.findViewById<TextView>(R.id.tv_event_name)

        val event = events[position]

        tvDate.text = event.date
        tvName.text = event.name

        return view
    }
}