package com.example.nandogami.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nandogami.databinding.ItemTitleBinding
import com.example.nandogami.model.Title

class TitleAdapter(private val list: List<Title>) :
    RecyclerView.Adapter<TitleAdapter.TitleViewHolder>() {

    class TitleViewHolder(val binding: ItemTitleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TitleViewHolder {
        val binding = ItemTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TitleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TitleViewHolder, position: Int) {
        val data = list[position]
        holder.binding.tvTitle.text = data.title
        holder.binding.tvAuthor.text = data.author
        Glide.with(holder.itemView.context)
            .load(data.imageUrl)
            .into(holder.binding.imgCover)
    }

    override fun getItemCount() = list.size
}
