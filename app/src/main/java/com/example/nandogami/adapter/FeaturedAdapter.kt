package com.example.nandogami.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nandogami.R
import com.example.nandogami.model.Title

class FeaturedAdapter(
    private val items: List<Title>,
    private val onClick: (Title) -> Unit = {}
) : RecyclerView.Adapter<FeaturedAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgFeatured)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_featured, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val title = items[position]
        Glide.with(holder.img.context)
            .load(title.imageUrl)
            .into(holder.img)

        holder.itemView.setOnClickListener { onClick(title) }
    }

    override fun getItemCount(): Int = items.size
}
