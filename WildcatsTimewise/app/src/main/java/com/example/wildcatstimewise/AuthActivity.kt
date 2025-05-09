package com.example.wildcatstimewise

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.wildcatstimewise.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private var isLoginFragmentVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
                replaceFragment(LoginFragment.newInstance(), addToBackStack = false, useSlideRightAnim = false)
            } else {
                isLoginFragmentVisible = supportFragmentManager.findFragmentById(R.id.fragment_container) is LoginFragment
            }
            updateToggleButtons(isLogin = isLoginFragmentVisible)
        } else {
            isLoginFragmentVisible = supportFragmentManager.findFragmentById(R.id.fragment_container) is LoginFragment
            updateToggleButtons(isLogin = isLoginFragmentVisible)
        }

        setupToggleButtons()
    }

    private fun setupToggleButtons() {
        binding.loginToggleButton.setOnClickListener {
            if (!isLoginFragmentVisible) {
                replaceFragment(LoginFragment.newInstance(), addToBackStack = true, useSlideRightAnim = false)
                isLoginFragmentVisible = true
                updateToggleButtons(isLogin = true)
            }
        }

        binding.signupToggleButton.setOnClickListener {
            if (isLoginFragmentVisible) {
                replaceFragment(RegisterFragment.newInstance(), addToBackStack = true, useSlideRightAnim = true)
                isLoginFragmentVisible = false
                updateToggleButtons(isLogin = false)
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean, useSlideRightAnim: Boolean) {
        supportFragmentManager.commit {
            val enterAnim: Int
            val exitAnim: Int
            val popEnterAnim: Int
            val popExitAnim: Int

            if (useSlideRightAnim) {
                enterAnim = R.anim.slide_in_right
                exitAnim = R.anim.slide_out_left
                popEnterAnim = R.anim.slide_in_left
                popExitAnim = R.anim.slide_out_right
            } else {
                enterAnim = R.anim.slide_in_left
                exitAnim = R.anim.slide_out_right
                popEnterAnim = R.anim.slide_in_right
                popExitAnim = R.anim.slide_out_left
            }

            setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
            replace(R.id.fragment_container, fragment, fragment::class.java.simpleName)
            if (addToBackStack) {
                addToBackStack(fragment::class.java.simpleName)
            }
            setReorderingAllowed(true)
        }
    }

    private fun updateToggleButtons(isLogin: Boolean) {
        val activeBg = R.drawable.filledrec
        val inactiveBg = R.drawable.filledrecpastel
        val activeTextColor = ContextCompat.getColor(this, R.color.white)
        val inactiveTextColor = ContextCompat.getColor(this, R.color.crimson)
        val activeElevation = resources.getDimension(R.dimen.active_button_elevation)
        val inactiveElevation = resources.getDimension(R.dimen.inactive_button_elevation)

        binding.loginToggleButton.apply {
            setBackgroundResource(if (isLogin) activeBg else inactiveBg)
            setTextColor(if (isLogin) activeTextColor else inactiveTextColor)
            elevation = if (isLogin) activeElevation else inactiveElevation
        }

        binding.signupToggleButton.apply {
            setBackgroundResource(if (!isLogin) activeBg else inactiveBg)
            setTextColor(if (!isLogin) activeTextColor else inactiveTextColor)
            elevation = if (!isLogin) activeElevation else inactiveElevation
        }

        if (isLogin) {
            binding.loginregisterTextView.text = "Welcome! Please log in to continue."
        } else {
            binding.loginregisterTextView.text = "New here? Create an account to get started."
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            binding.root.postDelayed({
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                isLoginFragmentVisible = currentFragment is LoginFragment
                updateToggleButtons(isLogin = isLoginFragmentVisible)
            }, 50)
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                super.onBackPressed()
            } else {
                super.onBackPressed()
            }
        }
    }

    fun navigateToLoginFragment(registeredEmail: String? = null) {
        val loginFragment = LoginFragment.newInstance().apply {
            arguments = Bundle().apply {
                putString("REGISTERED_EMAIL", registeredEmail)
            }
        }
        replaceFragment(loginFragment, addToBackStack = true, useSlideRightAnim = false)
        isLoginFragmentVisible = true
        updateToggleButtons(isLogin = true)
    }
}