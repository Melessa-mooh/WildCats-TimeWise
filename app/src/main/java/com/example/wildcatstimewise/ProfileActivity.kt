package com.example.wildcatstimewise

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var editButton: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var courseYearTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var studentIdTextView: TextView

    private lateinit var event1TextView: TextView
    private lateinit var event2TextView: TextView
    private lateinit var event3TextView: TextView
    private lateinit var event4TextView: TextView
    private lateinit var schedule1TextView: TextView
    private lateinit var schedule2TextView: TextView
    private lateinit var schedule3TextView: TextView
    private lateinit var schedule4TextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Views
        backButton = findViewById(R.id.backButton)
        editButton = findViewById(R.id.editButton)
        nameTextView = findViewById(R.id.nameTextView)
        courseYearTextView = findViewById(R.id.courseYearTextView)
        phoneTextView = findViewById(R.id.phoneTextView)
        studentIdTextView = findViewById(R.id.studentIdTextView)

        event1TextView = findViewById(R.id.event1TextView)
        event2TextView = findViewById(R.id.event2TextView)
        event3TextView = findViewById(R.id.event3TextView)
        event4TextView = findViewById(R.id.event4TextView)
        schedule1TextView = findViewById(R.id.schedule1TextView)
        schedule2TextView = findViewById(R.id.schedule2TextView)
        schedule3TextView = findViewById(R.id.schedule3TextView)
        schedule4TextView = findViewById(R.id.schedule4TextView)

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val name = sharedPreferences.getString("name", "Lisa Manoban")
        val course = sharedPreferences.getString("course", "BSIT")
        val yearLevel = sharedPreferences.getString("yearLevel", "3")
        val phone = sharedPreferences.getString("phone", "+63-5222-156")
        val studentId = sharedPreferences.getString("studentId", "23-5555-333")

        val event1 = sharedPreferences.getString("event1", "Exam:\nComputer Programming\nOctober 20, 2025")
        val event2 = sharedPreferences.getString("event2", "Submission:\nProject Proposal\nOct 25, 2025")
        val event3 = sharedPreferences.getString("event3", "Presentation:\nSoftware Project\nNovember 5, 2025")
        val event4 = sharedPreferences.getString("event4", "Assignment Due:\nCloud Computing\nNov 12, 2025")
        val schedule1 = sharedPreferences.getString("schedule1", "Database Systems\nMWF 9:00 AM - 10:30 AM")
        val schedule2 = sharedPreferences.getString("schedule2", "Web Development\nTTh 1:00 PM - 2:30 PM")
        val schedule3 = sharedPreferences.getString("schedule3", "Mobile App Development\nTTh 10:00 AM - 11:30 AM")
        val schedule4 = sharedPreferences.getString("schedule4", "Network Security\nMW 2:00 PM - 3:30 PM")
//Show all the data
        nameTextView.text = name
        courseYearTextView.text = "$course - ${yearLevel}rd Year"
        phoneTextView.text = phone
        studentIdTextView.text = studentId

        event1TextView.text = event1
        event2TextView.text = event2
        event3TextView.text = event3
        event4TextView.text = event4
        schedule1TextView.text = schedule1
        schedule2TextView.text = schedule2
        schedule3TextView.text = schedule3
        schedule4TextView.text = schedule4

        // Set click listeners
        backButton.setOnClickListener {
            // Navigate back to DashboardActivity
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish() // Finish current activity
        }
        editButton.setOnClickListener {
            // Navigate to EditProfileActivity
            val intent = Intent(this, EditProfileActivity::class.java)

            intent.putExtra("name", nameTextView.text.toString())
            intent.putExtra("courseYear", courseYearTextView.text.toString())
            intent.putExtra("phone", phoneTextView.text.toString())
            intent.putExtra("studentId", studentIdTextView.text.toString())
            intent.putExtra("event1", event1TextView.text.toString())
            intent.putExtra("event2", event2TextView.text.toString())
            intent.putExtra("event3", event3TextView.text.toString())
            intent.putExtra("event4", event4TextView.text.toString())
            intent.putExtra("schedule1", schedule1TextView.text.toString())
            intent.putExtra("schedule2", schedule2TextView.text.toString())
            intent.putExtra("schedule3", schedule3TextView.text.toString())
            intent.putExtra("schedule4", schedule4TextView.text.toString())
            startActivity(intent)
        }
    }
}