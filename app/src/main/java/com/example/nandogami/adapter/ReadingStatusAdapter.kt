package com.example.nandogami.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nandogami.R
import com.example.nandogami.databinding.ItemReadingStatusBinding
import com.example.nandogami.model.ReadingStatus
import com.example.nandogami.model.ReadingStatusType
import com.example.nandogami.model.Title

class ReadingStatusAdapter(
    private var readingStatuses: List<ReadingStatus>,
    private val titles: MutableMap<String, Title>,
    private val onItemClick: (ReadingStatus) -> Unit,
    private val onStatusChange: (ReadingStatus, ReadingStatusType) -> Unit
) : RecyclerView.Adapter<ReadingStatusAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemReadingStatusBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReadingStatusBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val readingStatus = readingStatuses[position]
        val title = titles[readingStatus.titleId]
        
        title?.let {
            holder.binding.tvTitle.text = it.title
            holder.binding.tvAuthor.text = it.author
            holder.binding.tvStatus.text = getStatusText(readingStatus.status)
            holder.binding.tvProgress.text = "${readingStatus.currentChapter}/${readingStatus.totalChapters}"
            
            // Load image
            Glide.with(holder.binding.root.context)
                .load(it.imageUrl)
                .placeholder(R.drawable.sample)
                .into(holder.binding.ivCover)
            
            // Set status color
            holder.binding.tvStatus.setTextColor(getStatusColor(readingStatus.status, holder.binding.root.context))
            
            // Set click listeners
            holder.binding.root.setOnClickListener {
                onItemClick(readingStatus)
            }
            
            // Status change buttons
            holder.binding.btnReading.setOnClickListener {
                onStatusChange(readingStatus, ReadingStatusType.READING)
            }
            
            holder.binding.btnCompleted.setOnClickListener {
                onStatusChange(readingStatus, ReadingStatusType.COMPLETED)
            }
            
            holder.binding.btnPlanToRead.setOnClickListener {
                onStatusChange(readingStatus, ReadingStatusType.PLAN_TO_READ)
            }
            
            holder.binding.btnDropped.setOnClickListener {
                onStatusChange(readingStatus, ReadingStatusType.DROPPED)
            }
        }
    }

    override fun getItemCount() = readingStatuses.size

    fun updateData(newReadingStatuses: List<ReadingStatus>, newTitles: Map<String, Title>) {
        readingStatuses = newReadingStatuses
        titles.clear()
        titles.putAll(newTitles)
        notifyDataSetChanged()
    }

    private fun getStatusText(status: ReadingStatusType): String {
        return when (status) {
            ReadingStatusType.PLAN_TO_READ -> "Plan to Read"
            ReadingStatusType.READING -> "Reading"
            ReadingStatusType.COMPLETED -> "Completed"
            ReadingStatusType.DROPPED -> "Dropped"
            ReadingStatusType.ON_HOLD -> "On Hold"
        }
    }

    private fun getStatusColor(status: ReadingStatusType, context: Context): Int {
        return when (status) {
            ReadingStatusType.PLAN_TO_READ -> context.getColor(R.color.blue)
            ReadingStatusType.READING -> context.getColor(R.color.green)
            ReadingStatusType.COMPLETED -> context.getColor(R.color.purple)
            ReadingStatusType.DROPPED -> context.getColor(R.color.red)
            ReadingStatusType.ON_HOLD -> context.getColor(R.color.orange)
        }
    }
} 