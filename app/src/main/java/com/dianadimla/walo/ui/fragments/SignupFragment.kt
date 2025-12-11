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
import com.dianadimla.walo.databinding.FragmentSignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupFragment : Fragment() {

    // View binding
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    // Firebase authentication and Firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPrefs: SharedPreferences

    // Inflate the layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    // View created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        sharedPrefs = requireContext().getSharedPreferences("WaloPrefs", Context.MODE_PRIVATE)

        // Start animation
        val swimAnimation = AnimationUtils.loadAnimation(context, R.anim.slow_swim)
        binding.appLogoSignup.startAnimation(swimAnimation)

        // Signup button click listener
        binding.signupButton.setOnClickListener {
            val firstName = binding.firstNameEditText.text.toString().trim()
            val lastName = binding.lastNameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            // Validate first name
            if (firstName.isEmpty()) {
                binding.firstNameEditText.error = "First Name is required"
                return@setOnClickListener
            }
            // Validate last name
            if (lastName.isEmpty()) {
                binding.lastNameEditText.error = "Last Name is required"
                return@setOnClickListener
            }
            // Validate email
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailEditText.error = "Invalid Email"
                return@setOnClickListener
            }
            // Validate password length
            if (password.length < 6) {
                binding.passwordEditText.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            // Validate password confirmation
            if (password != confirmPassword) {
                binding.confirmPasswordEditText.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Create user with email and password
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser!!.uid
                        val user = mapOf(
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "email" to email
                        )

                        // Save user data to Firestore
                        firestore.collection("users").document(userId).set(user)
                            .addOnSuccessListener {
                                sharedPrefs.edit().putBoolean("rememberMe", true).apply()
                                findNavController().navigate(R.id.action_signup_to_home)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to save user data: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        // Show error toast
                        Toast.makeText(context, "Signup failed: ${'$'}{task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Go to login text click listener
        binding.goToLoginText.setOnClickListener {
            findNavController().navigate(R.id.action_signup_to_login)
        }
    }

    // Destroy view
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}
