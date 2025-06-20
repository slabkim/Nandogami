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
import com.google.firebase.firestore.FirebaseFirestore

class TitleAdapter(
    private val items: List<Title>,
    private val onClick: (Title) -> Unit = {}   // default: no-op
) : RecyclerView.Adapter<TitleAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCover: ImageView     = itemView.findViewById(R.id.imgCover)
        val tvTitle: TextView       = itemView.findViewById(R.id.tvTitle)
        val tvAuthor: TextView      = itemView.findViewById(R.id.tvAuthor)
        val tvTypeBadge: TextView   = itemView.findViewById(R.id.tvTypeBadge)
        val ratingBar: RatingBar    = itemView.findViewById(R.id.ratingBar)
        val tvRatingValue: TextView = itemView.findViewById(R.id.tvRatingValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_title, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val title = items[position]
        holder.tvTitle.text       = title.title
        holder.tvAuthor.text      = title.author
        holder.tvTypeBadge.text   = title.type
        holder.ratingBar.rating   = title.rating
        holder.tvRatingValue.text = String.format("%.1f", title.rating)

        Glide.with(holder.imgCover.context)
            .load(title.imageUrl)
            .into(holder.imgCover)

        holder.itemView.setOnClickListener { onClick(title) }
    }

    override fun getItemCount(): Int = items.size
}
