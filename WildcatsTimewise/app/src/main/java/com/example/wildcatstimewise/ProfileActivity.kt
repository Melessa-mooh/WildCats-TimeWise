package com.example.wildcatstimewise

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ProfileActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var profileImageView: ShapeableImageView
    private lateinit var profileNameTextView: TextView
    private lateinit var profileFirstNameEditText: TextInputEditText
    private lateinit var profileLastNameEditText: TextInputEditText
    private lateinit var profileEmailEditText: TextInputEditText
    private lateinit var profileStudentIdEditText: TextInputEditText
    private lateinit var profileChangePasswordButton: Button
    private lateinit var profileSaveChangesButton: Button
    private lateinit var profileFirstNameLayout: TextInputLayout
    private lateinit var profileLastNameLayout: TextInputLayout


    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private var currentUserEmail: String? = null
    private var selectedImageUri: Uri? = null


    private var isEditMode = false

    private var originalFirstName: String? = null
    private var originalLastName: String? = null


    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val TAG = "ProfileActivity"

        const val PROFILE_PREFS_NAME = "UserProfilePrefs"
        const val KEY_PREFIX_PROFILE_IMAGE_URI = "profile_image_uri_"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)


        sharedPreferences = getSharedPreferences(PROFILE_PREFS_NAME, Context.MODE_PRIVATE)

        toolbar = findViewById(R.id.profileToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        profileImageView = findViewById(R.id.profileImageView)
        profileNameTextView = findViewById(R.id.profileNameTextView)
        profileFirstNameLayout = findViewById(R.id.profileFirstNameLayout)
        profileLastNameLayout = findViewById(R.id.profileLastNameLayout)
        profileFirstNameEditText = findViewById(R.id.profileFirstNameEditText)
        profileLastNameEditText = findViewById(R.id.profileLastNameEditText)
        profileEmailEditText = findViewById(R.id.profileEmailEditText)
        profileStudentIdEditText = findViewById(R.id.profileStudentIdEditText)
        profileChangePasswordButton = findViewById(R.id.profileChangePasswordButton)
        profileSaveChangesButton = findViewById(R.id.profileSaveChangesButton)

        currentUserEmail = intent.getStringExtra("USER_EMAIL")

        if (currentUserEmail == null) {
            Toast.makeText(this, "Error: User email not provided.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupImagePicker()
        loadUserData()
        setupClickListeners()
        setupMenu()
        updateUiForEditMode()
    }


    private fun setupMenu() {
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.profile_menu, menu)
            }

            override fun onPrepareMenu(menu: Menu) {

                val editItem = menu.findItem(R.id.action_edit_profile)
                if (isEditMode) {
                    editItem?.title = "Cancel"
                    editItem?.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                } else {
                    editItem?.title = "Edit"
                    editItem?.setIcon(android.R.drawable.ic_menu_edit)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit_profile -> {
                        handleEditToggle()
                        true
                    }
                    android.R.id.home -> {

                        finish()
                        true
                    }
                    else -> false
                }
            }
        }, this, Lifecycle.State.RESUMED)
    }

    private fun handleEditToggle() {
        isEditMode = !isEditMode
        if (!isEditMode) {

            profileFirstNameEditText.setText(originalFirstName ?: "")
            profileLastNameEditText.setText(originalLastName ?: "")
            selectedImageUri = null
            loadProfileImage()
        }
        updateUiForEditMode()
        invalidateOptionsMenu()
    }


    private fun updateUiForEditMode() {
        Log.d(TAG, "Updating UI for edit mode: $isEditMode")

        val nameLayoutVisibility = if (isEditMode) View.VISIBLE else View.GONE
        profileFirstNameLayout.visibility = nameLayoutVisibility
        profileLastNameLayout.visibility = nameLayoutVisibility

        profileFirstNameEditText.isFocusable = isEditMode
        profileFirstNameEditText.isFocusableInTouchMode = isEditMode
        profileFirstNameEditText.isClickable = isEditMode

        profileLastNameEditText.isFocusable = isEditMode
        profileLastNameEditText.isFocusableInTouchMode = isEditMode
        profileLastNameEditText.isClickable = isEditMode


        profileImageView.isClickable = isEditMode
        profileImageView.alpha = if (isEditMode) 1.0f else 0.6f


        profileChangePasswordButton.visibility = if (isEditMode) View.VISIBLE else View.GONE
        profileSaveChangesButton.visibility = if (isEditMode) View.VISIBLE else View.GONE


        if (isEditMode) {
            profileFirstNameEditText.requestFocus()
        }
    }


    private fun setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                if (isEditMode) {
                    selectedImageUri = it
                    profileImageView.setImageURI(it)
                    Log.d(TAG, "Image selected: $it")
                }
            }
        }
    }

    private fun loadUserData() {
        val email = currentUserEmail ?: return

        val userFullName = UserStore.userDetails[email]
        val studentId = UserStore.studentIdToEmail.entries.find { it.value == email }?.key

        profileNameTextView.text = userFullName ?: "User Name"
        profileEmailEditText.setText(email)

        if (userFullName != null && userFullName.contains(" ")) {
            val names = userFullName.split(" ", limit = 2)
            originalFirstName = names.getOrElse(0) { "" }
            originalLastName = names.getOrElse(1) { "" }
        } else {
            originalFirstName = userFullName ?: ""
            originalLastName = ""
        }
        profileFirstNameEditText.setText(originalFirstName)
        profileLastNameEditText.setText(originalLastName)

        profileStudentIdEditText.setText(studentId ?: "N/A")

        loadProfileImage()
    }


    private fun loadProfileImage() {
        val email = currentUserEmail ?: return
        val key = KEY_PREFIX_PROFILE_IMAGE_URI + email
        val savedUriString = sharedPreferences.getString(key, null)

        if (!savedUriString.isNullOrEmpty()) {
            try {
                val loadedUri = Uri.parse(savedUriString)
                profileImageView.setImageURI(loadedUri)
                Log.d(TAG, "Loaded profile image URI for $email from SharedPreferences")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing/loading saved URI for profile: $savedUriString", e)
                profileImageView.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        } else {
            profileImageView.setImageResource(android.R.drawable.sym_def_app_icon)
            Log.d(TAG, "No saved profile image URI found for $email")
        }
    }


    private fun setupClickListeners() {
        profileImageView.setOnClickListener {
            if (isEditMode) {
                try {
                    imagePickerLauncher.launch("image/*")
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching image picker", e)
                    Toast.makeText(this, "Cannot open image picker", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Tap Edit to change profile picture", Toast.LENGTH_SHORT).show()
            }
        }

        profileSaveChangesButton.setOnClickListener {

            saveProfileChanges()
        }

        profileChangePasswordButton.setOnClickListener {

            showChangePasswordDialog()
        }
    }

    private fun saveProfileChanges() {

        if (!isEditMode) {
            Log.w(TAG, "Save changes clicked when not in edit mode.")
            return
        }

        val email = currentUserEmail ?: return
        val firstName = profileFirstNameEditText.text.toString().trim()
        val lastName = profileLastNameEditText.text.toString().trim()


        var isValid = true
        if (firstName.isBlank()) {
            profileFirstNameEditText.error = "First name cannot be blank"
            isValid = false
        } else {
            profileFirstNameEditText.error = null
        }
        if (lastName.isBlank()) {
            profileLastNameEditText.error = "Last name cannot be blank"
            isValid = false
        } else {
            profileLastNameEditText.error = null
        }

        if (!isValid) {
            return
        }


        val newFullName = "$firstName $lastName"
        UserStore.userDetails[email] = newFullName
        profileNameTextView.text = newFullName


        originalFirstName = firstName
        originalLastName = lastName


        selectedImageUri?.let { uri ->
            val uriString = uri.toString()
            val key = KEY_PREFIX_PROFILE_IMAGE_URI + email
            try {
                sharedPreferences.edit().putString(key, uriString).apply()
                Log.d(TAG, "Saved profile image URI to SharedPreferences for $email: $uriString")
                selectedImageUri = null
            } catch (e: Exception) {
                Log.e(TAG, "Error saving profile image URI to SharedPreferences", e)
                Toast.makeText(this, "Could not save profile picture change", Toast.LENGTH_SHORT).show()

            }
        }


        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()


        isEditMode = false
        updateUiForEditMode()
        invalidateOptionsMenu()


    }

    private fun showChangePasswordDialog() {
        val email = currentUserEmail ?: return

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val currentPasswordEditText = dialogView.findViewById<EditText>(R.id.currentPasswordEditText)
        val newPasswordEditText = dialogView.findViewById<EditText>(R.id.newPasswordEditText)
        val confirmNewPasswordEditText = dialogView.findViewById<EditText>(R.id.confirmNewPasswordEditText)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change", null)
            .setNegativeButton("Cancel") { d, _ -> d.cancel() }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val currentPassword = currentPasswordEditText.text.toString()
                val newPassword = newPasswordEditText.text.toString()
                val confirmPassword = confirmNewPasswordEditText.text.toString()

                (currentPasswordEditText.parent.parent as? TextInputLayout)?.error = null
                (newPasswordEditText.parent.parent as? TextInputLayout)?.error = null
                (confirmNewPasswordEditText.parent.parent as? TextInputLayout)?.error = null


                var passValidation = true
                if (currentPassword.isEmpty()) {
                    (currentPasswordEditText.parent.parent as? TextInputLayout)?.error = "Required"
                    passValidation = false
                }
                if (newPassword.isEmpty()) {
                    (newPasswordEditText.parent.parent as? TextInputLayout)?.error = "Required"
                    passValidation = false
                }
                if (confirmPassword.isEmpty()) {
                    (confirmNewPasswordEditText.parent.parent as? TextInputLayout)?.error = "Required"
                    passValidation = false
                }
                if (!passValidation) return@setOnClickListener

                if (UserStore.users[email] != currentPassword) {
                    (currentPasswordEditText.parent.parent as? TextInputLayout)?.error = "Incorrect password"

                    return@setOnClickListener
                }

                if (newPassword.length < 6) {
                    (newPasswordEditText.parent.parent as? TextInputLayout)?.error = "Min 6 characters"

                    return@setOnClickListener
                }

                if (newPassword != confirmPassword) {
                    (confirmNewPasswordEditText.parent.parent as? TextInputLayout)?.error = "Passwords don't match"

                    return@setOnClickListener
                }


                UserStore.users[email] = newPassword
                Toast.makeText(this@ProfileActivity, "Password updated successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}