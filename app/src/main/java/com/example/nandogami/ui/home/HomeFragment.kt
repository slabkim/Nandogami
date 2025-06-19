package com.example.nandogami.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nandogami.AuthActivity
import com.example.nandogami.adapter.TitleAdapter
import com.example.nandogami.databinding.FragmentHomeBinding
import com.example.nandogami.model.Title
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var adapter: TitleAdapter? = null
    private val titleList = mutableListOf<Title>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi RecyclerView
        adapter = TitleAdapter(titleList)
        binding.rvTitles.adapter = adapter
        binding.rvTitles.layoutManager = LinearLayoutManager(requireContext())

        // Event klik Logout
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        fetchTitlesFromFirestore()
    }

    private fun fetchTitlesFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection("titles")
            .get()
            .addOnSuccessListener { snapshot ->
                val allTitles = snapshot.documents.mapNotNull { it.toObject(Title::class.java) }
                titleList.clear()
                titleList.addAll(allTitles)
                adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // TODO: tampilkan pesan error jika ingin
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
