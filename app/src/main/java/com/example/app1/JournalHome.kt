package com.example.app1

import android.os.Bundle

class JournalHome : NavigationDrawer() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Aquí no es necesario llamar a setContentView, se manejará en NavigationDrawer
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_journal_home
    }
}
