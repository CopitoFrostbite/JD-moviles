package com.example.app1.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.app1.R
import com.example.app1.data.model.User
import com.example.app1.viewmodel.UserViewModel

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userViewModel.getCurrentUser().observe(this, Observer { user ->
            if (user != null) {
                // Usuario encontrado, iniciar sesión automáticamente y transicionar a NewJournal
                startActivity(Intent(this, NewJournal::class.java))
                finish()
            } else {
                // No hay usuario, mostrar LoginFragment
                if (savedInstanceState == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LoginFragment())
                        .commit()
                }
            }
        })
    }
}