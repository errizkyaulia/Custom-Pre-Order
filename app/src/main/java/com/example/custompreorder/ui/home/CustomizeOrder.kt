package com.example.custompreorder.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.custompreorder.R
import com.example.custompreorder.databinding.ActivityCostomizeOrderBinding
import com.example.custompreorder.ui.home.ui.cart.Cart
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class CustomizeOrder : AppCompatActivity() {
    private lateinit var binding: ActivityCostomizeOrderBinding
    private var defaultColor: Int = 0
    private var blackColor: Int = 0
    private var redColor: Int = 0
    private var selectedSize: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        binding = ActivityCostomizeOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        defaultColor = ContextCompat.getColor(this, android.R.color.white)
        redColor = ContextCompat.getColor(this, android.R.color.holo_red_light)
        blackColor = ContextCompat.getColor(this, android.R.color.black)

        val productId = intent.getStringExtra("productId")
        val sizeFromCart = intent.getStringExtra("size")
        if (sizeFromCart != null) {
            // Set ulang text dari addToCart button menjadi "save cart"
            binding.addToCart.text = "Save Cart"
            selectedSize = sizeFromCart
        }

        val qty = intent.getStringExtra("qty")
        if (qty != null) {
            binding.editQty.setText(qty)
        }
        val imgUrl = intent.getStringExtra("designUrl")
        if (imgUrl != "") {
            binding.cekCostumDesign.isChecked = true
            Glide.with(this)
                .load(imgUrl)
                .into(binding.costumImage)
        }

        loadProduct(productId)
        setupSizeButtons(productId)
        // Select the button that matches sizeFromCart
        if (sizeFromCart != null) {
            when (sizeFromCart) {
                "S" -> binding.SizeS.performClick()
                "M" -> binding.SizeM.performClick()
                "L" -> binding.SizeL.performClick()
                "XL" -> binding.SizeXL.performClick()
            }
        }
        setupQuantityChangeListener()

        binding.cekCostumDesign.setOnCheckedChangeListener { _, isChecked ->
            binding.costumImage.isEnabled = isChecked
            if (!isChecked) {
                binding.costumImage.setImageResource(R.drawable.baseline_add_photo_alternate_24)
            } else {
                if (imgUrl != "") {
                    Glide.with(this)
                        .load(imgUrl)
                        .into(binding.costumImage)
                }
                binding.costumImage.setOnClickListener {
                    chooseImage()
                }
            }
        }

        binding.addToCart.setOnClickListener {
            // Loading Screen and disable buttons
            binding.progressBar4.visibility = View.VISIBLE
            binding.addToCart.isEnabled = false
            binding.CheckOut.isEnabled = false

            lifecycleScope.launch {
                try {
                    addToCart(productId)
                } finally {
                    // Hide progress bar and enable buttons
                    binding.progressBar4.visibility = View.GONE
                    binding.addToCart.isEnabled = true
                    binding.CheckOut.isEnabled = true
                }
            }
        }

        binding.CheckOut.setOnClickListener {
            val customizeOrderId = System.identityHashCode(this)

            if (selectedSize == null) {
                Toast.makeText(this, "Please select a size", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.cekCostumDesign.isChecked) {
                if (binding.costumImage.drawable == null) {
                    Toast.makeText(this, "Please select a costume image", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            val quantityString = binding.editQty.text.toString()
            if (quantityString.isEmpty()) {
                Toast.makeText(this, "Please enter a quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val quantity = quantityString.toInt()
            if (quantity <= 0) {
                Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, CheckoutOrder::class.java)
            intent.putExtra("customizeOrderId", customizeOrderId)
            intent.putExtra("productId", productId)
            intent.putExtra("name", binding.productName.text.toString())
            intent.putExtra("size", selectedSize)
            intent.putExtra("price", binding.initPrice.text.toString())
            intent.putExtra("totalPrice", binding.totalPrice.text.toString())
            intent.putExtra("quantity", binding.editQty.text.toString())
            intent.putExtra("designURL", binding.costumImage.drawable.toString())

            if (binding.cekCostumDesign.isChecked) {
                val bitmap = (binding.costumImage.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    uploadCheckOutImage(bitmap) { imageUrl ->
                        intent.putExtra("customDesignUrl", imageUrl)
                        startActivity(intent)
                    }
                }
            } else {
                startActivity(intent) // Start activity directly if cekCostumDesign is false
            }
        }
    }

    private fun uploadCheckOutImage(bitmap: Bitmap, callback: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRefCart = storageRef.child("image/design/${FirebaseAuth.getInstance().currentUser?.uid}/checkout.jpg")

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

    private fun setupSizeButtons(productId: String?) {
        val sizes = listOf("S", "M", "L", "XL")
        sizes.forEach { size ->
            val button = when (size) {
                "S" -> binding.SizeS
                "M" -> binding.SizeM
                "L" -> binding.SizeL
                "XL" -> binding.SizeXL
                else -> null
            }
            button?.setOnClickListener {
                selectedSize = size
                resetButtons()
                button.isSelected = true
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.blue))
                button.setTextColor(defaultColor)
                loadPrice(productId, size)
                calculateTotalPrice()
            }
        }
    }

    private fun resetButtons() {
        if (binding.SizeS.isEnabled) {
            binding.SizeS.resetColor(defaultColor, blackColor)
        }
        if (binding.SizeM.isEnabled) {
            binding.SizeM.resetColor(defaultColor, blackColor)
        }
        if (binding.SizeL.isEnabled) {
            binding.SizeL.resetColor(defaultColor, blackColor)
        }
        if (binding.SizeXL.isEnabled) {
            binding.SizeXL.resetColor(defaultColor, blackColor)
        }
    }

    private fun View.resetColor(backgroundColor: Int, textColor: Int) {
        isEnabled = true
        isSelected = false
        setBackgroundColor(backgroundColor)
        if (this is TextView) {
            this.setTextColor(textColor)
        }
    }

    private fun setupQuantityChangeListener() {
        binding.editQty.addTextChangedListener { text ->
            if (text.toString().isNotEmpty()) {
                calculateTotalPrice()
            }
        }
    }

    private fun calculateTotalPrice() {
        val initPriceText = binding.initPrice.text.toString()
        val initPrice = initPriceText.replace("Rp. ", "").toDoubleOrNull() ?: return
        val quantity = binding.editQty.text.toString().toIntOrNull() ?: return
        val totalPrice = initPrice * quantity
        binding.totalPrice.text = "Rp. $totalPrice"
    }

    private fun loadPrice(productId: String?, size: String) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("products").document(productId!!)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val sizes = document.get("sizes") as? Map<String, Map<String, Any>>
                    sizes?.let { sizeMap ->
                        val selectedSize = sizeMap[size]
                        if (selectedSize != null) {
                            val availability = selectedSize["availability"] as? Boolean
                            if (availability == true) {
                                val price = selectedSize["price"] as? Double
                                binding.initPrice.text = "Rp. ${price ?: "Product not Available"}"
                            } else {
                                "Product not Available".also { binding.initPrice.text = it }
                            }
                        } else {
                            Log.d("PriceInfo", "Size $size not found")
                        }
                    }
                } else {
                    Log.d("Firestore", "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("Firestore", "get failed with ", exception)
            }
    }

    private fun loadProduct(productId: String?) {
        binding.progressBar4.visibility = View.VISIBLE
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("products").document(productId!!)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val productName = document.getString("name")
                    val productImageURL = document.getString("imageProductUrl")
                    val sizes = document.get("sizes") as? Map<String, Map<String, Any>>
                    sizes?.let { sizeMap ->
                        sizeMap.forEach { (size, sizeData) ->
                            val availability = sizeData["availability"] as? Boolean
                            if (availability == false) {
                                val button = when (size) {
                                    "S" -> binding.SizeS
                                    "M" -> binding.SizeM
                                    "L" -> binding.SizeL
                                    "XL" -> binding.SizeXL
                                    else -> null
                                }
                                button?.isEnabled = false
                                button?.setBackgroundColor(redColor)
                                button?.setTextColor(defaultColor)
                            }
                        }
                    }
                    val loadProductImage: ImageView = binding.productImage
                    if (!productImageURL.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(productImageURL)
                            .into(loadProductImage)
                    } else {
                        loadProductImage.setImageResource(R.drawable.baseline_broken_image_24)
                    }
                    binding.productName.text = productName
                    binding.progressBar4.visibility = View.GONE
                }
            }
    }

    @Suppress("DEPRECATION")
    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    @Deprecated("This method has been deprecated.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            binding.costumImage.setImageURI(selectedImageUri)
        }
    }

    private suspend fun addToCart(productId: String?) {
        if (selectedSize == null) {
            Toast.makeText(this, "Please select a size", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val quantity = binding.editQty.text.toString().toIntOrNull() ?: return
        if (quantity <= 0) {
            Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
            return
        }

        var designURL = ""
        val costumeDesign = binding.cekCostumDesign.isChecked
        if (costumeDesign){
            val costumeImage = binding.costumImage.drawable as? BitmapDrawable
            if (costumeImage != null) {
                designURL = uploadCostumeImage(productId, costumeImage.bitmap)
            } else {
                Toast.makeText(this, "Please select a costume image", Toast.LENGTH_SHORT).show()
                return
            }
        }
        addToCartWithDesign(productId, userId, quantity, designURL, selectedSize!!)
    }

    private suspend fun uploadCostumeImage(productId: String?, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRefCart = storageRef.child("image/design/${FirebaseAuth.getInstance().currentUser?.uid}/cart/$productId/$selectedSize.jpg")

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = imageRefCart.putBytes(data).await()
        val downloadUri = imageRefCart.downloadUrl.await()
        return@withContext downloadUri.toString()
    }

    private suspend fun addToCartWithDesign(productId: String?, userId: String?, quantity: Int, designURL: String, selectedSize: String) = withContext(Dispatchers.IO) {
        // Siapkan data yang akan dimasukkan
        val cartItem = hashMapOf(
            "date" to Timestamp.now(),
            "user_id" to userId,
            "product_id" to productId,
            "size" to selectedSize,
            "quantity" to quantity,
            "designURL" to designURL
            // Tambahkan informasi lain yang diperlukan
        )

        // Inisialisasi Firestore
        val db = FirebaseFirestore.getInstance()
        val cartRef = db.collection("cart")

        // Cari dokumen di Firestore dengan kondisi yang diberikan
        try {
            val documents = cartRef.whereEqualTo("user_id", userId)
                .whereEqualTo("product_id", productId)
                .whereEqualTo("size", selectedSize)
                .get()
                .await()

            if (documents.isEmpty) {
                // Jika tidak ada dokumen yang cocok, buat dokumen baru
                cartRef.add(cartItem).  await()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CustomizeOrder, "New Item added to cart", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@CustomizeOrder, Cart::class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                // Jika dokumen yang cocok ditemukan, perbarui jumlahnya
                val doc = documents.documents[0] // Ambil dokumen pertama yang cocok
                val currentQuantity = doc.getLong("quantity") ?: 0

                // Tampilkan dialog konfirmasi untuk memperbarui item
                withContext(Dispatchers.Main) {
                    val builder = AlertDialog.Builder(this@CustomizeOrder)
                    builder.setTitle("Update Cart Item")
                        .setMessage("Do you want to update the quantity of this item? From: $currentQuantity to $quantity")
                        .setPositiveButton("Yes") { _, _ ->
                            // Pengguna menekan tombol Yes, lakukan pembaruan
                            lifecycleScope.launch {
                                try {
                                    doc.reference.update(cartItem as Map<String, Any>).await()
                                    Toast.makeText(this@CustomizeOrder, "Your cart has been updated", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@CustomizeOrder, Cart::class.java)
                                    startActivity(intent)
                                    finish()
                                } catch (e: Exception) {
                                    Toast.makeText(this@CustomizeOrder, "Failed to update item quantity", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .setNegativeButton("No") { _, _ ->
                            // Pengguna menekan tombol No, tidak lakukan apa-apa
                            // Tidak perlu tindakan
                        }
                        .show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                // Gagal melakukan pencarian dokumen
                Log.w(TAG, "Error getting documents: ", e)
                Toast.makeText(this@CustomizeOrder, "Failed to retrieve cart items", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val REQUEST_IMAGE_PICK = 100
    }
}