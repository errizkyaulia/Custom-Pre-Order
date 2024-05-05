package com.example.custompreorder

import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.custompreorder.data.UserData
import com.example.custompreorder.databinding.ActivityMenuBinding
import com.google.firebase.auth.FirebaseUser

class Menu : AppCompatActivity() {

    private lateinit var userData: UserData
    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cek Apakah Login valid
        userData = UserData()

        // Memuat data pengguna saat ini
        val currentUser: FirebaseUser? = userData.getCurrentUser()

        if (currentUser == null) {
            // User is not authenticated, redirect to login screen
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
            return
        }

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_menu)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_settings //, R.id.profileEditor
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


    }
}