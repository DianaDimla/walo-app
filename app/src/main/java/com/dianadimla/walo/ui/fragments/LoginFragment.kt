/**
 * Fragment responsible for user authentication and session management.
 * Handles login validation, Firebase integration, and persistence of the "Remember Me" preference.
 */
package com.dianadimla.walo.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dianadimla.walo.R
import com.dianadimla.walo.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        
        // Initialises shared preferences for local configuration storage
        sharedPrefs = requireContext().getSharedPreferences("WaloPrefs", Context.MODE_PRIVATE)

        // Triggers the background brand animation
        val swimAnimation = AnimationUtils.loadAnimation(context, R.anim.slow_swim)
        binding.appLogo.startAnimation(swimAnimation)

        // Automatically bypasses login if a valid session exists and persistent login is enabled
        if (auth.currentUser != null && sharedPrefs.getBoolean("rememberMe", false)) {
            findNavController().navigate(R.id.action_login_to_home)
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val rememberMe = binding.rememberMeCheckBox.isChecked

            // Basic format validation before attempting server-side authentication
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailEditText.error = "Invalid Email"
                return@setOnClickListener
            }
            
            if (password.isEmpty()) {
                binding.passwordEditText.error = "Enter password"
                return@setOnClickListener
            }

            // Attempts to verify credentials with Firebase Authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Persists the session preference before navigating to the main interface
                        sharedPrefs.edit().putBoolean("rememberMe", rememberMe).apply()
                        findNavController().navigate(R.id.action_login_to_home)
                    } else {
                        Toast.makeText(context, "Login failed: ${'$'}{task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        binding.goToSignupText.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signup)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
