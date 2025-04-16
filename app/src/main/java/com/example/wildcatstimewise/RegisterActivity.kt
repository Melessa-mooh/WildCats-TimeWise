package com.example.wildcatstimewise

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.util.Calendar

class RegisterActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val firstName = findViewById<EditText>(R.id.edittext_firstname)
        val lastName = findViewById<EditText>(R.id.edittext_lastname)
        val email = findViewById<EditText>(R.id.edittext_email)
        val dob = findViewById<EditText>(R.id.edittext_dob)
        val password = findViewById<EditText>(R.id.edittext_password)
        val confirmPassword = findViewById<EditText>(R.id.edittext_confirm_password)
        val buttonRegister = findViewById<Button>(R.id.button_register)


        dob.setOnClickListener {
            val calendar = Calendar.getInstance()

            calendar.set(Calendar.YEAR, 2003)
            calendar.set(Calendar.MONTH, 0)
            calendar.set(Calendar.DAY_OF_MONTH, 1)

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                    dob.setText(formattedDate)
                },
                year,
                month,
                day
            )

            datePickerDialog.show()
        }



        buttonRegister.setOnClickListener {
            val firstNameText = firstName.text.toString()
            val lastNameText = lastName.text.toString()
            val emailText = email.text.toString()
            val dobText = dob.text.toString()
            val passwordText = password.text.toString()
            val confirmPasswordText = confirmPassword.text.toString()

            if (firstNameText.isEmpty() || lastNameText.isEmpty() || emailText.isEmpty() ||
                dobText.isEmpty() || passwordText.isEmpty() || confirmPasswordText.isEmpty()
            ) {
                Toast.makeText(this, "Fill out the form completely", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!passwordText.matches(Regex("^\\d{6}$"))) {
                Toast.makeText(
                    this,
                    "Password must be exactly 6 digits (numbers only)",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (passwordText != confirmPasswordText) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show()

            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    putExtra("username", emailText)
                    putExtra("password", passwordText)
                    putExtra("firstname", firstNameText) // 👈 Add this line
                }
            )
        }
    }
}