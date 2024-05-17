package com.example.custompreorder.ui.home

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.example.custompreorder.Menu
import com.example.custompreorder.R
import com.example.custompreorder.databinding.ActivityCheckoutOrderBinding
import com.example.custompreorder.ui.settings.profile.ProfileActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class CheckoutOrder : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutOrderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Hide action bar
        supportActionBar?.hide()
        // Set content view
        binding = ActivityCheckoutOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val product = intent.getStringExtra("productId")
        // Return if product is null
        if (product == null) {
            finish()
            return
        }
        loadUser()
        loadCheckoutOrder(product)

        binding.Purchase.setOnClickListener {
            purchase(product)
        }
    }

    private fun loadCheckoutOrder(product: String) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("products").document(product)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    binding.progressBar4.visibility = View.GONE

                    val productSize = intent.getStringExtra("size") ?: ""
                    val productQty = intent.getStringExtra("quantity") ?: ""
                    val totalPrice = intent.getStringExtra("totalPrice") ?: ""

                    val name = document.getString("name") ?: ""
                    val price = intent.getStringExtra("price") ?: ""
                    val productImage = document.getString("imageProductUrl") ?: ""

                    val chosenSize = "Chosen Size: $productSize"

                    binding.chosenSize.text = chosenSize
                    binding.productPrice.text = price
                    binding.qtyProduct.text = productQty
                    binding.totalCekoutPrice.text = totalPrice

                    val imageUrl = intent.getStringExtra("customDesignUrl") // Dapatkan URL gambar kustom dari intent Costumize Order
                    if (imageUrl != null) {
                        val costume = "$name With Custom Design"
                        binding.productName.text = costume
                        Glide.with(this)
                            .load(imageUrl)
                            .into(binding.productImage)
                    } else {
                        if (productImage.isNotEmpty()) {
                            binding.productName.text = name
                            Glide.with(this)
                                .load(productImage)
                                .into(binding.productImage)
                        } else {
                            binding.productName.text = name
                            binding.productImage.setImageResource(R.drawable.baseline_broken_image_24)
                        }
                    }
                } else {
                    Log.d(TAG, "Document does not exist or null")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting documents: ", exception)
            }
    }

    private fun loadUser() {
        // Load user data from Firestore
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            finish()
            return
        }
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document(uid)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    binding.progressBar4.visibility = View.GONE
                    val name = document.getString("fullname")
                    val phone = document.getString("phonenumber")
                    val address = document.getString("address")

                    binding.cekoutFullName.setText(name)
                    binding.cekoutPhoneNumber.setText(phone)
                    binding.cekoutAddress.setText(address)

                    // Jika tidak ada data, ingatkan untuk mengupdate Profile data
                    if (name == "" || phone == "" || address == "") {
                        // Make alret dialog confirmation, if yes redirect to ProfileActivity if no do nothing
                        val builder = android.app.AlertDialog.Builder(this)
                        builder.setTitle("Warning")
                        builder.setMessage("Please update your Profile data")
                        builder.setPositiveButton("Update") { _, _ ->
                            val intent = Intent(this, ProfileActivity::class.java)
                            startActivity(intent)
                        }
                        builder.setNegativeButton("Cancel") { _, _ ->
                            // Do nothing
                            Toast.makeText(this, "Please update your Profile data", Toast.LENGTH_SHORT).show()
                        }
                        builder.show()
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Log error
                Log.e(TAG, "Error getting documents: ", exception)
            }


    }

    private fun uploadCheckOutImage(bitmap: Bitmap, transactionId: String, callback: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRefCart = storageRef.child("image/transaction/$transactionId.jpg")

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = imageRefCart.putBytes(data)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            imageRefCart.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                val imageUrl = downloadUri.toString()
                callback(imageUrl)
            } else {
                Toast.makeText(this, "Failed to upload costume image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun purchase(product: String) {
        // Get current user ID
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        // Get Firestore instance
        val db = FirebaseFirestore.getInstance()

        // Load other necessary data from intent or UI elements
        val size = intent.getStringExtra("size")
        val quantity = intent.getStringExtra("quantity")?.toIntOrNull() ?: 0

        val initPriceText = binding.totalCekoutPrice.text.toString()
        val initPrice = initPriceText.replace("Rp. ", "").toDoubleOrNull() ?: return
        val productPrice = binding.productPrice.text.toString().replace("Rp. ", "").toDoubleOrNull() ?: return

        // Get user details from UI elements
        val name = binding.cekoutFullName.text.toString()
        val phone = binding.cekoutPhoneNumber.text.toString()
        val address = binding.cekoutAddress.text.toString()
        val notes = binding.editNotes.text.toString()

        // Check if any field is empty
        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Generate a new transaction ID
        val transactionId = db.collection("transactions").document().id

        // Create a new transaction object
        val transactionData = mapOf(
            "user_id" to uid,
            "date" to Timestamp.now(),
            "total_price" to initPrice,
            "product_id" to product,
            "product_name" to binding.productName.text,
            "product_price" to productPrice,
            "size" to size,
            "quantity" to quantity,
            "designUrl" to "", // Empty for now, will be updated later if there's a custom image
            "name" to name,
            "phone" to phone,
            "address" to address,
            "notes" to notes,
            "status" to "Ordered"
        )
        // LOG transactionData
        Log.d(TAG, "Transaction Data: $transactionData")

        // Add the transaction data to Firestore
        db.collection("transactions").document(transactionId)
            .set(transactionData)
            .addOnSuccessListener {
                // Transaction data added successfully
                // You can perform any additional actions here if needed
                Toast.makeText(this, "Transaction added successfully", Toast.LENGTH_SHORT).show()

                // Check if there's a custom image to upload
                if (intent.hasExtra("customDesignUrl")) {
                    val customDesignBitmap = binding.productImage.drawable?.toBitmap()
                    if (customDesignBitmap != null) {
                        uploadCheckOutImage(customDesignBitmap, transactionId) { imageUrl ->
                            // Update the transaction with the custom design URL
                            db.collection("transactions").document(transactionId)
                                .update("designUrl", imageUrl)
                                .addOnSuccessListener {
                                    // Remove the product from the cart
                                    val docCart = intent.getStringExtra("documentId")
                                    if (docCart != null) {
                                        removeCart(docCart)
                                    }
                                    // Remove temporary design image
                                    val tempDesignImage = intent.getStringExtra("customDesignUrl")
                                    if (tempDesignImage != null) {
                                        removeImage(tempDesignImage)
                                    }
                                    Log.d(TAG, "Custom design URL updated successfully")
                                    Toast.makeText(this, "Transaction with Costume Image added successfully", Toast.LENGTH_SHORT).show()
                                    val intentMenu = Intent(this, Menu::class.java)
                                    intentMenu.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    startActivity(intentMenu)
                                    // Finish the CheckoutOrder activity
                                    finish()
                                    // Finish CustomizeOrder activity
                                    val cuzId = intent.getIntExtra("customizeOrderId", 0)
                                    if (cuzId != 0) {
                                        Log.d(TAG, "Finish CustomizeOrder activity $cuzId")
                                        finishActivity(cuzId)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error updating your custom design", Toast.LENGTH_SHORT).show()
                                    Log.e(TAG, "Error updating custom design URL", e)
                                }
                        }
                    } else {
                        finish()
                    }
                } else {
                    // Remove the product from the cart
                    val docCart = intent.getStringExtra("documentId")
                    if (docCart != null) {
                        removeCart(docCart)
                    }
                    Toast.makeText(this, "Transaction added successfully", Toast.LENGTH_SHORT).show()
                    val intentMenu = Intent(this, Menu::class.java)
                    //intent.putExtra("transactionId", transactionId)
                    startActivity(intentMenu)
                    finish()
                }
            }
            .addOnFailureListener { e ->
                // Error occurred while adding transaction data
                // Handle the error appropriately
                Toast.makeText(this, "Error adding transaction", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error adding transaction", e)
            }
    }

    private fun removeCart(docCart: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("cart").document(docCart)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully deleted!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error deleting document", e)
                Toast.makeText(this, "Sorry couldn't remove product from your cart", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeImage(tempDesignImage: String) {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(tempDesignImage)
        storageRef.delete()
            .addOnSuccessListener {
                Log.d(TAG, "Image deleted successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting image", e)
            }
    }
}