package com.example.app1.ui

import android.os.Bundle
import android.widget.Button
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

//import com.example.app1.JournalHome
//import com.example.app1.MyJournals
//import com.example.app1.NewJournal

import com.example.app1.R
import com.example.app1.SetReminder
import com.example.app1.UserProfile
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvLogin: TextView = findViewById(R.id.linkText)

        val btnLogin: Button = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val intent: Intent = Intent(this, SetReminder::class.java)
            startActivity(intent)
        }


        tvLogin.setOnClickListener{
            val intent: Intent = Intent(this, Pantalla2::class.java)
            startActivity(intent)
        }
    }
}