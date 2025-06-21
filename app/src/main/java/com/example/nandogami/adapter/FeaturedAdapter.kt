package com.example.nandogami.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nandogami.R
import com.example.nandogami.model.Title

class FeaturedAdapter(
    private val items: List<Title>,
    private val onClick: (Title) -> Unit = {}   // default: no-op
) : RecyclerView.Adapter<FeaturedAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCover: ImageView     = itemView.findViewById(R.id.imgCover)
        val tvTitle: TextView       = itemView.findViewById(R.id.tvTitle)
        val tvFeatured: TextView    = itemView.findViewById(R.id.rvFeatured) // Label "Featured"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_title, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val title = items[position]
        holder.tvTitle.text       = title.title
        Glide.with(holder.imgCover.context)
            .load(title.imageUrl)
            .into(holder.imgCover)

        // Menampilkan label "Featured" jika isFeatured true
        holder.tvFeatured.visibility = if (title.isFeatured) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onClick(title) }
    }

    override fun getItemCount(): Int = items.size
}
