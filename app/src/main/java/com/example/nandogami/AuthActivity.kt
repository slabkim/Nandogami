package com.example.nandogami

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        // FragmentContainerView akan menampilkan nav_auth.xml (login/register)
    }
}
