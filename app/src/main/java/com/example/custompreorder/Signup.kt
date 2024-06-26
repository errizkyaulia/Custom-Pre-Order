package com.example.custompreorder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.custompreorder.databinding.ActivitySignupBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class Signup : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Disabled action bar
        supportActionBar?.hide()

        // Initialize Firebase Auth
        auth = Firebase.auth

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.signup.setOnClickListener {
            val email = binding.emailSignup.text.toString()
            val password = binding.passwordSignup.text.toString()
            val confirmPassword = binding.confirmpassword.text.toString()

            // Validate input
            if (email.isEmpty()) {
                Toast.makeText(this, "Wrong Email Format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Password do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create user
            createUser(email, password)
        }

        binding.backLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }

    private fun createUser(email: String, password: String){
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser

                    // Store new user in firestore database
                    val db = Firebase.firestore
                    val newUser = hashMapOf(
                        "picture" to "",
                        "fullname" to "",
                        "address" to "",
                        "phonenumber" to ""
                    )

                    // Add new user to firestore database
                    db.collection("users")
                        .document(user?.uid!!)
                        .set(newUser)
                        .addOnSuccessListener {
                            // Send verification email
                            user.sendEmailVerification()
                                .addOnSuccessListener {
                                    // Show success message
                                    Toast.makeText(this, "Verification email sent to: ${user.email}", Toast.LENGTH_SHORT).show()
                                    Log.d(TAG, "Verification email sent to: ${user.email}")
                                }
                            Log.d(TAG, "User added to firestore database") }
                        .addOnFailureListener { e ->
                            // Show error message
                            Toast.makeText(this, "Failed to Store User Data", Toast.LENGTH_SHORT).show()
                            Log.w(TAG, "Error adding user to firestore database", e) }


                    // Navigate to login page
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    companion object {
        private const val TAG = "SignupActivity"
    }
}