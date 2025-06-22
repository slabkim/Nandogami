package com.example.nandogami.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nandogami.adapter.RecentSearchAdapter
import com.example.nandogami.adapter.TitleAdapter
import com.example.nandogami.databinding.FragmentDashboardBinding
import com.example.nandogami.model.Title
import com.google.android.material.chip.Chip

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var searchResultsAdapter: TitleAdapter
    private lateinit var recentSearchesAdapter: RecentSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchResults()
        setupRecentSearches()
        setupSearchBox()
        observeViewModel()
    }

    private fun setupSearchBox() {
        binding.searchEditText.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = textView.text.toString()
                if (query.isNotEmpty()) {
                    dashboardViewModel.search(query)
                }
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun setupSearchResults() {
        searchResultsAdapter = TitleAdapter(emptyList()) { title ->
            // Handle title click
        }
        binding.searchResultsRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = searchResultsAdapter
        }
    }

    private fun setupRecentSearches() {
        recentSearchesAdapter = RecentSearchAdapter(emptyList()) { searchTerm ->
            dashboardViewModel.removeRecentSearch(searchTerm)
        }
        binding.recentSearchesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentSearchesAdapter
        }

        binding.tvClearAll.setOnClickListener {
            dashboardViewModel.clearRecentSearches()
        }
    }

    private fun observeViewModel() {
        dashboardViewModel.searchResults.observe(viewLifecycleOwner) { titles ->
            // This is not the best way to update data in an adapter.
            // A better way is to use DiffUtil or ListAdapter,
            // but for simplicity, we create a new adapter instance.
            binding.searchResultsRecyclerView.adapter = TitleAdapter(titles ?: emptyList())
        }

        dashboardViewModel.recentSearches.observe(viewLifecycleOwner) { searches ->
            recentSearchesAdapter.updateData(searches ?: emptyList())
        }

        dashboardViewModel.popularSearches.observe(viewLifecycleOwner) { searches ->
            binding.popularSearchesChipGroup.removeAllViews()
            searches.forEach { searchTerm ->
                val chip = Chip(context)
                chip.text = searchTerm
                chip.setOnClickListener {
                    dashboardViewModel.search(searchTerm)
                    binding.searchEditText.setText(searchTerm)
                }
                binding.popularSearchesChipGroup.addView(chip)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}