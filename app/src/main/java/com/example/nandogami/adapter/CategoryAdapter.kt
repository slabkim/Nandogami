package com.example.nandogami.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.nandogami.R

class CategoryAdapter(
    private val items: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CatVH>() {

    inner class CatVH(view: View) : RecyclerView.ViewHolder(view) {
        val btnCategory: Button = view.findViewById(R.id.btnCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CatVH(view)
    }

    override fun onBindViewHolder(holder: CatVH, position: Int) {
        val category = items[position]
        holder.btnCategory.text = category
        holder.btnCategory.setOnClickListener {
            onClick(category)
        }
    }

    override fun getItemCount(): Int = items.size
}
