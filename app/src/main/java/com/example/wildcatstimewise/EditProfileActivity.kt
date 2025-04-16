package com.example.wildcatstimewise

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class EditProfileActivity : Activity() {

    private lateinit var backButton: ImageView

    private lateinit var tilName: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var tilCourse: TextInputLayout
    private lateinit var etCourse: TextInputEditText
    private lateinit var tilYearLevel: TextInputLayout
    private lateinit var etYearLevel: TextInputEditText
    private lateinit var tilPhone: TextInputLayout
    private lateinit var etPhone: TextInputEditText
    private lateinit var tilStudentId: TextInputLayout
    private lateinit var etStudentId: TextInputEditText

    private lateinit var tilEvent1: TextInputLayout
    private lateinit var etEvent1: TextInputEditText
    private lateinit var tilEvent2: TextInputLayout
    private lateinit var etEvent2: TextInputEditText
    private lateinit var etEvent3: TextInputEditText
    private lateinit var etEvent4: TextInputEditText
    private lateinit var tilSchedule1: TextInputLayout
    private lateinit var etSchedule1: TextInputEditText
    private lateinit var tilSchedule2: TextInputLayout
    private lateinit var etSchedule2: TextInputEditText
    private lateinit var etSchedule3: TextInputEditText
    private lateinit var etSchedule4: TextInputEditText

    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        backButton = findViewById(R.id.backButton)
        tilName = findViewById(R.id.tilName)
        etName = findViewById(R.id.etName)
        tilCourse = findViewById(R.id.tilCourse)
        etCourse = findViewById(R.id.etCourse)
        tilYearLevel = findViewById(R.id.tilYearLevel)
        etYearLevel = findViewById(R.id.etYearLevel)
        tilPhone = findViewById(R.id.tilPhone)
        etPhone = findViewById(R.id.etPhone)
        tilStudentId = findViewById(R.id.tilStudentId)
        etStudentId = findViewById(R.id.etStudentId)

        tilEvent1 = findViewById(R.id.tilEvent1)
        etEvent1 = findViewById(R.id.etEvent1)
        tilEvent2 = findViewById(R.id.tilEvent2)
        etEvent2 = findViewById(R.id.etEvent2)
        tilSchedule1 = findViewById(R.id.tilSchedule1)
        etSchedule1 = findViewById(R.id.etSchedule1)
        tilSchedule2 = findViewById(R.id.tilSchedule2)
        etSchedule2 = findViewById(R.id.etSchedule2)
        saveButton = findViewById(R.id.saveButton)

        // Get data from intent
        val name = intent.getStringExtra("name")
        val courseYear = intent.getStringExtra("courseYear")
        val phone = intent.getStringExtra("phone")
        val studentId = intent.getStringExtra("studentId")
        val event1 = intent.getStringExtra("event1")
        val event2 = intent.getStringExtra("event2")
        val schedule1 = intent.getStringExtra("schedule1")
        val schedule2 = intent.getStringExtra("schedule2")

        // Set data to EditTexts
        etName.setText(name)
        etCourse.setText(courseYear?.split(" - ")?.get(0) ?: "")
        etYearLevel.setText(courseYear?.split(" - ")?.get(1)?.replace("rd Year", "") ?: "")
        etPhone.setText(phone)
        etStudentId.setText(studentId)
        etEvent1.setText(event1)
        etEvent2.setText(event2)
        etSchedule1.setText(schedule1)
        etSchedule2.setText(schedule2)

        backButton.setOnClickListener {
            // Navigate back to ProfileActivity
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish() // Finish current activity
        }

        saveButton.setOnClickListener {
            // Get updated values from EditTexts
            val updatedName = etName.text.toString()
            val updatedCourse = etCourse.text.toString()
            val updatedYearLevel = etYearLevel.text.toString()
            val updatedPhone = etPhone.text.toString()
            val updatedStudentId = etStudentId.text.toString()

            val updatedEvent1 = etEvent1.text.toString()
            val updatedEvent2 = etEvent2.text.toString()
            val updatedEvent3 = etEvent3.text.toString()
            val updatedEvent4 = etEvent4.text.toString()
            val updatedSchedule1 = etSchedule1.text.toString()
            val updatedSchedule2 = etSchedule2.text.toString()
            val updatedSchedule3 = etSchedule3.text.toString()
            val updatedSchedule4 = etSchedule4.text.toString()

            // Save updated values to SharedPreferences
            val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("name", updatedName)
            editor.putString("course", updatedCourse)
            editor.putString("yearLevel", updatedYearLevel)
            editor.putString("phone", updatedPhone)
            editor.putString("studentId", updatedStudentId)

            editor.putString("event1", updatedEvent1)
            editor.putString("event2", updatedEvent2)
            editor.putString("event3", updatedEvent3)
            editor.putString("event4", updatedEvent4)
            editor.putString("schedule1", updatedSchedule1)
            editor.putString("schedule2", updatedSchedule2)
            editor.putString("schedule3", updatedSchedule3)
            editor.putString("schedule4", updatedSchedule4)
            editor.apply()
        }
    }
}