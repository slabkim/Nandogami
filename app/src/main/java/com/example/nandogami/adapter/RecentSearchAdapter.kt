package com.example.nandogami.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nandogami.R

class RecentSearchAdapter(
    private var searches: List<String>,
    private val onRemoveClick: (String) -> Unit
) : RecyclerView.Adapter<RecentSearchAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recentSearchText: TextView = itemView.findViewById(R.id.tvRecentSearch)
        val removeButton: ImageView = itemView.findViewById(R.id.ivRemoveRecent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val searchTerm = searches[position]
        holder.recentSearchText.text = searchTerm
        holder.removeButton.setOnClickListener {
            onRemoveClick(searchTerm)
        }
    }

    override fun getItemCount(): Int = searches.size

    fun updateData(newSearches: List<String>) {
        searches = newSearches
        notifyDataSetChanged()
    }
} 