package com.example.app1.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.app1.R
import com.example.app1.viewmodel.JournalViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.activity_home) {
    private val journalViewModel: JournalViewModel by viewModels()

    // Initialize views and observe data here
}