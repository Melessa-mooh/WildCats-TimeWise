package com.example.wildcatstimewise

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.wildcatstimewise.R

class RegisterPage : AppCompatActivity() {

    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var studentIdEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var passwordToggle: ImageView
    private lateinit var confirmPasswordToggle: ImageView
    private lateinit var registerButton: TextView
    private lateinit var loginTextView: TextView

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    private val studentIdRegex = Regex("^\\d{2}-\\d{4}-\\d{3}$")
    private val capitalLetterRegex = Regex("^[A-Z].*")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_page)

        firstNameEditText = findViewById(R.id.firstNameEditText)
        lastNameEditText = findViewById(R.id.lastNameEditText)
        studentIdEditText = findViewById(R.id.studentIdEditText)
        emailEditText = findViewById(R.id.gmailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        passwordToggle = findViewById(R.id.passwordToggle)
        confirmPasswordToggle = findViewById(R.id.confirmPasswordToggle)
        registerButton = findViewById(R.id.registerButton)
        loginTextView = findViewById(R.id.login)

        setupListeners()
        setupFocusChangeListeners()
    }

    private fun setupListeners() {
        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(passwordEditText, passwordToggle, isPasswordVisible)
        }

        confirmPasswordToggle.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(confirmPasswordEditText, confirmPasswordToggle, isConfirmPasswordVisible)
        }

        registerButton.setOnClickListener {
            performRegistration()
        }

        loginTextView.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)

        }
    }

    private fun setupFocusChangeListeners() {
        firstNameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateFirstName()
            }
        }
        lastNameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateLastName()
            }
        }
        studentIdEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateStudentId()
            }
        }
        emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateEmail()
            }
        }
        passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validatePassword()
                if (confirmPasswordEditText.text.isNotEmpty()) {
                    validateConfirmPassword()
                }
            }
        }
        confirmPasswordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateConfirmPassword()
            }
        }
    }

    private fun validateFirstName(): Boolean {
        val firstName = firstNameEditText.text.toString().trim()
        return when {
            firstName.isEmpty() -> {
                firstNameEditText.error = "Required"
                false
            }
            !firstName.matches(capitalLetterRegex) -> {
                firstNameEditText.error = "Must start with a capital letter"
                false
            }
            else -> {
                firstNameEditText.error = null
                true
            }
        }
    }

    private fun validateLastName(): Boolean {
        val lastName = lastNameEditText.text.toString().trim()
        return when {
            lastName.isEmpty() -> {
                lastNameEditText.error = "Required"
                false
            }
            !lastName.matches(capitalLetterRegex) -> {
                lastNameEditText.error = "Must start with a capital letter"
                false
            }
            else -> {
                lastNameEditText.error = null
                true
            }
        }
    }

    private fun validateStudentId(): Boolean {
        val studentId = studentIdEditText.text.toString().trim()
        return when {
            studentId.isEmpty() -> {
                studentIdEditText.error = "Required"
                false
            }
            !studentIdRegex.matches(studentId) -> {
                studentIdEditText.error = "Invalid format (e.g., 12-3456-789)"
                false
            }
            else -> {
                studentIdEditText.error = null
                true
            }
        }
    }

    private fun validateEmail(): Boolean {
        val email = emailEditText.text.toString().trim()
        return when {
            email.isEmpty() -> {
                emailEditText.error = "Required"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailEditText.error = "Invalid email format"
                false
            }
            !email.endsWith("@gmail.com", ignoreCase = true) -> {
                emailEditText.error = "Must be a @gmail.com address"
                false
            }
            else -> {
                emailEditText.error = null
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        val password = passwordEditText.text.toString().trim()
        return when {
            password.isEmpty() -> {
                passwordEditText.error = "Required"
                false
            }
            password.length < 6 -> {
                passwordEditText.error = "Password must be at least 6 characters"
                false
            }
            else -> {
                passwordEditText.error = null
                true
            }
        }
    }

    private fun validateConfirmPassword(): Boolean {
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()
        return when {
            confirmPassword.isEmpty() -> {
                confirmPasswordEditText.error = "Required"
                false
            }
            password != confirmPassword -> {
                confirmPasswordEditText.error = "Passwords do not match"
                false
            }
            else -> {
                confirmPasswordEditText.error = null
                true
            }
        }
    }

    private fun performRegistration() {
        val isFirstNameValid = validateFirstName()
        val isLastNameValid = validateLastName()
        val isStudentIdValid = validateStudentId()
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()
        val isConfirmPasswordValid = validateConfirmPassword()

        if (!isFirstNameValid || !isLastNameValid || !isStudentIdValid || !isEmailValid || !isPasswordValid || !isConfirmPasswordValid) {
            showToast("Please correct the errors above")
            return
        }

        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val studentId = studentIdEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (UserStore.users.containsKey(email)) {
            showAlertDialog("Registration Failed", "This email address is already registered.")
            return
        }
        if (UserStore.studentIdToEmail.containsKey(studentId)) {
            showAlertDialog("Registration Failed", "This Student ID is already registered.")
            return
        }

        UserStore.users[email] = password
        UserStore.userDetails[email] = "$firstName $lastName"
        UserStore.studentIdToEmail[studentId] = email
        println("User Registered: Email=$email, Name=$firstName $lastName, StudentID=$studentId")

        showToast("Registration Successful!")

        val intent = Intent(this, LoginPage::class.java)
        intent.putExtra("REGISTERED_EMAIL", email)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }

    private fun togglePasswordVisibility(editText: EditText, toggleView: ImageView, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleView.setImageResource(R.drawable.visiblity)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleView.setImageResource(R.drawable.hide)
        }
        editText.setSelection(editText.length())
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}