package com.example.nandogami.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nandogami.AuthActivity
import com.example.nandogami.R
import com.example.nandogami.adapter.CategoryAdapter
import com.example.nandogami.adapter.FeaturedAdapter // Pastikan ini adalah FeaturedAdapter yang sudah Anda modifikasi
import com.example.nandogami.adapter.TitleAdapter
import com.example.nandogami.databinding.FragmentHomeBinding // Binding untuk fragment_home.xml
import com.example.nandogami.model.Title // Model Title yang sudah Anda definisikan
import com.example.nandogami.ui.detail.DetailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val allTitles = mutableListOf<Title>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        // Setup LayoutManagers untuk semua RecyclerView
        setupRecyclerViewLayoutManagers()

        // Fetch data dari Firestore
        fetchTitlesFromFirestore()

        // Setup Logout Button
        setupLogoutButton()
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
                        doc.toObject(Title::class.java)
                    } catch (e: Exception) {
                        Log.e("FirestoreDebug", "Gagal memetakan dokumen: ${doc.id}", e)
                        null
                    }
                }

                allTitles.clear()
                allTitles.addAll(mappedTitles)

                // =================== MODIFIKASI DIMULAI DI SINI ===================

                // 2. Filter data, ACAK, lalu kirim ke adapter
                val featuredList = allTitles.filter { it.isFeatured }.shuffled() // Diacak
                val newReleaseList = allTitles.filter { it.isNewRelease }.shuffled() // Diacak
                val popularList = allTitles.shuffled() // Semua item juga diacak untuk "Popular"

                val categories = allTitles.flatMap { it.categories }.distinct()

                Log.d("FirestoreDebug", "Setelah filter: ${featuredList.size} featured, ${newReleaseList.size} new releases.")

                binding.rvFeatured.adapter = FeaturedAdapter(featuredList) { navigateToDetail(it) }
                binding.rvNewRelease.adapter = TitleAdapter(newReleaseList) { navigateToDetail(it) }
                // Gunakan daftar yang sudah diacak untuk "Popular"
                binding.rvPopular.adapter = TitleAdapter(popularList) { navigateToDetail(it) }

                // =================== MODIFIKASI SELESAI ===================

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
        val intent = Intent(requireContext(), DetailActivity::class.java).apply {
            // Pastikan properti "id" di model Title Anda ada dan terisi dari Firestore
            putExtra("titleId", title.id)
            // Anda juga bisa mengirim objek Title secara langsung jika Title mengimplementasikan Parcelable/Serializable
            // putExtra("title", title)
        }
        startActivity(intent)
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            // Kembali ke AuthActivity dan bersihkan task stack
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