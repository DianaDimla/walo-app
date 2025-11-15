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
        // Initialize SharedPreferences for "Remember Me" functionality
        sharedPrefs = requireContext().getSharedPreferences("WaloPrefs", Context.MODE_PRIVATE)

        // Start the swimming animation on the logo
        val swimAnimation = AnimationUtils.loadAnimation(context, R.anim.slow_swim)
        binding.appLogo.startAnimation(swimAnimation)

        // If the user is already logged in and the "Remember Me" flag is set,
        // navigate directly to the main dashboard.
        if (auth.currentUser != null && sharedPrefs.getBoolean("rememberMe", false)) {
            findNavController().navigate(R.id.action_login_to_home)
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val rememberMe = binding.rememberMeCheckBox.isChecked

            // Basic email and password validation
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailEditText.error = "Invalid Email"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.passwordEditText.error = "Enter password"
                return@setOnClickListener
            }

            // Attempt to sign in with Firebase Authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // If login is successful, save the "Remember Me" preference
                        sharedPrefs.edit().putBoolean("rememberMe", rememberMe).apply()
                        // Navigate to the main dashboard, clearing the back stack
                        findNavController().navigate(R.id.action_login_to_home)
                    } else {
                        // If login fails, show an error message to the user
                        Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Navigate to the signup screen if the user doesn't have an account
        binding.goToSignupText.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signup)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks by cleaning up the binding reference
    }
}
