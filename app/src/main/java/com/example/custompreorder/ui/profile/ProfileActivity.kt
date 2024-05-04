package com.example.custompreorder.ui.profile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.custompreorder.Login
import com.example.custompreorder.data.UserData
import com.example.custompreorder.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import com.bumptech.glide.Glide

@Suppress("DEPRECATION")
class ProfileActivity : AppCompatActivity() {

    private lateinit var userData: UserData

    private lateinit var binding: ActivityProfileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Disabled action bar
        supportActionBar?.hide()

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

        // UserData is authenticated, proceed with inflating layout and setting up views
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up click listener for the profile pictureww
        binding.profilePicture.setOnClickListener(
            View.OnClickListener {
                // Handle profile picture click and choose an image from the gallery
                chooseProfilePicture()
            }
        )

        val userEmailView: TextView = binding.emailProfile
        userEmailView.text = currentUser.email

        // Load user data from Firestore
        loadUserData(currentUser.uid)

        binding.saveProfile.setOnClickListener {
            // Handle save profile button click to update user data in Firestore
            saveUpdatedUserData()
        }
    }

    private fun saveUpdatedUserData() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid ?: return

        val name = binding.editName.text.toString()
        val address = binding.editAddress.text.toString()
        val phoneNumber = binding.editTextPhone.text.toString()

        val userData = hashMapOf(
            "fullname" to name,
            "address" to address,
            "phonenumber" to phoneNumber,
            "profilePictureUrl" to ""
        )

        // If a profile picture is set, convert it to Bitmap first
        binding.profilePicture.drawable?.let { drawable ->
            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                is VectorDrawable -> {
                    // If it's a VectorDrawable, convert it to Bitmap
                    val bitmap = Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                }
                else -> null
            }

            bitmap?.let {
                uploadProfilePicture(it) { profilePictureUrl ->
                    // Once the profile picture is uploaded, save its URL along with other user data
                    userData["profilePictureUrl"] = profilePictureUrl
                    saveUserData(db, uid, userData)
                }
            } ?: saveUserData(db, uid, userData)
        } ?: saveUserData(db, uid, userData)
    }

    private fun uploadProfilePicture(bitmap: Bitmap, onComplete: (String) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid ?: return

        val storageRef = FirebaseStorage.getInstance().reference
        val profilePicRef = storageRef.child("profile_pictures/$uid.jpg")

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()

        profilePicRef.putBytes(imageData)
            .addOnSuccessListener { taskSnapshot ->
                // Upload berhasil
                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                    // Dapatkan URL unduhan gambar yang diunggah
                    onComplete(uri.toString())
                } ?: run {
                    // Gagal mendapatkan URL unduhan
                    showToast("Failed to get download URL.")
                }
            }
            .addOnFailureListener { e ->
                // Upload gagal
                showToast("Failed to upload profile picture: ${e.message}")
                Log.e("ProfileActivity", "Failed to upload profile picture", e)
            }
    }

    private fun saveUserData(db: FirebaseFirestore, uid: String, userData: HashMap<String, String>) {
        db.collection("users").document(uid)
            .set(userData)
            .addOnSuccessListener {
                showToast("ProfileFragment updated successfully")
            }
            .addOnFailureListener { e ->
                showToast("Failed to update profile: ${e.message}")
            }
    }

    private fun loadUserData(uid: String) {
        // Implement your logic to load user data from Firestore
        // Load user data from Firestore
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document(uid)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Jika dokumen ada, set data pengguna ke tampilan
                    val userData = document.toObject(UserData::class.java)
                    // Set profile picture
                    val profilePic: ImageView = binding.profilePicture
                    // Mendapatkan URL picture
                    val profilePictureUrl = document.getString("profilePictureUrl")

                    // Jika URL picture tidak kosong, tampilkan gambar
                    if (!profilePictureUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profilePictureUrl)
                            .into(profilePic)
                    } else {
                        // Jika URL picture kosong, tampilkan gambar default
                    }

                    // Set nama lengkap, address, phonenumber jika null set ke "Harap Di ISI"
                    val name : TextView = binding.editName
                    val address : TextView = binding.editAddress
                    val phoneNumber : TextView = binding.editTextPhone

                    // lakukan pengecekan apabila dokumen kosong
                    if (document.getString("fullname") == null) {
                        name.text = "Harap Di ISI"
                    } else {
                        name.text = document.getString("fullname")
                    }
                    if (document.getString("address") == null) {
                        address.text = "Harap Di ISI"
                    } else {
                        address.text = document.getString("address")
                    }
                    if (document.getString("phonenumber") == null) {
                        phoneNumber.text = "Harap Di ISI"
                    } else {
                        phoneNumber.text = document.getString("phonenumber")
                    }

                } else {
                    showToast("Please fill in your profile data")
                    binding.editName.setText("Harap Di ISI")
                    binding.editAddress.setText("Harap Di ISI")
                    binding.editTextPhone.setText("Harap Di ISI")
                }
            }
            .addOnFailureListener { exception ->
                // Gagal mendapatkan dokumen
                showToast("Failed to load user data: ${exception.message}")
            }
    }

    private fun chooseProfilePicture() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 100
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            // Handle the selected image here
            val selectedImageUri = data.data
            binding.profilePicture.setImageURI(selectedImageUri)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}