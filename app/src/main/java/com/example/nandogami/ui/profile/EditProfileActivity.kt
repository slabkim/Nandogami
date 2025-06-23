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
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.bumptech.glide.Glide
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedImageUri: Uri? = null
    private var uploadedPhotoUrl: String? = null
    private var isUploadingPhoto = false
    companion object {
        private const val PICK_IMAGE_REQUEST = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"

        // Set default image sebelum load data
        binding.profileImageView.setImageResource(R.drawable.ic_user_profile)

        // Set a loading state
        binding.displayNameTextView.text = "Loading..."
        binding.usernameTextView.text = ""
        
        loadUserData()
        setupBioCharacterCounter()

        // Tambahkan listener untuk memilih gambar
        binding.profileImageView.setOnClickListener { openImagePicker() }
        binding.cameraIcon.setOnClickListener { openImagePicker() }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            selectedImageUri?.let {
                Glide.with(this).load(it).into(binding.profileImageView)
                uploadToCloudinary(it)
            }
        }
    }

    private fun uploadToCloudinary(imageUri: Uri) {
        isUploadingPhoto = true
        runOnUiThread { invalidateOptionsMenu() }
        val cloudName = "dkcz4v94a"
        val uploadPreset = "android_unsigned"
        val url = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
        val filePath = getRealPathFromURI(imageUri)
        if (filePath == null) {
            Toast.makeText(this, "Gagal mendapatkan file gambar", Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(filePath)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, RequestBody.create("image/*".toMediaTypeOrNull(), file))
            .addFormDataPart("upload_preset", uploadPreset)
            .build()
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    isUploadingPhoto = false
                    invalidateOptionsMenu()
                    Toast.makeText(this@EditProfileActivity, "Upload gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    val imageUrl = json.getString("secure_url")
                    uploadedPhotoUrl = imageUrl
                    isUploadingPhoto = false
                    runOnUiThread {
                        Toast.makeText(this@EditProfileActivity, "Foto profil berhasil diupload", Toast.LENGTH_SHORT).show()
                        invalidateOptionsMenu()
                    }
                } else {
                    runOnUiThread {
                        isUploadingPhoto = false
                        invalidateOptionsMenu()
                        Toast.makeText(this@EditProfileActivity, "Upload gagal: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun getRealPathFromURI(contentUri: Uri): String? {
        var result: String? = null
        val cursor = contentResolver.query(contentUri, null, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                if (idx >= 0) result = cursor.getString(idx)
            }
            cursor.close()
        }
        if (result == null) {
            result = contentUri.path
        }
        return result
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
                        val photoUrl = document.getString("photoUrl")
                        if (!photoUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_user_profile)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                                .into(binding.profileImageView)
                            uploadedPhotoUrl = photoUrl
                        } else {
                            binding.profileImageView.setImageResource(R.drawable.ic_user_profile)
                        }
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
                        binding.profileImageView.setImageResource(R.drawable.ic_user_profile)
                    }
                }
                .addOnFailureListener { e ->
                    // Handle failure to load data
                    Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.displayNameTextView.text = "Error"
                    binding.usernameTextView.text = "Could not load profile"
                    binding.profileImageView.setImageResource(R.drawable.ic_user_profile)
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
        menu?.findItem(R.id.action_save)?.isEnabled = !isUploadingPhoto
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
        if (isUploadingPhoto) {
            Toast.makeText(this, "Tunggu upload foto selesai!", Toast.LENGTH_SHORT).show()
            return
        }
        val user = auth.currentUser
        if (user != null) {
            val displayName = binding.displayNameEditText.text.toString().trim()
            val username = binding.usernameEditText.text.toString().trim()
            val bio = binding.bioEditText.text.toString().trim()
            val photoUrl = uploadedPhotoUrl ?: ""
            Log.d("EditProfile", "photoUrl yang akan disimpan: $photoUrl")
            if (displayName.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Display Name and Username cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }
            val userProfile = hashMapOf(
                "username" to displayName,
                "handle" to username,
                "bio" to bio,
                "photoUrl" to photoUrl
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