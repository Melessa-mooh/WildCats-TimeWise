package com.example.wildcatstimewise

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.*

class StudentsActivity : Activity() {
    private lateinit var tvTime: TextView
    private lateinit var adapter: ArrayAdapter<String>
    private val handler = Handler(Looper.getMainLooper())
    private val eventsList = mutableListOf(
        "Aug 6   Cebu Province Founding Anniversary",
        "Aug 12  Classes Begin",
        "Aug 21  Ninoy Aquino Day",
        "Aug 28  Colors Day",
        "Sep 2   National Heroes Day",
        "Sep 23  Pres. Sergio Osmeña Day",
        "Sep 25  Acquaintance Day",
        "Nov 1   All Saints Day (Non-Working)",
        "Nov 2   All Souls Day",
        "Dec 6   Founder’s Day",
        "Dec 8   Feast of the Immaculate Conception",
        "Dec 13  Classes End",
        "Dec 21  Commencement Rites",
        "Dec 24  Special Non-Working Day",
        "Dec 25  Christmas Day",
        "Dec 30  Rizal Day",
        "Dec 31  Special Non-Working Day",
        "Jan 1   New Year’s Day",
        "Jan 8   Classes Begin",
        "Jan 18  Feast of Sr. Niño",
        "Feb 24  Cebu Charter Day",
        "Feb 26  EDSA Revolution Anniversary",
        "Mar 4-5 University Days",
        "Apr 17  Araw ng Kagitingan",
        "Apr 17  Holy Thursday",
        "Apr 18  Good Friday",
        "Apr 19  Black Saturday",
        "May 1   Labor Day",
        "May 12  Parangal",
        "May 17  Commencement Rites",
        "May 26  Mid-Year Classes Begin",
        "Jun 17  Eid’l Adha (Feast of Sacrifice)",
        "Jun 19  Independence Day",
        "Jul 1   University Conferral Day",
        "Jul 12  Mid-Year Classes End"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_students)

        tvTime = findViewById(R.id.tv_time)
        val btnDashboard = findViewById<Button>(R.id.btn_dashboard)
        val btnAddEvent = findViewById<Button>(R.id.btn_add_event)
        val etNewEvent = findViewById<EditText>(R.id.et_new_event)
        val listViewEvents = findViewById<ListView>(R.id.listView_events)
        val profileImage = findViewById<ImageView>(R.id.profile_image)


        // Function to update time every second
        val updateTime = object : Runnable {
            override fun run() {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                tvTime.text = sdf.format(Date())
                handler.postDelayed(this, 1000) // Update every second
            }
        }
        handler.post(updateTime)

        // Adapter for ListView
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, eventsList)
        listViewEvents.adapter = adapter

        // Set ListView height dynamically (only show 5 items initially)
        setListViewHeight(listViewEvents, 5)

        // Handle Dashboard Button Click
        btnDashboard.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Handle Profile Image Click (Go to Profile Activity)
        profileImage.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)

            // Convert the event list to an ArrayList
            val eventsArray = ArrayList(eventsList)


            // Add the events to the intent as a String ArrayList but we can only call 8 here
            intent.putStringArrayListExtra("events", ArrayList(eventsList.take(8)))
            startActivity(intent)
        }
        // Handle Add Event Button Click
        btnAddEvent.setOnClickListener {
            val newEvent = etNewEvent.text.toString().trim()
            if (newEvent.isNotEmpty()) {
                eventsList.add(newEvent) // Add new event to list
                adapter.notifyDataSetChanged() // Update ListView
                etNewEvent.text.clear() // Clear input field
                setListViewHeight(listViewEvents, 5) // Adjust height dynamically
            } else {
                Toast.makeText(this, "Please enter an event!", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle Item Long Press (Delete Event)
        listViewEvents.setOnItemLongClickListener { _, _, position, _ ->
            val eventToDelete = eventsList[position]

            // Show confirmation dialog before deleting
            AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete '$eventToDelete'?")
                .setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                    eventsList.removeAt(position) // Remove item from list
                    adapter.notifyDataSetChanged() // Update ListView
                    setListViewHeight(listViewEvents, 5) // Adjust height
                    Toast.makeText(this, "Event deleted!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()

            true // Return true to indicate long-click was handled
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Stop updates to prevent memory leaks
    }

    // Function to set ListView height dynamically
    private fun setListViewHeight(listView: ListView, itemsToShow: Int) {
        val adapter = listView.adapter ?: return
        var totalHeight = 0

        for (i in 0 until minOf(itemsToShow, adapter.count)) {
            val item = adapter.getView(i, null, listView)
            item.measure(0, 0)
            totalHeight += item.measuredHeight
        }

        val params = listView.layoutParams
        params.height = totalHeight + (listView.dividerHeight * (itemsToShow - 1))
        listView.layoutParams = params
        listView.requestLayout()

    }
}