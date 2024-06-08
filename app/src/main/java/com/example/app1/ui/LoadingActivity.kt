package com.example.app1.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.app1.R
import com.example.app1.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoadingActivity : AppCompatActivity() {

    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val welcomeTextView: TextView = findViewById(R.id.welcomeTextView)

        // Observa los datos del usuario
        userViewModel.getCurrentUser().observe(this, Observer { user ->
            if (user != null) {
                // Actualiza el TextView con el nombre del usuario
                welcomeTextView.text = getString(R.string.welcome_user, user.username)
            } else {
                welcomeTextView.text = getString(R.string.welcome_guest)
            }
        })

        // Simula una carga de datos
        Handler().postDelayed({
            startActivity(Intent(this, Home::class.java))
            finish()
        }, 3000)
    }
}