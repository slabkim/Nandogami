package com.example.nandogami.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nandogami.R
import com.example.nandogami.network.GiphyApiService
import com.example.nandogami.network.GifData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GifSearchDialogFragment(val onGifSelected: (String) -> Unit) : BottomSheetDialogFragment() {
    private lateinit var gifAdapter: GifAdapter
    private var searchJob: Job? = null
    private val apiKey = "p6voZ9nHm0dUKU1umv6F9TlC263MDUQy"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_gif_search, container, false)
        val etSearch = view.findViewById<EditText>(R.id.etGifSearch)
        val rvResults = view.findViewById<RecyclerView>(R.id.rvGifResults)
        gifAdapter = GifAdapter { url ->
            onGifSelected(url)
            dismiss()
        }
        rvResults.layoutManager = GridLayoutManager(context, 2)
        rvResults.adapter = gifAdapter
        etSearch.addTextChangedListener(object : TextWatcher {
            private var searchJob: Job? = null
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                val query = s?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    searchJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(400) // debounce
                        searchGif(query)
                    }
                } else {
                    gifAdapter.submitList(emptyList())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        return view
    }

    private fun searchGif(query: String) {
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.IO).launch {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.giphy.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(OkHttpClient())
                .build()
            val api = retrofit.create(GiphyApiService::class.java)
            try {
                val response = api.searchGifs(apiKey, query)
                withContext(Dispatchers.Main) {
                    gifAdapter.submitList(response.data)
                }
            } catch (e: Exception) {
                // Handle error (bisa tampilkan Toast)
            }
        }
    }

    class GifAdapter(val onClick: (String) -> Unit) : RecyclerView.Adapter<GifViewHolder>() {
        private val items = mutableListOf<GifData>()
        fun submitList(newItems: List<GifData>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GifViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gif_result, parent, false)
            return GifViewHolder(view, onClick)
        }
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: GifViewHolder, position: Int) {
            holder.bind(items[position])
        }
    }

    class GifViewHolder(itemView: View, val onClick: (String) -> Unit) : RecyclerView.ViewHolder(itemView) {
        fun bind(data: GifData) {
            val iv = itemView.findViewById<android.widget.ImageView>(R.id.ivGifResult)
            Glide.with(itemView.context).asGif().load(data.images.fixed_height.url).into(iv)
            itemView.setOnClickListener { onClick(data.images.fixed_height.url) }
        }
    }
} 