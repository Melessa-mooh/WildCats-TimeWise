package com.example.wildcatstimewise

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.util.Log

class LandingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        val welcomeTextView = findViewById<TextView>(R.id.textViewWelcome)

        // Get the first name from the intent or default to "User"
        val firstName = intent.getStringExtra("firstname") ?: "User"
        Log.d("DEBUG", "Received firstname: $firstName") // Optional debug log

        welcomeTextView.text = "Welcome, $firstName!"

        // Buttons for navigation
        val dashboardButton = findViewById<Button>(R.id.buttonDashboard)
        val profileButton = findViewById<Button>(R.id.buttonProfile)
        val settingsButton = findViewById<Button>(R.id.buttonSettings)
        val eventsButton = findViewById<Button>(R.id.buttonEvents)
        val logoutButton = findViewById<Button>(R.id.buttonLogout)

        // Dashboard button click
        dashboardButton.setOnClickListener {
            val dashboardIntent = Intent(this, DashboardActivity::class.java).apply {
                putExtra("firstname", firstName)
            }
            startActivity(dashboardIntent)
        }

        // Profile button click
        profileButton.setOnClickListener {
            val profileIntent = Intent(this, ProfileActivity::class.java).apply {
                putExtra("firstname", firstName)
            }
            startActivity(profileIntent)
        }

        // Settings button click
        settingsButton.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }

        // Events button click
        eventsButton.setOnClickListener {
            val eventsIntent = Intent(this, StudentsActivity::class.java)
            startActivity(eventsIntent)
        }

        // Logout button click
        logoutButton.setOnClickListener {
            // Add logic for logout, for now just navigate back to LoginActivity
            val logoutIntent = Intent(this, LoginActivity::class.java)
            startActivity(logoutIntent)
            finish()  // Finish LandingActivity to remove it from the back stack
        }
    }
}