package com.example.custompreorder.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
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
import com.bumptech.glide.Glide
import com.example.custompreorder.R
import com.example.custompreorder.databinding.ActivityCostomizeOrderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

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
        loadProduct(productId)
        setupSizeButtons(productId)
        setupQuantityChangeListener()

        binding.cekCostumDesign.setOnCheckedChangeListener { _, isChecked ->
            binding.costumImage.isEnabled = isChecked
            if (!isChecked) {
                binding.costumImage.setImageResource(R.drawable.baseline_add_photo_alternate_24)
            } else {
                binding.costumImage.setOnClickListener {
                    chooseImage()
                }
            }
        }

        binding.addToCart.setOnClickListener {
            addToCart(productId)
        }

        binding.CheckOut.setOnClickListener {
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

    private fun addToCart(productId: String?) {

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
                uploadCostumeImage(productId, costumeImage.bitmap) { imageUrl ->
                    designURL = imageUrl
                    addToCartWithDesign(productId, userId, quantity, designURL, selectedSize!!)
                }
            } else {
                Toast.makeText(this, "Please select a costume image", Toast.LENGTH_SHORT).show()
            }
        } else {
            addToCartWithDesign(productId, userId, quantity, designURL, selectedSize!!)
        }
    }

    private fun uploadCostumeImage(productId: String?, bitmap: Bitmap, callback: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRefCart = storageRef.child("image/design/${FirebaseAuth.getInstance().currentUser?.uid}/cart/$productId/$selectedSize.jpg")

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

    private fun addToCartWithDesign(productId: String?, userId: String?, quantity: Int, designURL: String, selectedSize: String) {
        val cartItem = hashMapOf(
            "size" to selectedSize,
            "quantity" to quantity,
            "designURL" to designURL
            // Tambahkan informasi lain yang diperlukan
        )

        val db = FirebaseFirestore.getInstance()
        val cartRef = db.collection("cart").document(userId ?: "")
            .collection("items").document(productId ?: "")
            .collection("sizes").document(selectedSize) // Menambahkan subkoleksi "sizes" dengan dokumen berdasarkan ukuran yang dipilih

        cartRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                // Produk dengan ukuran yang sama telah ditambahkan sebelumnya
                val existingQuantity = documentSnapshot.getLong("quantity") ?: 0
                AlertDialog.Builder(this)
                    .setTitle("Product already exists")
                    .setMessage("You have already added this product with the same size to the cart. Do you want to update the quantity? From $existingQuantity to $quantity")
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        // Perbarui jumlah produk yang sudah ada
                        cartRef.update("quantity", quantity)
                            .addOnSuccessListener {
                                // Keranjang berhasil diperbarui
                                Log.d("Firestore", "Cart updated successfully!")
                                Toast.makeText(this, "Item added to cart", Toast.LENGTH_SHORT).show()
                                // Pindahkan ke menu cart
                                finish()
                            }
                            .addOnFailureListener { e ->
                                // Gagal memperbarui keranjang
                                Log.w("Firestore", "Error updating cart", e)
                                Toast.makeText(this, "Failed to add item to cart", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton(android.R.string.no) { _, _ ->
                        // Pengguna memilih untuk tidak memperbarui jumlah, tidak ada tindakan yang diambil
                    }
                    .show()
            } else {
                // Produk belum ada di keranjang dengan ukuran yang sama, tambahkan baru
                cartRef.set(cartItem)
                    .addOnSuccessListener {
                        // Produk berhasil ditambahkan ke keranjang
                        Log.d("Firestore", "Item added to cart successfully!")
                        Toast.makeText(this, "Item added to cart", Toast.LENGTH_SHORT).show()
                        // Pindahkan ke menu cart
                        finish()
                    }
                    .addOnFailureListener { e ->
                        // Gagal menambahkan produk ke keranjang
                        Log.w("Firestore", "Error adding item to cart", e)
                        Toast.makeText(this, "Failed to add item to cart", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener { e ->
            // Gagal mendapatkan data dari Firestore
            Log.w("Firestore", "Error getting cart item", e)
            Toast.makeText(this, "Failed to add item to cart", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val REQUEST_IMAGE_PICK = 100
    }
}