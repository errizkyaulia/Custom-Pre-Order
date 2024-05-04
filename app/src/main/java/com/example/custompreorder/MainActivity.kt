package com.example.custompreorder

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialize Firebase Auth
        auth = Firebase.auth
        setContentView(R.layout.activity_main)

        // Disable action bar
        supportActionBar?.hide()

        // Tambahkan penanganan window insets
        setupWindowInsets()

        // Mulai proses pengecekan koneksi internet secara asinkron
        Thread {
            // Memeriksa koneksi internet
            val isConnected = isInternetConnected()

            // Setelah pengecekan selesai, beralih ke tata letak login jika terhubung
            runOnUiThread {
                if (isConnected) {
                    showWelcomeTextWithAnimation()
                    // Check if user is signed in (non-null) and update UI accordingly.
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        val intent = Intent(this, Menu::class.java)
                        startActivity(intent)
                        finish()
                    }
                    switchToLoginLayout()
                } else {
                    // Tampilkan pesan kesalahan koneksi atau tindakan lain yang sesuai
                }
            }
        }.start()
    }

    private fun setupWindowInsets() {
        // Tambahkan penanganan window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun isInternetConnected(): Boolean {
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun showWelcomeTextWithAnimation() {
        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        welcomeText.visibility = View.VISIBLE // Menggunakan View.VISIBLE
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_slow) // Memuat animasi dari folder anim
        welcomeText.startAnimation(fadeInAnimation) // Mulai animasi pada welcomeText
    }

    private fun switchToLoginLayout() {
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish() // Opsional, tergantung pada kebutuhan Anda
    }
}