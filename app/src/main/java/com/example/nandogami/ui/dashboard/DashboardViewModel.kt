package com.example.nandogami.ui.dashboard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.nandogami.data.TitleRepository
import com.example.nandogami.model.Title
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TitleRepository()
    private val sharedPreferences = application.getSharedPreferences("NandogamiPrefs", Context.MODE_PRIVATE)

    private val _searchResults = MutableLiveData<List<Title>>()
    val searchResults: LiveData<List<Title>> = _searchResults

    private val _recentSearches = MutableLiveData<List<String>>()
    val recentSearches: LiveData<List<String>> = _recentSearches

    val popularSearches = MutableLiveData<List<String>>().apply {
        value = listOf("Solo Leveling", "One Piece", "Jujutsu Kaisen", "Chainsaw Man", "Tower of God", "Fantasy")
    }

    init {
        loadRecentSearches()
    }

    fun search(query: String) {
        viewModelScope.launch {
            val results = repository.searchTitles(query)
            _searchResults.postValue(results)
            addRecentSearch(query)
        }
    }

    private fun loadRecentSearches() {
        val searches = sharedPreferences.getStringSet("recent_searches", emptySet()) ?: emptySet()
        _recentSearches.value = searches.toList().sorted() // Keep a consistent order
    }

    private fun saveRecentSearches(searches: List<String>) {
        sharedPreferences.edit().putStringSet("recent_searches", searches.toSet()).apply()
    }

    fun addRecentSearch(query: String) {
        val currentList = _recentSearches.value?.toMutableList() ?: mutableListOf()
        if (!query.isBlank() && !currentList.contains(query)) {
            currentList.add(0, query)
            if (currentList.size > 5) {
                currentList.removeAt(currentList.size - 1)
            }
            _recentSearches.value = currentList
            saveRecentSearches(currentList)
        }
    }

    fun removeRecentSearch(query: String) {
        val currentList = _recentSearches.value?.toMutableList() ?: mutableListOf()
        currentList.remove(query)
        _recentSearches.value = currentList
        saveRecentSearches(currentList)
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        saveRecentSearches(emptyList())
    }
}