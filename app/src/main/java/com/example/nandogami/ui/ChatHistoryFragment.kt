package com.example.nandogami.ui

import android.content.Intent
import android.util.Log
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nandogami.adapter.ChatHistoryAdapter
import com.example.nandogami.databinding.FragmentChatHistoryBinding
import com.example.nandogami.ui.chat.ChatActivity
import com.example.nandogami.viewmodel.ChatHistoryViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class ChatHistoryFragment : Fragment() {
    private var _binding: FragmentChatHistoryBinding? = null
    private val binding get() = _binding!!

    // Inisialisasi ViewModel
    private val viewModel: ChatHistoryViewModel by viewModels()

    private lateinit var chatHistoryAdapter: ChatHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // 2. Setup Adapter dan RecyclerView SEJAK AWAL
        setupRecyclerView()

        // 3. Observe data dari ViewModel
        observeChatHistory()

        // 4. Minta ViewModel untuk mengambil data
        viewModel.fetchChatHistory()
    }


    private fun setupRecyclerView() {
        chatHistoryAdapter = ChatHistoryAdapter { chatHistory ->
            // Aksi saat item riwayat obrolan di-klik
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("otherUserId", chatHistory.otherUserId)
            }
            startActivity(intent)
        }
        binding.rvChatHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatHistoryAdapter // <-- Adapter langsung dipasang di sini
        }
    }

    private fun observeChatHistory() {
        viewModel.chatHistory.observe(viewLifecycleOwner) { historyList ->
            Log.d("ChatHistoryFragment", "Observed chat history: $historyList")
            // Saat data datang, perbarui adapter
            chatHistoryAdapter.submitList(historyList)
        }
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = activity?.findViewById<BottomNavigationView>(com.example.nandogami.R.id.bottomNavigationView)
        bottomNav?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        val bottomNav = activity?.findViewById<BottomNavigationView>(com.example.nandogami.R.id.bottomNavigationView)
        bottomNav?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}