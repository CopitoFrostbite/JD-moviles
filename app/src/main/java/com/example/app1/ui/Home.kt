package com.example.app1.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import com.example.app1.R
import com.example.app1.viewmodel.UserViewModel
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Home : AppCompatActivity() {

    private lateinit var drawer: DrawerLayout
    private lateinit var navigationView: NavigationView
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        drawer = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        userViewModel.getCurrentUser().observe(this, Observer { user ->
            if (user != null) {
                // Actualiza el TextView con el nombre del usuario
                val headerView = navigationView.getHeaderView(0)

            }
        })

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, MyJournalsFragment())
            }
        }

        setupDrawerPeek()
        setupDrawerContent(navigationView)
    }

    private fun setupDrawerPeek() {
        drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

            }

            override fun onDrawerOpened(drawerView: View) {

            }

            override fun onDrawerClosed(drawerView: View) {

            }

            override fun onDrawerStateChanged(newState: Int) {

            }
        })
        drawer.post {
            drawer.openDrawer(GravityCompat.START)
            drawer.postDelayed({
                drawer.closeDrawer(GravityCompat.START)
            }, 8000) //
        }
    }

    private fun setupDrawerContent(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            selectDrawerItem(menuItem)
            true
        }
    }

    private fun selectDrawerItem(menuItem: android.view.MenuItem) {
        when (menuItem.itemId) {
            R.id.nav_item_one -> {
                supportFragmentManager.commit {
                    replace(R.id.fragment_container, MyJournalsFragment())
                }
            }
            R.id.nav_item_two -> {
                supportFragmentManager.commit {
                    replace(R.id.fragment_container, NewJournalFragment())
                }
            }
            R.id.nav_item_three -> {
                supportFragmentManager.commit {
                    replace(R.id.fragment_container, UserProfileFragment())
                }
            }
            R.id.nav_item_four -> {
                supportFragmentManager.commit {
                    replace(R.id.fragment_container, ReminderFragment())
                }
            }
        }

        drawer.closeDrawer(GravityCompat.START)
    }
}