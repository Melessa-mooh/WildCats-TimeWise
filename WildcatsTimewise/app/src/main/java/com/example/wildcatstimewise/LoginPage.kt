package com.example.wildcatstimewise

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import com.example.wildcatstimewise.R

class LoginPage : AppCompatActivity() {

    private lateinit var identifierEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var passwordToggle: ImageView
    private lateinit var loginButton: TextView
    private lateinit var signupTextView: TextView
    private lateinit var forgotPasswordTextView: TextView

    private var isPasswordVisible = false
    private val studentIdRegex = Regex("^\\d{2}-\\d{4}-\\d{3}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        identifierEditText = findViewById(R.id.gmailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        passwordToggle = findViewById(R.id.passwordToggle)
        loginButton = findViewById(R.id.loginButton)
        signupTextView = findViewById(R.id.signup)
        //forgotPasswordTextView = findViewById(R.id.forgotPasswordButton)

        setupListeners()
        setupFocusChangeListeners()

        val registeredEmail = intent.getStringExtra("REGISTERED_EMAIL")
        if (registeredEmail != null) {
            identifierEditText.setText(registeredEmail)
        }
    }

    private fun setupListeners() {
        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(passwordEditText, passwordToggle, isPasswordVisible)
        }

        loginButton.setOnClickListener {
            performLogin()
        }

        signupTextView.setOnClickListener {
            val intent = Intent(this, RegisterPage::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // If forgotPasswordTextView is used, its listener remains
        // forgotPasswordTextView.setOnClickListener {
        //     showToast("Forgot Password clicked (Implement logic)")
        // }
    }

    private fun setupFocusChangeListeners() {
        identifierEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateIdentifier()
            }
        }
        passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validatePassword()
            }
        }
    }

    private fun validateIdentifier(): Boolean {
        val identifier = identifierEditText.text.toString().trim()
        if (identifier.isEmpty()) {
            identifierEditText.error = "Please enter Email or Student ID"
            return false
        }

        val isEmail = Patterns.EMAIL_ADDRESS.matcher(identifier).matches()
        val isStudentId = studentIdRegex.matches(identifier)

        return if (isEmail || isStudentId) {
            identifierEditText.error = null
            true
        } else {
            identifierEditText.error = "Invalid Email or Student ID format"
            false
        }
    }

    private fun validatePassword(): Boolean {
        val password = passwordEditText.text.toString().trim()
        return if (password.isEmpty()) {
            passwordEditText.error = "Please enter password"
            false
        } else {
            passwordEditText.error = null
            true
        }
    }

    private fun performLogin() {
        val isIdentifierValid = validateIdentifier()
        val isPasswordValid = validatePassword()

        if (!isIdentifierValid || !isPasswordValid) {
            showToast("Please correct the errors above")
            if (!isIdentifierValid) identifierEditText.requestFocus()
            else if (!isPasswordValid) passwordEditText.requestFocus()
            return
        }

        val identifier = identifierEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        var userEmail: String? = null

        if (Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
            if (UserStore.users.containsKey(identifier)) {
                userEmail = identifier
            }
        } else if (studentIdRegex.matches(identifier)) {
            userEmail = UserStore.studentIdToEmail[identifier]
        }

        if (userEmail == null || !UserStore.users.containsKey(userEmail)) {
            showMaterialAlertDialog("Login Failed", "Account not found for the provided email or student ID.")
            return
        }

        val storedPassword = UserStore.users[userEmail]
        if (storedPassword != password) {
            passwordEditText.error = "Incorrect password"
            showMaterialAlertDialog("Login Failed", "Incorrect password.")
            return
        }
        passwordEditText.error = null

        val userName = UserStore.userDetails[userEmail] ?: "User"
        showToast("Login Successful! Welcome $userName")

        val intent = Intent(this, DashboardPage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        println("Login successful for $userEmail, navigating to main app...")
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

    private fun showMaterialAlertDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}