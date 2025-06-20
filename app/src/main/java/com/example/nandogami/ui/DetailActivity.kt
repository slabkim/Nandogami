package com.example.nandogami.ui.detail

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nandogami.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve data passed from HomeFragment
        val imageUrl = intent.getStringExtra("EXTRA_IMAGE_URL")

        // TODO: Load the image into your ImageView (e.g., using Glide or Coil)
        // Example with Glide:
        // Glide.with(this)
        //     .load(imageUrl)
        //     .into(binding.ivDetailImage)
    }
}
