package com.example.nandogami.ui.detail

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.nandogami.adapter.TitleAdapter
import com.example.nandogami.databinding.ActivityDetailBinding
import com.example.nandogami.model.Title
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val titleId = intent.getStringExtra("titleId")

        binding.detailToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        if (titleId.isNullOrBlank()) {
            handleDataError("Title ID is missing.")
            return
        }

        fetchTitleDetails(titleId)
        // Panggil fungsi untuk setup section Discover
        setupDiscoverSection(titleId)
    }

    private fun fetchTitleDetails(id: String) {
        db.collection("titles").document(id).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val title = document.toObject(Title::class.java)
                    if (title != null) {
                        title.id = document.id
                        populateUi(title)
                    } else {
                        handleDataError("Failed to parse title data.")
                    }
                } else {
                    handleDataError("Title not found in database.")
                }
            }
            .addOnFailureListener { exception ->
                handleDataError("Error fetching details: ${exception.message}")
            }
    }

    // FUNGSI BARU UNTUK MENGAMBIL DATA DISCOVER
    private fun setupDiscoverSection(currentTitleId: String) {
        binding.rvDiscover.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        db.collection("titles")
            .get()
            .addOnSuccessListener { documents ->
                val allTitles = documents.mapNotNull { doc ->
                    val title = doc.toObject(Title::class.java)
                    title.id = doc.id
                    title
                }

                // Filter judul yang sedang ditampilkan, acak sisanya, dan ambil 4
                val discoverList = allTitles.filter { it.id != currentTitleId }.shuffled().take(4)

                if (discoverList.isNotEmpty()) {
                    binding.headerDiscover.visibility = View.VISIBLE
                    binding.rvDiscover.visibility = View.VISIBLE

                    val adapter = TitleAdapter(discoverList) { title ->
                        // Saat item discover di-klik, buka halaman detail baru
                        val intent = Intent(this, DetailActivity::class.java).apply {
                            putExtra("titleId", title.id)
                            // Flag ini agar tidak menumpuk activity yang sama
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    }
                    binding.rvDiscover.adapter = adapter
                } else {
                    // Sembunyikan section Discover jika tidak ada item lain
                    binding.headerDiscover.visibility = View.GONE
                    binding.rvDiscover.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Log.e("DetailActivity", "Failed to fetch discover titles", e)
                binding.headerDiscover.visibility = View.GONE
                binding.rvDiscover.visibility = View.GONE
            }
    }

    private fun populateUi(title: Title) {
        // --- Header ---
        binding.collapsingToolbar.title = title.title
        Glide.with(this).load(title.imageUrl).into(binding.ivDetailImage)

        // --- Info Utama ---
        binding.tvDetailTitle.text = title.title
        binding.tvDetailAuthor.text = title.author
        binding.tvDetailTypeBadge.text = title.type
        binding.detailRatingBar.rating = title.rating.toFloat()
        binding.tvDetailRatingValue.text = String.format("%.1f", title.rating)

        // --- Categories (Chips) ---
        binding.chipGroupCategories.removeAllViews()
        title.categories.forEach { categoryName ->
            val chip = Chip(this).apply { text = categoryName }
            binding.chipGroupCategories.addView(chip)
        }

        // --- Synopsis ---
        binding.tvDetailSynopsis.text = title.synopsis

        // --- Alternative Titles ---
        if (title.alternativesTitles.isNotEmpty()) {
            binding.headerAlternativeTitles.visibility = View.VISIBLE
            binding.tvAlternativeTitles.visibility = View.VISIBLE
            binding.tvAlternativeTitles.text = title.alternativesTitles.joinToString("\n")
        } else {
            binding.headerAlternativeTitles.visibility = View.GONE
            binding.tvAlternativeTitles.visibility = View.GONE
        }

        // --- Information Table ---
        binding.tvInfoType.text = title.type
        binding.tvInfoFormat.text = title.format
        binding.tvInfoReleaseYear.text = title.release_year
        binding.tvInfoChapters.text = title.chapters.toString()

        // --- Themes (Chips) ---
        binding.chipGroupThemes.removeAllViews()
        title.theme.forEach { themeName ->
            val chip = Chip(this).apply { text = themeName }
            binding.chipGroupThemes.addView(chip)
        }

        // --- Adaptations ---
        binding.layoutAdaptations.removeAllViews()
        title.adaptations.forEach { adaptation ->
            val adaptationView = TextView(this).apply {
                text = "${adaptation.type}: ${adaptation.title} (${adaptation.studio}, ${adaptation.year})"
                textSize = 14f
                setPadding(0, 4, 0, 4)
            }
            binding.layoutAdaptations.addView(adaptationView)
        }
    }

    private fun handleDataError(message: String) {
        Log.e("DetailActivity", message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}
