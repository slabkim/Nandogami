package com.example.nandogami.ui.profile

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.nandogami.R
import com.example.nandogami.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"

        // Set a loading state
        binding.displayNameTextView.text = "Loading..."
        binding.usernameTextView.text = ""
        
        loadUserData()
        setupBioCharacterCounter()
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            binding.emailEditText.setText(user.email)
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        binding.displayNameEditText.setText(document.getString("username"))
                        binding.usernameEditText.setText(document.getString("handle"))
                        binding.bioEditText.setText(document.getString("bio"))

                        binding.displayNameTextView.text = document.getString("username")
                        binding.usernameTextView.text = "@${document.getString("handle")}"
                    } else {
                        // Document doesn't exist, create a default view for a new user
                        val email = user.email ?: ""
                        val defaultUsername = email.substringBefore('@').replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
                        val defaultHandle = email.substringBefore('@')

                        binding.displayNameEditText.setText(defaultUsername)
                        binding.usernameEditText.setText(defaultHandle)
                        binding.displayNameTextView.text = defaultUsername
                        binding.usernameTextView.text = "@$defaultHandle"
                        binding.bioEditText.setText("") // Ensure bio is empty
                    }
                }
                .addOnFailureListener { e ->
                    // Handle failure to load data
                    Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.displayNameTextView.text = "Error"
                    binding.usernameTextView.text = "Could not load profile"
                }
        }
    }

    private fun setupBioCharacterCounter() {
        binding.bioEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                binding.bioCharCounter.text = "$length/150 characters"
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit_profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save -> {
                saveUserProfile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            val displayName = binding.displayNameEditText.text.toString().trim()
            val username = binding.usernameEditText.text.toString().trim()
            val bio = binding.bioEditText.text.toString().trim()

            if (displayName.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Display Name and Username cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }

            val userProfile = hashMapOf(
                "username" to displayName,
                "handle" to username,
                "bio" to bio
            )

            db.collection("users").document(user.uid).set(userProfile, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
} 