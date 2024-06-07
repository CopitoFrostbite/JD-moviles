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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }
    }
}