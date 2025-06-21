package com.example.nandogami.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.nandogami.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(ProfileViewModel::class.java)

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val email = currentUser.email
            val usernameRaw = email?.substringBefore("@") ?: "User"

            val usernameCapital = usernameRaw.replaceFirstChar { it.uppercaseChar() }

            binding.username.text = usernameCapital            // Akmal
            binding.userhandle.text = "@$usernameRaw"          // @akmal
        }

        return root
    }



    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid

        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username")
                        val handle = document.getString("handle")

                        binding.username.text = username ?: "Tidak ditemukan"
                        binding.userhandle.text = handle ?: "@handle"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileFragment", "Gagal ambil profil", e)
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
