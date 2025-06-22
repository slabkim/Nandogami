package com.example.nandogami.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nandogami.R
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import com.example.nandogami.MainActivity
class LoginFragment : Fragment(R.layout.fragment_login) {
    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnSignupTab = view.findViewById<Button>(R.id.btnSignupTab)

        // Navigasi ke RegisterFragment
        btnSignupTab.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(requireContext(), "Isi semua field!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Proses login Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // TODO: Redirect ke home/dashboard, misal:
                        Toast.makeText(requireContext(), "Login sukses!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                        requireActivity().finish()
                        // findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    } else {
                        Toast.makeText(requireContext(), "Login gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
