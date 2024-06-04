package com.example.app1.ui

import android.os.Bundle
import android.widget.Button
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.app1.Pantalla2
import android.widget.TextView
import com.example.app1.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvLogin: TextView = findViewById(R.id.linkText)

        tvLogin.setOnClickListener{
            val intent: Intent = Intent(this, Pantalla2::class.java)
            startActivity(intent)
        }
    }
}