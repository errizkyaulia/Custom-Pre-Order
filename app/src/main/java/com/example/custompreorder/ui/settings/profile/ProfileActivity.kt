package com.example.custompreorder.ui.settings.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
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
import com.example.custompreorder.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.*

@Suppress("DEPRECATION")
class ProfileActivity : AppCompatActivity() {

    private lateinit var userData: UserData
    private var saveJob: Job? = null // Job untuk menyimpan referensi pekerjaan
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
        // Agar text tidak dapat di edit user
        userEmailView.isEnabled = false

        // Load user data from Firestore
        loadUserData(currentUser.uid)

        binding.saveProfile.setOnClickListener {
            // Handle save profile changePassword click to update user data in Firestore
            saveUpdatedUserData()
        }

        binding.changePassword.setOnClickListener {
            // Handle changePassword click to send email reset password
            val email = binding.emailProfile.text.toString()
            sendResetPassword(email)
        }

        binding.reqDeleteAccount.setOnClickListener {
            // Handle save profile changePassword click to update user data in Firestore
            deleteAccount()
        }
    }

    private fun sendResetPassword(email: String) {
        binding.changePassword.isEnabled = false
        binding.progressBar3.visibility = View.VISIBLE

        // TODO: Implement your reset password logic here
        val emailAddress = email

        Firebase.auth.sendPasswordResetEmail(emailAddress)
            .addOnCompleteListener { task ->
                // Sembunyikan loading screen
                binding.progressBar3.visibility = View.GONE
                if (task.isSuccessful) {
                    // Tampilkan dalam log
                    Log.d("ResetPasswordActivity", "Password reset email sent to $emailAddress")
                    Toast.makeText(this, "Email sent to $emailAddress", Toast.LENGTH_SHORT).show()

                    // Tampilkan alert dialog
                    val alertDialogBuilder = AlertDialog.Builder(this)
                    alertDialogBuilder.setTitle("Success")
                    alertDialogBuilder.setMessage("Password reset email sent to\n$emailAddress")
                    alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    val alertDialog = alertDialogBuilder.create()
                    alertDialog.show()

                    binding.changePassword.isEnabled = true
                } else {
                    // Tampilkan dalam log
                    Log.e(
                        "ResetPasswordActivity",
                        "Failed to send password reset email",
                        task.exception
                    )
                    Toast.makeText(this, "Failed to send email.", Toast.LENGTH_SHORT).show()
                    binding.changePassword.isEnabled = true
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar3.visibility = View.GONE
                Log.e("ResetPasswordActivity", "Exception occurred", exception)
                Toast.makeText(this, "An error occurred. Please try again later.", Toast.LENGTH_SHORT).show()
                binding.changePassword.isEnabled = true
            }
    }

    private fun saveUpdatedUserData() {
        // Nonaktifkan tombol
        binding.saveProfile.isEnabled = false

        // Tampilkan loading screen
        binding.progressBar3.visibility = View.VISIBLE

        // Batalkan pekerjaan sebelumnya (jika ada)
        saveJob?.cancel()

        // Mulai pekerjaan baru
        saveJob = CoroutineScope(Dispatchers.Main).launch {
            // Lakukan pekerjaan di luar UI thread
            withContext(Dispatchers.IO) {
                // Panggil fungsi untuk menyimpan data pengguna
                saveUserDataAndUpdateUI()
            }

            // Sembunyikan loading screen dan aktifkan tombol kembali di UI thread
            binding.progressBar3.visibility = View.GONE
            binding.saveProfile.isEnabled = true
        }
    }

    private fun saveUserDataAndUpdateUI() {
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

        // Jika gambar profil diatur, konversi ke Bitmap terlebih dahulu
        binding.profilePicture.drawable?.let { drawable ->
            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                is VectorDrawable -> {
                    // Jika itu VectorDrawable, konversi ke Bitmap
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
                    // Setelah gambar profil diunggah, simpan URL-nya bersama data pengguna lainnya
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
                showToast("Your Profile has been updated")
            }
            .addOnFailureListener { e ->
                showToast("Failed to update profile: ${e.message}")
            }
    }

    private fun loadUserData(uid: String) {
        // Tampilkan loading screen
        binding.progressBar3.visibility = View.VISIBLE

        // Load user data from Firestore
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document(uid)

        // Mendapatkan dokumen
        docRef.get()
            .addOnSuccessListener { document ->
                // Sembunyikan loading screen
                binding.progressBar3.visibility = View.GONE
                // Cek apakah dokumen ada
                if (document.exists()) {
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
                        "Harap Di ISI".also { name.text = it }
                    } else {
                        name.text = document.getString("fullname")
                    }
                    if (document.getString("address") == null) {
                        "Harap Di ISI".also { address.text = it }
                    } else {
                        address.text = document.getString("address")
                    }
                    if (document.getString("phonenumber") == null) {
                        "Harap Di ISI".also { phoneNumber.text = it }
                    } else {
                        phoneNumber.text = document.getString("phonenumber")
                    }

                } else {
                    showToast("Please fill in your profile data")
                    binding.editName.setText(getString(R.string.no_data_found))
                    binding.editAddress.setText(getString(R.string.no_data_found))
                    binding.editTextPhone.setText(getString(R.string.no_data_found))
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

    private fun deleteAccount() {
        val user = Firebase.auth.currentUser

        user?.let {
            // Membuat dialog konfirmasi sebelum menghapus akun
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmation")
                .setMessage("Are you sure you want to delete your account?")
                .setPositiveButton("Yes") { _, _ ->
                    // User menekan tombol "Yes", hapus akun
                    deleteUserAccount()
                }
                .setNegativeButton("No") { dialog, _ ->
                    // User menekan tombol "No", tutup dialog
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun deleteUserAccount() {
        val user = Firebase.auth.currentUser

        user?.delete()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Hapus data pengguna dari Firestore
                    deleteUserDataFromFirestore(user.uid)
                    // Hapus gambar profil dari Firebase Storage
                    deleteProfilePictureFromStorage(user.uid)

                    Log.d(TAG, "Successfully deleted user account")
                    showToast("Your account has been deleted")
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.e(TAG, "Failed to delete user account: ${task.exception?.message}")
                    showToast("Failed to delete user account: ${task.exception?.message}")
                }
            }
    }

    private fun deleteUserDataFromFirestore(userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "User data deleted from Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete user data from Firestore: ${e.message}")
            }
    }

    private fun deleteProfilePictureFromStorage(userId: String) {
        val storageRef = FirebaseStorage.getInstance().reference
        val profilePicRef = storageRef.child("profile_pictures/$userId.jpg")

        profilePicRef.delete()
            .addOnSuccessListener {
                Log.d(TAG, "Profile picture deleted from Firebase Storage.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete profile picture from Firebase Storage: ${e.message}")
            }
    }

}