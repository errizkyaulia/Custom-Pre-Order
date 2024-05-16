package com.example.custompreorder

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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

                    val idToken = getIdToken()
                    if (idToken != null) {
                        // Re-authenticate
                        reAuthenticateUserWithToken(idToken)
                    } else {
                        switchToLoginLayout()
                    }
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

    private fun reAuthenticateUserWithToken(idToken: String) {
        auth.signInWithCustomToken(idToken)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Setelah login ulang, periksa status akun
                    checkUserStatus(auth.currentUser)
                    // LOG Token
                    val token = auth.currentUser?.getIdToken(true)?.result?.token
                    Toast.makeText(this, "Token: $token", Toast.LENGTH_SHORT).show()
                    Log.d("UserStatus", "Token: $token")
                } else {
                    // Jika re-authentication gagal, arahkan ke layar login
                    switchToLoginLayout()
                }
            }
    }

    private fun checkUserStatus(user: FirebaseUser?) {
        user?.getIdToken(true)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val claims = task.result?.claims
                val isDisabled = claims?.get("disabled") as Boolean? ?: false

                // Toast disabled or no
                Toast.makeText(this, "Disabled: $isDisabled", Toast.LENGTH_SHORT).show()
                // LOG ke dalam logcat
                Log.d("UserStatus", "Disabled: $isDisabled")

                if (isDisabled) {
                    // Jika akun dinonaktifkan, logout dan arahkan ke layar login
                    auth.signOut()
                    switchToLoginLayout()
                } else {
                    // Jika akun tidak dinonaktifkan, lanjutkan ke aktivitas utama
                    val intent = Intent(this, Menu::class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                // Jika gagal memeriksa status akun, arahkan ke layar login
                switchToLoginLayout()
            }
        }
    }

    private fun getIdToken(): String? {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("id_token", null)
    }
}