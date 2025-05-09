package com.example.wildcatstimewise

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.wildcatstimewise.databinding.ActivityLoginFragmentBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LoginFragment : Fragment() {

    private var _binding: ActivityLoginFragmentBinding? = null
    private val binding get() = _binding!!

    private var isPasswordVisible = false
    private val studentIdRegex = Regex("^\\d{2}-\\d{4}-\\d{3}$")
    private var isLoginWithStudentId = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityLoginFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPasswordToggle()
        setupFocusChangeListeners()
        updateLoginMethodUI()

        binding.loginLoginButton.setOnClickListener {
            performLogin()
        }

        binding.loginToggleMethodButton.setOnClickListener {
            isLoginWithStudentId = !isLoginWithStudentId
            updateLoginMethodUI()
            binding.loginIdentifierEditText.text?.clear()
            binding.loginIdentifierEditText.error = null
        }

        val registeredEmail = arguments?.getString("REGISTERED_EMAIL")
        if (registeredEmail != null) {
            isLoginWithStudentId = false
            updateLoginMethodUI()
            binding.loginIdentifierEditText.setText(registeredEmail)
        }
    }

    private fun updateLoginMethodUI() {
        if (isLoginWithStudentId) {
            binding.loginIdentifierLabel.text = "Enter Student ID"
            binding.loginIdentifierEditText.hint = "Student ID"
            binding.loginIdentifierEditText.inputType = InputType.TYPE_CLASS_TEXT
            binding.loginIdentifierIcon.setImageResource(R.drawable.studentid)
            binding.loginToggleMethodButton.setImageResource(R.drawable.email)
            binding.loginToggleMethodButton.contentDescription = "Switch to Gmail login"
        } else {
            binding.loginIdentifierLabel.text = "Enter Gmail Address"
            binding.loginIdentifierEditText.hint = "Gmail Address"
            binding.loginIdentifierEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            binding.loginIdentifierIcon.setImageResource(R.drawable.gmail)
            binding.loginToggleMethodButton.setImageResource(R.drawable.studentid1)
            binding.loginToggleMethodButton.contentDescription = "Switch to Student ID login"
        }
    }


    private fun setupPasswordToggle() {
        binding.loginPasswordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(binding.loginPasswordEditText, binding.loginPasswordToggle, isPasswordVisible)
        }
        togglePasswordVisibility(binding.loginPasswordEditText, binding.loginPasswordToggle, false)
    }

    private fun setupFocusChangeListeners() {
        binding.loginIdentifierEditText.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validateIdentifier() }
        binding.loginPasswordEditText.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validatePassword() }
    }

    private fun validateIdentifier(): Boolean {
        val identifier = binding.loginIdentifierEditText.text.toString().trim()
        if (identifier.isEmpty()) {
            binding.loginIdentifierEditText.error = if (isLoginWithStudentId) "Please enter Student ID" else "Please enter Gmail Address"
            return false
        }

        return if (isLoginWithStudentId) {
            if (studentIdRegex.matches(identifier)) {
                binding.loginIdentifierEditText.error = null
                true
            } else {
                binding.loginIdentifierEditText.error = "Invalid Student ID format (e.g., 12-3456-789)"
                false
            }
        } else {
            if (Patterns.EMAIL_ADDRESS.matcher(identifier).matches() && identifier.endsWith("@gmail.com", ignoreCase = true)) {
                binding.loginIdentifierEditText.error = null
                true
            } else if (!identifier.endsWith("@gmail.com", ignoreCase = true) && Patterns.EMAIL_ADDRESS.matcher(identifier).matches()){
                binding.loginIdentifierEditText.error = "Must be a @gmail.com address"
                false
            }
            else {
                binding.loginIdentifierEditText.error = "Invalid Gmail Address format"
                false
            }
        }
    }

    private fun validatePassword(): Boolean {
        val password = binding.loginPasswordEditText.text.toString().trim()
        return if (password.isEmpty()) {
            binding.loginPasswordEditText.error = "Please enter password"
            false
        } else {
            binding.loginPasswordEditText.error = null
            true
        }
    }

    private fun performLogin() {
        val isIdentifierValid = validateIdentifier()
        val isPasswordValid = validatePassword()

        if (!isIdentifierValid || !isPasswordValid) {
            showToast("Please correct the errors above")
            if (!isIdentifierValid) binding.loginIdentifierEditText.requestFocus()
            else if (!isPasswordValid) binding.loginPasswordEditText.requestFocus()
            return
        }

        val identifier = binding.loginIdentifierEditText.text.toString().trim()
        val password = binding.loginPasswordEditText.text.toString().trim()

        var userEmail: String? = null
        var loginMethodForMessage: String

        if (isLoginWithStudentId) {
            userEmail = UserStore.studentIdToEmail[identifier]
            loginMethodForMessage = "Student ID"
        } else {
            userEmail = identifier
            loginMethodForMessage = "Gmail"
        }

        if (userEmail == null || !UserStore.users.containsKey(userEmail)) {
            showMaterialAlertDialog("Login Failed", "Account not found for the provided $loginMethodForMessage.")
            return
        }

        val storedPassword = UserStore.users[userEmail]
        if (storedPassword != password) {
            binding.loginPasswordEditText.error = "Incorrect password"
            showMaterialAlertDialog("Login Failed", "Incorrect password.")
            return
        }
        binding.loginPasswordEditText.error = null

        val userName = UserStore.userDetails[userEmail] ?: "User"
        showToast("Login Successful! Welcome $userName")

        val intent = Intent(requireActivity(), DashboardPage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        requireActivity().startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        requireActivity().finish()
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
        fun newInstance() = LoginFragment()
    }
}