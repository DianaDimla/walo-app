package com.dianadimla.walo.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dianadimla.walo.R
import com.dianadimla.walo.databinding.FragmentSignupBinding
import com.google.firebase.auth.FirebaseAuth

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        // Initialize SharedPreferences to manage user preferences
        sharedPrefs = requireContext().getSharedPreferences("WaloPrefs", Context.MODE_PRIVATE)

        binding.signupButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            // Basic email and password validation
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailEditText.error = "Invalid Email"
                return@setOnClickListener
            }
            if (password.length < 6) {
                binding.passwordEditText.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            // Attempt to create a new user with Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // If signup is successful, automatically set the "Remember Me" flag
                        sharedPrefs.edit().putBoolean("rememberMe", true).apply()
                        // Navigate to the main dashboard, clearing the back stack
                        findNavController().navigate(R.id.action_signup_to_home)
                    } else {
                        // If signup fails, show an error message to the user
                        Toast.makeText(context, "Signup failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Navigate to the login screen if the user already has an account
        binding.goToLoginText.setOnClickListener {
            findNavController().navigate(R.id.action_signup_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks by cleaning up the binding reference
    }
}
