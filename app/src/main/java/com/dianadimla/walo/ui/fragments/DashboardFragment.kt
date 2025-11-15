package com.dianadimla.walo.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.dianadimla.walo.R
import com.dianadimla.walo.databinding.FragmentDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // --- UI Logic ---
        val swimAnimation = AnimationUtils.loadAnimation(context, R.anim.slow_swim)
        binding.waloMascot.startAnimation(swimAnimation)

        binding.profileIcon.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        // --- Data Fetching Logic ---
        fetchAndDisplayUserData()
    }

    private fun fetchAndDisplayUserData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // If the user is not found, navigate back to the login screen
            // and clear the back stack to prevent returning to the dashboard.
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.dashboardFragment, true)
                .build()
            findNavController().navigate(R.id.loginFragment, null, navOptions)
            return
        }

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName")
                    if (firstName != null) {
                        binding.greetingText.text = "Hi $firstName, here’s your weekly summary!"
                    } else {
                        binding.greetingText.text = "Hi, here’s your weekly summary!"
                    }
                } else {
                    // Handle case where user document doesn't exist
                    Toast.makeText(context, "Could not find user data.", Toast.LENGTH_SHORT).show()
                    binding.greetingText.text = "Hi, here’s your weekly summary!"
                }
            }
            .addOnFailureListener { e ->
                // Handle failure to fetch data
                Toast.makeText(context, "Error fetching user data: ${e.message}", Toast.LENGTH_LONG).show()
                binding.greetingText.text = "Hi, here’s your weekly summary!"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}
