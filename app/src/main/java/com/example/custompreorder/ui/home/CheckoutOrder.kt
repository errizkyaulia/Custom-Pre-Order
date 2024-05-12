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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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

                    val imageUrl = intent.getStringExtra("customDesignUrl") // Dapatkan URL gambar kustom dari intent
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

    // Function to get nested price based on size
    private fun DocumentSnapshot.getNestedPrice(size: String?): String? {
        return if (size != null) {
            val nestedData = this.get(size) as? Map<*, *>
            nestedData?.get("price") as? String
        } else {
            null
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
        if (uid == null) {
            // Handle the case where the user is not authenticated
            return
        }

        // Get Firestore instance
        val db = FirebaseFirestore.getInstance()

        // Load other necessary data from intent or UI elements
        val size = intent.getStringExtra("size")
        val quantity = intent.getStringExtra("quantity")?.toIntOrNull() ?: 0

        val initPriceText = binding.totalCekoutPrice.text.toString()
        val initPrice = initPriceText.replace("Rp. ", "").toDoubleOrNull() ?: return

        // Create a new transaction item
        val transactionItem = TransactionItem(product, size.toString(), quantity)

        // Create a list of transaction items (assuming only one item for now)
        val productList = listOf(transactionItem)

        // Generate a new transaction ID
        val transactionId = db.collection("transactions").document().id

        // Create a new transaction object
        val transactionData = hashMapOf(
            "user_id" to uid,
            "date" to Timestamp.now(),
            "total_price" to initPrice,
            "items" to productList.map { item ->
                hashMapOf(
                    "product_id" to item.productId,
                    "size" to item.size,
                    "quantity" to item.quantity,
                    "designUrl" to "" // Empty for now, will be updated later if there's a custom image
                )
            },
            "status" to "Ordered"
        )

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
                                .update("items.0.designUrl", imageUrl)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Custom design URL updated successfully")
                                    val intent = Intent(this, Menu::class.java)
                                    //intent.putExtra("transactionId", transactionId)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error updating custom design URL", e)
                                }
                        }
                    } else {
                        finish()
                    }
                } else {
                    val intent = Intent(this, Menu::class.java)
                    //intent.putExtra("transactionId", transactionId)
                    startActivity(intent)
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

    private data class TransactionItem(
        val productId: String,
        val size: String,
        val quantity: Int
    )
}