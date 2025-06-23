package com.example.nandogami.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.nandogami.AuthActivity
import com.example.nandogami.R
import com.example.nandogami.adapter.CategoryAdapter
import com.example.nandogami.adapter.FeaturedAdapter
import com.example.nandogami.adapter.TitleAdapter
import com.example.nandogami.databinding.FragmentHomeBinding
import com.example.nandogami.model.Title
import com.example.nandogami.ui.detail.DetailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val allTitles = mutableListOf<Title>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

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

        // Klik ke halaman profile
        binding.profileImageHome.setOnClickListener {
            // Navigasi ke ProfileFragment
            val intent = Intent(requireContext(), com.example.nandogami.ui.profile.EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Setup LayoutManagers untuk semua RecyclerView
        setupRecyclerViewLayoutManagers()

        // Fetch data dari Firestore
        fetchTitlesFromFirestore()
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

                // Proses mapping data dari Firestore ke objek Title
                val mappedTitles = documents.mapNotNull { doc ->
                    try {
                        // 1. Ubah dokumen ke objek Title
                        val title = doc.toObject(Title::class.java)

                        // 2. Masukkan ID dokumen ke dalam objek title (INI PERBAIKANNYA)
                        title.id = doc.id

                        // 3. Kembalikan objek yang sudah lengkap
                        title
                    } catch (e: Exception) {
                        Log.e("FirestoreDebug", "Gagal memetakan dokumen: ${doc.id}", e)
                        null
                    }
                }

                allTitles.clear()
                allTitles.addAll(mappedTitles)

                // Filter data, acak, lalu kirim ke adapter
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
        // Cek apakah ID tidak kosong sebelum memulai activity
        if (title.id.isBlank()) {
            Log.e("HomeFragment", "Gagal navigasi: Title ID kosong untuk judul '${title.title}'")
            return
        }
        val intent = Intent(requireContext(), DetailActivity::class.java).apply {
            putExtra("titleId", title.id)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}