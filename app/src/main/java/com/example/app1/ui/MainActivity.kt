package com.example.app1.ui

import android.os.Bundle
import android.widget.Button
import android.content.Intent
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
//import com.example.app1.JournalHome
//import com.example.app1.MyJournals
//import com.example.app1.NewJournal

import com.example.app1.R
import com.example.app1.SetReminder
import com.example.app1.UserProfile
import dagger.hilt.android.AndroidEntryPoint
import com.example.app1.viewmodel.UserViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvLogin: TextView = findViewById(R.id.linkText)
        val btnLogin: Button = findViewById(R.id.btnLogin)
        val emailEditText: EditText = findViewById(R.id.emailEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)

        btnLogin.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            performLogin(email, password)
        }

        tvLogin.setOnClickListener {
            val intent = Intent(this, Pantalla2::class.java)
            startActivity(intent)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        userViewModel.user.observe(this, Observer { user ->
            user?.let {
                // Actualiza la UI con los datos del usuario
                Toast.makeText(this, "Bienvenido, ${it.username}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun performLogin(email: String, password: String) {
        userViewModel.loginUser(email, password).observe(this, Observer { response ->
            response.body()?.let { user ->
                userViewModel.setUser(user)
            } ?: run {
                // Manejar el caso de error en el login
                Toast.makeText(this, "Error de autenticación", Toast.LENGTH_SHORT).show()
            }
        })
    }
}