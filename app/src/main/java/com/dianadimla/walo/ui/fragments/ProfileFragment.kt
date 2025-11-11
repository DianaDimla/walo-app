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

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
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
        sharedPrefs = requireContext().getSharedPreferences("WaloPrefs", Context.MODE_PRIVATE)

        binding.logoutButton.setOnClickListener {
            // Sign the user out of their Firebase session.
            auth.signOut()

            // Clear the "Remember Me" preference so the user is not automatically logged in next time.
            sharedPrefs.edit().putBoolean("rememberMe", false).apply()

            // Navigate back to the login screen. The action is configured to clear the
            // entire back stack, so the user cannot press "back" to return to the dashboard.
            findNavController().navigate(R.id.action_profile_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks by cleaning up the binding reference
    }
}
