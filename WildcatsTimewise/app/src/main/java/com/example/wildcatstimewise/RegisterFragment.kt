package com.example.wildcatstimewise

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.wildcatstimewise.databinding.ActivityRegisterFragmentBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RegisterFragment : Fragment() {

    private var _binding: ActivityRegisterFragmentBinding? = null
    private val binding get() = _binding!!

    private var isPassword1Visible = false
    private var isPassword2Visible = false

    private val studentIdRegex = Regex("^\\d{2}-\\d{4}-\\d{3}$")
    private val capitalLetterRegex = Regex("^[A-Z].*")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityRegisterFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPasswordToggles()
        setupFocusChangeListeners()

        binding.registerRegisterButton.setOnClickListener {
            performRegistration()
        }
    }

    private fun setupPasswordToggles() {
        binding.registerPasswordToggle.setOnClickListener {
            isPassword1Visible = !isPassword1Visible
            togglePasswordVisibility(binding.registerPasswordEditText, binding.registerPasswordToggle, isPassword1Visible)
        }
        binding.registerConfirmPasswordToggle.setOnClickListener {
            isPassword2Visible = !isPassword2Visible
            togglePasswordVisibility(binding.registerConfirmPasswordEditText, binding.registerConfirmPasswordToggle, isPassword2Visible)
        }
        togglePasswordVisibility(binding.registerPasswordEditText, binding.registerPasswordToggle, false)
        togglePasswordVisibility(binding.registerConfirmPasswordEditText, binding.registerConfirmPasswordToggle, false)
    }

    private fun setupFocusChangeListeners() {
        binding.registerFirstNameEditText.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validateFirstName() }
        binding.registerLastNameEditText.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validateLastName() }
        binding.registerStudentIdEditText.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validateStudentId() }
        binding.registerEmailEditText.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validateEmail() }
        binding.registerPasswordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validatePassword()
                if (binding.registerConfirmPasswordEditText.text.isNotEmpty()) validateConfirmPassword()
            }
        }
        binding.registerConfirmPasswordEditText.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validateConfirmPassword() }
    }


    private fun validateFirstName(): Boolean {
        val firstName = binding.registerFirstNameEditText.text.toString().trim()
        return when {
            firstName.isEmpty() -> {
                binding.registerFirstNameEditText.error = "Required"
                false
            }
            !firstName.matches(capitalLetterRegex) -> {
                binding.registerFirstNameEditText.error = "Must start with a capital letter"
                false
            }
            else -> {
                binding.registerFirstNameEditText.error = null
                true
            }
        }
    }

    private fun validateLastName(): Boolean {
        val lastName = binding.registerLastNameEditText.text.toString().trim()
        return when {
            lastName.isEmpty() -> {
                binding.registerLastNameEditText.error = "Required"
                false
            }
            !lastName.matches(capitalLetterRegex) -> {
                binding.registerLastNameEditText.error = "Must start with a capital letter"
                false
            }
            else -> {
                binding.registerLastNameEditText.error = null
                true
            }
        }
    }

    private fun validateStudentId(): Boolean {
        val studentId = binding.registerStudentIdEditText.text.toString().trim()
        return when {
            studentId.isEmpty() -> {
                binding.registerStudentIdEditText.error = "Required"
                false
            }
            !studentIdRegex.matches(studentId) -> {
                binding.registerStudentIdEditText.error = "Invalid format (e.g., 12-3456-789)"
                false
            }
            else -> {
                binding.registerStudentIdEditText.error = null
                true
            }
        }
    }

    private fun validateEmail(): Boolean {
        val email = binding.registerEmailEditText.text.toString().trim()
        return when {
            email.isEmpty() -> {
                binding.registerEmailEditText.error = "Required"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.registerEmailEditText.error = "Invalid email format"
                false
            }
            !email.endsWith("@gmail.com", ignoreCase = true) -> {
                binding.registerEmailEditText.error = "Must be a @gmail.com address"
                false
            }
            else -> {
                binding.registerEmailEditText.error = null
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        val password = binding.registerPasswordEditText.text.toString().trim()
        return when {
            password.isEmpty() -> {
                binding.registerPasswordEditText.error = "Required"
                false
            }
            password.length < 6 -> {
                binding.registerPasswordEditText.error = "Password must be at least 6 characters"
                false
            }
            else -> {
                binding.registerPasswordEditText.error = null
                true
            }
        }
    }

    private fun validateConfirmPassword(): Boolean {
        val password = binding.registerPasswordEditText.text.toString().trim()
        val confirmPassword = binding.registerConfirmPasswordEditText.text.toString().trim()
        return when {
            confirmPassword.isEmpty() -> {
                binding.registerConfirmPasswordEditText.error = "Required"
                false
            }
            password != confirmPassword -> {
                binding.registerConfirmPasswordEditText.error = "Passwords do not match"
                false
            }
            else -> {
                binding.registerConfirmPasswordEditText.error = null
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

        val firstName = binding.registerFirstNameEditText.text.toString().trim()
        val lastName = binding.registerLastNameEditText.text.toString().trim()
        val studentId = binding.registerStudentIdEditText.text.toString().trim()
        val email = binding.registerEmailEditText.text.toString().trim()
        val password = binding.registerPasswordEditText.text.toString().trim()

        if (UserStore.users.containsKey(email)) {
            showMaterialAlertDialog("Registration Failed", "This email address is already registered.")
            return
        }
        if (UserStore.studentIdToEmail.containsKey(studentId)) {
            showMaterialAlertDialog("Registration Failed", "This Student ID is already registered.")
            return
        }

        UserStore.users[email] = password
        UserStore.userDetails[email] = "$firstName $lastName"
        UserStore.studentIdToEmail[studentId] = email
        println("User Registered: Email=$email, Name=$firstName $lastName, StudentID=$studentId")

        showToast("Registration Successful!")

        (activity as? AuthActivity)?.navigateToLoginFragment(email)

    }


    private fun togglePasswordVisibility(editText: EditText, toggleView: ImageView, isVisible: Boolean) {
        val selection = editText.selectionEnd
        if (isVisible) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleView.setImageResource(R.drawable.show)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleView.setImageResource(R.drawable.hide)
        }
        editText.setSelection(selection)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showMaterialAlertDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = RegisterFragment()
    }
}