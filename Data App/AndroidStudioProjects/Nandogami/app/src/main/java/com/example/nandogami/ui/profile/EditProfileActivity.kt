package com.example.nandogami.ui.profile

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.core.content.ContextCompat
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
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.io.FileOutputStream
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import androidx.activity.result.contract.ActivityResultContracts
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.InputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedImageUri: Uri? = null
    private var uploadedPhotoUrl: String? = null
    private var isUploadingPhoto = false

    // Untuk referensi genre chip
    private val genreChipList = mutableListOf<com.google.android.material.chip.Chip>()

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
        binding.displayNameTextView.text = "Loading..."
        binding.usernameTextView.text = ""

        loadUserData()
        setupBioCharacterCounter()

        binding.profileImageView.setOnClickListener { openImagePicker() }
        binding.cameraIcon.setOnClickListener { openImagePicker() }

        // ==== TAB SWITCH LOGIC ====
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                binding.layoutBasicInfo.visibility = if (tab.position == 0) android.view.View.VISIBLE else android.view.View.GONE
                binding.layoutPreferences.visibility = if (tab.position == 1) android.view.View.VISIBLE else android.view.View.GONE
                binding.layoutPrivacy.visibility = if (tab.position == 2) android.view.View.VISIBLE else android.view.View.GONE
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // ==== FAVORITE GENRES 2 KOLOM ====
        setupGenreChips2Columns()
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data != null && data.data != null) {
                selectedImageUri = data.data
                selectedImageUri?.let {
                    Glide.with(this).load(it).into(binding.profileImageView)
                    uploadToCloudinary(it) // Your existing function
                }
            }
        }
    }

    private fun setupGenreChips2Columns() {
        val genres = listOf(
            "Action", "Adventure", "Comedy", "Drama",
            "Fantasy", "Horror", "Isekai", "Romance",
            "Sci-Fi", "Slice of Life", "Shounen", "Shoujo",
            "Seinen", "Josei", "Webtoon", "Martial Arts",
            "School Life", "Supernatural"
        )
        val container = binding.genreContainer
        container.removeAllViews()
        genreChipList.clear()
        val chipMargin = 8 // dp

        fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

        for (i in genres.indices step 2) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(0)
                    bottomMargin = dpToPx(12) // Atur jarak antar baris di sini!
                }
            }

            // Chip 1
            val chip1 = createGenreChip(genres[i])
            chip1.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dpToPx(chipMargin / 2)
            }
            row.addView(chip1)
            genreChipList.add(chip1)

            // Chip 2 (kalau ada)
            if (i + 1 < genres.size) {
                val chip2 = createGenreChip(genres[i + 1])
                chip2.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dpToPx(chipMargin / 2)
                }
                row.addView(chip2)
                genreChipList.add(chip2)
            } else {
                val space = Space(this)
                space.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                row.addView(space)
            }
            container.addView(row)
        }

        // Set listener ke semua chip untuk update count
        for (chip in genreChipList) {
            chip.setOnCheckedChangeListener { _, _ -> updateGenreCount() }
        }
        updateGenreCount()
    }

    private fun createGenreChip(text: String): com.google.android.material.chip.Chip {
        return com.google.android.material.chip.Chip(this).apply {
            this.text = text
            isCheckable = true
            isCheckedIconVisible = false
            setTextColor(ContextCompat.getColor(context, R.color.white))
            chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.chip_selector)
            textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
            textSize = 15f
            val cornerRadius = resources.getDimension(R.dimen.chip_radius)
            this.shapeAppearanceModel = com.google.android.material.shape.ShapeAppearanceModel.builder()
                .setAllCorners(com.google.android.material.shape.CornerFamily.ROUNDED, cornerRadius)
                .build()
            minWidth = 0
        }
    }

    private fun updateGenreCount() {
        val checkedCount = genreChipList.count { it.isChecked }
        binding.selectedGenreCount.text = "Selected: $checkedCount genres"
    }

    // ---- FUNGSI LAINNYA TIDAK BERUBAH ----

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent) // Use the new launcher
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
        runOnUiThread { invalidateOptionsMenu() } // Pastikan ini aman untuk UI thread

        val cloudName = "dkcz4v94a"
        val uploadPreset = "android_unsigned"
        val url = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

        // --- Salin file ke cache dan gunakan path dari cache ---
        val fileToUpload: File? = try {
            val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Log.e("EditProfileActivity", "Gagal membuka input stream dari Uri.")
                runOnUiThread {
                    Toast.makeText(this, "Gagal mendapatkan file gambar (stream null)", Toast.LENGTH_SHORT).show()
                    isUploadingPhoto = false
                    invalidateOptionsMenu()
                }
                return
            }
            // Buat nama file unik atau gunakan yang sudah ada jika memungkinkan
            val originalFileName = getFileName(imageUri) ?: "upload_image_${System.currentTimeMillis()}"
            val tempFile = File(cacheDir, originalFileName) // Gunakan cacheDir
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            tempFile
        } catch (e: IOException) {
            Log.e("EditProfileActivity", "Gagal menyalin file ke cache", e)
            runOnUiThread {
                Toast.makeText(this, "Gagal memproses file gambar", Toast.LENGTH_SHORT).show()
                isUploadingPhoto = false
                invalidateOptionsMenu()
            }
            null
        }

        if (fileToUpload == null) {
            Log.e("EditProfileActivity", "File yang akan diunggah null setelah mencoba menyalin.")
            // Pesan error sudah ditampilkan di dalam try-catch
            return
        }
        // --- End of copy file to cache ---


        Log.d("EditProfileActivity", "Mengunggah file: ${fileToUpload.absolutePath}, Ukuran: ${fileToUpload.length()} bytes")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileToUpload.name, fileToUpload.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("EditProfileActivity", "Upload gagal ke Cloudinary", e)
                runOnUiThread {
                    isUploadingPhoto = false
                    invalidateOptionsMenu()
                    Toast.makeText(this@EditProfileActivity, "Upload gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBodyString = response.body?.string() // Baca body sekali saja
                Log.d("EditProfileActivity", "Respon Cloudinary: ${response.code} - $responseBodyString")
                if (response.isSuccessful && responseBodyString != null) {
                    try {
                        val json = JSONObject(responseBodyString)
                        val imageUrl = json.getString("secure_url")
                        uploadedPhotoUrl = imageUrl
                        Log.d("EditProfileActivity", "URL Gambar Terunggah: $imageUrl")
                        runOnUiThread {
                            Toast.makeText(this@EditProfileActivity, "Foto profil berhasil diupload", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: JSONException) {
                        Log.e("EditProfileActivity", "Error parsing JSON dari Cloudinary", e)
                        runOnUiThread {
                            Toast.makeText(this@EditProfileActivity, "Upload berhasil tapi gagal memproses respon", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("EditProfileActivity", "Upload ke Cloudinary tidak berhasil. Kode: ${response.code}, Pesan: ${response.message}, Body: $responseBodyString")
                    runOnUiThread {
                        val errorHeader = response.header("X-Cld-Error") // [1]
                        val errorMessage = errorHeader ?: "Upload gagal (server): ${response.message}"
                        Toast.makeText(this@EditProfileActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
                runOnUiThread {
                    isUploadingPhoto = false
                    invalidateOptionsMenu()
                }
                response.body?.close() // Pastikan body ditutup
            }
        })
    }

    // Helper function untuk mendapatkan nama file dari Uri (opsional tapi bagus)
    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) { // Tambahkan null check untuk cut
                result = result.substring(cut + 1)
            }
        }
        return result?.replace("[^a-zA-Z0-9._-]".toRegex(), "_") // Sanitize nama file
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
