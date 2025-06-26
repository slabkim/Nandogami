package com.example.nandogami.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
<<<<<<< Updated upstream
import com.example.nandogami.AuthActivity
=======
import com.bumptech.glide.Glide
>>>>>>> Stashed changes
import com.example.nandogami.R
import com.example.nandogami.adapter.CategoryAdapter
import com.example.nandogami.adapter.FeaturedAdapter
import com.example.nandogami.adapter.TitleAdapter
import com.example.nandogami.databinding.FragmentHomeBinding
import com.example.nandogami.model.Title
import com.example.nandogami.ui.detail.DetailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val allTitles = mutableListOf<Title>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup menu dengan cara modern
        setupMenu()

<<<<<<< Updated upstream
=======
        // Tampilkan foto profil user di pojok kanan atas
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
                            .placeholder(R.drawable.ic_user_profile)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                            .into(binding.profileImageHome)
                    } else {
                        binding.profileImageHome.setImageResource(R.drawable.ic_user_profile)
                    }
                }
        } else {
            binding.profileImageHome.setImageResource(R.drawable.ic_user_profile)
        }

        binding.ivNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_chatHistoryFragment)
        }

        // Klik ke halaman profile
        binding.profileImageHome.setOnClickListener {
            val bottomNav = activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
            bottomNav?.selectedItemId = R.id.notificationsFragment
        }

>>>>>>> Stashed changes
        // Setup LayoutManagers untuk semua RecyclerView
        setupRecyclerViewLayoutManagers()

        // Fetch data dari Firestore
        fetchTitlesFromFirestore()

        // Setup Logout Button
        setupLogoutButton()
    }

    private fun setupMenu() {
        // Menggunakan MenuHost untuk menambahkan menu
        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Tambahkan item menu di sini
                menuInflater.inflate(R.menu.home_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle aksi klik item menu
                return when (menuItem.itemId) {
                    R.id.action_notifications -> {
                        findNavController().navigate(R.id.action_homeFragment_to_chatHistoryFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerViewLayoutManagers() {
        // Untuk rvFeatured, rvPopular, rvNewRelease
        listOf(
            binding.rvFeatured,
            binding.rvPopular,
            binding.rvNewRelease
        ).forEach {
            it.layoutManager = LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false)
        }
        // Untuk rvCategories
        binding.rvCategories.layoutManager =
            LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false)
    }

    private fun fetchTitlesFromFirestore() {
        FirebaseFirestore.getInstance()
            .collection("titles")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("FirestoreDebug", "Berhasil mengambil ${documents.size()} dokumen.")

                val mappedTitles = documents.mapNotNull { doc ->
                    try {
                        val title = doc.toObject(Title::class.java)
                        title.id = doc.id
                        title
                    } catch (e: Exception) {
                        Log.e("FirestoreDebug", "Gagal memetakan dokumen: ${doc.id}", e)
                        null
                    }
                }

                allTitles.clear()
                allTitles.addAll(mappedTitles)

                val featuredList = allTitles.filter { it.isFeatured }.shuffled()
                val newReleaseList = allTitles.filter { it.isNewRelease }.shuffled()
                val popularList = allTitles.shuffled()
                val categories = allTitles.flatMap { it.categories }.distinct()

                Log.d("FirestoreDebug", "Setelah filter: ${featuredList.size} featured, ${newReleaseList.size} new releases.")

                binding.rvFeatured.adapter = FeaturedAdapter(featuredList) { navigateToDetail(it) }
                binding.rvNewRelease.adapter = TitleAdapter(newReleaseList) { navigateToDetail(it) }
                binding.rvPopular.adapter = TitleAdapter(popularList) { navigateToDetail(it) }

                binding.rvCategories.adapter =
                    CategoryAdapter(categories) { category ->
                        val filteredTitles = allTitles.filter { it.categories.contains(category) }
                        binding.rvPopular.adapter = TitleAdapter(filteredTitles) { title ->
                            navigateToDetail(title)
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDebug", "Error saat mengambil data", e)
            }
    }

    private fun navigateToDetail(title: Title) {
        if (title.id.isBlank()) {
            Log.e("HomeFragment", "Gagal navigasi: Title ID kosong untuk judul '${title.title}'")
            return
        }
        val intent = Intent(requireContext(), DetailActivity::class.java).apply {
            putExtra("titleId", title.id)
        }
        startActivity(intent)
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}