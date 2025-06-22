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
import com.example.nandogami.adapter.CommentAdapter
import com.example.nandogami.adapter.TitleAdapter
import com.example.nandogami.databinding.ActivityDetailBinding
import com.example.nandogami.model.Comment
import com.example.nandogami.model.Title
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.nandogami.R

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var currentTitle: Title? = null
    private lateinit var titleId: String
    private var isFavorited = false
    private var favoriteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        titleId = intent.getStringExtra("titleId") ?: ""

        if (titleId.isBlank()) {
            handleDataError("Title ID is missing.")
            return
        }

        binding.detailToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupTabLayout()
        fetchTitleDetails(titleId)

        // **LOGIKA UNTUK MENGIRIM KOMENTAR**
        binding.btnPostComment.setOnClickListener {
            val commentText = binding.etCommentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                // Panggil fungsi untuk memposting komentar
                postComment(titleId, commentText)
            } else {
                Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        checkFavoriteStatus()

        // Listener untuk ikon favorit
        binding.ivFavorite.setOnClickListener {
            toggleFavoriteStatus()
        }
    }

    private fun checkFavoriteStatus() {
        val userId = auth.currentUser?.uid
        if (userId == null) return

        db.collection("favorites")
            .whereEqualTo("userId", userId)
            .whereEqualTo("titleId", titleId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    isFavorited = true // Now this assignment is valid
                    favoriteId = documents.documents.first().id // This assignment is valid
                    binding.ivFavorite.setImageResource(R.drawable.ic_favorite_filled)
                } else {
                    isFavorited = false // And this one
                    favoriteId = null   // And this one
                    binding.ivFavorite.setImageResource(R.drawable.ic_favorite_border)
                }
            }
    }

    private fun toggleFavoriteStatus() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "You need to be logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (isFavorited) { // Now this condition is valid
            // Hapus dari favorit
            favoriteId?.let {
                db.collection("favorites").document(it).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
                        checkFavoriteStatus() // Perbarui status
                    }
            }
        } else {
            // Tambah ke favorit
            val favoriteData = hashMapOf(
                "userId" to userId,
                "titleId" to titleId,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("favorites").add(favoriteData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
                    checkFavoriteStatus() // Perbarui status
                }
        }
    }

    private fun setupTabLayout() {
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

    // Fungsi ini akan menampilkan tab "About"
    private fun showAboutContent() {
        binding.contentAbout.visibility = View.VISIBLE
        binding.contentWhereToRead.visibility = View.GONE
        binding.contentComments.visibility = View.GONE
        currentTitle?.let { populateAboutContent(it) }
    }

    // Fungsi ini akan menampilkan tab "Where to Read"
    private fun showWhereToReadContent() {
        binding.contentAbout.visibility = View.GONE
        binding.contentWhereToRead.visibility = View.VISIBLE
        binding.contentComments.visibility = View.GONE
        setupWhereToReadClickListeners()
    }

    // Fungsi ini akan menampilkan tab "Comments"
    private fun showCommentsContent() {
        binding.contentAbout.visibility = View.GONE
        binding.contentWhereToRead.visibility = View.GONE
        binding.contentComments.visibility = View.VISIBLE
        // Mengambil data komentar dari database
        fetchComments(titleId)
    }

    /**
     * Mengambil daftar komentar dari Firestore untuk titleId yang spesifik
     * dan menampilkannya di RecyclerView.
     */
    private fun fetchComments(titleId: String) {
        db.collection("comments")
            .whereEqualTo("titleId", titleId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val comments = documents.toObjects(Comment::class.java)
                binding.rvComments.layoutManager = LinearLayoutManager(this)
                binding.rvComments.adapter = CommentAdapter(comments)
            }
            .addOnFailureListener { e ->
                Log.e("DetailActivity", "Error fetching comments", e)
            }
    }

    /**
     * **FUNGSI UTAMA UNTUK MENAMBAHKAN KOMENTAR**
     * Fungsi ini membuat objek Comment baru dan menyimpannya ke koleksi "comments" di Firestore.
     */
    private fun postComment(titleId: String, commentText: String) {
        val user = auth.currentUser
        // Pastikan pengguna sudah login
        if (user == null) {
            Toast.makeText(this, "You need to be logged in to comment", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil username dari profil pengguna untuk ditampilkan
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val username = document.getString("username") ?: "Anonymous"

                // Buat ID unik untuk dokumen komentar baru
                val commentId = db.collection("comments").document().id

                // Buat objek Comment baru
                val newComment = Comment(
                    id = commentId,
                    titleId = titleId,
                    userId = user.uid,
                    userName = username,
                    commentText = commentText,
                    timestamp = System.currentTimeMillis()
                )

                // Simpan objek ke Firestore
                db.collection("comments").document(commentId).set(newComment)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Comment posted!", Toast.LENGTH_SHORT).show()
                        binding.etCommentInput.text.clear() // Kosongkan input field
                        fetchComments(titleId) // Muat ulang komentar untuk menampilkan yang baru
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to post comment: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to get user profile.", Toast.LENGTH_SHORT).show()
            }
    }

    // --- Fungsi-fungsi lain untuk menampilkan data detail (TIDAK PERLU DIUBAH) ---

    private fun fetchTitleDetails(id: String) {
        db.collection("titles").document(id).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val title = document.toObject(Title::class.java)
                    if (title != null) {
                        title.id = document.id
                        currentTitle = title
                        populateHeaderInfo(title)
                        // Secara default, tampilkan tab "About" saat halaman pertama kali dibuka
                        showAboutContent()
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
        binding.collapsingToolbar.title = title.title
        Glide.with(this).load(title.imageUrl).into(binding.ivDetailImage)
        binding.tvDetailTitle.text = title.title
        binding.tvDetailAuthor.text = title.author
        binding.tvDetailTypeBadge.text = title.type
        binding.detailRatingBar.rating = title.rating.toFloat()
        binding.tvDetailRatingValue.text = String.format("%.1f", title.rating)
        binding.chipGroupCategories.removeAllViews()
        title.categories.forEach { categoryName ->
            val chip = Chip(this).apply { text = categoryName }
            binding.chipGroupCategories.addView(chip)
        }
        binding.tvDetailSynopsis.text = title.synopsis
    }

    private fun populateAboutContent(title: Title) {
        if (title.alternativesTitles.isNotEmpty()) {
            binding.headerAlternativeTitles.visibility = View.VISIBLE
            binding.tvAlternativeTitles.visibility = View.VISIBLE
            binding.tvAlternativeTitles.text = title.alternativesTitles.joinToString("\n")
        } else {
            binding.headerAlternativeTitles.visibility = View.GONE
            binding.tvAlternativeTitles.visibility = View.GONE
        }
        binding.tvInfoType.text = title.type
        binding.tvInfoFormat.text = title.format
        binding.tvInfoReleaseYear.text = title.release_year.toString()
        binding.tvInfoChapters.text = title.chapters.toString()
        binding.chipGroupThemes.removeAllViews()
        title.theme.forEach { themeName ->
            val chip = Chip(this).apply { text = themeName }
            binding.chipGroupThemes.addView(chip)
        }
        binding.layoutAdaptations.removeAllViews()
        title.adaptations.forEach { adaptation ->
            val adaptationView = TextView(this).apply {
                text = "${adaptation.type}: ${adaptation.title} (${adaptation.studio}, ${adaptation.year})"
                textSize = 14f
                setPadding(0, 4, 0, 4)
            }
            binding.layoutAdaptations.addView(adaptationView)
        }
        setupDiscoverSection(title.id)
    }

    private fun setupDiscoverSection(currentTitleId: String) {
        binding.rvDiscover.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        db.collection("titles").get().addOnSuccessListener { documents ->
            val allTitles = documents.mapNotNull { doc ->
                val title = doc.toObject(Title::class.java)
                title.id = doc.id
                title
            }
            val discoverList = allTitles.filter { it.id != currentTitleId }.shuffled().take(4)
            if (discoverList.isNotEmpty()) {
                binding.headerDiscover.visibility = View.VISIBLE
                binding.rvDiscover.visibility = View.VISIBLE
                binding.rvDiscover.adapter = TitleAdapter(discoverList) { title ->
                    val intent = Intent(this, DetailActivity::class.java).apply {
                        putExtra("titleId", title.id)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                }
            } else {
                binding.headerDiscover.visibility = View.GONE
                binding.rvDiscover.visibility = View.GONE
            }
        }
    }

    private fun setupWhereToReadClickListeners() {
        binding.cardMangaPlus.setOnClickListener { openUrl("https://mangaplus.shueisha.co.jp/") }
        binding.cardViz.setOnClickListener { openUrl("https://www.viz.com/") }
        binding.cardCrunchyroll.setOnClickListener { openUrl("https://www.crunchyroll.com/manga") }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleDataError(message: String) {
        Log.e("DetailActivity", message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}