package com.example.wildcatstimewise

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class AboutAppActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var aboutAppTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_app)

        toolbar = findViewById(R.id.aboutAppToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "About the App"

        aboutAppTextView = findViewById(R.id.aboutAppTextView)

        val appDescription = "Wildcats TimeWise is your essential companion for navigating academic life. Seamlessly manage your class schedule, keep track of important university events and holidays, and set timely reminders so you never miss a deadline or meeting. Stay organized, stay informed, and make the most of your time."
        aboutAppTextView.text = appDescription
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
