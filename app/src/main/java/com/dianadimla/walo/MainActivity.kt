package com.dianadimla.walo.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.dianadimla.walo.R
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Test Firestore connection
        testFirestore()


        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)
    }

    private fun testFirestore() {
        val db = FirebaseFirestore.getInstance()
        val dummy = hashMapOf(
            "name" to "Diana",
            "budget" to 5000,
            "currency" to "EUR"
        )

        // Save dummy data
        db.collection("users")
            .add(dummy)
            .addOnSuccessListener { doc ->
                Log.d("FirestoreTest", "Document added with ID: ${doc.id}")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreTest", "Error adding document", e)
            }

        // Read dummy data
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d("FirestoreTest", "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreTest", "Error getting documents", e)
            }
    }
}
