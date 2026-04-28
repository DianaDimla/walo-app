/**
 * Fragment responsible for displaying and managing user profile information.
 * Supports feedback submission, account logout, and real-time name retrieval from Firestore.
 */
package com.dianadimla.walo.ui.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dianadimla.walo.R
import com.dianadimla.walo.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        sharedPrefs = requireContext().getSharedPreferences("WaloPrefs", Context.MODE_PRIVATE)

        val currentUser = auth.currentUser

        currentUser?.let { user ->
            binding.profileEmail.text = "Email: ${user.email}"
            
            // Initialises with a fallback name from the email prefix or auth display name
            val emailPrefix = user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            val fallbackName = if (!user.displayName.isNullOrEmpty()) user.displayName else emailPrefix
            binding.profileName.text = fallbackName ?: "User"

            // Attempts to fetch the registered first name for personalisation
            fetchUserFirstName(user.uid)
        }

        binding.btnSendFeedback.setOnClickListener {
            sendFeedbackEmail()
        }

        binding.btnBackProfile.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            sharedPrefs.edit().putBoolean("rememberMe", false).apply()
            findNavController().navigate(R.id.action_profile_to_login)
        }
    }

    /**
     * Initialises an email intent with pre-configured developer support details.
     */
    private fun sendFeedbackEmail() {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("dianadimla@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Walo App Feedback")
        }
        
        try {
            startActivity(Intent.createChooser(emailIntent, "Send feedback via..."))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No email app found.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Retrieves the user's specific first name from the Firestore database.
     */
    private fun fetchUserFirstName(uid: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
                
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName")
                    if (!firstName.isNullOrEmpty()) {
                        binding.profileName.text = firstName
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error fetching user data", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
