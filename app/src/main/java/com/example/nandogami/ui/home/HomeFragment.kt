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
            .addOnSuccessListener { snap ->
                allTitles.clear()
                // Map dokumen Firestore ke objek Title
                allTitles.addAll(snap.mapNotNull { it.toObject(Title::class.java) })

                // Inisialisasi dan set adapter untuk rvFeatured
                binding.rvFeatured.adapter =
                    FeaturedAdapter(allTitles.filter { it.isFeatured }) { title ->
                        // Handle klik item Featured
                        navigateToDetail(title)
                    }

                // Inisialisasi dan set adapter untuk rvPopular (menampilkan semua manga)
                binding.rvPopular.adapter =
                    TitleAdapter(allTitles) { title ->
                        // Handle klik item Popular
                        navigateToDetail(title)
                    }
                Log.d("HomeFragment", "Titles fetched: $allTitles")
                // Inisialisasi dan set adapter untuk rvNewRelease
                binding.rvNewRelease.adapter =
                    TitleAdapter(allTitles.filter { it.isNewRelease }) { title ->
                        // Handle klik item New Release
                        navigateToDetail(title)
                    }

                // Inisialisasi dan set adapter untuk rvCategories
                val categories = allTitles.flatMap { it.categories }.distinct()
                binding.rvCategories.adapter =
                    CategoryAdapter(categories) { category ->
                        // Filter rvPopular berdasarkan kategori yang dipilih
                        val filteredTitles = allTitles.filter { it.categories.contains(category) }
                        binding.rvPopular.adapter = TitleAdapter(filteredTitles) { title ->
                            // Handle klik item Popular setelah filter
                            navigateToDetail(title)
                        }
                    }
            }
            .addOnFailureListener { e ->
                // TODO: Handle error, misalnya log error atau tampilkan Toast
                // Log.e("HomeFragment", "Error fetching titles: ${e.message}", e)
                Log.e("HomeFragment", "Error fetching titles: ${e.message}")
                // Toast.makeText(requireContext(), "Failed to load data: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
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