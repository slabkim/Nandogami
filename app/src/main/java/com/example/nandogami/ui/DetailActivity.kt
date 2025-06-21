package com.example.nandogami.ui.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.nandogami.R
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
        binding.chipGroupCategories.removeAllViews() // Hapus chip lama jika ada
        title.categories.forEach { categoryName ->
            val chip = Chip(this).apply {
                text = categoryName
            }
            binding.chipGroupCategories.addView(chip)
        }

        // --- Synopsis ---
        binding.tvDetailSynopsis.text = title.synopsis

        // --- Alternative Titles ---
        Log.d("DetailActivity", "alternativesTitles: ${title.alternativesTitles}") // Tambahkan ini
        if (title.alternativesTitles.isNotEmpty()) {
            binding.tvAlternativeTitles.text = title.alternativesTitles.joinToString("\n")
        } else {
            binding.tvAlternativeTitles.text = "N/A" // Atau sembunyikan TextView ini
            // binding.tvAlternativeTitles.visibility = View.GONE
            Log.d("DetailActivity", "Alternative Titles list is empty.")
        }


        // --- Information Table ---
        binding.tvInfoType.text = title.type
        binding.tvInfoFormat.text = title.format
        binding.tvInfoReleaseYear.text = title.release_year
        binding.tvInfoChapters.text = title.chapters.toString()

        // --- Themes (Chips) ---
        binding.chipGroupThemes.removeAllViews()
        title.theme.forEach { themeName ->
            val chip = Chip(this).apply {
                text = themeName
            }
            binding.chipGroupThemes.addView(chip)
        }

        // --- Adaptations ---
        binding.layoutAdaptations.removeAllViews()
        title.adaptations.forEach { adaptation ->
            // Anda bisa membuat layout item_adaptation.xml yang lebih kompleks
            // atau cukup menggunakan TextView untuk sekarang
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
