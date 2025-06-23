package com.example.nandogami.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.nandogami.databinding.ActivityOtherProfileBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.example.nandogami.R

class OtherProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOtherProfileBinding
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtherProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        val userId = intent.getStringExtra("userId")
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadOtherUserProfile(userId)
    }

    private fun loadOtherUserProfile(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.displayNameTextView.text = document.getString("username")
                    binding.bioTextView.text = document.getString("bio") ?: ""
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