package com.example.custompreorder

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.custompreorder.databinding.ActivityForgotPasswordBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class ForgotPassword : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.ResetPassword.setOnClickListener {
            val email = binding.email.text.toString()

            sendResetPassword(email)
        }
        binding.BackToLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }

    private fun sendResetPassword(email: String) {
        binding.ResetPassword.isEnabled = false
        // TODO: Implement your reset password logic here
        val emailAddress = email

        Firebase.auth.sendPasswordResetEmail(emailAddress)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email sent.")
                    Toast.makeText(this, "Email sent to $emailAddress", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                } else {
                    Log.w(TAG, "Failed to send email.", task.exception)
                    Toast.makeText(this, "Failed to send email.", Toast.LENGTH_SHORT).show()
                    binding.ResetPassword.isEnabled = true
                }
            }
    }

    companion object {
        private const val TAG = "ResetPasswordActivity"
    }
}