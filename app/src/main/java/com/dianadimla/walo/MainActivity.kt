package com.dianadimla.walo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the NavHostFragment and get its NavController. This is the main container
        // for all the fragments in the app.
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        // This listener handles the visibility of the bottom navigation bar
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // List of main destinations where the bottom navigation should be visible.
                R.id.dashboardFragment,
                R.id.addTransactionFragment,
                R.id.reportsFragment,
                R.id.profileFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                }
                // Hide the bottom navigation on all other screens (e.g., login, signup).
                else -> {
                    bottomNavigationView.visibility = View.GONE
                }
            }
        }
    }
}
