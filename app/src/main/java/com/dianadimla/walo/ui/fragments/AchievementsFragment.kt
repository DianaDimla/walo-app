/**
 * Fragment for displaying the user's collection of earned and locked achievements.
 * Visualises financial milestones as collectible badges in a grid layout.
 */
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
        
        // Starts real-time observation of achievement milestones in Firestore
        viewModel.startListeningForAchievements()
    }

    // Initialises the user interface components and the grid layout
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Configures a 2-column grid for displaying achievement badges
        adapter = AchievementAdapter(emptyList())
        binding.rvAchievements.apply {
            this.adapter = this@AchievementsFragment.adapter
            layoutManager = GridLayoutManager(context, 2)
            itemAnimator = null 
        }
    }

    // Subscribes to achievement data changes to refresh the UI
    private fun observeViewModel() {
        viewModel.achievements.observe(viewLifecycleOwner) { achievements ->
            adapter.updateData(achievements)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
