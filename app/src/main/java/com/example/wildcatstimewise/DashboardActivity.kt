package com.example.wildcatstimewise

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.navigation.NavigationView

class DashboardActivity : AppCompatActivity() {
    private lateinit var calendarView: CalendarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.itemBackground = ContextCompat.getDrawable(this, R.drawable.stroke)

        val tvCurrentDate = findViewById<TextView>(R.id.idTVDate)
        val tvHolidayList = findViewById<TextView>(R.id.tv_holiday_list)
        calendarView = findViewById(R.id.calendarView)

        val holidays = mapOf(
            "01" to listOf("Jan 1 – New Year's Day", "Jan 29 – Chinese New Year"),
            "04" to listOf(
                "Apr 1 – Eid'l Fitr (Feast of Ramadhan)",
                "Apr 9 – Day of Valor (Araw ng Kagitingan)",
                "Apr 17 – Maundy Thursday",
                "Apr 18 – Good Friday",
                "Apr 19 – Black Saturday"
            ),
            "05" to listOf("May 1 – Labor Day"),
            "06" to listOf("Jun 6 – Eid'l Adha (Feast of Sacrifice)", "Jun 12 – Independence Day"),
            "08" to listOf("Aug 21 – Ninoy Aquino Day", "Aug 25 – National Heroes Day"),
            "11" to listOf("Nov 1 – All Saints' Day", "Nov 30 – Bonifacio Day"),
            "12" to listOf(
                "Dec 8 – Feast of the Immaculate Conception",
                "Dec 24 – Christmas Eve",
                "Dec 25 – Christmas Day",
                "Dec 30 – Rizal Day",
                "Dec 31 – New Year's Eve"
            )
        )

        // Set holidays for the current month
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = String.format("%02d", calendar.get(java.util.Calendar.MONTH) + 1)
        tvHolidayList.text = holidays[currentMonth]?.joinToString("\n") ?: "No holidays this month."

        tvCurrentDate.text = getString(R.string.select_date)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "$month/$dayOfMonth/$year"
            tvCurrentDate.text = selectedDate
            Toast.makeText(this, "Selected Date: $selectedDate", Toast.LENGTH_SHORT).show()

            val selectedMonth = String.format("%02d", month + 1)
            tvHolidayList.text = holidays[selectedMonth]?.joinToString("\n") ?: "No holidays this month."
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                return true
            }
            R.id.action_student_c -> {
                startActivity(Intent(this, StudentsActivity::class.java))
                return true
            }
            R.id.action_logout -> {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}