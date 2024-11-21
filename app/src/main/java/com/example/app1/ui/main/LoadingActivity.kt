package com.example.app1.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app1.R
import com.example.app1.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoadingActivity : AppCompatActivity() {

    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val welcomeTextView: TextView = findViewById(R.id.welcomeTextView)

        // Observa los datos del usuario
        userViewModel.getCurrentUser().observe(this) { user ->
            welcomeTextView.text = when {
                user != null -> getString(R.string.welcome_user, user.username)
                else -> getString(R.string.welcome_guest)
            }
        }

        // Simula una carga de datos
        lifecycleScope.launch {
            delay(3000)
            startActivity(Intent(this@LoadingActivity, Home::class.java))
            finish()
        }
    }
}