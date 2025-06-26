package com.example.nandogami.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nandogami.data.ChatRealtimeRepository // Ganti dengan repositori Anda jika berbeda
import com.example.nandogami.model.ChatHistory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ChatHistoryViewModel : ViewModel() {

    private val repository = ChatRealtimeRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _chatHistory = MutableLiveData<List<ChatHistory>>()
    val chatHistory: LiveData<List<ChatHistory>> = _chatHistory
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun fetchChatHistory() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _chatHistory.value = emptyList() // User tidak login
            _isLoading.value = false
            Log.d("ChatHistoryViewModel", "User not logged in, fetching empty history.")
            return
        }

        _isLoading.value = true // Set loading state
        _errorMessage.value = null // Clear previous errors

        viewModelScope.launch {
            Log.d("ChatHistoryViewModel", "Fetching chat history for user: $userId")
            // Call the repository method which now returns Result
            val result: Result<List<ChatHistory>> = repository.getChatHistoryForUser(userId)

            _isLoading.postValue(false) // Set loading state to false after completion

            // Handle the Result
            result.fold(
                onSuccess = { historyList ->
                    Log.d("ChatHistoryViewModel", "Fetched history successfully: $historyList")
                    _chatHistory.postValue(historyList)
                },
                onFailure = { exception ->
                    Log.e("ChatHistoryViewModel", "Error fetching chat history", exception)
                    _errorMessage.postValue(exception.message ?: "An unknown error occurred")
                    _chatHistory.postValue(emptyList()) // Post empty list on error or handle as needed
                }
            )
        }
    }
}