/**
 * Main entry point for the application.
 * Manages the primary navigation structure, runtime permissions, and the global
 * background monitoring service for AI-driven financial nudges.
 */
package com.dianadimla.walo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dianadimla.walo.data.NudgeManager
import com.dianadimla.walo.utils.NotificationHelper
import com.dianadimla.walo.viewmodels.GoalsViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    // Singleton instance for evaluating and triggering behavioural nudges
    private val nudgeManager = NudgeManager.getInstance()
    private lateinit var notificationHelper: NotificationHelper
    private val goalsViewModel: GoalsViewModel by viewModels()

    // Infrastructure for asynchronous background polling
    private val nudgeHandler = Handler(Looper.getMainLooper())
    private lateinit var nudgeRunnable: Runnable
    private val checkIntervalMs = 10000L // Polling frequency to balance responsiveness and battery efficiency

    // Launcher for handling the POST_NOTIFICATIONS runtime permission on Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Notifications permitted by user
        } else {
            // Notifications restricted; nudges will be suppressed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialises support helpers
        notificationHelper = NotificationHelper(this)

        // Request notification access required for system-level alerts
        askNotificationPermission()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        // Controls bottom navigation visibility based on the active destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.dashboardFragment,
                R.id.addTransactionFragment,
                R.id.goalsFragment,
                R.id.reportsFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                }
                else -> {
                    bottomNavigationView.visibility = View.GONE
                }
            }
        }

        // Start global periodic monitoring for user engagement
        setupPeriodicNudgeCheck()
    }

    /**
     * Initialises the background task to monitor goal inactivity.
     * Evaluates goal progress and user interaction to trigger motivational feedback.
     */
    private fun setupPeriodicNudgeCheck() {
        nudgeRunnable = Runnable {
            val currentGoals = goalsViewModel.goals.value
            currentGoals?.forEach { goal ->
                // Evaluates inactivity status for each active financial goal
                nudgeManager.checkGoalInactivityNudges(goal) { message ->
                    // Triggers a high-priority system-level alert
                    notificationHelper.showNotification(message)
                    
                    // Displays immediate visual feedback within the app context
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                }
            }
            // Schedules the subsequent evaluation cycle
            nudgeHandler.postDelayed(nudgeRunnable, checkIntervalMs)
        }
        nudgeHandler.post(nudgeRunnable)
    }

    /**
     * Checks for and requests notification permissions on compatible Android versions.
     */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Terminates the background task to prevent memory leaks
        nudgeHandler.removeCallbacks(nudgeRunnable)
    }
}
