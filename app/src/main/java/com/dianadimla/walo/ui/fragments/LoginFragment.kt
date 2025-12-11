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

    // View binding
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Firebase authentication
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPrefs: SharedPreferences

    // Inflate the layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    // View created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        // Initialize SharedPreferences
        sharedPrefs = requireContext().getSharedPreferences("WaloPrefs", Context.MODE_PRIVATE)

        // Start animation
        val swimAnimation = AnimationUtils.loadAnimation(context, R.anim.slow_swim)
        binding.appLogo.startAnimation(swimAnimation)

        // Check if user is already logged in
        if (auth.currentUser != null && sharedPrefs.getBoolean("rememberMe", false)) {
            findNavController().navigate(R.id.action_login_to_home)
        }

        // Login button click listener
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val rememberMe = binding.rememberMeCheckBox.isChecked

            // Validate email
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailEditText.error = "Invalid Email"
                return@setOnClickListener
            }
            // Validate password
            if (password.isEmpty()) {
                binding.passwordEditText.error = "Enter password"
                return@setOnClickListener
            }

            // Firebase sign in
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Save "Remember Me" preference
                        sharedPrefs.edit().putBoolean("rememberMe", rememberMe).apply()
                        // Navigate to home screen
                        findNavController().navigate(R.id.action_login_to_home)
                    } else {
                        // Show error toast
                        Toast.makeText(context, "Login failed: ${'$'}{task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Go to signup text click listener
        binding.goToSignupText.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signup)
        }
    }

    // Destroy view
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}
