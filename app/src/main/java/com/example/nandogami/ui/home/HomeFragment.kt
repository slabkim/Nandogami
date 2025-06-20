package com.example.nandogami.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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

        // Setup LayoutManagers
        listOf(
            binding.rvFeatured,
            binding.rvPopular,
            binding.rvNewRelease
        ).forEach {
            it.layoutManager = LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false)
        }
        binding.rvCategories.layoutManager =
            LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false)

        // Fetch data
        FirebaseFirestore.getInstance()
            .collection("titles")
            .get()
            .addOnSuccessListener { snap ->
                allTitles.clear()
                allTitles.addAll(snap.mapNotNull { it.toObject(Title::class.java) })

                // Adapter Featured tetap
                binding.rvFeatured.adapter =
                    FeaturedAdapter(allTitles.filter { it.isFeatured }) { title ->
                        // intent ke detail, dst...
                    }

                // Tampilkan semua manga di Popular langsung tanpa klik
                binding.rvPopular.adapter =
                    TitleAdapter(allTitles) { /* onClick */ }

                binding.rvNewRelease.adapter =
                    TitleAdapter(allTitles.filter { it.isNewRelease }) { /* onClick */ }

                val cats = allTitles.flatMap { it.categories }.distinct()
                binding.rvCategories.adapter =
                    CategoryAdapter(cats) { category ->
                        val filtered = allTitles.filter { it.categories.contains(category) }
                        binding.rvPopular.adapter = TitleAdapter(filtered) { /* onClick */ }
                    }
            }
            .addOnFailureListener {
                // TODO: handle error
            }

        // Logout
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), AuthActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
