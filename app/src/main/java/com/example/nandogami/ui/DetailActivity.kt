package com.example.nandogami.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.nandogami.adapter.CommentAdapter
import com.example.nandogami.adapter.TitleAdapter
import com.example.nandogami.databinding.ActivityDetailBinding
import com.example.nandogami.model.Comment
import com.example.nandogami.model.Title
import com.example.nandogami.model.ReadingStatus
import com.example.nandogami.model.ReadingStatusType
import com.example.nandogami.data.ReadingStatusRepository
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.nandogami.R
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import android.widget.Button
import android.widget.EditText
import com.example.nandogami.ui.GifSearchDialogFragment
import com.example.nandogami.ui.recommendation.UserSelectionDialogFragment
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity(), AppBarLayout.OnOffsetChangedListener {
    private lateinit var binding: ActivityDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val readingStatusRepository = ReadingStatusRepository()
    private var currentTitle: Title? = null
    private lateinit var titleId: String
    private var isFavorited = false
    private var favoriteId: String? = null
    private var selectedGifUrl: String? = null
    private var replyingToCommentId: String? = null
    private var replyingToUserName: String? = null
    private var currentReadingStatus: ReadingStatus? = null
    private var isFavoriteIconVisible = true
    private var lastVerticalOffset = 0

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

        val btnGif = findViewById<ImageButton>(R.id.btnGif)
        val ivGifPreview = findViewById<ImageView>(R.id.ivGifPreview)
        btnGif.setOnClickListener {
            val dialog = GifSearchDialogFragment { gifUrl: String ->
                selectedGifUrl = gifUrl
                ivGifPreview.visibility = View.VISIBLE
                Glide.with(this).asGif().load(gifUrl).into(ivGifPreview)
            }
            dialog.show(supportFragmentManager, "GifSearchDialog")
        }
        findViewById<Button>(R.id.btnPostComment).setOnClickListener {
            val commentText = findViewById<EditText>(R.id.etCommentInput).text.toString().trim()
            if (commentText.isEmpty() && (selectedGifUrl == null || selectedGifUrl!!.isEmpty())) {
                Toast.makeText(this, "Komentar tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            postComment(titleId, commentText, selectedGifUrl)
            // Reset preview setelah post
            selectedGifUrl = null
            ivGifPreview.visibility = View.GONE
        }
        checkFavoriteStatus()
        checkReadingStatus()
        setupReadingStatusButtons()

        // Listener untuk ikon favorit
        binding.ivFavorite.setOnClickListener {
            toggleFavoriteStatus()
        }

        binding.appBar.addOnOffsetChangedListener(this)
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        val dy = lastVerticalOffset - verticalOffset
        lastVerticalOffset = verticalOffset

        val totalScrollRange = appBarLayout.totalScrollRange

        val hideThreshold = binding.ivFavorite.height + (binding.ivFavorite.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin

        if (totalScrollRange + verticalOffset < hideThreshold) {
            if (isFavoriteIconVisible && dy > 0) {
                hideFavoriteIcon()
            }
        } else {
            if (!isFavoriteIconVisible && dy < 0) {
                showFavoriteIcon()
            }
        }

        if (verticalOffset == 0 && !isFavoriteIconVisible) {
            showFavoriteIcon()
        }

        if (totalScrollRange + verticalOffset == 0 && isFavoriteIconVisible) {
            hideFavoriteIcon()
        }
    }

    private fun showFavoriteIcon() {
        if (!isFavoriteIconVisible) {
            binding.ivFavorite.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(200).withStartAction {
                binding.ivFavorite.visibility = View.VISIBLE
            }
            isFavoriteIconVisible = true
        }
    }

    private fun hideFavoriteIcon() {
        if (isFavoriteIconVisible) {
            binding.ivFavorite.animate().alpha(0.0f).scaleX(0.0f).scaleY(0.0f).setDuration(200).withEndAction {
                binding.ivFavorite.visibility = View.GONE
            }
            isFavoriteIconVisible = false
        }
    }

    // Penting untuk menghapus listener saat Activity dihancurkan untuk menghindari memory leak
    override fun onDestroy() {
        binding.appBar.removeOnOffsetChangedListener(this)
        super.onDestroy()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_recommend -> {
                showRecommendDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showRecommendDialog() {
        val dialog = UserSelectionDialogFragment.newInstance(titleId) { username, userId ->
            // Handle user selection
            Toast.makeText(this, "Selected user: $username", Toast.LENGTH_SHORT).show()
            
            // TODO: Open UserRecommendationActivity with selected user
            // val intent = Intent(this, UserRecommendationActivity::class.java)
            // intent.putExtra("toUserId", userId)
            // intent.putExtra("titleId", titleId)
            // startActivity(intent)
        }
        dialog.show(supportFragmentManager, "UserSelectionDialog")
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

    private fun checkReadingStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val status = readingStatusRepository.getReadingStatusByTitle(titleId)
                withContext(Dispatchers.Main) {
                    currentReadingStatus = status
                    updateReadingStatusUI(status)
                }
            } catch (e: Exception) {
                Log.e("DetailActivity", "Error checking reading status", e)
            }
        }
    }

    private fun updateReadingStatusUI(status: ReadingStatus?) {
        if (status != null) {
            // Tampilkan status saat ini
            binding.layoutCurrentStatus.visibility = View.VISIBLE
            binding.tvCurrentStatus.text = getStatusText(status.status)
            binding.tvCurrentStatus.setTextColor(getStatusColor(status.status))
            
            // Tampilkan progress chapter jika sedang reading
            if (status.status == ReadingStatusType.READING) {
                binding.layoutChapterProgress.visibility = View.VISIBLE
                binding.etCurrentChapter.setText(status.currentChapter.toString())
                binding.tvTotalChapters.text = status.totalChapters.toString()
            } else {
                binding.layoutChapterProgress.visibility = View.GONE
            }
            
            // Update total chapters dari manga
            currentTitle?.let { title ->
                binding.tvTotalChapters.text = title.chapters.toString()
            }
        } else {
            // Sembunyikan status display jika belum ada status
            binding.layoutCurrentStatus.visibility = View.GONE
            binding.layoutChapterProgress.visibility = View.GONE
        }
    }

    private fun getStatusText(status: ReadingStatusType): String {
        return when (status) {
            ReadingStatusType.PLAN_TO_READ -> "Plan to Read"
            ReadingStatusType.READING -> "Reading"
            ReadingStatusType.COMPLETED -> "Completed"
            ReadingStatusType.DROPPED -> "Dropped"
            ReadingStatusType.ON_HOLD -> "On Hold"
        }
    }

    private fun getStatusColor(status: ReadingStatusType): Int {
        return when (status) {
            ReadingStatusType.PLAN_TO_READ -> getColor(R.color.blue)
            ReadingStatusType.READING -> getColor(R.color.green)
            ReadingStatusType.COMPLETED -> getColor(R.color.purple)
            ReadingStatusType.DROPPED -> getColor(R.color.red)
            ReadingStatusType.ON_HOLD -> getColor(R.color.orange)
        }
    }

    private fun addToReadingList(status: ReadingStatusType) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = if (currentReadingStatus != null) {
                    readingStatusRepository.updateReadingStatus(
                        currentReadingStatus!!.id,
                        status,
                        currentTitle?.chapters?.toInt()
                    )
                } else {
                    readingStatusRepository.addReadingStatus(
                        titleId,
                        status,
                        currentTitle?.chapters?.toInt() ?: 0
                    )
                }
                
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { readingStatus ->
                            currentReadingStatus = readingStatus
                            updateReadingStatusUI(readingStatus)
                            val statusText = when (status) {
                                ReadingStatusType.PLAN_TO_READ -> "Added to Plan to Read"
                                ReadingStatusType.READING -> "Added to Reading"
                                ReadingStatusType.COMPLETED -> "Marked as Completed"
                                ReadingStatusType.DROPPED -> "Marked as Dropped"
                                ReadingStatusType.ON_HOLD -> "Marked as On Hold"
                            }
                            Toast.makeText(this@DetailActivity, statusText, Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { exception ->
                            Toast.makeText(this@DetailActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                android.util.Log.d("DetailActivity", "Jumlah komentar: ${documents.size()}")
                val allComments = documents.toObjects(Comment::class.java)
                android.util.Log.d("DetailActivity", "Isi komentar: $allComments")
                val commentIds = allComments.map { it.id }
                if (commentIds.isEmpty()) {
                    binding.rvComments.layoutManager = LinearLayoutManager(this)
                    binding.rvComments.adapter = CommentAdapter(
                        emptyList(),
                        ::onLikeClick,
                        ::onReplyClick,
                        replyingToCommentId,
                        onSendReply = { parentComment, replyText ->
                            postReply(parentComment, replyText, null)
                            replyingToCommentId = null
                            replyingToUserName = null
                        },
                        isLikedSet = emptySet(),
                        onUserClick = { userId ->
                            val intent = Intent(this, com.example.nandogami.ui.profile.OtherProfileActivity::class.java)
                            intent.putExtra("userId", userId)
                            startActivity(intent)
                        }
                    )
                    return@addOnSuccessListener
                }
                db.collection("comment_likes")
                    .whereIn("commentId", commentIds)
                    .get()
                    .addOnSuccessListener { likeDocs ->
                        val user = auth.currentUser
                        val likeMap = likeDocs.groupBy { it.getString("commentId") ?: "" }
                        val userLikeSet = likeDocs.filter { it.getString("userId") == user?.uid }
                            .mapNotNull { it.getString("commentId") }.toSet()
                        val mainComments = allComments.filter { it.parentId.isNullOrEmpty() || it.parentId == "" }
                        val replies = allComments.filter { !it.parentId.isNullOrEmpty() && it.parentId != "" }
                        val replyMap = replies.groupBy { it.parentId }
                        val nestedComments = mutableListOf<Comment>()
                        for (main in mainComments) {
                            val likeCount = likeMap[main.id]?.size ?: 0
                            nestedComments.add(main.copy(likeCount = likeCount, replies = emptyList()))
                            val childReplies = replyMap[main.id] ?: emptyList()
                            for (reply in childReplies) {
                                val replyLikeCount = likeMap[reply.id]?.size ?: 0
                                val replyWithParent = reply.copy(likeCount = replyLikeCount, replies = emptyList(), userNameParent = main.userName)
                                nestedComments.add(replyWithParent)
                            }
                        }
                        binding.rvComments.layoutManager = LinearLayoutManager(this)
                        binding.rvComments.adapter = CommentAdapter(
                            nestedComments,
                            ::onLikeClick,
                            ::onReplyClick,
                            replyingToCommentId,
                            onSendReply = { parentComment, replyText ->
                                postReply(parentComment, replyText, null)
                                replyingToCommentId = null
                                replyingToUserName = null
                            },
                            isLikedSet = userLikeSet,
                            onUserClick = { userId ->
                                val intent = Intent(this, com.example.nandogami.ui.profile.OtherProfileActivity::class.java)
                                intent.putExtra("userId", userId)
                                startActivity(intent)
                            }
                        )
                    }
                    .addOnFailureListener {
                        val mainComments = allComments.filter { it.parentId.isNullOrEmpty() || it.parentId == "" }
                        val replies = allComments.filter { !it.parentId.isNullOrEmpty() && it.parentId != "" }
                        val replyMap = replies.groupBy { it.parentId }
                        val nestedComments = mutableListOf<Comment>()
                        for (main in mainComments) {
                            nestedComments.add(main.copy(likeCount = 0, replies = emptyList()))
                            val childReplies = replyMap[main.id] ?: emptyList()
                            for (reply in childReplies) {
                                nestedComments.add(reply.copy(likeCount = 0, replies = emptyList()))
                            }
                        }
                        binding.rvComments.layoutManager = LinearLayoutManager(this)
                        binding.rvComments.adapter = CommentAdapter(
                            nestedComments,
                            ::onLikeClick,
                            ::onReplyClick,
                            replyingToCommentId,
                            onSendReply = { parentComment, replyText ->
                                postReply(parentComment, replyText, null)
                                replyingToCommentId = null
                                replyingToUserName = null
                            },
                            isLikedSet = emptySet(),
                            onUserClick = { userId ->
                                val intent = Intent(this, com.example.nandogami.ui.profile.OtherProfileActivity::class.java)
                                intent.putExtra("userId", userId)
                                startActivity(intent)
                            }
                        )
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DetailActivity", "Error fetching comments", e)
            }
    }

    /**
     * **FUNGSI UTAMA UNTUK MENAMBAHKAN KOMENTAR**
     * Fungsi ini membuat objek Comment baru dan menyimpannya ke koleksi "comments" di Firestore.
     */
    private fun postComment(titleId: String, commentText: String, gifUrl: String?) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "You need to be logged in to comment", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val username = document.getString("username") ?: "Anonymous"
                val userAvatar = document.getString("photoUrl") ?: ""
                val commentId = db.collection("comments").document().id // pastikan id di-generate
                val newComment = Comment(
                    id = commentId,
                    titleId = titleId,
                    userId = user.uid,
                    userName = username,
                    userAvatar = userAvatar,
                    commentText = commentText,
                    gifUrl = gifUrl ?: "",
                    timestamp = System.currentTimeMillis()
                )
                db.collection("comments").document(commentId).set(newComment)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Comment posted!", Toast.LENGTH_SHORT).show()
                        findViewById<EditText>(R.id.etCommentInput).text.clear()
                        findViewById<ImageView>(R.id.ivGifPreview).visibility = View.GONE
                        fetchComments(titleId)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to post comment: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to get user profile.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun onReplyClick(comment: Comment) {
        replyingToCommentId = comment.id
        replyingToUserName = comment.userName
        fetchComments(titleId) // refresh adapter agar input reply muncul di bawah komentar yang di-reply
    }

    private fun postReply(parentComment: Comment, replyText: String, gifUrl: String?) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "You need to be logged in to reply", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val username = document.getString("username") ?: "Anonymous"
                val userAvatar = document.getString("photoUrl") ?: ""
                val commentId = db.collection("comments").document().id
                val replyComment = Comment(
                    id = commentId,
                    titleId = parentComment.titleId,
                    userId = user.uid,
                    userName = username,
                    userAvatar = userAvatar,
                    commentText = replyText,
                    gifUrl = gifUrl ?: "",
                    timestamp = System.currentTimeMillis(),
                    parentId = parentComment.id
                )
                db.collection("comments").document(commentId).set(replyComment)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reply posted!", Toast.LENGTH_SHORT).show()
                        replyingToCommentId = null
                        replyingToUserName = null
                        fetchComments(titleId)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to post reply: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to get user profile.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun onLikeClick(comment: Comment) {
        val user = auth.currentUser ?: return
        val likeDocId = "${comment.id}_${user.uid}"
        val likesRef = db.collection("comment_likes").document(likeDocId)
        likesRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                // Sudah like, maka unlike (hapus dokumen)
                likesRef.delete().addOnSuccessListener { fetchComments(titleId) }
            } else {
                // Belum like, maka like (tambah dokumen)
                val likeData = hashMapOf(
                    "commentId" to comment.id,
                    "userId" to user.uid,
                    "timestamp" to System.currentTimeMillis()
                )
                likesRef.set(likeData).addOnSuccessListener { fetchComments(titleId) }
            }
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

    private fun setupReadingStatusButtons() {
        binding.btnPlanToRead.setOnClickListener {
            addToReadingList(ReadingStatusType.PLAN_TO_READ)
        }
        
        binding.btnReading.setOnClickListener {
            addToReadingList(ReadingStatusType.READING)
        }
        
        binding.btnCompleted.setOnClickListener {
            addToReadingList(ReadingStatusType.COMPLETED)
        }
        
        binding.btnDropped.setOnClickListener {
            addToReadingList(ReadingStatusType.DROPPED)
        }
        
        binding.btnOnHold.setOnClickListener {
            addToReadingList(ReadingStatusType.ON_HOLD)
        }
    }
}