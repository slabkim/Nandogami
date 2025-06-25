package com.example.nandogami.ui.recommendation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.nandogami.R
import com.example.nandogami.databinding.DialogUserSelectionBinding
import com.example.nandogami.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class UserSelectionDialogFragment : DialogFragment() {
    private var _binding: DialogUserSelectionBinding? = null
    private val binding get() = _binding!!
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    var titleId: String = ""
    var onUserSelected: ((String, String) -> Unit)? = null

    companion object {
        fun newInstance(titleId: String, onUserSelected: (String, String) -> Unit): UserSelectionDialogFragment {
            return UserSelectionDialogFragment().apply {
                this.titleId = titleId
                this.onUserSelected = onUserSelected
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogUserSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        loadUsers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
        
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    performSearch()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUsers() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        // Load users that the current user is following
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val followingUsers = loadFollowingUsers(currentUserId)
                withContext(Dispatchers.Main) {
                    setupUsersRecyclerView(followingUsers)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error loading users: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun loadFollowingUsers(currentUserId: String): List<User> {
        return try {
            val followingDocs = db.collection("follows")
                .whereEqualTo("followerId", currentUserId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val followingIds = followingDocs.documents.mapNotNull { doc -> doc.getString("followingId") }
            
            if (followingIds.isEmpty()) {
                return emptyList()
            }

            val userDocs = db.collection("users")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), followingIds)
                .get()
                .await()

            userDocs.toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun setupUsersRecyclerView(users: List<User>) {
        // For now, we'll just show a simple list
        // In a real implementation, you'd create a proper adapter
        if (users.isEmpty()) {
            binding.tvNoUsers.text = "You're not following anyone yet.\nFollow some users to recommend manga to them!"
            binding.tvNoUsers.visibility = View.VISIBLE
            binding.rvUsers.visibility = View.GONE
        } else {
            binding.tvNoUsers.visibility = View.GONE
            binding.rvUsers.visibility = View.VISIBLE
            
            // TODO: Create proper UserAdapter
            // For now, we'll just show the first user as an example
            val firstUser = users.first()
            binding.tvExampleUser.text = "Example: ${firstUser.username}"
            binding.tvExampleUser.setOnClickListener {
                onUserSelected?.invoke(firstUser.username, "dummy_user_id")
                dismiss()
            }
        }
    }

    private fun performSearch() {
        val searchQuery = binding.etSearch.text.toString().trim()
        if (searchQuery.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a search term", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Implement user search
        Toast.makeText(requireContext(), "Search feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 