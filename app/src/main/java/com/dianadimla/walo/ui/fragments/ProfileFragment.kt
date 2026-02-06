package com.dianadimla.walo.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dianadimla.walo.R
import com.dianadimla.walo.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    // View binding
    private var _binding: FragmentProfileBinding? = null
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
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    // View created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        sharedPrefs = requireContext().getSharedPreferences("WaloPrefs", Context.MODE_PRIVATE)

        // Get the current user
        val currentUser = auth.currentUser

        // Update the UI with the user's information
        currentUser?.let { user ->
            // Use displayName if available, otherwise use email prefix
            val nameToShow = user.displayName ?: user.email?.substringBefore("@") ?: "User"
            binding.profileName.text = nameToShow
            binding.profileEmail.text = user.email
        }

        // Back button click listener
        binding.btnBackProfile.setOnClickListener {
            findNavController().navigateUp()
        }

        // Logout button click listener
        binding.logoutButton.setOnClickListener {
            // Sign out from Firebase
            auth.signOut()

            // Clear "Remember Me" preference
            sharedPrefs.edit().putBoolean("rememberMe", false).apply()

            // Navigate to login screen
            findNavController().navigate(R.id.action_profile_to_login)
        }
    }

    // Destroy view
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}
