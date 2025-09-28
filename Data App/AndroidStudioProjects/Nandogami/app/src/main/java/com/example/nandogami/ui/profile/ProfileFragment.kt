package com.example.nandogami.ui.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.nandogami.AuthActivity
import com.example.nandogami.R
import com.example.nandogami.adapter.TitleAdapter
import com.example.nandogami.databinding.FragmentProfileBinding
import com.example.nandogami.model.Title
import com.example.nandogami.ui.detail.DetailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var favoritesAdapter: TitleAdapter
    private val favoriteTitles = mutableListOf<Title>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Set default image sebelum load data
        binding.profileImage.setImageResource(R.drawable.ic_user_profile)

        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(activity, EditProfileActivity::class.java)
            startActivity(intent)
        }

        setupFavoritesRecyclerView()
        setupLogoutButton2()

        return root
    }

    override fun onResume() {
        super.onResume()
        // Muat ulang data setiap kali fragment ditampilkan
        loadUserProfile()
        loadFavoriteTitles()
    }

    private fun setupFavoritesRecyclerView() {
        favoritesAdapter = TitleAdapter(emptyList()) { title ->
            val intent = Intent(requireContext(), DetailActivity::class.java).apply {
                putExtra("titleId", title.id)
            }
            startActivity(intent)
        }
        binding.rvFavorites.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = favoritesAdapter
        }
    }

    private fun loadFavoriteTitles() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("ProfileFragment", "User not logged in, cannot load favorites.")
            binding.tvFavoritesCount.text = "0" // Set count ke 0 jika user tidak login
            return
        }

        db.collection("favorites")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { favoriteDocs ->
                // **UPDATE JUMLAH FAVORIT DI SINI**
                binding.tvFavoritesCount.text = favoriteDocs.size().toString()

                if (favoriteDocs.isEmpty) {
                    // Jika tidak ada favorit, kosongkan RecyclerView
                    favoritesAdapter.updateData(emptyList())
                    return@addOnSuccessListener
                }

                val titleIds = favoriteDocs.mapNotNull { it.getString("titleId") }

                if (titleIds.isNotEmpty()) {
                    fetchTitlesByIds(titleIds)
                } else {
                    favoritesAdapter.updateData(emptyList())
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error getting favorite documents", e)
                binding.tvFavoritesCount.text = "0" // Set ke 0 jika terjadi error
            }
    }

    private fun fetchTitlesByIds(titleIds: List<String>) {
        db.collection("titles").whereIn(FieldPath.documentId(), titleIds)
            .get()
            .addOnSuccessListener { titleDocuments ->
                val fetchedTitles = titleDocuments.mapNotNull { doc ->
                    doc.toObject(Title::class.java).apply { id = doc.id }
                }
                val orderedTitles = titleIds.mapNotNull { id ->
                    fetchedTitles.find { it.id == id }
                }
                favoritesAdapter.updateData(orderedTitles)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error fetching title details by IDs", e)
            }
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val email = auth.currentUser?.email
                        val usernameFromEmail = email?.substringBefore('@')?.replaceFirstChar { it.uppercase() } ?: "user"
                        val handleFromEmail = "@${usernameFromEmail.lowercase()}"
                        binding.username.text = document.getString("username") ?: usernameFromEmail
                        val handle = document.getString("handle")
                        binding.userhandle.text = if (!handle.isNullOrEmpty()) "@${handle.lowercase()}" else handleFromEmail
                        val photoUrl = document.getString("photoUrl")
                        if (!photoUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_user_profile)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                                .into(binding.profileImage)
                        } else {
                            binding.profileImage.setImageResource(R.drawable.ic_user_profile)
                        }
                    } else {
                        // Jika dokumen pengguna tidak ada, gunakan informasi dari email
                        val email = auth.currentUser?.email
                        val usernameFromEmail = email?.substringBefore('@')?.replaceFirstChar { it.uppercase() } ?: "user"
                        val handleFromEmail = "@${usernameFromEmail.lowercase()}"
                        binding.username.text = usernameFromEmail
                        binding.userhandle.text = handleFromEmail
                        binding.profileImage.setImageResource(R.drawable.ic_user_profile)
                    }
                }
        }
    }
    private fun setupLogoutButton2() {
        binding.btnLogout2.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}