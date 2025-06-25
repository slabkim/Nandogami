package com.example.nandogami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nandogami.databinding.ItemFeaturedBinding // Pastikan import ini benar
import com.example.nandogami.model.Title

class FeaturedAdapter(
    private val items: List<Title>,
    private val onClick: (Title) -> Unit = {}
) : RecyclerView.Adapter<FeaturedAdapter.VH>() {

    inner class VH(private val binding: ItemFeaturedBinding) : RecyclerView.ViewHolder(binding.root) {
        // Fungsi bind sekarang juga mengatur teks untuk judul dan badge
        fun bind(title: Title) {
            // Mengatur gambar
            Glide.with(binding.imgFeatured.context)
                .load(title.imageUrl)
                .into(binding.imgFeatured)

            // =================== KODE BARU DITAMBAHKAN DI SINI ===================
            // Mengatur teks untuk judul
            binding.tvTitle.text = title.title

            // Mengatur teks untuk badge
            binding.tvTypeBadge.text = title.type
            // ====================================================================

            // Mengatur listener klik
            binding.root.setOnClickListener { onClick(title) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFeaturedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}