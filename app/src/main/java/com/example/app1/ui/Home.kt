package com.example.app1.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
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
import javax.inject.Inject

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


                // Puedes actualizar otras vistas aquí también
            } else {
                // Maneja el caso donde el usuario no está logueado
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
                // No implementation needed
            }

            override fun onDrawerOpened(drawerView: View) {
                // No implementation needed
            }

            override fun onDrawerClosed(drawerView: View) {
                // No implementation needed
            }

            override fun onDrawerStateChanged(newState: Int) {
                // No implementation needed
            }
        })
        drawer.post {
            drawer.openDrawer(GravityCompat.START)
            drawer.postDelayed({
                drawer.closeDrawer(GravityCompat.START)
            }, 8000) // Ajusta el retraso según sea necesario
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
        }

        drawer.closeDrawer(GravityCompat.START)
    }
}