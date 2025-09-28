package com.example.nandogami.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.nandogami.databinding.ActivityOtherProfileBinding
import com.example.nandogami.data.FollowRepository
import com.example.nandogami.ui.chat.ChatActivity
import com.example.nandogami.ui.recommendation.UserRecommendationActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.nandogami.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class OtherProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOtherProfileBinding
    private lateinit var db: FirebaseFirestore
    private val followRepository = FollowRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private var otherUserId: String = ""
    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtherProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        otherUserId = intent.getStringExtra("userId") ?: ""
        if (otherUserId.isEmpty()) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupButtons()
        loadOtherUserProfile(otherUserId)
        checkFollowStatus()

        // Tambahkan click listener untuk follower/following/reading
        binding.tvFollowersCount.setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("userId", otherUserId)
            intent.putExtra("listType", "followers")
            startActivity(intent)
        }
        binding.tvFollowingCount.setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("userId", otherUserId)
            intent.putExtra("listType", "following")
            startActivity(intent)
        }
        binding.tvReadingCount.setOnClickListener {
            val intent = Intent(this, ReadingListActivity::class.java)
            intent.putExtra("userId", otherUserId)
            startActivity(intent)
        }
    }

    private fun setupToolbar() {
        binding.otherProfileToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupButtons() {
        binding.btnFollow.setOnClickListener {
            toggleFollowStatus()
        }
        
        binding.btnChat.setOnClickListener {
            startChat()
        }
    }

    private fun toggleFollowStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = if (isFollowing) {
                    followRepository.unfollowUser(otherUserId)
                } else {
                    followRepository.followUser(otherUserId)
                }
                
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = {
                            isFollowing = !isFollowing
                            updateFollowButton()
                            val message = if (isFollowing) "Following" else "Unfollowed"
                            Toast.makeText(this@OtherProfileActivity, message, Toast.LENGTH_SHORT).show()
                            loadUserStats() // Reload stats
                        },
                        onFailure = { exception ->
                            Toast.makeText(this@OtherProfileActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OtherProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkFollowStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                isFollowing = followRepository.isFollowing(otherUserId)
                withContext(Dispatchers.Main) {
                    updateFollowButton()
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    private fun updateFollowButton() {
        if (isFollowing) {
            binding.btnFollow.text = "Unfollow"
            binding.btnFollow.setBackgroundColor(getColor(R.color.red))
        } else {
            binding.btnFollow.text = "Follow"
            binding.btnFollow.setBackgroundColor(getColor(R.color.purple_accent))
        }
    }

    private fun startChat() {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("otherUserId", otherUserId)
        startActivity(intent)
    }

    private fun loadUserStats() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Add a small delay to ensure database update is complete
                kotlinx.coroutines.delay(500)
                
                // Get counts from repository
                val followersCount = followRepository.getFollowersCount(otherUserId)
                val followingCount = followRepository.getFollowingCount(otherUserId)
                
                // Also refresh from Firestore document
                val userDoc = db.collection("users").document(otherUserId).get().await()
                val firestoreFollowersCount = userDoc.getLong("followersCount") ?: 0
                val firestoreFollowingCount = userDoc.getLong("followingCount") ?: 0
                
                withContext(Dispatchers.Main) {
                    // Use the higher count to ensure we show the most recent data
                    binding.tvFollowersCount.text = maxOf(followersCount, firestoreFollowersCount.toInt()).toString()
                    binding.tvFollowingCount.text = maxOf(followingCount, firestoreFollowingCount.toInt()).toString()
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    private fun loadOtherUserProfile(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.displayNameTextView.text = document.getString("username")
                    binding.bioTextView.text = document.getString("bio") ?: ""
                    
                    // Load user stats
                    binding.tvFollowersCount.text = document.getLong("followersCount")?.toString() ?: "0"
                    binding.tvFollowingCount.text = document.getLong("followingCount")?.toString() ?: "0"
                    binding.tvReadingCount.text = document.getLong("readingCount")?.toString() ?: "0"
                    
                    val photoUrl = document.getString("photoUrl")
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_user_profile)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                            .into(binding.profileImageView)
                    } else {
                        binding.profileImageView.setImageResource(R.drawable.ic_user_profile)
                    }
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
} 