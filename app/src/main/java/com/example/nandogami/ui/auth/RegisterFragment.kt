package com.example.nandogami.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nandogami.R
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import com.example.nandogami.MainActivity
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment(R.layout.fragment_register) {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnSignup = view.findViewById<Button>(R.id.btnSignup)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val etUsername = view.findViewById<EditText>(R.id.etUsername)
        val cbTerms = view.findViewById<CheckBox>(R.id.cbTerms)
        val btnLoginTab = view.findViewById<Button>(R.id.btnLoginTab)

        // Navigasi ke LoginFragment
        btnLoginTab.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        btnSignup.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val username = etUsername.text.toString().trim()

            if (email.isBlank() || password.isBlank() || username.isBlank()) {
                Toast.makeText(requireContext(), "Isi semua field!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!cbTerms.isChecked) {
                Toast.makeText(requireContext(), "Setujui Terms dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 8) {
                Toast.makeText(requireContext(), "Password minimal 8 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Proses register Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        if (firebaseUser != null) {
                            val userProfile = hashMapOf(
                                "username" to username,
                                "handle" to username.lowercase().replace(" ", ""),
                                "bio" to "",
                                "email" to email
                            )
                            db.collection("users").document(firebaseUser.uid)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "Register sukses!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(requireContext(), MainActivity::class.java))
                                    requireActivity().finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Gagal menyimpan profil: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Register gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
