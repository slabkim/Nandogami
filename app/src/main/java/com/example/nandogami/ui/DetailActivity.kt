package com.example.nandogami.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.nandogami.adapter.TitleAdapter
import com.example.nandogami.adapter.CommentAdapter
import com.example.nandogami.databinding.ActivityDetailBinding
import com.example.nandogami.model.Title
import com.example.nandogami.model.Comment
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private var currentTitle: Title? = null

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

        setupTabLayout()
        fetchTitleDetails(titleId)
    }

    private fun setupTabLayout() {
        // Setup tab selection listener
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showAboutContent()
                    1 -> showWhereToReadContent()
                    2 -> showCommentsContent()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showAboutContent() {
        // Tampilkan konten About dengan data dari database
        binding.contentAbout.visibility = View.VISIBLE
        binding.contentWhereToRead.visibility = View.GONE
        binding.contentComments.visibility = View.GONE
        
        // Populate data dari database
        currentTitle?.let { title ->
            populateAboutContent(title)
        }
    }

    private fun showWhereToReadContent() {
        // Tampilkan konten Where to Read dengan mock data
        binding.contentAbout.visibility = View.GONE
        binding.contentWhereToRead.visibility = View.VISIBLE
        binding.contentComments.visibility = View.GONE
        
        // Setup click listeners untuk card
        setupWhereToReadClickListeners()
    }

    private fun showCommentsContent() {
        // Tampilkan konten Comments dengan mock data
        binding.contentAbout.visibility = View.GONE
        binding.contentWhereToRead.visibility = View.GONE
        binding.contentComments.visibility = View.VISIBLE
        
        // Setup comments
        setupComments()
    }

    private fun setupWhereToReadClickListeners() {
        binding.cardMangaPlus.setOnClickListener {
            openUrl("https://mangaplus.shueisha.co.jp/")
        }

        binding.cardViz.setOnClickListener {
            openUrl("https://www.viz.com/")
        }

        binding.cardCrunchyroll.setOnClickListener {
            openUrl("https://www.crunchyroll.com/manga")
        }
    }

    private fun setupComments() {
        val mockComments = createMockComments()
        
        binding.rvComments.layoutManager = LinearLayoutManager(this)
        binding.rvComments.adapter = CommentAdapter(mockComments)
    }

    private fun createMockComments(): List<Comment> {
        val currentTime = System.currentTimeMillis()
        val oneHour = 60 * 60 * 1000L
        val oneDay = 24 * 60 * 60 * 1000L
        
        return listOf(
            Comment(
                id = "1",
                userName = "MangaFan123",
                userAvatar = "",
                commentText = "This manga is absolutely incredible! The story is so engaging and the characters are well-developed. I can't wait for the next chapter!",
                timestamp = currentTime - oneHour,
                likeCount = 24
            ),
            Comment(
                id = "2",
                userName = "OtakuLife",
                userAvatar = "",
                commentText = "The art style is amazing and the action scenes are so dynamic. This is definitely one of my favorite manga series right now.",
                timestamp = currentTime - 2 * oneHour,
                likeCount = 18
            ),
            Comment(
                id = "3",
                userName = "WeebMaster",
                userAvatar = "",
                commentText = "I love how the story explores deeper themes while still being entertaining. The character development is top-notch!",
                timestamp = currentTime - oneDay,
                likeCount = 31
            ),
            Comment(
                id = "4",
                userName = "AnimeLover",
                userAvatar = "",
                commentText = "Just finished reading the latest chapter. The plot twist was unexpected but brilliant! Can't wait to see what happens next.",
                timestamp = currentTime - 3 * oneHour,
                likeCount = 15
            ),
            Comment(
                id = "5",
                userName = "MangaReader",
                userAvatar = "",
                commentText = "The world-building in this manga is fantastic. Every detail feels thought out and the lore is so interesting.",
                timestamp = currentTime - 2 * oneDay,
                likeCount = 42
            )
        )
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchTitleDetails(id: String) {
        db.collection("titles").document(id).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val title = document.toObject(Title::class.java)
                    if (title != null) {
                        title.id = document.id
                        currentTitle = title
                        populateHeaderInfo(title)
                        showAboutContent() // Default show About tab
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

    private fun populateHeaderInfo(title: Title) {
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
    }

    private fun populateAboutContent(title: Title) {
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
        binding.tvInfoReleaseYear.text = title.release_year.toString()
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

        // --- Discover Section ---
        setupDiscoverSection(title.id)
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

    private fun handleDataError(message: String) {
        Log.e("DetailActivity", message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}
