package com.dianadimla.walo.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.dianadimla.walo.adapters.AchievementAdapter
import com.dianadimla.walo.databinding.FragmentAchievementsBinding
import com.dianadimla.walo.viewmodels.AchievementsViewModel

// Fragment that displays the user's earned and locked achievements
class AchievementsFragment : Fragment() {

    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AchievementsViewModel by viewModels()
    private lateinit var adapter: AchievementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
        
        // Start listening for real time achievement updates from Firestore
        viewModel.startListeningForAchievements()
    }

    private fun setupUI() {
        // Back Button click listener
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Configure RecyclerView with a 2 column grid layout for badges
        adapter = AchievementAdapter(emptyList())
        binding.rvAchievements.apply {
            this.adapter = this@AchievementsFragment.adapter
            layoutManager = GridLayoutManager(context, 2)
            itemAnimator = null 
        }
    }

    private fun observeViewModel() {
        // Observe achievement list changes to update the adapter
        viewModel.achievements.observe(viewLifecycleOwner) { achievements ->
            adapter.updateData(achievements)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
