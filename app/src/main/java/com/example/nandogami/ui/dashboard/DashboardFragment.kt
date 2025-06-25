package com.example.nandogami.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nandogami.adapter.RecentSearchAdapter
import com.example.nandogami.adapter.TitleAdapter
import com.example.nandogami.databinding.FragmentDashboardBinding
import com.example.nandogami.model.Title
import com.example.nandogami.ui.detail.DetailActivity
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide
import androidx.navigation.fragment.findNavController
import androidx.navigation.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
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
        // Pindahkan inisialisasi visibilitas ke sini, setelah binding diinisialisasi
        binding.filtersGroup.visibility = View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tampilkan foto profil user di ivProfile
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val photoUrl = document.getString("photoUrl")
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .placeholder(com.example.nandogami.R.drawable.ic_user_profile)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                            .into(binding.ivProfile)
                    } else {
                        binding.ivProfile.setImageResource(com.example.nandogami.R.drawable.ic_user_profile)
                    }
                }
        } else {
            binding.ivProfile.setImageResource(com.example.nandogami.R.drawable.ic_user_profile)
        }

        // Klik ke halaman profile
        binding.ivProfile.setOnClickListener {
            // Navigasi ke ProfileFragment/tab profile dengan BottomNavigationView
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(com.example.nandogami.R.id.bottomNavigationView)
            bottomNav.selectedItemId = com.example.nandogami.R.id.navigation_notifications
        }

        setupSearchResults()
        setupRecentSearches()
        setupSearchBox()
        observeViewModel()
    }

    private fun navigateToDetail(title: Title) {
        if (title.id.isBlank()) {
            Log.e("DashboardFragment", "Gagal navigasi: Title ID kosong untuk judul '${title.title}'")
            return
        }
        val intent = Intent(requireContext(), DetailActivity::class.java).apply {
            putExtra("titleId", title.id)
        }
        startActivity(intent)
    }

    private fun setupSearchBox() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()

                val isSearching = query.isNotEmpty()

                // Sembunyikan popular/recent saat search aktif
                binding.popularSearchesChipGroup.visibility = if (isSearching) View.GONE else View.VISIBLE
                binding.recentSearchesRecyclerView.visibility = if (isSearching) View.GONE else View.VISIBLE
                binding.tvClearAll.visibility = if (isSearching) View.GONE else View.VISIBLE
                binding.tvRecentSearches.visibility = if (isSearching) View.GONE else View.VISIBLE
                binding.tvPopularSearches.visibility = if (isSearching) View.GONE else View.VISIBLE
                binding.filtersGroup.visibility = if (isSearching) View.GONE else View.VISIBLE

                if (isSearching) {
                    dashboardViewModel.search(query)
                } else {
                    dashboardViewModel.clearSearchResults()
                }
            }

        })
    }

    private fun setupSearchResults() {
        searchResultsAdapter = TitleAdapter(emptyList()) { title ->
            navigateToDetail(title)
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
            val hasResults = !titles.isNullOrEmpty()

            // Update data adapter
            searchResultsAdapter.updateData(titles ?: emptyList())

            // Tampilkan hasil dan jumlah
            binding.tvTitlesCount.text = "${titles?.size ?: 0} titles"
            binding.tvTitlesCount.visibility = if (hasResults) View.VISIBLE else View.GONE
            binding.searchResultsRecyclerView.visibility = if (hasResults) View.VISIBLE else View.GONE

            // Atur visibilitas UI tambahan
            val showExtraUI = !hasResults && binding.searchEditText.text.isNullOrEmpty()
            binding.popularSearchesChipGroup.visibility = if (showExtraUI) View.VISIBLE else View.GONE
            binding.recentSearchesRecyclerView.visibility = if (showExtraUI) View.VISIBLE else View.GONE
            binding.tvClearAll.visibility = if (showExtraUI) View.VISIBLE else View.GONE

            // Tambahkan ini jika kamu bungkus filter dalam satu grup layout
            binding.filtersGroup.visibility = if (showExtraUI) View.VISIBLE else View.GONE

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
