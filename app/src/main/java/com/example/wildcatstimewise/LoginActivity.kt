package com.example.wildcatstimewise

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.util.Patterns

class LoginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize UI elements
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.button_login)
        val btnRegister = findViewById<TextView>(R.id.button_register) // If register is a TextView

        var firstname = ""

        // Handling data from RegisterActivity
        intent?.let {
            it.getStringExtra("username")?.let { email -> etEmail.setText(email) }
            it.getStringExtra("password")?.let { password -> etPassword.setText(password) }
            it.getStringExtra("firstname")?.let { fname -> firstname = fname }
        }

        // Login Button Click Handling
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, LandingActivity::class.java).apply {
                putExtra("email", email)
                putExtra("firstname", firstname)
            }
            startActivity(intent)
            finish()
        }


        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}