package com.example.wildcatstimewise

import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.imageview.ShapeableImageView

class AboutDevelopersActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var dev1ImageView: ShapeableImageView
    private lateinit var dev1NameTextView: TextView
    private lateinit var dev1CourseYearTextView: TextView
    private lateinit var dev1BioTextView: TextView
    private lateinit var dev2ImageView: ShapeableImageView
    private lateinit var dev2NameTextView: TextView
    private lateinit var dev2CourseYearTextView: TextView
    private lateinit var dev2BioTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_developers)

        toolbar = findViewById(R.id.aboutDevToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "About the Developers"

        // Find Views
        dev1ImageView = findViewById(R.id.dev1ImageView)
        dev1NameTextView = findViewById(R.id.dev1NameTextView)
        dev1CourseYearTextView = findViewById(R.id.dev1CourseYearTextView)
        dev1BioTextView = findViewById(R.id.dev1BioTextView)

        dev2ImageView = findViewById(R.id.dev2ImageView)
        dev2NameTextView = findViewById(R.id.dev2NameTextView)
        dev2CourseYearTextView = findViewById(R.id.dev2CourseYearTextView)
        dev2BioTextView = findViewById(R.id.dev2BioTextView)


        // dev1ImageView.setImageResource(R.drawable.chris_daniel_photo)
        dev1NameTextView.text = "Chris Daniel Cabataña"
        dev1CourseYearTextView.text = "BS Information Technology - 2nd Year"
        dev1BioTextView.text = "Daniel is a dedicated Information Technology student with a strong interest in Android development and backend systems. He enjoys tackling logical challenges and building efficient, reliable applications. When not coding, he's often exploring new technologies or contributing to open-source projects." // Re-added bio


        // dev2ImageView.setImageResource(R.drawable.melessa_cabasag_photo)
        dev2NameTextView.text = "Ma. Melessa V. Cabasag"
        dev2CourseYearTextView.text = "BS Information Technology - 2nd Year"
        dev2BioTextView.text = "Melessa is a creative Information Technology student specializing in UI/UX design and front-end development. She focuses on crafting user-friendly and visually appealing interfaces that enhance the user experience. Outside of academics, she enjoys digital art and photography." // Re-added bio

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
