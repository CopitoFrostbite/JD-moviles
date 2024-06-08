package com.example.app1.ui

import android.os.Bundle
import com.example.app1.R

class JournalHome : NavigationDrawer() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_journal_home
    }
}
