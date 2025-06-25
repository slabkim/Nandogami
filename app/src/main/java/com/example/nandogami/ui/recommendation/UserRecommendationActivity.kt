package com.example.nandogami.ui.recommendation

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.nandogami.R
import com.example.nandogami.databinding.ActivityUserRecommendationBinding
import com.example.nandogami.model.Title
import com.example.nandogami.data.UserRecommendationRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserRecommendationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserRecommendationBinding
    private val recommendationRepository = UserRecommendationRepository()
    private val db = FirebaseFirestore.getInstance()
    
    private var toUserId: String = ""
    private var toUserName: String = ""
    private var titleId: String = ""
    private var title: Title? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserRecommendationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toUserId = intent.getStringExtra("toUserId") ?: ""
        titleId = intent.getStringExtra("titleId") ?: ""
        
        if (toUserId.isEmpty() || titleId.isEmpty()) {
            Toast.makeText(this, "Missing required data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        loadUserInfo()
        loadTitleInfo()
        setupSendButton()
    }

    private fun setupToolbar() {
        binding.recommendationToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadUserInfo() {
        db.collection("users").document(toUserId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    toUserName = document.getString("username") ?: "Unknown User"
                    val userPhoto = document.getString("photoUrl") ?: ""
                    
                    binding.tvRecommendTo.text = "Recommend to $toUserName"
                    
                    if (userPhoto.isNotEmpty()) {
                        Glide.with(this)
                            .load(userPhoto)
                            .placeholder(R.drawable.ic_user_profile)
                            .into(binding.ivUserPhoto)
                    } else {
                        binding.ivUserPhoto.setImageResource(R.drawable.ic_user_profile)
                    }
                }
            }
    }

    private fun loadTitleInfo() {
        db.collection("titles").document(titleId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    title = document.toObject(Title::class.java)
                    title?.let { manga ->
                        binding.tvMangaTitle.text = manga.title
                        binding.tvMangaAuthor.text = manga.author
                        
                        Glide.with(this)
                            .load(manga.imageUrl)
                            .placeholder(R.drawable.sample)
                            .into(binding.ivMangaCover)
                    }
                }
            }
    }

    private fun setupSendButton() {
        binding.btnSendRecommendation.setOnClickListener {
            val message = binding.etRecommendationMessage.text.toString().trim()
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter a recommendation message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            sendRecommendation(message)
        }
    }

    private fun sendRecommendation(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = recommendationRepository.sendRecommendation(toUserId, titleId, message)
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { recommendation ->
                            Toast.makeText(this@UserRecommendationActivity, "Recommendation sent successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onFailure = { exception ->
                            Toast.makeText(this@UserRecommendationActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserRecommendationActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
} 