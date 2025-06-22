package com.example.nandogami

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // Belum login, arahkan ke AuthActivity (login/register)
            startActivity(Intent(this, AuthActivity::class.java))
        } else {
            // Sudah login, arahkan ke MainActivity (home)
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}

