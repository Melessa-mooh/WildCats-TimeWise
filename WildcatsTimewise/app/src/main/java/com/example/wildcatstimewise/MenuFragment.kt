package com.example.wildcatstimewise

import android.content.Context // Added for SharedPreferences
import android.content.Intent
import android.content.SharedPreferences // Added for SharedPreferences
import android.net.Uri // Added for Uri
import android.os.Bundle
import android.util.Log // Added for logging
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MenuFragment : BottomSheetDialogFragment() {

    private lateinit var menuProfileSection: LinearLayout
    private lateinit var menuProfileImageView: ImageView
    private lateinit var menuUserNameTextView: TextView
    private lateinit var menuAboutAppTextView: TextView
    private lateinit var menuAboutDevTextView: TextView
    private lateinit var menuLogoutButton: TextView

    private var currentUserEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        menuProfileSection = view.findViewById(R.id.menuProfileSection)
        menuProfileImageView = view.findViewById(R.id.menuProfileImageView)
        menuUserNameTextView = view.findViewById(R.id.menuUserNameTextView)
        menuAboutAppTextView = view.findViewById(R.id.menuAboutAppTextView)
        menuAboutDevTextView = view.findViewById(R.id.menuAboutDevTextView)
        menuLogoutButton = view.findViewById(R.id.menuLogoutButton)

        // Get current user's email (Replace with proper session management)
        currentUserEmail = UserStore.users.keys.firstOrNull()

        val userName = currentUserEmail?.let { UserStore.userDetails[it] } ?: "User Name"
        menuUserNameTextView.text = userName

        // --- Load Profile Picture from SharedPreferences ---
        loadProfileImageForMenu()
        // -------------------------------------------------

        menuProfileSection.setOnClickListener {
            if (currentUserEmail != null) {
                val intent = Intent(activity, ProfileActivity::class.java)
                intent.putExtra("USER_EMAIL", currentUserEmail)
                startActivity(intent)
                dismiss()
            } else {
                Toast.makeText(context, "Error: User not logged in.", Toast.LENGTH_SHORT).show()
            }
        }

        menuAboutAppTextView.setOnClickListener {
            val intent = Intent(activity, AboutAppActivity::class.java)
            startActivity(intent)
            dismiss()
        }

        menuAboutDevTextView.setOnClickListener {
            val intent = Intent(activity, AboutDevelopersActivity::class.java)
            startActivity(intent)
            dismiss()
        }

        menuLogoutButton.setOnClickListener {
            Toast.makeText(context, "Logout clicked (Implement logic)", Toast.LENGTH_SHORT).show()
            // TODO: Implement actual logout logic
            val intent = Intent(activity, LoginPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
            dismiss()
        }
    }

    // --- Load Profile Image for Menu ---
    private fun loadProfileImageForMenu() {
        if (currentUserEmail != null && context != null) {
            // Use the same SharedPreferences file name and key prefix as ProfileActivity
            val sharedPreferences = requireActivity().getSharedPreferences(
                ProfileActivity.PROFILE_PREFS_NAME, // Access constant via class name
                Context.MODE_PRIVATE
            )
            val key = ProfileActivity.KEY_PREFIX_PROFILE_IMAGE_URI + currentUserEmail // Construct key
            val savedUriString = sharedPreferences.getString(key, null)

            if (!savedUriString.isNullOrEmpty()) {
                try {
                    val loadedUri = Uri.parse(savedUriString)
                    menuProfileImageView.setImageURI(loadedUri)
                    Log.d(TAG, "Loaded profile image URI into menu for $currentUserEmail")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing or loading saved URI for menu: $savedUriString", e)
                    menuProfileImageView.setImageResource(android.R.drawable.sym_def_app_icon) // Fallback
                }
            } else {
                menuProfileImageView.setImageResource(android.R.drawable.sym_def_app_icon) // Default if none saved
                Log.d(TAG, "No saved profile image URI found for menu for $currentUserEmail")
            }
        } else {
            menuProfileImageView.setImageResource(android.R.drawable.sym_def_app_icon) // Default if no email/context
        }
    }
    // --------------------------------

    companion object {
        const val TAG = "MenuBottomSheetFragment"
        fun newInstance(): MenuFragment {
            return MenuFragment()
        }
    }
}