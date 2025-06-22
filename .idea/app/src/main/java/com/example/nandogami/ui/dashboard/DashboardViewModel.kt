package com.example.nandogami.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nandogami.data.TitleRepository
import com.example.nandogami.model.Title
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val repository = TitleRepository()

    private val _searchResults = MutableLiveData<List<Title>>()
    val searchResults: LiveData<List<Title>> = _searchResults

    private val _recentSearches = MutableLiveData<List<String>>().apply {
        value = listOf("Solo Leveling", "Jujutsu Kaisen", "Action")
    }
    val recentSearches: LiveData<List<String>> = _recentSearches

    val popularSearches = MutableLiveData<List<String>>().apply {
        value = listOf("Solo Leveling", "One Piece", "Jujutsu Kaisen", "Chainsaw Man", "Tower of God", "Fantasy")
    }

    fun search(query: String) {
        viewModelScope.launch {
            val results = repository.searchTitles(query)
            _searchResults.postValue(results)
            addRecentSearch(query)
        }
    }

    fun addRecentSearch(query: String) {
        val currentList = _recentSearches.value?.toMutableList() ?: mutableListOf()
        if (!currentList.contains(query)) {
            currentList.add(0, query)
            // Keep a fixed number of recent searches
            if (currentList.size > 5) {
                currentList.removeAt(currentList.size - 1)
            }
            _recentSearches.postValue(currentList)
        }
    }

    fun removeRecentSearch(query: String) {
        val currentList = _recentSearches.value?.toMutableList() ?: mutableListOf()
        currentList.remove(query)
        _recentSearches.postValue(currentList)
    }

    fun clearRecentSearches() {
        _recentSearches.postValue(emptyList())
    }
}